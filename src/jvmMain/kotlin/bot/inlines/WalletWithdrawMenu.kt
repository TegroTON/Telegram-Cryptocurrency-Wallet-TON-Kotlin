package bot.inlines

import bot.api.Bot
import bot.objects.BotMessage
import bot.objects.Messages
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
                        Messages.menuWalletWithdrawMessage,
                        "TON",
                        Messages.menuWalletWithdrawTON
                    )
                )

                CryptoCurrency.TGR -> appendLine(
                    String.format(
                        Messages.menuWalletWithdrawMessage,
                        "TGR",
                        Messages.menuWalletWithdrawTGR
                    )
                )

                CryptoCurrency.USDT -> appendLine(
                    String.format(
                        Messages.menuWalletWithdrawMessage,
                        "USDT",
                        Messages.menuWalletWithdrawUSDT
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
                        Messages.menuButtonBack,
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