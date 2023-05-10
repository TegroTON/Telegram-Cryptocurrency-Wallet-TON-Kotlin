package money.tegro.bot.receipts

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import money.tegro.bot.objects.User
import money.tegro.bot.utils.UUIDSerializer
import money.tegro.bot.wallet.Coins
import java.util.*

@Serializable
data class Receipt(
    @Serializable(UUIDSerializer::class)
    val id: UUID,
    val issueTime: Instant,
    val issuer: User,
    val coins: Coins,
    val activations: Int,
    val recipient: User? = null,
    val captcha: Boolean = true,
    val onlyNew: Boolean = false,
    val onlyPremium: Boolean = false,
    val isActive: Boolean = true
)