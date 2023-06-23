package money.tegro.bot.utils

import money.tegro.bot.objects.Log
import money.tegro.bot.objects.LogType
import money.tegro.bot.objects.PostgresUserPersistent
import money.tegro.bot.objects.User
import money.tegro.bot.utils.PostgresLogsPersistent.UsersLogs.info
import money.tegro.bot.utils.PostgresLogsPersistent.UsersLogs.logType
import money.tegro.bot.utils.PostgresLogsPersistent.UsersLogs.time
import money.tegro.bot.utils.PostgresLogsPersistent.UsersLogs.userId
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction

interface LogsPersistent {
    suspend fun pushLogs(
        logs: List<Log>
    )

    suspend fun getLogsByUser(user: User): List<Log>

    suspend fun getLogsByType(logType: LogType): List<Log>
}

object PostgresLogsPersistent : LogsPersistent {

    object UsersLogs : UUIDTable("users_logs") {
        val userId = uuid("user_id").references(PostgresUserPersistent.Users.id)
        val time = timestamp("time")
        val logType = enumeration<LogType>("log_type")
        val info = text("info")

        init {
            transaction { SchemaUtils.create(this@UsersLogs) }
        }
    }

    override suspend fun pushLogs(logs: List<Log>) {
        transaction {
            for (log: Log in logs) {
                UsersLogs.insert {
                    it[userId] = log.userId
                    it[time] = log.time
                    it[logType] = log.logType
                    it[info] = log.info
                }
            }
        }
    }

    override suspend fun getLogsByUser(user: User): List<Log> {
        val logs = suspendedTransactionAsync {
            UsersLogs.select {
                userId.eq(user.id)
            }.mapNotNull {
                Log(
                    it[UsersLogs.id].value,
                    it[userId],
                    it[time],
                    it[logType],
                    it[info]
                )
            }
        }
        return logs.await()
    }

    override suspend fun getLogsByType(logType: LogType): List<Log> {
        val logs = suspendedTransactionAsync {
            UsersLogs.select {
                UsersLogs.logType.eq(logType)
            }.mapNotNull {
                Log(
                    it[UsersLogs.id].value,
                    it[userId],
                    it[time],
                    it[UsersLogs.logType],
                    it[info]
                )
            }
        }
        return logs.await()
    }
}