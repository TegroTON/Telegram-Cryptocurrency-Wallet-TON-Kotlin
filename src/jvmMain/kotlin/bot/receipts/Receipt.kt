package bot.receipts

import bot.objects.User
import bot.utils.UUIDSerializer
import bot.wallet.Coins
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Receipt(
    @Serializable(UUIDSerializer::class)
    val id: UUID,
    val issueTime: Instant,
    val issuer: User,
    val coins: Coins,
    val activations: Int,
    var recipient: User? = null,
    val isActive: Boolean = true
)