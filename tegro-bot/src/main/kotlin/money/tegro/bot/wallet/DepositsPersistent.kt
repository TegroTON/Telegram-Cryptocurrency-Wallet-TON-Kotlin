package money.tegro.bot.wallet

import money.tegro.bot.objects.Deposit
import money.tegro.bot.objects.DepositPeriod
import money.tegro.bot.objects.PostgresUserPersistent
import money.tegro.bot.objects.User
import money.tegro.bot.wallet.PostgresDepositsPersistent.UsersDeposits.amount
import money.tegro.bot.wallet.PostgresDepositsPersistent.UsersDeposits.cryptoCurrency
import money.tegro.bot.wallet.PostgresDepositsPersistent.UsersDeposits.depositPeriod
import money.tegro.bot.wallet.PostgresDepositsPersistent.UsersDeposits.finishDate
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction

interface DepositsPersistent {
    suspend fun saveDeposit(deposit: Deposit)
    suspend fun getAllByUser(user: User): List<Deposit>
    suspend fun check(): List<Deposit>
}

object PostgresDepositsPersistent : DepositsPersistent {

    object UsersDeposits : Table("users_deposits") {
        val userId = uuid("user_id").references(PostgresUserPersistent.Users.id)
        val depositPeriod = enumeration<DepositPeriod>("deposit_period")
        val finishDate = timestamp("finish_date")
        val cryptoCurrency = enumeration<CryptoCurrency>("crypto_currency")
        val amount = long("amount")

        init {
            transaction { SchemaUtils.create(this@UsersDeposits) }
        }
    }

    override suspend fun saveDeposit(deposit: Deposit) {
        suspendedTransactionAsync {
            UsersDeposits.insert {
                it[userId] = deposit.userId
                it[depositPeriod] = deposit.depositPeriod
                it[finishDate] = deposit.finishDate
                it[cryptoCurrency] = deposit.coins.currency
                it[amount] = deposit.coins.amount.toLong()
            }
        }
    }

    override suspend fun getAllByUser(user: User): List<Deposit> {
        val deposits = suspendedTransactionAsync {
            UsersDeposits.select {
                UsersDeposits.userId.eq(user.id)
            }.mapNotNull {
                Deposit(
                    user.id,
                    it[depositPeriod],
                    it[finishDate],
                    Coins(
                        currency = it[cryptoCurrency],
                        amount = it[amount].toBigInteger()
                    )
                )
            }
        }
        return deposits.await()
    }

    override suspend fun check(): List<Deposit> {
        TODO("Not yet implemented")
    }

}