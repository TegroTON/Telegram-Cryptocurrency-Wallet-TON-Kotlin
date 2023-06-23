package money.tegro.bot.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import money.tegro.bot.ADMIN_CONFERENCE_ID
import money.tegro.bot.MASTER_KEY
import money.tegro.bot.api.Bot
import money.tegro.bot.api.TgBot
import money.tegro.bot.blockchain.BlockchainManager
import money.tegro.bot.objects.*
import money.tegro.bot.utils.PostgresSecurityPersistent.UsersFinanceRequests.blockchainType
import money.tegro.bot.utils.SecurityPersistent.Companion.printFinanceRequestToAdmins
import money.tegro.bot.wallet.BlockchainType
import money.tegro.bot.wallet.Coins
import money.tegro.bot.wallet.CryptoCurrency
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction
import java.text.SimpleDateFormat
import java.util.*

interface SecurityPersistent {
    //TODO blacklist, phone check, count deposits and etc...
    companion object {
        private val adminConferenceId = ADMIN_CONFERENCE_ID.toLong()
        private var tgBot: Bot? = null

        suspend fun init() {
            PostgresSecurityPersistent.blacklist.addAll(PostgresSecurityPersistent.loadBlacklist())
        }

        fun isBlacklisted(user: User): Boolean {
            for (entry in PostgresSecurityPersistent.blacklist) {
                if (user.id == entry.userId) return true else continue
            }
            return false
        }

        fun isBlacklisted(blacklistEntry: BlacklistEntry): Boolean {
            for (entry in PostgresSecurityPersistent.blacklist) {
                if (blacklistEntry.userId == entry.userId ||
                    blacklistEntry.address == entry.address ||
                    blacklistEntry.phone == entry.phone
                ) return true else continue
            }
            return false
        }

        fun log(user: User, coins: Coins, info: String, logType: LogType) {
            val log = Log(
                UUID.randomUUID(),
                user.id,
                Clock.System.now(),
                logType,
                info
            )
            LogsUtil.log(log)
            PostgresSecurityPersistent.logFinanceMovement(user, coins, logType)
            val date = Date.from(log.time.toJavaInstant())
            val time =
                SimpleDateFormat("dd.MM.yyyy HH:mm").format(date)
            when (logType) {
                LogType.DEPOSIT_ADMIN, LogType.WITHDRAW_ADMIN, LogType.DEPOSIT_PAID -> {
                    informAdmins("[$time] ${log.userId} >> ${log.logType.displayName}: ${log.info}")
                }

                else -> {

                }
            }
        }

        fun informAdmins(bot: TgBot, info: String) {
            tgBot = bot
            bot.sendMessage(adminConferenceId, "⚠ $info")
        }

        private fun informAdmins(info: String) {
            tgBot?.sendMessage(adminConferenceId, "⚠ $info") ?: error("TgBot is null at SecurityPersistent")
        }

        suspend fun printFinanceRequestToAdmins(
            user: User,
            financeRequest: FinanceRequest
        ) {
            val message = buildString {
                val blockchainType = financeRequest.blockchainType
                when (financeRequest.logType) {
                    LogType.DEPOSIT -> {
                        val privateKey = UserPrivateKey(user.id, MASTER_KEY)
                        val userAddress = BlockchainManager[blockchainType].getAddress(privateKey.key.toByteArray())
                        val scan =
                            if (blockchainType == BlockchainType.TON) "https://tonscan.org/address/" else "https://bscscan.com/address/"
                        appendLine("Подтвердите пополнение ${financeRequest.coins}")
                        appendLine()
                        appendLine("Пользователь ${user.id}(tg=${user.tgId},vk=${user.vkId},addr=${user.settings.address})")
                        appendLine()
                        appendLine("$scan$userAddress")
                        appendLine()
                    }

                    LogType.WITHDRAW -> {
                        appendLine("Подтвердите вывод ${financeRequest.coins} на кошелёк:")
                        appendLine("<code>${financeRequest.address}</code>")
                        appendLine()
                        appendLine("Пользователь ${user.id}(tg=${user.tgId},vk=${user.vkId},addr=${user.settings.address})")
                        appendLine(LogsUtil.logsByUser(user))
                        appendLine()
                        //TODO("Рассчет дебита и кредита по FinanceMovements")
                    }

                    else -> {
                        appendLine("Подтвердите действие ${financeRequest.logType} в ${blockchainType.name}")
                        appendLine("На сумму ${financeRequest.coins}")
                        appendLine()
                        appendLine("Пользователь ${user.id}(tg=${user.tgId},vk=${user.vkId},addr=${user.settings.address})")
                        appendLine()
                    }
                }
                appendLine("<code>/accept ${financeRequest.id}</code>")
            }
            tgBot?.sendMessage(adminConferenceId, message) ?: error("TgBot is null at SecurityPersistent")
        }
    }
}

