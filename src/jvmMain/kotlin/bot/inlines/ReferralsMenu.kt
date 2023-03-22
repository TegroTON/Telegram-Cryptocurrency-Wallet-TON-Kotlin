package bot.inlines

import bot.api.Bot
import bot.api.TgBot
import bot.objects.BotMessage
import bot.objects.Messages
import bot.objects.User
import bot.objects.keyboard.BotKeyboard
import bot.utils.button
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
class ReferralsMenu(
    val user: User,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        val code = user.id.toString()
        val tgLink = String.format("t.me/%s?start=RF-%s", System.getenv("TG_USER_NAME"), code)
        val vkLink = String.format("https://vk.com/write-%s?ref=RF-%s", System.getenv("VK_GROUP_ID"), code)
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = String.format(Messages.menuReferralsMessage, if (bot is TgBot) tgLink else vkLink),
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
        val payload = message.payload ?: return false
        when (Json.decodeFromString<ButtonPayload>(payload)) {
            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, message.lastMenuMessageId)
            }
        }
        return true
    }

    @Serializable
    enum class ButtonPayload {
        BACK
    }
}