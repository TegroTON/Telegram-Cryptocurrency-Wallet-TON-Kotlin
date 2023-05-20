package money.tegro.bot.inlines

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.PostgresDepositsPersistent
import money.tegro.bot.utils.button

@Serializable
class DepositsMenu(
    val user: User,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = Messages[user.settings.lang].menuDepositsMessage,
            keyboard = BotKeyboard {
                row {
                    button(
                        Messages[user.settings.lang].menuDepositsNew,
                        ButtonPayload.serializer(),
                        ButtonPayload.NEW
                    )
                }
                row {
                    button(
                        Messages[user.settings.lang].menuDepositsCurrent,
                        ButtonPayload.serializer(),
                        ButtonPayload.CURRENT
                    )
                }
                row {
                    button(
                        Messages[user.settings.lang].menuDepositsCalculator,
                        ButtonPayload.serializer(),
                        ButtonPayload.CALC
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
            ButtonPayload.NEW -> {
                user.setMenu(bot, DepositSelectAmountMenu(user, false, this), message.lastMenuMessageId)
            }

            ButtonPayload.CURRENT -> {
                val list = PostgresDepositsPersistent.getAllByUser(user)
                user.setMenu(bot, DepositsListMenu(user, list.toMutableList(), 1, this), message.lastMenuMessageId)
            }

            ButtonPayload.CALC -> {
                user.setMenu(bot, DepositSelectAmountMenu(user, true, this), message.lastMenuMessageId)
            }

            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, message.lastMenuMessageId)
            }
        }
        return true
    }

    @Serializable
    private enum class ButtonPayload {
        NEW,
        CURRENT,
        CALC,
        BACK
    }
}