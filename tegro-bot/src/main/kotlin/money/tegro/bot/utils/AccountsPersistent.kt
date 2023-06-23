package money.tegro.bot.utils

import kotlinx.datetime.Clock
import money.tegro.bot.exceptions.*
import money.tegro.bot.objects.Account
import money.tegro.bot.objects.LogType
import money.tegro.bot.objects.PostgresUserPersistent
import money.tegro.bot.objects.User
import money.tegro.bot.utils.PostgresAccountsPersistent.UsersAccounts.activations
import money.tegro.bot.utils.PostgresAccountsPersistent.UsersAccounts.amount
import money.tegro.bot.utils.PostgresAccountsPersistent.UsersAccounts.currency
import money.tegro.bot.utils.PostgresAccountsPersistent.UsersAccounts.isActive
import money.tegro.bot.utils.PostgresAccountsPersistent.UsersAccounts.issueTime
import money.tegro.bot.utils.PostgresAccountsPersistent.UsersAccounts.issuerId
import money.tegro.bot.utils.PostgresAccountsPersistent.UsersAccounts.maxCoins
import money.tegro.bot.utils.PostgresAccountsPersistent.UsersAccounts.minAmount
import money.tegro.bot.utils.PostgresAccountsPersistent.UsersAccounts.oneTime
import money.tegro.bot.wallet.Coins
import money.tegro.bot.wallet.CryptoCurrency
import money.tegro.bot.wallet.PostgresWalletPersistent
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.*

interface AccountsPersistent {
    suspend fun createAccount(
        issuer: User,
        oneTime: Boolean,
        coins: Coins,
        minAmount: Coins,
        maxCoins: Coins,
        activations: Int
    ): Account

    suspend fun payAccount(
        account: Account,
        payer: User,
        coins: Coins
    )

    suspend fun loadAccounts(user: User): List<Account>

    suspend fun inactivateAccount(account: Account)
}

object PostgresAccountsPersistent : AccountsPersistent {

    object UsersAccounts : UUIDTable("users_accounts") {
        val issueTime = timestamp("issue_time")
        val issuerId = uuid("issuer_id").references(PostgresUserPersistent.Users.id)
        val oneTime = bool("one_time")
        val currency = enumeration<CryptoCurrency>("currency")
        val amount = long("amount")
        val minAmount = long("min_amount")
        val maxCoins = long("max_coins")
        val activations = integer("activations")
        val isActive = bool("is_active")

        init {
            transaction { SchemaUtils.create(this@UsersAccounts) }
        }
    }

    override suspend fun createAccount(
        issuer: User,
        oneTime: Boolean,
        coins: Coins,
        minAmount: Coins,
        maxCoins: Coins,
        activations: Int
    ): Account {
        try {
            val account = Account(
                id = UUID.randomUUID(),
                issueTime = Clock.System.now(),
                issuer = issuer,
                oneTime = oneTime,
                coins = coins,
                minAmount = minAmount,
                maxCoins = maxCoins,
                activations = activations,
                isActive = true,
            )
            SecurityPersistent.log(
                issuer,
                maxCoins,
                "${account.id} | min=$minAmount, max=$maxCoins, activations=$activations",
                LogType.ACCOUNT_CREATE
            )
            saveAccount(account)
            return account
        } catch (e: Throwable) {
            throw e
        }
    }

    fun saveAccount(account: Account) {
        transaction {
            //addLogger(StdOutSqlLogger)

            exec(
                """
                    INSERT INTO users_accounts (id, issue_time, issuer_id, one_time, currency, amount, min_amount,
                    max_coins, activations, is_active) values (?,?,?,?,?,?,?,?,?,?)
                    ON CONFLICT (id) DO UPDATE SET issue_time=?, issuer_id=?, one_time=?, currency=?, amount=?,
                    min_amount=?, max_coins=?, activations=?, is_active=?
                    """, args = listOf(
                    UsersAccounts.id.columnType to account.id,
                    issueTime.columnType to account.issueTime,
                    issuerId.columnType to account.issuer.id,
                    oneTime.columnType to account.oneTime,
                    currency.columnType to account.coins.currency,
                    amount.columnType to account.coins.amount,
                    minAmount.columnType to account.minAmount.amount,
                    maxCoins.columnType to account.maxCoins.amount,
                    activations.columnType to account.activations,
                    isActive.columnType to account.isActive,

                    issueTime.columnType to account.issueTime,
                    issuerId.columnType to account.issuer.id,
                    oneTime.columnType to account.oneTime,
                    currency.columnType to account.coins.currency,
                    amount.columnType to account.coins.amount,
                    minAmount.columnType to account.minAmount.amount,
                    maxCoins.columnType to account.maxCoins.amount,
                    activations.columnType to account.activations,
                    isActive.columnType to account.isActive,
                )
            )
        }
    }

