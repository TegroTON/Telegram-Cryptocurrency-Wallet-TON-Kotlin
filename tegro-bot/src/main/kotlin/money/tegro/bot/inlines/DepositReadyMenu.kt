package money.tegro.bot.inlines

import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.objects.*
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.button
import money.tegro.bot.wallet.Coins
import money.tegro.bot.wallet.PostgresDepositsPersistent
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class DepositReadyMenu(
    val user: User,
    val deposit: Deposit,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        val coins = deposit.coins
        val depositPeriod = deposit.depositPeriod
        val profit = (
                coins.toBigInteger()
                        * depositPeriod.yield
                        * (depositPeriod.period.toBigInteger() * 30.toBigInteger())
                        / 365.toBigInteger()) / 100.toBigInteger()
        val profitCoins = Coins(coins.currency, coins.currency.fromNano(profit))
        val date = Date.from(deposit.finishDate.toJavaInstant())
        val time =
            SimpleDateFormat("dd.MM.yyyy HH:mm").format(date)
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = Messages[user.settings.lang].menuDepositReadyMessage.format(
                coins,
                depositPeriod.period,
                DepositPeriod.getWord(
                    depositPeriod.period,
                    Messages[user.settings.lang].monthOne,
                    Messages[user.settings.lang].monthTwo,
                    Messages[user.settings.lang].monthThree
                ),
                depositPeriod.yield.toString(),
                profitCoins,
                time
            ),
            keyboard = BotKeyboard {
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
                val list = PostgresDepositsPersistent.getAllByUser(user)
                user.setMenu(
                    bot,
                    DepositsListMenu(user, list.toMutableList(), 1, DepositsMenu(user, MainMenu(user))),
                    message.lastMenuMessageId
                )
            }
        }
        return true
    }

    @Serializable
    private enum class ButtonPayload {
        BACK
    }
}