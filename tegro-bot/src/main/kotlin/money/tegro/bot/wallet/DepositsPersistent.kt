package money.tegro.bot.wallet

import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import money.tegro.bot.exceptions.NotEnoughCoinsException
import money.tegro.bot.objects.Deposit
import money.tegro.bot.objects.DepositPeriod
import money.tegro.bot.objects.PostgresUserPersistent
import money.tegro.bot.objects.User
import money.tegro.bot.utils.JSON
import net.dzikoysk.exposed.upsert.upsert
import net.dzikoysk.exposed.upsert.withUnique
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.*

interface DepositsPersistent {
    suspend fun saveDeposit(deposit: Deposit)
    suspend fun getAllByUser(user: User): List<Deposit>
    suspend fun check(): List<Deposit>
}

object PostgresDepositsPersistent : DepositsPersistent {

    object UsersDeposits : Table("users_deposits") {
        val userId = uuid("user_id").references(PostgresUserPersistent.Users.id)
        val depositPeriod = enumeration<DepositPeriod>("deposit_period")
        val cryptoCurrency = enumeration<CryptoCurrency>("crypto_currency")
        val amount = long("amount")

        val uniqueTypeValue = withUnique("CyanP3tux", userId)

        init {
            transaction { SchemaUtils.create(this@UsersDeposits) }
        }
    }

    override suspend fun saveDeposit(deposit: Deposit) {
        suspendedTransactionAsync {
            UsersDeposits.insert {
                it[userId] = deposit.userId
                it[depositPeriod] = deposit.depositPeriod
                it[cryptoCurrency] = deposit.coins.currency
                it[amount] = deposit.coins.amount.toLong()
            }
        }
    }

    override suspend fun getAllByUser(user: User): List<Deposit> {
        TODO("Not yet implemented")
    }

    override suspend fun check(): List<Deposit> {
        TODO("Not yet implemented")
    }

}