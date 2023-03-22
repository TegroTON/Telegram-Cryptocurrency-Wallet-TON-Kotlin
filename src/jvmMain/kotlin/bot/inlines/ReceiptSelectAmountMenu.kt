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
        val payload = message.payload ?: return false
        val avalible = PostgresWalletPersistent.loadWalletState(user).active[currency]
        when (Json.decodeFromString<ButtonPayload>(payload)) {
            ButtonPayload.MIN -> {
                val min = Coins(currency, currency.minAmount.toBigInteger())
                user.setMenu(bot, ReceiptSelectActivationsMenu(user, min, this), message.lastMenuMessageId)
            }

            ButtonPayload.MAX -> {
                user.setMenu(bot, ReceiptSelectActivationsMenu(user, avalible, this), message.lastMenuMessageId)
            }

            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, message.lastMenuMessageId)
            }
        }
        return true
    }

    @Serializable
    enum class ButtonPayload {
        MIN,
        MAX,
        BACK
    }
}