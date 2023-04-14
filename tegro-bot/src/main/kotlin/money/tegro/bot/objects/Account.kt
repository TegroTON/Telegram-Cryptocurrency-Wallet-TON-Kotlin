package money.tegro.bot.objects

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import money.tegro.bot.api.Bot
import money.tegro.bot.api.TgBot
import money.tegro.bot.utils.UUIDSerializer
import money.tegro.bot.wallet.Coins
import java.util.*

@Serializable
data class Account(
    @Serializable(UUIDSerializer::class)
    val id: UUID,
    val issueTime: Instant,
    val issuer: User,
    val oneTime: Boolean,
    val coins: Coins,
    val minAmount: Coins,
    val maxCoins: Coins,
    val activations: Int,
    val isActive: Boolean = true
) {
    companion object {

        fun getTypeDisplay(account: Account, lang: Language): String {
            return if (account.maxCoins.amount == 0.toBigInteger()) Messages[lang].open else if (account.oneTime) Messages[lang].oneTime else Messages[lang].notOneTime
        }

        fun getProgress(bot: Bot, account: Account, lang: Language): String {
            return if (account.maxCoins.amount == 0.toBigInteger()) {
                notSet(bot, lang)
            } else {
                (account.maxCoins - account.coins).toString()
            }
        }

        fun getMinAmount(bot: Bot, account: Account, lang: Language): String {
            return if (account.minAmount.amount == 0.toBigInteger()) {
                notSet(bot, lang)
            } else {
                account.minAmount.toString()
            }
        }

        fun getActivations(bot: Bot, account: Account, lang: Language): String {
            return if (account.activations == Int.MAX_VALUE) {
                notSet(bot, lang)
            } else {
                account.activations.toString()
            }
        }

        fun notSet(bot: Bot, lang: Language): String {
            return buildString {
                if (bot is TgBot) append("<i>")
                append(Messages[lang].notSet)
                if (bot is TgBot) append("</i>")
            }
        }
    }
}