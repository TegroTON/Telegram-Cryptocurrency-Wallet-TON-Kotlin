package money.tegro.bot.inlines

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.api.TgBot
import money.tegro.bot.objects.Account
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.button
import money.tegro.bot.wallet.Coins
import money.tegro.bot.wallet.PostgresWalletPersistent

@Serializable
class AccountPaySelectAmountMenu(
    val user: User,
    val account: Account,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, botMessage: BotMessage) {
        val min = account.minAmount
        val max = if (account.maxCoins.amount > 0.toBigInteger()) account.maxCoins - account.coins else account.maxCoins
        val available = PostgresWalletPersistent.loadWalletState(user).active[account.coins.currency]
        val id = buildString {
            if (bot is TgBot) append("<code>")
            append("#")
            append(account.id.toString())
            if (bot is TgBot) append("</code>")
        }
        val notSet = buildString {
            if (bot is TgBot) append("<i>")
            append(Messages[user.settings.lang].notSet)
            if (bot is TgBot) append("</i>")
        }
        bot.updateKeyboard(
            to = botMessage.peerId,
            lastMenuMessageId = botMessage.lastMenuMessageId,
            message = Messages[user.settings.lang].menuAccountPaySelectAmountMessage.format(
                id,
                if (min.amount > 0.toBigInteger()) min else notSet,
                if (max.amount > 0.toBigInteger()) max else notSet,
                available
            ),
            keyboard = BotKeyboard {
                row {
                    if (min.amount > 0.toBigInteger()) {
                        button(
                            Messages[user.settings.lang].menuReceiptsSelectAmountMin + min,
                            ButtonPayload.serializer(),
                            ButtonPayload.MIN
                        )
                    }
                    if (max.amount > 0.toBigInteger()) {
                        button(
                            Messages[user.settings.lang].menuReceiptsSelectAmountMax + max,
                            ButtonPayload.serializer(),
                            ButtonPayload.MAX
                        )
                    }
                }
                row {
                    button(
                        Messages[user.settings.lang].menuAccountPayDeclineButton,
                        ButtonPayload.serializer(),
                        ButtonPayload.DECLINE
                    )
                }
            }
        )
    }

    override suspend fun handleMessage(bot: Bot, botMessage: BotMessage): Boolean {
        val payload = botMessage.payload
        val min = account.minAmount
        val max = if (account.maxCoins.amount > 0.toBigInteger()) account.maxCoins - account.coins else account.maxCoins
        val available = PostgresWalletPersistent.loadWalletState(user).active[account.coins.currency]
        if (payload != null) {
            when (Json.decodeFromString<ButtonPayload>(payload)) {

                ButtonPayload.DECLINE -> {
                    user.setMenu(bot, MainMenu(user), botMessage)
                }

                ButtonPayload.MIN -> {
                    user.setMenu(
                        bot,
                        AccountPayMenu(user, account, min, this),
                        botMessage
                    )
                }

                ButtonPayload.MAX -> {
                    if (max.amount > 0.toBigInteger() && max > available) {
                        return bot.sendPopup(
                            botMessage,
                            Messages[user.settings.lang].menuReceiptsSelectAmountNoMoney.format(max, available)
                        )
                    }
                    user.setMenu(
                        bot,
                        AccountPayMenu(user, account, max, this),
                        botMessage
                    )
                }
            }
        } else {
            if (isStringLong(botMessage.body)) {
                val currency = account.coins.currency
                val count = (botMessage.body!!.toDouble() * getFactor(currency.decimals)).toLong().toBigInteger()
                val coins = Coins(currency, count)
                if (count > available.amount) {
                    bot.sendMessage(
                        botMessage.peerId, Messages[user.settings.lang].menuReceiptsSelectAmountNoMoney.format(
                            coins,
                            available
                        )
                    )
                    return false
                }
                if (count < min.amount) {
                    bot.sendMessage(
                        botMessage.peerId, Messages[user.settings.lang].menuAccountPaySelectAmountErrorMinAmount.format(
                            min,
                            coins
                        )
                    )
                    return false
                }
                if (max.amount > 0.toBigInteger() && count > account.maxCoins.amount) {
                    bot.sendMessage(
                        botMessage.peerId, Messages[user.settings.lang].menuAccountPaySelectAmountErrorMaxCoins.format(
                            account.maxCoins,
                            coins
                        )
                    )
                    return false
                }
                user.setMenu(
                    bot,
                    AccountPayMenu(user, account, coins, MainMenu(user)),
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
        MAX,
        DECLINE
    }
}