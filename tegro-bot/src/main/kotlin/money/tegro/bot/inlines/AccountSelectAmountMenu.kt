package money.tegro.bot.inlines

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.objects.Account
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.PostgresAccountsPersistent
import money.tegro.bot.utils.button
import money.tegro.bot.wallet.Coins
import money.tegro.bot.wallet.CryptoCurrency
import java.util.*

@Serializable
class AccountSelectAmountMenu(
    val user: User,
    val activations: Int,
    val currency: CryptoCurrency,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, botMessage: BotMessage) {
        val min = Coins(currency, currency.minAmount)
        bot.updateKeyboard(
            to = botMessage.peerId,
            lastMenuMessageId = botMessage.lastMenuMessageId,
            message = Messages[user.settings.lang].menuAccountSelectAmountMessage.format(currency.ticker),
            keyboard = BotKeyboard {
                row {
                    button(
                        Messages[user.settings.lang].menuReceiptsSelectAmountMin + min,
                        ButtonPayload.serializer(),
                        ButtonPayload.MIN
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

    override suspend fun handleMessage(bot: Bot, botMessage: BotMessage): Boolean {
        val payload = botMessage.payload
        val min = Coins(currency, currency.minAmount)
        val zero = Coins(currency, 0.toBigInteger())
        if (payload != null) {
            when (Json.decodeFromString<ButtonPayload>(payload)) {
                ButtonPayload.BACK -> {
                    user.setMenu(bot, parentMenu, botMessage)
                }

                ButtonPayload.MIN -> {
                    val account = Account(
                        UUID.randomUUID(),
                        Clock.System.now(),
                        user,
                        activations == 0,
                        zero,
                        zero,
                        min,
                        activations,
                        true
                    )
                    PostgresAccountsPersistent.saveAccount(account)
                    user.setMenu(
                        bot,
                        AccountReadyMenu(user, account, AccountsMenu(user, MainMenu(user))),
                        botMessage
                    )
                }
            }
        } else {
            if (isStringLong(botMessage.body)) {
                val count = (botMessage.body!!.toDouble() * getFactor(currency.decimals)).toLong().toBigInteger()
                val coins = Coins(currency, count)
                if (count < min.amount) {
                    return bot.sendPopup(botMessage, Messages[user.settings.lang].menuSelectInvalidAmount)
                }
                val account = Account(
                    UUID.randomUUID(),
                    Clock.System.now(),
                    user,
                    activations == 1,
                    zero,
                    zero,
                    coins,
                    activations,
                    true
                )
                PostgresAccountsPersistent.saveAccount(account)
                user.setMenu(
                    bot,
                    AccountReadyMenu(user, account, AccountsMenu(user, MainMenu(user))),
                    botMessage
                )
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
        BACK
    }
}