package bot.inlines

import bot.api.Bot
import bot.objects.BotMessage
import bot.objects.MessagesContainer
import bot.objects.User
import bot.objects.keyboard.BotKeyboard
import bot.utils.button
import bot.wallet.Coins
import bot.wallet.CryptoCurrency
import bot.wallet.PostgresWalletPersistent
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
class ReceiptSelectAmountMenu(
    val user: User,
    val currency: CryptoCurrency,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        val avalible = PostgresWalletPersistent.loadWalletState(user).active[currency]
        val min = Coins(currency, currency.minAmount.toBigInteger())
        if (avalible < min) {
            bot.updateKeyboard(
                to = user.vkId ?: user.tgId ?: 0,
                lastMenuMessageId = lastMenuMessageId,
                message = String.format(
                    MessagesContainer[user.settings.lang].menuReceiptsSelectAmountNoMoney,
                    min,
                    avalible
                ),
                keyboard = BotKeyboard {
                    row {
                        button(
                            MessagesContainer[user.settings.lang].menuButtonBack,
                            ButtonPayload.serializer(),
                            ButtonPayload.BACK
                        )
                    }
                }
            )
            return
        }
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = String.format(
                MessagesContainer[user.settings.lang].menuReceiptsSelectAmountMessage,
                currency.ticker,
                avalible
            ),
            keyboard = BotKeyboard {
                row {
                    button(
                        MessagesContainer[user.settings.lang].menuReceiptsSelectAmountMin + min,
                        ButtonPayload.serializer(),
                        ButtonPayload.MIN
                    )
                }
                row {
                    button(
                        MessagesContainer[user.settings.lang].menuReceiptsSelectAmountMax + avalible,
                        ButtonPayload.serializer(),
                        ButtonPayload.MAX
                    )
                }
                row {
                    button(
                        MessagesContainer[user.settings.lang].menuButtonBack,
                        ButtonPayload.serializer(),
                        ButtonPayload.BACK
                    )
                }
            }
        )
    }

    override suspend fun handleMessage(bot: Bot, message: BotMessage): Boolean {
        val payload = message.payload
        val available = PostgresWalletPersistent.loadWalletState(user).active[currency]
        val min = Coins(currency, currency.minAmount.toBigInteger())
        if (payload != null) {
            when (Json.decodeFromString<ButtonPayload>(payload)) {
                ButtonPayload.MIN -> {
                    user.setMenu(bot, ReceiptSelectActivationsMenu(user, min, this), message.lastMenuMessageId)
                }

                ButtonPayload.MAX -> {
                    user.setMenu(bot, ReceiptSelectActivationsMenu(user, available, this), message.lastMenuMessageId)
                }

                ButtonPayload.BACK -> {
                    user.setMenu(bot, parentMenu, message.lastMenuMessageId)
                }
            }
        } else {
            if (isStringLong(message.body)) {
                val count = (message.body!!.toDouble() * getFactor(currency.decimals)).toLong().toBigInteger()
                val coins = Coins(currency, count)
                if (count < min.amount || count > available.amount) {
                    bot.sendMessage(message.peerId, MessagesContainer[user.settings.lang].menuSelectInvalidAmount)
                    return false
                }
                user.setMenu(bot, ReceiptSelectActivationsMenu(user, coins, this), message.lastMenuMessageId)
                return true
            } else {
                bot.sendMessage(message.peerId, MessagesContainer[user.settings.lang].menuSelectInvalidAmount)
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
    enum class ButtonPayload {
        MIN,
        MAX,
        BACK
    }
}