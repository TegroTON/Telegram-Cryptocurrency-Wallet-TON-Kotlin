package money.tegro.bot.inlines

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.receipts.PostgresReceiptPersistent
import money.tegro.bot.utils.button
import money.tegro.bot.wallet.Coins
import money.tegro.bot.wallet.PostgresWalletPersistent

@Serializable
class ReceiptSelectActivationsMenu(
    val user: User,
    val coins: Coins,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, botMessage: BotMessage) {
        val maxCoins = PostgresWalletPersistent.loadWalletState(user).active[coins.currency].amount
        bot.updateKeyboard(
            to = botMessage.peerId,
            lastMenuMessageId = botMessage.lastMenuMessageId,
            message = Messages[user.settings.lang].menuReceiptsSelectActivationsMessage,
            keyboard = BotKeyboard {
                row {
                    button(
                        Messages[user.settings.lang].menuReceiptsSelectActivationsSkip,
                        ButtonPayload.serializer(),
                        ButtonPayload.SKIP
                    )
                }
                row {
                    val maxActivations = maxCoins / coins.amount
                    button(
                        String.format(
                            Messages[user.settings.lang].menuReceiptsSelectActivationsMax,
                            maxActivations.toString()
                        ),
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
        val maxCoins = PostgresWalletPersistent.loadWalletState(user).active[coins.currency].amount
        val maxActivations = (maxCoins / coins.amount).toInt()
        if (payload != null) {
            when (Json.decodeFromString<ButtonPayload>(payload)) {
                ButtonPayload.SKIP -> user.setMenu(
                    bot,
                    ReceiptReadyMenu(
                        user,
                        PostgresReceiptPersistent.createReceipt(user, coins, 1),
                        ReceiptsMenu(user, MainMenu(user))
                    ),
                    botMessage
                )

                ButtonPayload.MAX -> user.setMenu(
                    bot,
                    ReceiptReadyMenu(
                        user,
                        PostgresReceiptPersistent.createReceipt(user, coins, maxActivations),
                        ReceiptsMenu(user, MainMenu(user))
                    ),
                    botMessage
                )

                ButtonPayload.BACK -> {
                    user.setMenu(bot, parentMenu, botMessage)
                }
            }
        } else {
            if (isStringInt(botMessage.body)) {
                val count = botMessage.body!!.toInt()
                if (count > maxActivations) {
                    bot.sendMessage(botMessage.peerId, Messages[user.settings.lang].menuSelectInvalidAmount)
                    return false
                }
                user.setMenu(
                    bot,
                    ReceiptReadyMenu(
                        user,
                        PostgresReceiptPersistent.createReceipt(user, coins, count),
                        ReceiptsMenu(user, MainMenu(user))
                    ),
                    botMessage
                )
            } else {
                bot.sendMessage(botMessage.peerId, Messages[user.settings.lang].menuSelectInvalidAmount)
                return false
            }
        }
        return true
    }

    private fun isStringInt(s: String?): Boolean {
        if (s == null) return false
        return try {
            s.toInt()
            true
        } catch (ex: NumberFormatException) {
            false
        }
    }

    @Serializable
    private enum class ButtonPayload {
        SKIP,
        MAX,
        BACK
    }
}