package money.tegro.bot.inlines

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.button
import money.tegro.bot.wallet.CryptoCurrency

@Serializable
class AccountSelectCurrencyMenu(
    val user: User,
    val activations: Int,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = Messages[user.settings.lang].menuAccountSelectCurrencyMessage,
            keyboard = BotKeyboard {
                CryptoCurrency.values().forEach { cryptoCurrency ->
                    row {
                        button(
                            label = if (cryptoCurrency.isEnabled) {
                                cryptoCurrency.ticker
                            } else {
                                "${cryptoCurrency.ticker} ${Messages[user].soon}"
                            },
                            serializer = ButtonPayload.serializer(),
                            payload = ButtonPayload.Currency(cryptoCurrency)
                        )
                    }
                }
                row {
                    button(
                        Messages[user.settings.lang].menuButtonBack,
                        ButtonPayload.serializer(),
                        ButtonPayload.Back
                    )
                }
            }
        )
    }

    override suspend fun handleMessage(bot: Bot, message: BotMessage): Boolean {
        val rawPayload = message.payload ?: return false
        when (val payload = Json.decodeFromString<ButtonPayload>(rawPayload)) {
            is ButtonPayload.Currency -> {
                val currency = payload.value
                if (!currency.isEnabled) {
                    return bot.sendPopup(message, Messages[user.settings.lang].soon)
                }
                user.setMenu(
                    bot,
                    AccountSelectAmountMenu(user, activations, payload.value, this),
                    message.lastMenuMessageId
                )
            }

            ButtonPayload.Back -> {
                user.setMenu(bot, parentMenu, message.lastMenuMessageId)
            }
        }
        return true
    }

    @Serializable
    private sealed class ButtonPayload {
        @Serializable
        @SerialName("back")
        object Back : ButtonPayload()

        @Serializable
        @SerialName("currency")
        data class Currency(
            val value: CryptoCurrency
        ) : ButtonPayload()
    }
}