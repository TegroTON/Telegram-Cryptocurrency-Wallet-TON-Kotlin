package money.tegro.bot.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.toJavaInstant
import money.tegro.bot.api.Bot
import money.tegro.bot.objects.*
import money.tegro.bot.utils.PostgresDepositsPersistent.UsersDeposits.amount
import money.tegro.bot.utils.PostgresDepositsPersistent.UsersDeposits.cryptoCurrency
import money.tegro.bot.utils.PostgresDepositsPersistent.UsersDeposits.depositPeriod
import money.tegro.bot.utils.PostgresDepositsPersistent.UsersDeposits.finishDate
import money.tegro.bot.utils.PostgresDepositsPersistent.UsersDeposits.isPaid
import money.tegro.bot.utils.PostgresDepositsPersistent.UsersDeposits.issuerId
import money.tegro.bot.wallet.Coins
import money.tegro.bot.wallet.CryptoCurrency
import money.tegro.bot.walletPersistent
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction
import java.text.SimpleDateFormat
import java.util.*

interface DepositsPersistent {
    suspend fun saveDeposit(deposit: Deposit)
    suspend fun getAllByUser(user: User): List<Deposit>
    suspend fun check(): List<Deposit>
}

object PostgresDepositsPersistent : DepositsPersistent {

    object UsersDeposits : UUIDTable("users_deposits") {
        val issuerId = uuid("issuer_id").references(PostgresUserPersistent.Users.id)
        val depositPeriod = enumeration<DepositPeriod>("deposit_period")
        val finishDate = timestamp("finish_date")
        val cryptoCurrency = enumeration<CryptoCurrency>("crypto_currency")
        val amount = long("amount")
        val isPaid = bool("is_paid")
        val paidDate = timestamp("paid_date").nullable()

        init {
            transaction { SchemaUtils.create(this@UsersDeposits) }
        }
    }

    override suspend fun saveDeposit(deposit: Deposit) {
        deposit.issuer.freeze(deposit.coins)
        transaction {
            UsersDeposits.insert {
                it[issuerId] = deposit.issuer.id
                it[depositPeriod] = deposit.depositPeriod
                it[finishDate] = deposit.finishDate
                it[cryptoCurrency] = deposit.coins.currency
                it[amount] = deposit.coins.amount.toLong()
                it[isPaid] = false
            }
        }
        SecurityPersistent.log(deposit.issuer, deposit.coins, "base=${deposit.coins}", LogType.DEPOSIT_CREATE)
    }

    override suspend fun getAllByUser(user: User): List<Deposit> {
        val deposits = suspendedTransactionAsync {
            UsersDeposits.select {
                issuerId.eq(user.id)
            }.mapNotNull {
                val issuer = PostgresUserPersistent.load(it[issuerId])
                    ?: return@mapNotNull null
                Deposit(
                    it[UsersDeposits.id].value,
                    issuer,
                    it[depositPeriod],
                    it[finishDate],
                    Coins(
                        it[cryptoCurrency],
                        it[amount].toBigInteger()
                    ),
                    it[isPaid]
                )
            }
        }
        return deposits.await()
    }

    override suspend fun check(): List<Deposit> {
        val deposits = suspendedTransactionAsync {
            UsersDeposits.select {
                (finishDate.lessEq(Clock.System.now())) and (isPaid.eq(false))
            }.mapNotNull {
                val issuer = PostgresUserPersistent.load(it[issuerId])
                    ?: return@mapNotNull null
                Deposit(
                    it[UsersDeposits.id].value,
                    issuer,
                    it[depositPeriod],
                    it[finishDate],
                    Coins(
                        it[cryptoCurrency],
                        it[amount].toBigInteger()
                    ),
                    it[isPaid]
                )
            }
        }
        return deposits.await()
    }

    suspend fun payDeposit(deposit: Deposit, bot: Bot) {
        transaction {
            UsersDeposits.update({ UsersDeposits.id eq deposit.id }) {
                it[isPaid] = true
                it[paidDate] = Clock.System.now()
            }
        }
        val coins = deposit.coins
        val depositPeriod = deposit.depositPeriod
        val yield = NftsPersistent.countStackingPercent(deposit.issuer, deposit.depositPeriod.yield)
        val profit = (
                coins.toBigDecimal()
                        * yield
                        * (depositPeriod.period.toBigDecimal() * 30.toBigDecimal())
                        / 365.toBigDecimal()) / 100.toBigDecimal()
        val profitCoins = Coins(coins.currency, coins.currency.fromNano(profit.toBigInteger()))
        walletPersistent.updateActive(deposit.issuer, deposit.coins.currency) { oldCoins ->
            (oldCoins + profitCoins).also { newCoins ->
                println(
                    "New deposit payment: ${deposit.issuer}\n" +
                            "     old coins: $oldCoins\n" +
                            " deposit coins: $profitCoins\n" +
                            "     new coins: $newCoins"
                )
            }
        }
        deposit.issuer.unfreeze(deposit.coins)
        val date =
            Date.from(deposit.finishDate.minus(depositPeriod.period, DateTimeUnit.HOUR * 24 * 31).toJavaInstant())
        val time =
            SimpleDateFormat("dd.MM.yyyy HH:mm").format(date)
        bot.sendMessage(
            deposit.issuer.tgId ?: deposit.issuer.tgId ?: 0,
            Messages[deposit.issuer.settings.lang].menuDepositPaid.format(
                time,
                coins,
                profitCoins
            )
        )
        SecurityPersistent.log(
            deposit.issuer,
            deposit.coins + profitCoins,
            "profit=$profitCoins, base=${deposit.coins}",
            LogType.DEPOSIT_PAID
        )
    }

}