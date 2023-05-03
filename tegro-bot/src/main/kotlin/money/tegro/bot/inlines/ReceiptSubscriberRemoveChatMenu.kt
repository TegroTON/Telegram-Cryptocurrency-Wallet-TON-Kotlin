package money.tegro.bot.inlines

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Chat
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.receipts.PostgresReceiptPersistent
import money.tegro.bot.receipts.Receipt
import money.tegro.bot.utils.button

@Serializable
data class ReceiptSubscriberRemoveChatMenu(
    val user: User,
    val receipt: Receipt,
    val chatId: Long,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        var chat = bot.getChat(chatId)
        if (chat == null) {
            chat = Chat(chatId, "Chat not found: $chatId", "null")
        }
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = Messages[user.settings.lang].menuReceiptSubscriberRemoveChatMessage.format(
                chat.title,
                chat.username
            ),
            keyboard = BotKeyboard {

                row {
                    button(
                        Messages[user.settings.lang].menuButtonApprove,
                        ButtonPayload.serializer(),
                        ButtonPayload.APPROVE
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

    override suspend fun handleMessage(bot: Bot, message: BotMessage): Boolean {
        val payload = message.payload ?: return false
        when (Json.decodeFromString<ButtonPayload>(payload)) {
            ButtonPayload.APPROVE -> {
                PostgresReceiptPersistent.deleteChatFromReceipt(receipt, chatId)
                user.setMenu(
                    bot,
                    ReceiptSubscriberMenu(
                        user,
                        receipt,
                        ReceiptReadyMenu(user, receipt, ReceiptsMenu(user, MainMenu(user)))
                    ),
                    message.lastMenuMessageId
                )
            }

            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, message.lastMenuMessageId)
            }
        }
        return true
    }

    @Serializable
    private enum class ButtonPayload {
        APPROVE,
        BACK
    }
}