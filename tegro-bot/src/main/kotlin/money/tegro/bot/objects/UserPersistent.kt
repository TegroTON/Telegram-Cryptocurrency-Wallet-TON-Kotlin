package money.tegro.bot.objects

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import money.tegro.bot.utils.PostgresNftsPersistent
import net.dzikoysk.exposed.upsert.withUnique
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.time.Duration

interface UserPersistent {

    suspend fun save(user: User): User
    suspend fun load(uuid: UUID): User?

    suspend fun loadByVk(long: Long): User?

    suspend fun loadByTg(long: Long): User?

    suspend fun getRefsByUser(user: User): List<User>

    suspend fun addCooldown(user: User, type: CooldownType, duration: Duration)

    suspend fun checkCooldown(user: User, type: CooldownType): Boolean

    suspend fun getCooldown(user: User, type: CooldownType): Instant
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
        val address = text("address").default("")
        val phone = text("phone").default("")

        val unique = withUnique("user_id", userId)

        init {
            transaction { SchemaUtils.create(this@UsersSettings) }
        }
    }

    object UsersCooldowns : UUIDTable("users_cooldowns") {
        val userId = uuid("user_id").references(Users.id)
        val cooldownType = enumeration<CooldownType>("type")
        val expireTime = timestamp("expire_time").default(Clock.System.now())

        init {
            transaction { SchemaUtils.create(this@UsersCooldowns) }
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
                it[address] = user.settings.address
            }
        }
        return user
    }

    fun saveSettings(settings: UserSettings) {
        transaction {
            exec(
                """
                    INSERT INTO users_settings (user_id, "language", local_currecny, referral_id, address, phone) 
                    values (?, ?, ?, ?, ?, ?)
                    ON CONFLICT (user_id) DO UPDATE SET "language"=?, local_currecny=?, referral_id=?, address=?, phone=?
                    """, args = listOf(
                    UsersSettings.userId.columnType to settings.userId,
                    UsersSettings.language.columnType to settings.lang,
                    UsersSettings.localCurrency.columnType to settings.localCurrency,
                    UsersSettings.referralId.columnType to settings.referralId,
                    UsersSettings.address.columnType to settings.address,
                    UsersSettings.phone.columnType to settings.phone,

                    UsersSettings.language.columnType to settings.lang,
                    UsersSettings.localCurrency.columnType to settings.localCurrency,
                    UsersSettings.referralId.columnType to settings.referralId,
                    UsersSettings.address.columnType to settings.address,
                    UsersSettings.phone.columnType to settings.phone,
                )
            )
        }
    }

    override suspend fun load(uuid: UUID): User? {
        return transaction {
            SchemaUtils.createMissingTablesAndColumns(UsersSettings)
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
                    settingsRow[UsersSettings.referralId],
                    settingsRow[UsersSettings.address]
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
            val nfts = PostgresNftsPersistent.UsersNfts.select {
                PostgresNftsPersistent.UsersNfts.ownerId.eq(userId)
            }.mapNotNull {
                Nft(
                    it[PostgresNftsPersistent.UsersNfts.id].value,
                    it[PostgresNftsPersistent.UsersNfts.name],
                    it[PostgresNftsPersistent.UsersNfts.address],
                    it[PostgresNftsPersistent.UsersNfts.ownerId],
                    it[PostgresNftsPersistent.UsersNfts.ownerAddress],
                    it[PostgresNftsPersistent.UsersNfts.imageLink],
                    it[PostgresNftsPersistent.UsersNfts.collection]
                )
            }
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
                    settingsRow[UsersSettings.referralId],
                    settingsRow[UsersSettings.address],
                    settingsRow[UsersSettings.phone],
                    nfts
                )
                User(
                    userRow[Users.id].value, userRow[Users.tgId], long, userSettings
                )
            }
        }
    }

    override suspend fun loadByTg(long: Long): User? {
        return transaction {
            SchemaUtils.createMissingTablesAndColumns(UsersSettings)
            val userRow = Users.select {
                Users.tgId.eq(long)
            }.firstOrNull() ?: return@transaction null
            val userId: UUID = userRow[Users.id].value
            val nfts = PostgresNftsPersistent.UsersNfts.select {
                PostgresNftsPersistent.UsersNfts.ownerId.eq(userId)
            }.mapNotNull {
                Nft(
                    it[PostgresNftsPersistent.UsersNfts.id].value,
                    it[PostgresNftsPersistent.UsersNfts.name],
                    it[PostgresNftsPersistent.UsersNfts.address],
                    it[PostgresNftsPersistent.UsersNfts.ownerId],
                    it[PostgresNftsPersistent.UsersNfts.ownerAddress],
                    it[PostgresNftsPersistent.UsersNfts.imageLink],
                    it[PostgresNftsPersistent.UsersNfts.collection]
                )
            }
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
                    settingsRow[UsersSettings.referralId],
                    settingsRow[UsersSettings.address],
                    settingsRow[UsersSettings.phone],
                    nfts
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

    override suspend fun addCooldown(user: User, type: CooldownType, duration: Duration) {
        val time = Clock.System.now() + duration
        transaction {
            UsersCooldowns.insert {
                it[userId] = user.id
                it[cooldownType] = type
                it[expireTime] = time
            }
        }
    }

    override suspend fun checkCooldown(user: User, type: CooldownType): Boolean {
        val resultRow = suspendedTransactionAsync {
            UsersCooldowns.select {
                UsersCooldowns.userId.eq(user.id) and UsersCooldowns.cooldownType.eq(type)
            }.firstOrNull()
        }.await() ?: return true
        val result = resultRow[UsersCooldowns.expireTime] < Clock.System.now()
        if (result) {
            transaction {
                UsersCooldowns.deleteWhere {
                    userId.eq(user.id) and cooldownType.eq(type)
                }
            }
        }
        return result
    }

    override suspend fun getCooldown(user: User, type: CooldownType): Instant {
        val resultRow = suspendedTransactionAsync {
            UsersCooldowns.select {
                UsersCooldowns.userId.eq(user.id) and UsersCooldowns.cooldownType.eq(type)
            }.firstOrNull()
        }.await() ?: return Clock.System.now()
        return resultRow[UsersCooldowns.expireTime]
    }

}