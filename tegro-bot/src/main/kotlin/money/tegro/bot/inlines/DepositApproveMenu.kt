package money.tegro.bot.inlines

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
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
import money.tegro.bot.wallet.PostgresWalletPersistent
import java.util.*

@Serializable
class DepositApproveMenu(
    val user: User,
    val coins: Coins,
    val depositPeriod: DepositPeriod,
    val calc: Boolean,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        val yield = NftsPersistent.countStackingPercent(user, depositPeriod.yield)
        val profit = (
                coins.toBigDecimal()
                        * yield
                        * (depositPeriod.period.toBigDecimal() * 30.toBigDecimal())
                        / 365.toBigDecimal()) / 100.toBigDecimal()
        val profitCoins = Coins(coins.currency, coins.currency.fromNano(profit.toBigInteger()))
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = if (calc) {
                Messages[user.settings.lang].menuDepositApproveMessageCalc.format(
                    coins,
                    depositPeriod.period,
                    DepositPeriod.getWord(
                        depositPeriod.period,
                        Messages[user.settings.lang].monthOne,
                        Messages[user.settings.lang].monthTwo,
                        Messages[user.settings.lang].monthThree
                    ),
                    yield.toString(),
                    profitCoins
                )
            } else {
                Messages[user.settings.lang].menuDepositApproveMessage.format(
                    coins,
                    depositPeriod.period,
                    DepositPeriod.getWord(
                        depositPeriod.period,
                        Messages[user.settings.lang].monthOne,
                        Messages[user.settings.lang].monthTwo,
                        Messages[user.settings.lang].monthThree
                    ),
                    yield.toString(),
                    profitCoins
                )
            },
            keyboard = BotKeyboard {
                row {
                    if (calc) {
                        button(
                            Messages[user.settings.lang].menuDepositCalculatorButton,
                            ButtonPayload.serializer(),
                            ButtonPayload.CREATE
                        )
                    } else {
                        button(
                            Messages[user.settings.lang].menuButtonApprove,
                            ButtonPayload.serializer(),
                            ButtonPayload.APPROVE
                        )
                    }
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
            ButtonPayload.CREATE -> {
                user.setMenu(bot, DepositSelectAmountMenu(user, false, this), message.lastMenuMessageId)
            }

            ButtonPayload.APPROVE -> {
                val deposit = Deposit(
                    UUID.randomUUID(),
                    user,
                    depositPeriod,
                    Clock.System.now().plus(depositPeriod.period, DateTimeUnit.HOUR * 24 * 31),
                    coins,
                    false
                )
                val min = Coins(deposit.coins.currency, 2_500_000_000_000.toBigInteger())
                if (deposit.coins < min) {
                    return bot.sendPopup(
                        message,
                        Messages[user.settings.lang].accountMinAmountException.format(min)
                    )
                }
                val available = PostgresWalletPersistent.loadWalletState(user).active[coins.currency]
                if (available >= deposit.coins) {
                    PostgresDepositsPersistent.saveDeposit(deposit)
                    user.setMenu(
                        bot,
                        DepositReadyMenu(user, deposit, DepositsMenu(user, MainMenu(user))),
                        message.lastMenuMessageId
                    )
                } else return bot.sendPopup(
                    message,
                    Messages[user.settings.lang].menuReceiptsSelectAmountNoMoney.format(deposit.coins, available)
                )
            }

            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, message.lastMenuMessageId)
            }
        }
        return true
    }

    @Serializable
    private enum class ButtonPayload {
        CREATE,
        APPROVE,
        BACK
    }
}
