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
import money.tegro.bot.wallet.Coins

@Serializable
class DepositSelectPeriodMenu(
    val user: User,
    val coins: Coins,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = Messages[user.settings.lang].menuDepositSelectPeriodMessage.format(coins),
            keyboard = BotKeyboard {
                row {
                    button(
                        Messages[user.settings.lang].menuDepositSelectPeriodM3,
                        ButtonPayload.serializer(),
                        ButtonPayload.M3
                    )
                }
                row {
                    button(
                        Messages[user.settings.lang].menuDepositSelectPeriodM6,
                        ButtonPayload.serializer(),
                        ButtonPayload.M6
                    )
                }
                row {
                    button(
                        Messages[user.settings.lang].menuDepositSelectPeriodM12,
                        ButtonPayload.serializer(),
                        ButtonPayload.M12
                    )
                }
                row {
                    button(
                        Messages[user.settings.lang].menuDepositSelectPeriodM24,
                        ButtonPayload.serializer(),
                        ButtonPayload.M24
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

            ButtonPayload.M3 -> {
                user.setMenu(bot, DepositApproveMenu(user, coins, 3, this), message.lastMenuMessageId)
            }

            ButtonPayload.M6 -> {
                user.setMenu(bot, DepositApproveMenu(user, coins, 6, this), message.lastMenuMessageId)
            }

            ButtonPayload.M12 -> {
                user.setMenu(bot, DepositApproveMenu(user, coins, 12, this), message.lastMenuMessageId)
            }

            ButtonPayload.M24 -> {
                user.setMenu(bot, DepositApproveMenu(user, coins, 24, this), message.lastMenuMessageId)
            }
        }
        return true
    }

    @Serializable
    private enum class ButtonPayload {
        M3,
        M6,
        M12,
        M24,
        BACK
    }
}