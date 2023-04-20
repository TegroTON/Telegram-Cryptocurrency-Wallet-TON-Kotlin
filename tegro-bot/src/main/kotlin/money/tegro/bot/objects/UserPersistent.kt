package money.tegro.bot.objects

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

interface UserPersistent {

    suspend fun save(user: User): User
    suspend fun load(uuid: UUID): User?

    suspend fun loadByVk(long: Long): User?

    suspend fun loadByTg(long: Long): User?

    suspend fun getRefsByUser(user: User): List<User>
}

object PostgresUserPersistent : UserPersistent {

    object Users : UUIDTable("users") {
        val tgId = long("tg_id").nullable()
        val vkId = long("vk_id").nullable()

        init {
            transaction { SchemaUtils.create(this@Users) }
        }
    }

    object UsersSettings : Table("users_settings") {
        val userId = uuid("user_id").references(Users.id)
        val language = enumeration<Language>("language")
        val localCurrency = enumeration<LocalCurrency>("local_currecny")
        val referralId = uuid("referral_id").references(Users.id).nullable()

        init {
            transaction { SchemaUtils.create(this@UsersSettings) }
        }
    }

    override suspend fun save(user: User): User {
        transaction {
            Users.insert {
                it[id] = user.id
                it[tgId] = user.tgId
                it[vkId] = user.vkId
            }
            UsersSettings.insert {
                it[userId] = user.id
                it[language] = user.settings.lang
                it[localCurrency] = user.settings.localCurrency
                it[referralId] = user.settings.referralId
            }
        }
        return user
    }

    fun saveSettings(settings: UserSettings) {
        transaction {
            UsersSettings.update({ UsersSettings.userId eq settings.userId }) {
                it[userId] = settings.userId
                it[language] = settings.lang
                it[localCurrency] = settings.localCurrency
                it[referralId] = settings.referralId
            }
        }
    }

    override suspend fun load(uuid: UUID): User? {
        return transaction {
            val userRow = Users.select {
                Users.id.eq(uuid)
            }.firstOrNull() ?: return@transaction null
            val settingsRow = UsersSettings.select {
                UsersSettings.userId.eq(uuid)
            }.firstOrNull()
            if (settingsRow == null) {
                val userSettings = UserSettings(
                    uuid,
                    Language.RU,
                    LocalCurrency.RUB,
                    null
                )
                User(
                    uuid, userRow[Users.tgId], userRow[Users.vkId], userSettings
                )
            } else {
                val userSettings = UserSettings(
                    uuid,
                    settingsRow[UsersSettings.language],
                    settingsRow[UsersSettings.localCurrency],
                    settingsRow[UsersSettings.referralId]
                )
                User(
                    uuid, userRow[Users.tgId], userRow[Users.vkId], userSettings
                )
            }
        }
    }

    override suspend fun loadByVk(long: Long): User? {
        return transaction {
            val userRow = Users.select {
                Users.vkId.eq(long)
            }.firstOrNull() ?: return@transaction null
            val userId: UUID = userRow[Users.id].value
            val settingsRow = UsersSettings.select {
                UsersSettings.userId.eq(userId)
            }.firstOrNull()
            if (settingsRow == null) {
                val userSettings = UserSettings(
                    userId,
                    Language.RU,
                    LocalCurrency.RUB,
                    null
                )
                User(
                    userRow[Users.id].value, userRow[Users.tgId], long, userSettings
                )
            } else {
                val userSettings = UserSettings(
                    userId,
                    settingsRow[UsersSettings.language],
                    settingsRow[UsersSettings.localCurrency],
                    settingsRow[UsersSettings.referralId]
                )
                User(
                    userRow[Users.id].value, userRow[Users.tgId], long, userSettings
                )
            }
        }
    }

    override suspend fun loadByTg(long: Long): User? {
        return transaction {
            val userRow = Users.select {
                Users.tgId.eq(long)
            }.toList().also {
//                println("Users by $long - ${it.size} - $it")
            }.firstOrNull() ?: return@transaction null.also {
//                println("tg not found: $long")
            }
//            println("userRow: $userRow")
            val userId: UUID = userRow[Users.id].value
            val settingsRow = UsersSettings.select {
                UsersSettings.userId.eq(userId)
            }.firstOrNull()
            if (settingsRow == null) {
                val userSettings = UserSettings(
                    userId,
                    Language.RU,
                    LocalCurrency.RUB,
                    null
                )
                User(
                    userRow[Users.id].value, long, userRow[Users.vkId], userSettings
                )
            } else {
                val userSettings = UserSettings(
                    userId,
                    settingsRow[UsersSettings.language],
                    settingsRow[UsersSettings.localCurrency],
                    settingsRow[UsersSettings.referralId]
                )
                User(
                    userRow[Users.id].value, long, userRow[Users.vkId], userSettings
                )
            }

        }
    }

    override suspend fun getRefsByUser(user: User): List<User> {
        val referralUsers = suspendedTransactionAsync {
            UsersSettings.select {
                UsersSettings.referralId.eq(user.id)
            }.mapNotNull {
                val referralId = it[UsersSettings.referralId] ?: return@mapNotNull null
                load(referralId) ?: return@mapNotNull null
            }
        }
        return referralUsers.await()
    }

}