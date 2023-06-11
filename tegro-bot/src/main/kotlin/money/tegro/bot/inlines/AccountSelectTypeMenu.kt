package money.tegro.bot.inlines

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.button

@Serializable
class AccountSelectTypeMenu(
    val user: User,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, botMessage: BotMessage) {
        bot.updateKeyboard(
            to = botMessage.peerId,
            lastMenuMessageId = botMessage.lastMenuMessageId,
            message = Messages[user.settings.lang].menuAccountSelectTypeMessage,
            keyboard = BotKeyboard {
                row {
                    button(
                        Messages[user.settings.lang].oneTime,
                        ButtonPayload.serializer(),
                        ButtonPayload.ONETIME
                    )
                    button(
                        Messages[user.settings.lang].notOneTime,
                        ButtonPayload.serializer(),
                        ButtonPayload.NOTONETIME
                    )
                }
                row {
                    button(
                        Messages[user.settings.lang].open,
                        ButtonPayload.serializer(),
                        ButtonPayload.OPEN
                    )
                }
                row {
                    button(
                        Messages[user.settings.lang].menuButtonBack,
                        ButtonPayload.serializer(),
                        ButtonPayload.BACK
                    )
                }
            }
        )
    }

    override suspend fun handleMessage(bot: Bot, botMessage: BotMessage): Boolean {
        val payload = botMessage.payload ?: return false
        when (Json.decodeFromString<ButtonPayload>(payload)) {
            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, botMessage)
            }

            ButtonPayload.ONETIME -> {
                user.setMenu(bot, AccountSelectCurrencyMenu(user, 1, this), botMessage)
            }

            ButtonPayload.NOTONETIME -> {
                user.setMenu(bot, AccountSelectActivationsMenu(user, this), botMessage)
            }

            ButtonPayload.OPEN -> {
                user.setMenu(bot, OpenAccountSelectCurrencyMenu(user, this), botMessage)
            }
        }
        return true
    }

    @Serializable
    private enum class ButtonPayload {
        ONETIME,
        NOTONETIME,
        OPEN,
        BACK
    }
}