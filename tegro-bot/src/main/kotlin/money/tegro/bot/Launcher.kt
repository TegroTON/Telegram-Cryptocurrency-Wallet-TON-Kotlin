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
import money.tegro.bot.utils.UserPrivateKey
import money.tegro.bot.wallet.BlockchainType
import money.tegro.bot.wallet.PostgresWalletPersistent
import org.jetbrains.exposed.sql.Database
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

val menuPersistent = PostgresMenuPersistent
val walletPersistent = PostgresWalletPersistent
val receiptPersistent = PostgresReceiptPersistent
val masterKey get() = hex(System.getenv(ENV_MASTER_KEY) ?: error("'$ENV_MASTER_KEY' not set"))
val testnet get() = (System.getenv(ENV_TESTNET) ?: "true").toBoolean()

suspend fun main() {
    val vkGroupId = System.getenv(ENV_VK_GROUP_ID) ?: error("'$ENV_VK_GROUP_ID' not set")
    val vkAccessToken = System.getenv(ENV_VK_API_TOKEN) ?: error("'$ENV_VK_API_TOKEN' not set")

    val tgUsername = System.getenv(ENV_TG_USER_NAME) ?: error("'$ENV_TG_USER_NAME' not set")
    val tgAccessToken = System.getenv(ENV_TG_API_TOKEN) ?: error("'$ENV_TG_API_TOKEN' not set")

    val pgLink = System.getenv(ENV_PG_URL) ?: error("'$ENV_PG_URL' not set")
    val pgUser = System.getenv(ENV_PG_USER) ?: error("'$ENV_PG_USER' not set")
    val pgPassword = System.getenv(ENV_PG_PASSWORD) ?: error("'$ENV_PG_PASSWORD' not set")

    printMasterContracts()
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

private fun printMasterContracts() {
    val masterContractsString = "=".repeat(5) + " MASTER CONTRACTS " + "=".repeat(5)
    println(masterContractsString)
    BlockchainType.values().forEach { blockchainType ->
        if (blockchainType != BlockchainType.TON) return@forEach // TODO: other blockchains

        val blockchainManager = BlockchainManager[blockchainType]
        val masterAddress = blockchainManager.getAddress(UserPrivateKey(UUID(0, 0), masterKey).key.toByteArray())
        println("${blockchainType.displayName}: $masterAddress")
    }
    println("=".repeat(masterContractsString.length))
}