    suspend fun deleteAccount(account: Account) {
        SecurityPersistent.log(
            account.issuer,
            account.maxCoins,
            "${account.id} | profit=${account.coins}",
            LogType.ACCOUNT_INACTIVATE
        )
        inactivateAccount(account)
    }

    override suspend fun inactivateAccount(account: Account) {
        transaction {
            UsersAccounts.update({ UsersAccounts.id eq account.id }) {
                it[isActive] = false
            }
        }
    }

    override suspend fun payAccount(account: Account, payer: User, coins: Coins) {
        val accounts = loadAccounts(account.issuer).toMutableList()
        val currentReceipt = accounts.find { it.id == account.id } ?: throw UnknownAccountException(account)
        val payerWallet = PostgresWalletPersistent.loadWalletState(payer)

        if (currentReceipt.issuer == payer) {
            throw AccountIssuerActivationException(account)
        }
        if (!currentReceipt.isActive || currentReceipt.activations < 1) {
            throw AccountNotActiveException(account)
        }
        if (payerWallet.active[account.coins.currency] < coins) {
            throw NotEnoughCoinsException(payer, coins)
        }
        if (account.minAmount.amount != 0.toBigInteger() && coins < account.minAmount) {
            throw AccountMinAmountException(account)
        }
        if (account.maxCoins.amount > 0.toBigInteger() && account.coins + coins > account.maxCoins) {
            throw AccountOverdraftException(account)
        }

        payer.freeze(coins)
        payer.transfer(account.issuer, coins)
        SecurityPersistent.log(payer, coins, "paid $coins for account ${account.id}", LogType.ACCOUNT_PAID)
        SecurityPersistent.log(account.issuer, coins, "got $coins from account ${account.id}", LogType.ACCOUNT_GOT)
        var currentActivations = account.activations
        if (currentActivations != Int.MAX_VALUE) currentActivations--
        if (currentActivations < 1) {
            inactivateAccount(account)
        } else {
            transaction {
                UsersAccounts.update({ UsersAccounts.id eq account.id }) {
                    it[activations] = currentActivations
                    it[amount] = account.coins.toBigInteger().toLong() + coins.amount.toLong()
                }
            }
        }
    }

    suspend fun loadAccount(accountId: UUID): Account? {
        val receipt = suspendedTransactionAsync {
            val result = UsersAccounts.select {
                UsersAccounts.id.eq(accountId)
            }.firstOrNull() ?: return@suspendedTransactionAsync null
            val issuer = PostgresUserPersistent.load(result[issuerId]) ?: return@suspendedTransactionAsync null
            Account(
                id = result[UsersAccounts.id].value,
                issueTime = result[issueTime],
                issuer = issuer,
                oneTime = result[oneTime],
                coins = Coins(
                    currency = result[currency],
                    amount = result[amount].toBigInteger()
                ),
                minAmount = Coins(
                    currency = result[currency],
                    amount = result[minAmount].toBigInteger()
                ),
                maxCoins = Coins(
                    currency = result[currency],
                    amount = result[maxCoins].toBigInteger()
                ),
                activations = result[activations],
                isActive = result[isActive]
            )
        }
        return receipt.await()
    }

    override suspend fun loadAccounts(user: User): List<Account> {
        val receipts = suspendedTransactionAsync {
            UsersAccounts.select {
                issuerId.eq(user.id)
            }.mapNotNull {
                val issuer = PostgresUserPersistent.load(it[issuerId]) ?: return@mapNotNull null
                Account(
                    id = it[UsersAccounts.id].value,
                    issueTime = it[issueTime],
                    issuer = issuer,
                    oneTime = it[oneTime],
                    coins = Coins(
                        currency = it[currency],
                        amount = it[amount].toBigInteger()
                    ),
                    minAmount = Coins(
                        currency = it[currency],
                        amount = it[minAmount].toBigInteger()
                    ),
                    maxCoins = Coins(
                        currency = it[currency],
                        amount = it[maxCoins].toBigInteger()
                    ),
                    activations = it[activations],
                    isActive = it[isActive]
                )
            }
        }
        return receipts.await()
    }
}