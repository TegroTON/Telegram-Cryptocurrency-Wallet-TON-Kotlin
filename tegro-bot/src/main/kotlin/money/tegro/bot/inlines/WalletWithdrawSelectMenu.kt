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
class WalletWithdrawSelectMenu(
    val user: User,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = Messages[user.settings.lang].menuWalletWithdrawSelectMessage,
            keyboard = BotKeyboard {
                CryptoCurrency.values().forEach { cryptoCurrency ->
                    row {
                        button(
                            label = if (cryptoCurrency.isEnabled) {
                                cryptoCurrency.ticker
                            } else {
                                "${cryptoCurrency.ticker} ${Messages[user].soon}"
                            },
                            ButtonPayload.serializer(),
                            ButtonPayload.Currency(cryptoCurrency)
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
                if (!currency.isEnabled) return false
                val nativeBlockchain = currency.nativeBlockchainType
                if (nativeBlockchain != null) {
                    user.setMenu(
                        bot,
                        WalletWithdrawSelectAmountMenu(user, payload.value, nativeBlockchain, this),
                        message.lastMenuMessageId
                    )
                } else {
                    TODO()
                }
            }

            ButtonPayload.Back -> {
                user.setMenu(bot, parentMenu, message.lastMenuMessageId)
            }
        }
        return true
    }

    private fun getBackKeyboard(): BotKeyboard = BotKeyboard {
        BotKeyboard {
            row {
                button(
                    Messages[user.settings.lang].menuButtonBack,
                    ButtonPayload.serializer(),
                    ButtonPayload.Back
                )
            }
        }
    }

    @Serializable
    private sealed class ButtonPayload {
        @Serializable
        @SerialName("back")
        object Back : ButtonPayload()

        @Serializable
        @SerialName("currency")
        data class Currency(val value: CryptoCurrency) : ButtonPayload()
    }
}

//sealed class Result
//object Success : Result()
//class Error(val string: String) : Result()
//
//fun a() {
//    val r = Any() as Result
//
//
//    when (r) {
//        is Error -> {
//            r.string
//        }
//        Success -> {
//
//        }
//    }
//}