package bot.inlines

import bot.api.Bot
import bot.objects.BotMessage
import bot.objects.Messages
import bot.objects.User
import bot.objects.keyboard.BotKeyboard
import bot.utils.button
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
class AccountsMenu(
    val user: User,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = Messages.menuAccountsMessage,
            keyboard = BotKeyboard {
                row {
                    button(Messages.menuAccountsCreate, ButtonPayload.serializer(), ButtonPayload.CREATE)
                }
                row {
                    button(Messages.menuAccountsList, ButtonPayload.serializer(), ButtonPayload.LIST)
                }
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
        val payload = message.payload ?: return false
        when (Json.decodeFromString<ButtonPayload>(payload)) {
            ButtonPayload.CREATE -> {

            }

            ButtonPayload.LIST -> {

            }

            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, message.lastMenuMessageId)
            }
        }
        return true
    }

    @Serializable
    enum class ButtonPayload {
        CREATE,
        LIST,
        BACK
    }
}