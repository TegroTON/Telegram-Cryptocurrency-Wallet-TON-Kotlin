package money.tegro.bot.objects

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import money.tegro.bot.utils.UUIDSerializer
import money.tegro.bot.wallet.Coins
import java.util.*

@Serializable
data class Account(
    @Serializable(UUIDSerializer::class)
    val id: UUID,
    val issueTime: Instant,
    val oneTime: Boolean,
    val issuer: User,
    val coins: Coins,
    val minAmount: Coins?,
    val maxCoins: Coins?,
    val activations: Int,
    val isActive: Boolean = true
) {
    companion object {
        fun getTypeDisplay(account: Account, lang: Language): String {
            return if (account.oneTime) Messages[lang].oneTime else Messages[lang].notOneTime
        }

        fun getProgress(account: Account, lang: Language): String {
            return if (account.maxCoins != null) (account.maxCoins - account.coins).toString() else Messages[lang].notSet
        }

        fun getMinAmount(account: Account, lang: Language): String {
            return if (account.minAmount != null) account.minAmount.toString() else Messages[lang].notSet
        }
    }
}