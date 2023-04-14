package money.tegro.bot.inlines

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.DepositPeriod
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.button
import money.tegro.bot.wallet.Coins

@Serializable
class DepositSelectPeriodMenu(
    val user: User,
    val coins: Coins,
    val calc: Boolean,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = Messages[user.settings.lang].menuDepositSelectPeriodMessage.format(coins),
            keyboard = BotKeyboard {
                DepositPeriod.values().forEach { period ->
                    row {
                        button(
                            DepositPeriod.getDisplayName(period, user.settings.lang),
                            ButtonPayload.serializer(),
                            ButtonPayload.Period(period)
                        )
                    }
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

    override suspend fun handleMessage(bot: Bot, message: BotMessage): Boolean {
        val payload = message.payload ?: return false
        when (val payload = Json.decodeFromString<ButtonPayload>(payload)) {

            ButtonPayload.Back -> user.setMenu(bot, parentMenu, message.lastMenuMessageId)

            is ButtonPayload.Period -> {
                user.setMenu(bot, DepositApproveMenu(user, coins, payload.value, calc, this), message.lastMenuMessageId)
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
        @SerialName("period")
        data class Period(val value: DepositPeriod) : ButtonPayload()
    }
}