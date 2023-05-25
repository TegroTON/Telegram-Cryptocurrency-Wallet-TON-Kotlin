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
import money.tegro.bot.utils.NftsPersistent
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
        val nft = buildString {
            if (user.settings.nfts.isNotEmpty()) {
                append("\n\n")
                append(Messages[user].nftPlusPercent.format(NftsPersistent.getUserProfitStacking(user)))
            } else {
                append("")
            }
        }
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = Messages[user.settings.lang].menuDepositSelectPeriodMessage.format(nft, coins),
            keyboard = BotKeyboard {
                DepositPeriod.values().forEach { period ->
                    row {
                        button(
                            DepositPeriod.getDisplayName(period, user),
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
        when (val payloadValue = Json.decodeFromString<ButtonPayload>(payload)) {
            ButtonPayload.Back -> user.setMenu(bot, parentMenu, message.lastMenuMessageId)

            is ButtonPayload.Period -> {
                user.setMenu(
                    bot,
                    DepositApproveMenu(user, coins, payloadValue.value, calc, this),
                    message.lastMenuMessageId
                )
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