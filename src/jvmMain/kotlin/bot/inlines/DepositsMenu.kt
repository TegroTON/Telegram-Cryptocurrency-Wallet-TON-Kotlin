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
class DepositsMenu(
    val user: User,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = MessagesContainer[user.settings.lang].menuDepositsMessage,
            keyboard = BotKeyboard {
                row {
                    button(
                        MessagesContainer[user.settings.lang].menuDepositsNew,
                        ButtonPayload.serializer(),
                        ButtonPayload.NEW
                    )
                }
                row {
                    button(
                        MessagesContainer[user.settings.lang].menuDepositsCurrent,
                        ButtonPayload.serializer(),
                        ButtonPayload.CURRENT
                    )
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
            ButtonPayload.NEW -> {

            }

            ButtonPayload.CURRENT -> {

            }

            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, message.lastMenuMessageId)
            }
        }
        return true
    }

    @Serializable
    enum class ButtonPayload {
        NEW,
        CURRENT,
        BACK
    }
}