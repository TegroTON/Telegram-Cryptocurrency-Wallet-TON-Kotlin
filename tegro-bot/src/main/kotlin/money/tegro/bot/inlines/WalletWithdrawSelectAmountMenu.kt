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
import money.tegro.bot.utils.NftsPersistent
import money.tegro.bot.utils.button
import money.tegro.bot.wallet.BlockchainType
import money.tegro.bot.wallet.Coins
import money.tegro.bot.wallet.CryptoCurrency
import money.tegro.bot.wallet.PostgresWalletPersistent

@Serializable
class WalletWithdrawSelectAmountMenu(
    val user: User,
    val currency: CryptoCurrency,
    val network: BlockchainType,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, botMessage: BotMessage) {
        val fee = Coins(currency, NftsPersistent.countBotFee(user, currency))
        val balance = PostgresWalletPersistent.loadWalletState(user).active[currency]
        val min = Coins(currency, currency.minAmount)
        val minAvailable = Coins(currency, currency.minAmount) + fee
        if (balance < minAvailable) {
            bot.updateKeyboard(
                to = botMessage.peerId,
                lastMenuMessageId = botMessage.lastMenuMessageId,
                message = String.format(
                    Messages[user.settings.lang].menuReceiptsSelectAmountNoMoney,
                    minAvailable,
                    balance
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
        val available = balance - fee
        bot.updateKeyboard(
            to = botMessage.peerId,
            lastMenuMessageId = botMessage.lastMenuMessageId,
            message = String.format(
                Messages[user.settings.lang].menuWalletWithdrawSelectAmountMessage,
                currency.ticker,
                fee,
                balance,
                available
            ),
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
                        Messages[user.settings.lang].menuReceiptsSelectAmountMax + available,
                        ButtonPayload.serializer(),
                        ButtonPayload.MAX
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
        val fee = Coins(currency, NftsPersistent.countBotFee(user, currency))
        val available = try {
            PostgresWalletPersistent.loadWalletState(user).active[currency] - fee
        } catch (e: NegativeCoinsException) {
            return false
        }
        val min = Coins(currency, currency.minAmount)
        if (payload != null) {
            when (Json.decodeFromString<ButtonPayload>(payload)) {
                ButtonPayload.MIN -> {
                    user.setMenu(
                        bot,
                        WalletWithdrawSelectAddressMenu(user, network, min, this),
                        botMessage
                    )
                }

                ButtonPayload.MAX -> {
                    user.setMenu(
                        bot,
                        WalletWithdrawSelectAddressMenu(user, network, available, this),
                        botMessage
                    )
                }

                ButtonPayload.BACK -> {
                    user.setMenu(bot, parentMenu, botMessage)
                }
            }
        } else {
            if (isStringLong(botMessage.body)) {
                val count = (botMessage.body!!.toDouble() * getFactor(currency.decimals)).toLong().toBigInteger()
                val coins = Coins(currency, count)
                if (count < min.amount || count > available.amount) {
                    bot.sendMessage(botMessage.peerId, Messages[user.settings.lang].menuSelectInvalidAmount)
                    return false
                }
                user.setMenu(
                    bot,
                    WalletWithdrawSelectAddressMenu(user, network, coins, this),
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
        BACK
    }
}