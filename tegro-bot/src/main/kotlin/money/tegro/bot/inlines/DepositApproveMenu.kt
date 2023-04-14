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
import money.tegro.bot.utils.button
import money.tegro.bot.wallet.Coins
import money.tegro.bot.wallet.PostgresDepositsPersistent
import money.tegro.bot.wallet.PostgresWalletPersistent
import java.util.*

@Serializable
class DepositApproveMenu(
    val user: User,
    val coins: Coins,
    val depositPeriod: DepositPeriod,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        val profit = (
                coins.toBigInteger()
                        * depositPeriod.yield
                        * (depositPeriod.period.toBigInteger() * 30.toBigInteger())
                        / 365.toBigInteger()) / 100.toBigInteger()
        val profitCoins = Coins(coins.currency, coins.currency.fromNano(profit))
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = Messages[user.settings.lang].menuDepositApproveMessage.format(
                coins,
                depositPeriod.period,
                DepositPeriod.getWord(
                    depositPeriod.period,
                    Messages[user.settings.lang].monthOne,
                    Messages[user.settings.lang].monthTwo,
                    Messages[user.settings.lang].monthThree
                ),
                depositPeriod.yield.toString(),
                profitCoins
            ),
            keyboard = BotKeyboard {
                row {
                    button(
                        Messages[user.settings.lang].menuDepositApproveButton,
                        ButtonPayload.serializer(),
                        ButtonPayload.APPROVE
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
            ButtonPayload.APPROVE -> {
                val deposit = Deposit(
                    UUID.randomUUID(),
                    user,
                    depositPeriod,
                    Clock.System.now().plus(depositPeriod.period, DateTimeUnit.HOUR * 24 * 31),
                    coins,
                    false
                )
                val available = PostgresWalletPersistent.loadWalletState(user).active[coins.currency]
                if (deposit.coins <= available) {
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
        APPROVE,
        BACK
    }
}
