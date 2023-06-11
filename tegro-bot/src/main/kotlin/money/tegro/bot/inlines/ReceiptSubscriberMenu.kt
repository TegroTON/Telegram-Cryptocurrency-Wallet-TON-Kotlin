package money.tegro.bot.inlines

import kotlinx.serialization.SerialName
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
data class ReceiptSubscriberMenu(
    val user: User,
    val receipt: Receipt,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, botMessage: BotMessage) {
        val limit = 5
        val chatIds = PostgresReceiptPersistent.getChatsByReceipt(receipt)
        val chats = emptyList<Chat>().toMutableList()
        if (chatIds.isNotEmpty()) {
            for (chatId: Long in chatIds) {
                val chat = bot.getChat(chatId)
                if (chat != null) {
                    chats.add(chat)
                } else {
                    chats.add(Chat(chatId, "Chat not found: $chatId", "null"))
                }
            }
        }
        bot.updateKeyboard(
            to = botMessage.peerId,
            lastMenuMessageId = botMessage.lastMenuMessageId,
            message = Messages[user.settings.lang].menuReceiptSubscriberMessage.format(limit.toString()),
            keyboard = BotKeyboard {
                if (chats.isNotEmpty()) {
                    for (chat: Chat in chats) {
                        row {
                            button(
                                Messages[user.settings.lang].menuReceiptSubscriberEntry.format(
                                    chat.title,
                                    chat.username
                                ),
                                ButtonPayload.serializer(),
                                ButtonPayload.Chat(chat.id)
                            )
                        }
                    }
                }
                if (chats.size < limit) {
                    row {
                        button(
                            Messages[user.settings.lang].menuReceiptSubscriberAdd,
                            ButtonPayload.serializer(),
                            ButtonPayload.Add
                        )
                    }
                }
                row {
                    button(
                        Messages[user.settings.lang].menuButtonBack,
                        ButtonPayload.serializer(),
                        ButtonPayload.Back
                    )
                }
            }
        )
    }

    override suspend fun handleMessage(bot: Bot, botMessage: BotMessage): Boolean {
        val payload = botMessage.payload ?: return false
        when (val payloadValue = Json.decodeFromString<ButtonPayload>(payload)) {
            is ButtonPayload.Add -> {
                user.setMenu(bot, ReceiptSubscriberAddChatMenu(user, receipt, this), botMessage)
            }

            is ButtonPayload.Back -> {
                user.setMenu(bot, parentMenu, botMessage)
            }

            is ButtonPayload.Chat -> {
                user.setMenu(
                    bot,
                    ReceiptSubscriberRemoveChatMenu(user, receipt, payloadValue.value, this),
                    botMessage
                )
            }
        }
        return true
    }

    @Serializable
    private sealed class ButtonPayload {
        @Serializable
        @SerialName("back")
        object Back : ButtonPayload()

        @Serializable
        @SerialName("add")
        object Add : ButtonPayload()

        @Serializable
        @SerialName("chat")
        data class Chat(val value: Long) : ButtonPayload()
    }
}