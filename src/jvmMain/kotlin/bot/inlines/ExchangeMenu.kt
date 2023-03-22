package bot.inlines

import bot.api.Bot
import bot.objects.BotMessage
import bot.objects.MessagesContainer
import bot.objects.User
import bot.objects.keyboard.BotKeyboard
import bot.utils.button
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
class ExchangeMenu(
    val user: User,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = MessagesContainer[user.settings.lang].menuExchangeMessage,
            keyboard = BotKeyboard {
                row {
                    button("TON", ButtonPayload.serializer(), ButtonPayload.TON)
                }
                row {
                    button("TGR", ButtonPayload.serializer(), ButtonPayload.TGR)
                }
                row {
                    button("USDT", ButtonPayload.serializer(), ButtonPayload.USDT)
                }
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
        val payload = message.payload ?: return false
        when (Json.decodeFromString<ButtonPayload>(payload)) {
            ButtonPayload.TON -> TODO()
            ButtonPayload.TGR -> TODO()
            ButtonPayload.USDT -> TODO()
            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, message.lastMenuMessageId)
            }
        }
        return true
    }

    @Serializable
    enum class ButtonPayload {
        TON,
        TGR,
        USDT,
        BACK
    }
}