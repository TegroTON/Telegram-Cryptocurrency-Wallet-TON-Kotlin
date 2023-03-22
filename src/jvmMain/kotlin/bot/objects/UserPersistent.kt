package bot.objects

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

interface UserPersistent {

    suspend fun save(user: User): User
    suspend fun load(uuid: UUID): User?

    suspend fun loadByVk(long: Long): User?

    suspend fun loadByTg(long: Long): User?
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
            }.singleOrNull() ?: return@transaction null
            val settingsRow = UsersSettings.select {
                UsersSettings.userId.eq(uuid)
            }.singleOrNull()
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
            }.singleOrNull() ?: return@transaction null
            val userId: UUID = userRow[Users.id].value
            val settingsRow = UsersSettings.select {
                UsersSettings.userId.eq(userId)
            }.singleOrNull()
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
            }.singleOrNull() ?: return@transaction null
            val userId: UUID = userRow[Users.id].value
            val settingsRow = UsersSettings.select {
                UsersSettings.userId.eq(userId)
            }.singleOrNull()
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

}