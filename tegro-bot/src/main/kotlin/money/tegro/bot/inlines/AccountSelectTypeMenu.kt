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
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
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

    override suspend fun handleMessage(bot: Bot, message: BotMessage): Boolean {
        val payload = message.payload ?: return false
        when (Json.decodeFromString<ButtonPayload>(payload)) {
            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, message.lastMenuMessageId)
            }

            ButtonPayload.ONETIME -> {
                user.setMenu(bot, AccountSelectCurrencyMenu(user, 1, this), message.lastMenuMessageId)
            }

            ButtonPayload.NOTONETIME -> {
                user.setMenu(bot, AccountSelectActivationsMenu(user, this), message.lastMenuMessageId)
            }

            ButtonPayload.OPEN -> {
                user.setMenu(bot, OpenAccountSelectCurrencyMenu(user, this), message.lastMenuMessageId)
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