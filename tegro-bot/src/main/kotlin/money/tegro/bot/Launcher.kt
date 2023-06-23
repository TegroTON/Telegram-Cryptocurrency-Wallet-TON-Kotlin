package money.tegro.bot

import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import money.tegro.bot.api.TgBot
import money.tegro.bot.api.VkBot
import money.tegro.bot.blockchain.BlockchainManager
import money.tegro.bot.inlines.PostgresMenuPersistent
import money.tegro.bot.receipts.PostgresReceiptPersistent
import money.tegro.bot.utils.LogsUtil
import money.tegro.bot.utils.SecurityPersistent
import money.tegro.bot.utils.UserPrivateKey
import money.tegro.bot.wallet.BlockchainType
import money.tegro.bot.wallet.PostgresWalletPersistent
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory
import java.util.*

private const val ENV_VK_API_TOKEN = "VK_API_TOKEN"
private const val ENV_VK_GROUP_ID = "VK_GROUP_ID"

private const val ENV_TG_API_TOKEN = "TG_API_TOKEN"
private const val ENV_TG_USER_NAME = "TG_USER_NAME"

private const val ENV_PG_URL = "PG_URL"
private const val ENV_PG_USER = "PG_USER"
private const val ENV_PG_PASSWORD = "PG_PASSWORD"
private const val ENV_MASTER_KEY = "MASTER_KEY"
private const val ENV_TESTNET = "TESTNET"
private const val ENV_ADMIN_CONFERENCE_ID = "ADMIN_CONFERENCE_ID"

private val logger = LoggerFactory.getLogger("Launcher")
val menuPersistent = PostgresMenuPersistent
val walletPersistent = PostgresWalletPersistent
val receiptPersistent = PostgresReceiptPersistent
val MASTER_KEY get() = hex(System.getenv(ENV_MASTER_KEY) ?: error("'$ENV_MASTER_KEY' not set"))
val testnet get() = (System.getenv(ENV_TESTNET) ?: "true").toBoolean()
val ADMIN_CONFERENCE_ID get() = System.getenv(ENV_ADMIN_CONFERENCE_ID) ?: error("'$ENV_ADMIN_CONFERENCE_ID' not set")

suspend fun main() {
    val pgUrl = System.getenv(ENV_PG_URL) ?: error("'$ENV_PG_URL' not set")
    val pgUser = System.getenv(ENV_PG_USER) ?: error("'$ENV_PG_USER' not set")
    val pgPassword = System.getenv(ENV_PG_PASSWORD) ?: error("'$ENV_PG_PASSWORD' not set")

    printMasterContracts()
    try {
        Database.connect(pgUrl, user = pgUser, password = pgPassword)
        logger.info("Success connection to $pgUrl")
    } catch (e: Exception) {
        logger.error("Failed connect to DB", e)
    }

    val vkGroupId = System.getenv(ENV_VK_GROUP_ID)
    val vkAccessToken = System.getenv(ENV_VK_API_TOKEN)

    val vkScope = if (vkGroupId != null && vkAccessToken != null) {
        CoroutineScope(Dispatchers.Default).launch {
            VkBot().start(vkGroupId, vkAccessToken)
        }
    } else {
        logger.warn(
            "VK not initialized: ${
                buildList {
                    if (vkGroupId == null) add(ENV_VK_GROUP_ID)
                    if (vkAccessToken == null) add(ENV_VK_API_TOKEN)
                }.joinToString()
            }"
        )
        null
    }

    val tgUsername = System.getenv(ENV_TG_USER_NAME) ?: error("'$ENV_TG_USER_NAME' not set")
    val tgAccessToken = System.getenv(ENV_TG_API_TOKEN) ?: error("'$ENV_TG_API_TOKEN' not set")

    val tgBot = TgBot(tgAccessToken)
    tgBot.start(tgUsername)

    CoroutineScope(Dispatchers.Default).launch {
        LogsUtil.start()
        SecurityPersistent.init()
    }

    vkScope?.join()

    while (true) {

    }
}

private fun printMasterContracts() {
    val masterContractsString = "=".repeat(5) + " MASTER CONTRACTS " + "=".repeat(5)
    logger.info(buildString {
        appendLine(masterContractsString)
        BlockchainType.values().forEach { blockchainType ->
            if (blockchainType == BlockchainType.ETH) return@forEach // TODO: other blockchains

            val blockchainManager = BlockchainManager[blockchainType]
            val masterAddress = blockchainManager.getAddress(UserPrivateKey(UUID(0, 0), MASTER_KEY).key.toByteArray())
            appendLine("${blockchainType.displayName}: $masterAddress")
        }
        appendLine("=".repeat(masterContractsString.length))
    })
}
