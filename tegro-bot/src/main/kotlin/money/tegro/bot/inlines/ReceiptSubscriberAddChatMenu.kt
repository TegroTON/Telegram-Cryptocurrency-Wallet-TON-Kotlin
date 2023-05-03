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
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
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

    override suspend fun handleMessage(bot: Bot, message: BotMessage): Boolean {
        if (message.payload != null) {
            val payload = message.payload
            when (Json.decodeFromString<ButtonPayload>(payload)) {
                ButtonPayload.BACK -> {
                    user.setMenu(bot, parentMenu, message.lastMenuMessageId)
                }
            }
        } else if (message.forwardMessages.isNotEmpty()) {
            val chatId = message.forwardMessages[0].userId
            if (chatId > 0) {
                bot.sendMessage(message.peerId, Messages[user.settings.lang].menuReceiptSubscriberAddChatError)
                user.setMenu(bot, parentMenu, message.lastMenuMessageId)
                return true
            }

            val chat = bot.getChat(chatId)
            if (chat == null) {
                bot.sendMessage(message.peerId, Messages[user.settings.lang].menuReceiptSubscriberAddChatError)
                user.setMenu(bot, parentMenu, message.lastMenuMessageId)
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
                    ReceiptLimitationsMenu(
                        user,
                        receipt,
                        ReceiptReadyMenu(user, receipt, ReceiptsMenu(user, MainMenu(user)))
                    )
                ),
                message.lastMenuMessageId
            )
        } else {
            bot.sendMessage(message.peerId, Messages[user.settings.lang].menuReceiptSubscriberAddChatError)
            user.setMenu(bot, parentMenu, message.lastMenuMessageId)
            return true
        }
        return true
    }

    @Serializable
    private enum class ButtonPayload {
        BACK
    }
}