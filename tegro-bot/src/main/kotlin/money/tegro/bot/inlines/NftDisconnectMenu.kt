package money.tegro.bot.inlines

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.api.TgBot
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.PostgresUserPersistent
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.button

@Serializable
class NftDisconnectMenu(
    val user: User,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, botMessage: BotMessage) {
        val address = user.settings.address
        val displayAddress = buildString {
            if (bot is TgBot) append("<code>")
            append(address.substring(0, 4))
            append("...")
            append(address.substring(address.length - 5))
            if (bot is TgBot) append("</code>")
        }
        bot.updateKeyboard(
            to = botMessage.peerId,
            lastMenuMessageId = botMessage.lastMenuMessageId,
            message = Messages[user.settings.lang].menuNftDisconnectMessage.format(displayAddress),
            keyboard = BotKeyboard {
                row {
                    button(
                        Messages[user.settings.lang].menuButtonApprove,
                        ButtonPayload.serializer(),
                        ButtonPayload.Disconnect
                    )
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
        val payload = botMessage.payload ?: return false
        when (Json.decodeFromString<ButtonPayload>(payload)) {
            ButtonPayload.Back -> {
                user.setMenu(bot, parentMenu, botMessage)
            }

            is ButtonPayload.Disconnect -> {
                val userSettings = user.settings.copy(address = "")
                PostgresUserPersistent.saveSettings(userSettings)
                val newUser = user.copy(
                    settings = userSettings
                )
                newUser.setMenu(bot, NftMenu(newUser, MainMenu(newUser)), botMessage)
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
        @SerialName("disconnect")
        object Disconnect : ButtonPayload()
    }
}