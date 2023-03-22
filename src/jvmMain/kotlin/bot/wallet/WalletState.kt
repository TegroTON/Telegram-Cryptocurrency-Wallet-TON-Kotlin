package bot.wallet

import bot.objects.User
import kotlinx.serialization.Serializable

@Serializable
data class WalletState(
    val user: User,
    val active: CoinsCollection = CoinsCollection.ZERO,
    val frozen: CoinsCollection = CoinsCollection.ZERO
)