package bot.inlines

import bot.objects.PostgresUserPersistent
import bot.objects.User
import bot.utils.JSON
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.dzikoysk.exposed.upsert.upsert
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.*

interface MenuPersistent {
    suspend fun loadMenu(user: User): Menu?
    suspend fun saveMenu(user: User, menu: Menu): Menu
}

object PostgresMenuPersistent : MenuPersistent {

    object UsersMenus : Table("users_menus") {
        val userId = uuid("user_id").references(PostgresUserPersistent.Users.id).uniqueIndex()
        val menuJson = text("menu_json").nullable()

        init {
            transaction { SchemaUtils.create(this@UsersMenus) }
        }
    }

    override suspend fun loadMenu(user: User): Menu? {
        return transaction {
            val menuRow = UsersMenus.select {
                UsersMenus.userId.eq(user.id)
            }.singleOrNull() ?: return@transaction null

            JSON.decodeFromString<Menu>(menuRow[UsersMenus.menuJson] ?: return@transaction null)
        }
    }

    override suspend fun saveMenu(user: User, menu: Menu): Menu {
        transaction {
            UsersMenus.upsert(conflictColumn = UsersMenus.userId,
                insertBody = {
                    it[userId] = user.id
                    it[menuJson] = JSON.encodeToString(menu)
                },
                updateBody = {
                    it[menuJson] = JSON.encodeToString(menu)
                }
            )
        }
        return menu
    }
}

// FOR DEBUG ONLY!!! DON'T USE IT IN PRODUCTION!
class JsonMenuPersistent(
    val file: File
) : MenuPersistent {
    init {
        if (file.parentFile != null && !file.parentFile.exists()) file.parentFile.mkdirs()
        if (!file.exists()) {
            file.createNewFile()
            file.writeText("{}")
        }
    }

    private val fileLock = reentrantLock()

    override suspend fun loadMenu(user: User): Menu? {
        val menuMap = fileLock.withLock {
            loadMenuMap()
        }
        return menuMap[user.id]
    }

    override suspend fun saveMenu(user: User, menu: Menu): Menu {
        fileLock.withLock {
            val menuMap = loadMenuMap().toMutableMap()
            menuMap[user.id] = menu
            saveMenuMap(menuMap)
            return menu
        }
    }

    private fun loadMenuMap() =
        JSON.decodeFromString<Map<UUID, Menu>>(file.readText())

    private fun saveMenuMap(map: Map<UUID, Menu>) = file.writeText(
        JSON.encodeToString(map)
    )
}