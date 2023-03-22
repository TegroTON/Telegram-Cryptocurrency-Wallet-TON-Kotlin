package bot.inlines

import bot.api.Bot
import bot.objects.BotMessage
import bot.objects.MessagesContainer
import bot.objects.User
import bot.objects.keyboard.BotKeyboard
import bot.receipts.PostgresReceiptPersistent
import bot.utils.button
import bot.wallet.Coins
import bot.wallet.PostgresWalletPersistent
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
class ReceiptSelectActivationsMenu(
    val user: User,
    val coins: Coins,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        val maxCoins = PostgresWalletPersistent.loadWalletState(user).active[coins.currency].amount
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = MessagesContainer[user.settings.lang].menuReceiptsSelectActivationsMessage,
            keyboard = BotKeyboard {
                row {
                    //skip
                    button(
                        MessagesContainer[user.settings.lang].menuReceiptsSelectActivationsSkip,
                        ButtonPayload.serializer(),
                        ButtonPayload.SKIP
                    )
                }
                row {
                    val maxActivations = maxCoins / coins.amount
                    button(
                        String.format(
                            MessagesContainer[user.settings.lang].menuReceiptsSelectActivationsMax,
                            maxActivations.toString()
                        ),
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
        val maxCoins = PostgresWalletPersistent.loadWalletState(user).active[coins.currency].amount
        val maxActivations = maxCoins / coins.amount
        when (Json.decodeFromString<ButtonPayload>(payload)) {
            ButtonPayload.SKIP -> user.setMenu(
                bot,
                ReceiptReadyMenu(
                    user,
                    PostgresReceiptPersistent.createReceipt(user, coins, 1),
                    ReceiptsMenu(user, MainMenu(user))
                ),
                message.lastMenuMessageId
            )

            ButtonPayload.MAX -> user.setMenu(
                bot,
                ReceiptReadyMenu(
                    user,
                    PostgresReceiptPersistent.createReceipt(user, coins, maxActivations.toInt()),
                    ReceiptsMenu(user, MainMenu(user))
                ),
                message.lastMenuMessageId
            )

            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, message.lastMenuMessageId)
            }
        }
        return true
    }

    @Serializable
    enum class ButtonPayload {
        SKIP,
        MAX,
        BACK
    }
}