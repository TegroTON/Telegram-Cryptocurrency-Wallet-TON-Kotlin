package money.tegro.bot.objects

import kotlinx.serialization.Serializable
import money.tegro.bot.utils.UUIDSerializer
import money.tegro.bot.wallet.BlockchainType
import money.tegro.bot.wallet.Coins
import java.util.*

@Serializable
data class FinanceRequest(
    @Serializable(UUIDSerializer::class)
    val id: UUID,
    @Serializable(UUIDSerializer::class)
    val userId: UUID,
    val logType: LogType,
    val coins: Coins,
    val blockchainType: BlockchainType,
    val address: String,
)