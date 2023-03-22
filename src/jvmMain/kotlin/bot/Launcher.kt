package bot

import bot.api.TgBot
import bot.api.VkBot
import bot.inlines.PostgresMenuPersistent
import bot.receipts.PostgresReceiptPersistent
import bot.wallet.PostgresWalletPersistent
import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.Database

private const val ENV_VK_API_TOKEN = "VK_API_TOKEN"
private const val ENV_VK_GROUP_ID = "VK_GROUP_ID"

private const val ENV_TG_API_TOKEN = "TG_API_TOKEN"
private const val ENV_TG_USER_NAME = "TG_USER_NAME"

private const val ENV_PG_URL = "PG_URL"
private const val ENV_PG_USER = "PG_USER"
private const val ENV_PG_PASSWORD = "PG_PASSWORD"
private const val ENV_TON_MASTER_KEY = "TON_MASTER_KEY"

val menuPersistent = PostgresMenuPersistent
val walletPersistent = PostgresWalletPersistent
val receiptPersistent = PostgresReceiptPersistent
val tonMasterKey get() = hex(System.getenv(ENV_TON_MASTER_KEY) ?: error("'$ENV_TON_MASTER_KEY' not set"))

suspend fun main() {
    val vkGroupId = System.getenv(ENV_VK_GROUP_ID) ?: error("'$ENV_VK_GROUP_ID' not set")
    val vkAccessToken = System.getenv(ENV_VK_API_TOKEN) ?: error("'$ENV_VK_API_TOKEN' not set")

    val tgUsername = System.getenv(ENV_TG_USER_NAME) ?: error("'$ENV_TG_USER_NAME' not set")
    val tgAccessToken = System.getenv(ENV_TG_API_TOKEN) ?: error("'$ENV_TG_API_TOKEN' not set")

    val pgLink = System.getenv(ENV_PG_URL) ?: error("'$ENV_PG_URL' not set")
    val pgUser = System.getenv(ENV_PG_USER) ?: error("'$ENV_PG_USER' not set")
    val pgPassword = System.getenv(ENV_PG_PASSWORD) ?: error("'$ENV_PG_PASSWORD' not set")


    Database.connect(pgLink, user = pgUser, password = pgPassword)

    val vkScope = CoroutineScope(Dispatchers.Default).launch {
        VkBot().start(vkGroupId, vkAccessToken)
    }

    val tgScope = CoroutineScope(Dispatchers.Default).launch {
        TgBot().start(tgUsername, tgAccessToken)
    }

    vkScope.join()
    tgScope.join()
}