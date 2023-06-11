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
import money.tegro.bot.receipts.Receipt
import money.tegro.bot.utils.button

@Serializable
data class ReceiptSubscriberAddChatMenu(
    val user: User,
    val receipt: Receipt,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, botMessage: BotMessage) {
        bot.updateKeyboard(
            to = botMessage.peerId,
            lastMenuMessageId = botMessage.lastMenuMessageId,
            message = Messages[user.settings.lang].menuReceiptSubscriberAddChatMessage,
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
    }

    override suspend fun handleMessage(bot: Bot, botMessage: BotMessage): Boolean {
        if (botMessage.payload != null) {
            val payload = botMessage.payload
            when (Json.decodeFromString<ButtonPayload>(payload)) {
                ButtonPayload.BACK -> {
                    user.setMenu(bot, parentMenu, botMessage)
                }
            }
        } else if (botMessage.forwardMessages.isNotEmpty()) {
            val chatId = botMessage.forwardMessages[0].userId
            if (chatId > 0) {
                bot.sendMessage(botMessage.peerId, Messages[user.settings.lang].menuReceiptSubscriberAddChatError)
                user.setMenu(bot, parentMenu, botMessage)
                return true
            }

            val chat = bot.getChat(chatId)
            if (chat == null) {
                bot.sendMessage(botMessage.peerId, Messages[user.settings.lang].menuReceiptSubscriberAddChatError)
                user.setMenu(bot, parentMenu, botMessage)
                return true
            }

            val chatIds = PostgresReceiptPersistent.getChatsByReceipt(receipt)
            if (!chatIds.contains(chatId)) {
                PostgresReceiptPersistent.addChatToReceipt(receipt, chatId)
            }
            user.setMenu(
                bot,
                ReceiptSubscriberMenu(
                    user,
                    receipt,
                    ReceiptReadyMenu(user, receipt, ReceiptsMenu(user, MainMenu(user)))
                ),
                botMessage
            )
        } else {
            bot.sendMessage(botMessage.peerId, Messages[user.settings.lang].menuReceiptSubscriberAddChatError)
            user.setMenu(bot, parentMenu, botMessage)
            return true
        }
        return true
    }

    @Serializable
    private enum class ButtonPayload {
        BACK
    }
}