object PostgresSecurityPersistent : SecurityPersistent {

    val blacklist = emptyList<BlacklistEntry>().toMutableList()

    object UsersBlacklist : UUIDTable("users_blacklist") {
        val userId = uuid("user_id").references(PostgresUserPersistent.Users.id)
        val address = text("address").default("")
        val phone = text("phone").default("")
        val reason = text("reason").default("Not specified")
    }

    object UsersFinanceMovements : UUIDTable("users_finance_movements") {
        val userId = uuid("user_id").references(PostgresUserPersistent.Users.id)
        val type = enumeration<LogType>("log_type")
        val cryptoCurrency = enumeration<CryptoCurrency>("crypto_currency")
        val amount = long("amount")
        val time = timestamp("time")

        init {
            transaction { SchemaUtils.create(this@UsersFinanceMovements) }
        }
    }

    object UsersFinanceRequests : UUIDTable("users_finance_requests") {
        val userId = uuid("user_id").references(PostgresUserPersistent.Users.id)
        val type = enumeration<LogType>("log_type")
        val cryptoCurrency = enumeration<CryptoCurrency>("crypto_currency")
        val amount = long("amount")
        val blockchainType = enumeration<BlockchainType>("blockchain_type")
        val address = text("address").default("")

        init {
            transaction { SchemaUtils.create(this@UsersFinanceRequests) }
        }
    }

    suspend fun addFinanceRequest(
        user: User,
        logType: LogType,
        coins: Coins,
        network: BlockchainType,
        address: String
    ) {
        suspendedTransactionAsync {
            val financeRequest = FinanceRequest(
                UUID.randomUUID(),
                user.id,
                logType,
                coins,
                network,
                address
            )
            UsersFinanceRequests.insert {
                it[id] = financeRequest.id
                it[userId] = financeRequest.userId
                it[type] = financeRequest.logType
                it[cryptoCurrency] = financeRequest.coins.currency
                it[amount] = financeRequest.coins.amount.toLong()
                it[blockchainType] = financeRequest.blockchainType
                it[UsersFinanceRequests.address] = financeRequest.address
            }
            printFinanceRequestToAdmins(user, financeRequest)
        }.await()
    }

    suspend fun loadFinanceRequest(requestId: UUID): FinanceRequest {
        val financeRequest = suspendedTransactionAsync {
            val result = UsersFinanceRequests.select {
                UsersFinanceRequests.id eq requestId
            }.first()
            FinanceRequest(
                result[UsersFinanceRequests.id].value,
                result[UsersFinanceRequests.userId],
                result[UsersFinanceRequests.type],
                Coins(
                    result[UsersFinanceRequests.cryptoCurrency],
                    result[UsersFinanceRequests.amount].toBigInteger()
                ),
                result[blockchainType],
                result[UsersFinanceRequests.address],
            )
        }
        return financeRequest.await()
    }

    fun logFinanceMovement(user: User, coins: Coins, logType: LogType) {
        transaction {
            UsersFinanceMovements.insert {
                it[userId] = user.id
                it[type] = logType
                it[cryptoCurrency] = coins.currency
                it[amount] = coins.amount.toLong()
                it[time] = Clock.System.now()
            }
        }
    }

    suspend fun loadBlacklist(): List<BlacklistEntry> {
        val blacklist = suspendedTransactionAsync {
            UsersBlacklist.selectAll().mapNotNull {
                BlacklistEntry(
                    it[UsersBlacklist.id].value,
                    it[UsersBlacklist.userId],
                    it[UsersBlacklist.address],
                    it[UsersBlacklist.phone],
                    it[UsersBlacklist.reason],
                )
            }
        }
        return blacklist.await()
    }

    fun addToBlacklist(user: User, address: String?, phone: String?, reason: String?) {
        val entry = BlacklistEntry(UUID.randomUUID(), user.id, address, phone, reason)
        blacklist.add(entry)
        transaction {
            UsersBlacklist.insert {
                it[userId] = user.id
                if (address != null) {
                    it[UsersBlacklist.address] = address
                }
                if (phone != null) {
                    it[UsersBlacklist.phone] = phone
                }
                if (reason != null) {
                    it[UsersBlacklist.reason] = reason
                }
            }
        }
    }
}