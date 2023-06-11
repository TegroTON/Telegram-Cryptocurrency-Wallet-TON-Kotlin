package money.tegro.bot.inlines

import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.objects.Account
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.PostgresAccountsPersistent
import money.tegro.bot.utils.button
import money.tegro.bot.wallet.Coins
import money.tegro.bot.wallet.CryptoCurrency
import java.util.*

@Serializable
class OpenAccountSelectCurrencyMenu(
    val user: User,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, botMessage: BotMessage) {
        bot.updateKeyboard(
            to = botMessage.peerId,
            lastMenuMessageId = botMessage.lastMenuMessageId,
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

    override suspend fun handleMessage(bot: Bot, botMessage: BotMessage): Boolean {
        val rawPayload = botMessage.payload ?: return false
        when (val payload = Json.decodeFromString<ButtonPayload>(rawPayload)) {
            is ButtonPayload.Currency -> {
                val currency = payload.value
                if (!currency.isEnabled) {
                    return bot.sendPopup(botMessage, Messages[user.settings.lang].soon)
                }
                val zero = Coins(currency, 0.toBigInteger())
                val account = Account(
                    UUID.randomUUID(),
                    Clock.System.now(),
                    user,
                    false,
                    zero,
                    zero,
                    zero,
                    Int.MAX_VALUE,
                    true
                )
                PostgresAccountsPersistent.saveAccount(account)
                user.setMenu(
                    bot,
                    AccountReadyMenu(user, account, AccountsMenu(user, MainMenu(user))),
                    botMessage
                )
            }

            ButtonPayload.Back -> {
                user.setMenu(bot, parentMenu, botMessage)
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