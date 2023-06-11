package money.tegro.bot.inlines

import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.objects.*
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.NftsPersistent
import money.tegro.bot.utils.PostgresDepositsPersistent
import money.tegro.bot.utils.button
import money.tegro.bot.wallet.Coins
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class DepositReadyMenu(
    val user: User,
    val deposit: Deposit,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, botMessage: BotMessage) {
        val coins = deposit.coins
        val depositPeriod = deposit.depositPeriod
        val yield = NftsPersistent.countStackingPercent(user, depositPeriod.yield)
        val profit = (
                coins.toBigDecimal()
                        * yield
                        * (depositPeriod.period.toBigDecimal() * 30.toBigDecimal())
                        / 365.toBigDecimal()) / 100.toBigDecimal()
        val profitCoins = Coins(coins.currency, coins.currency.fromNano(profit.toBigInteger()))
        val date = Date.from(deposit.finishDate.toJavaInstant())
        val time =
            SimpleDateFormat("dd.MM.yyyy HH:mm").format(date)
        bot.updateKeyboard(
            to = botMessage.peerId,
            lastMenuMessageId = botMessage.lastMenuMessageId,
            message = Messages[user.settings.lang].menuDepositReadyMessage.format(
                coins,
                depositPeriod.period,
                DepositPeriod.getWord(
                    depositPeriod.period,
                    Messages[user.settings.lang].monthOne,
                    Messages[user.settings.lang].monthTwo,
                    Messages[user.settings.lang].monthThree
                ),
                yield.toString(),
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

    override suspend fun handleMessage(bot: Bot, botMessage: BotMessage): Boolean {
        val payload = botMessage.payload ?: return false
        when (Json.decodeFromString<ButtonPayload>(payload)) {
            ButtonPayload.BACK -> {
                val list = PostgresDepositsPersistent.getAllByUser(user)
                user.setMenu(
                    bot,
                    DepositsListMenu(user, list.toMutableList(), 1, DepositsMenu(user, MainMenu(user))),
                    botMessage
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