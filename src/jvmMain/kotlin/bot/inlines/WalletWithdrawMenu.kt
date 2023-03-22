package bot.inlines

import bot.api.Bot
import bot.objects.BotMessage
import bot.objects.MessagesContainer
import bot.objects.User
import bot.objects.keyboard.BotKeyboard
import bot.utils.button
import bot.wallet.CryptoCurrency
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
class WalletWithdrawMenu(
    val user: User,
    val currency: CryptoCurrency,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        val message = buildString {
            when (currency) {
                CryptoCurrency.TON -> appendLine(
                    String.format(
                        MessagesContainer[user.settings.lang].menuWalletWithdrawMessage,
                        "TON",
                        MessagesContainer[user.settings.lang].menuWalletWithdrawTON
                    )
                )

                CryptoCurrency.TGR -> appendLine(
                    String.format(
                        MessagesContainer[user.settings.lang].menuWalletWithdrawMessage,
                        "TGR",
                        MessagesContainer[user.settings.lang].menuWalletWithdrawTGR
                    )
                )

                CryptoCurrency.USDT -> appendLine(
                    String.format(
                        MessagesContainer[user.settings.lang].menuWalletWithdrawMessage,
                        "USDT",
                        MessagesContainer[user.settings.lang].menuWalletWithdrawUSDT
                    )
                )
            }
        }
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = message,
            keyboard = BotKeyboard {
                row {
                    button(
                        MessagesContainer[user.settings.lang].menuButtonBack,
                        WalletMenu.ButtonPayload.serializer(),
                        WalletMenu.ButtonPayload.BACK
                    )
                }
            }
        )
    }

    override suspend fun handleMessage(bot: Bot, message: BotMessage): Boolean {
        if (message.payload != null) {
            val payload = message.payload
            when (Json.decodeFromString<ButtonPayload>(payload)) {
                ButtonPayload.BACK -> {
                    user.setMenu(bot, parentMenu, message.lastMenuMessageId)
                }
            }
        } else {
            //TODO Вывод на кошелёк message.body
        }
        return true
    }

    @Serializable
    enum class ButtonPayload {
        BACK
    }
}