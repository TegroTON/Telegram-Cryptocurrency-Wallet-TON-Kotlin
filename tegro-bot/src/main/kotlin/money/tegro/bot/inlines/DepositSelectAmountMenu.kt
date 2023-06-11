package money.tegro.bot.inlines

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.exceptions.NegativeCoinsException
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.button
import money.tegro.bot.wallet.Coins
import money.tegro.bot.wallet.CryptoCurrency
import money.tegro.bot.wallet.PostgresWalletPersistent

@Serializable
class DepositSelectAmountMenu(
    val user: User,
    val calc: Boolean,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, botMessage: BotMessage) {
        val currency = CryptoCurrency.TGR
        val available = PostgresWalletPersistent.loadWalletState(user).active[currency]
        val min = Coins(currency, 2_500_000_000_000.toBigInteger())
        if (!calc && available < min) {
            bot.updateKeyboard(
                to = botMessage.peerId,
                lastMenuMessageId = botMessage.lastMenuMessageId,
                message = String.format(
                    Messages[user.settings.lang].menuReceiptsSelectAmountNoMoney,
                    min,
                    available
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
            return
        }
        bot.updateKeyboard(
            to = botMessage.peerId,
            lastMenuMessageId = botMessage.lastMenuMessageId,
            message = if (calc) Messages[user.settings.lang].menuDepositSelectAmountMessageCalc.format(currency.ticker)
            else Messages[user.settings.lang].menuDepositSelectAmountMessage.format(currency.ticker, available),
            keyboard = BotKeyboard {
                if (!calc) {
                    row {
                        button(
                            Messages[user.settings.lang].menuReceiptsSelectAmountMin + min,
                            ButtonPayload.serializer(),
                            ButtonPayload.MIN
                        )
                    }
                    row {
                        button(
                            Messages[user.settings.lang].menuReceiptsSelectAmountMax + available,
                            ButtonPayload.serializer(),
                            ButtonPayload.MAX
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

    override suspend fun handleMessage(bot: Bot, botMessage: BotMessage): Boolean {
        val payload = botMessage.payload
        val currency = CryptoCurrency.TGR
        val available = try {
            PostgresWalletPersistent.loadWalletState(user).active[currency] // TODO: Calc bot fee
        } catch (e: NegativeCoinsException) {
            return true
        }
        val min = Coins(currency, 2_500_000_000_000.toBigInteger())
        if (payload != null) {
            when (Json.decodeFromString<ButtonPayload>(payload)) {
                ButtonPayload.MIN -> {
                    user.setMenu(bot, DepositSelectPeriodMenu(user, min, false, this), botMessage)
                    return true
                }

                ButtonPayload.MAX -> {
                    user.setMenu(bot, DepositSelectPeriodMenu(user, available, false, this), botMessage)
                    return true
                }

                ButtonPayload.BACK -> {
                    user.setMenu(bot, parentMenu, botMessage)
                }
            }
        } else {
            if (isStringLong(botMessage.body)) {
                val count = (botMessage.body!!.toDouble() * getFactor(currency.decimals)).toLong().toBigInteger()
                val coins = Coins(currency, count)
                if (calc) {
                    user.setMenu(bot, DepositSelectPeriodMenu(user, coins, true, this), botMessage)
                    return true
                }
                if (count < min.amount || count > available.amount) {
                    return bot.sendPopup(botMessage, Messages[user.settings.lang].menuSelectInvalidAmount)
                }
                user.setMenu(bot, DepositSelectPeriodMenu(user, coins, false, this), botMessage)
                return true
            } else {
                bot.sendMessage(botMessage.peerId, Messages[user.settings.lang].menuSelectInvalidAmount)
                return false
            }
        }
        return true
    }

    private fun getFactor(decimals: Int): Long {
        val string = buildString {
            append("1")
            for (i in 1..decimals) {
                append("0")
            }
        }
        return string.toLong()
    }

    private fun isStringLong(s: String?): Boolean {
        if (s == null) return false
        return try {
            s.toDouble()
            true
        } catch (ex: NumberFormatException) {
            false
        }
    }

    @Serializable
    private enum class ButtonPayload {
        MIN,
        MAX,
        BACK
    }
}