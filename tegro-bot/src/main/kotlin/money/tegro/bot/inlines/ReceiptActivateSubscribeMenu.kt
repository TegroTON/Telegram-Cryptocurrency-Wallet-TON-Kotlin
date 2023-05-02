package money.tegro.bot.inlines

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.api.TgBot
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Chat
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.receipts.PostgresReceiptPersistent
import money.tegro.bot.receipts.Receipt
import money.tegro.bot.utils.button
import money.tegro.bot.utils.linkButton

@Serializable
data class ReceiptActivateSubscribeMenu(
    val user: User,
    val receipt: Receipt,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        val code = receipt.id.toString()
        val tgLink = String.format("t.me/%s?start=RC-%s", System.getenv("TG_USER_NAME"), code)
        val chatIds = PostgresReceiptPersistent.getChatsByReceipt(receipt)
        val chats = emptyList<Chat>().toMutableList()
        if (chatIds.isNotEmpty()) {
            for (chatId: Long in chatIds) {
                val chat = bot.getChat(chatId)
                chats.add(chat)
            }
        }
        val subscribed = "✅"
        val unsubscribed = "❌"
        val map = emptyMap<Chat, String>().toMutableMap()
        for (chat: Chat in chats) {
            map[chat] = if (bot.isUserInChat(
                    chat.id,
                    (if (bot is TgBot) user.tgId else user.vkId) ?: 0
                )
            ) subscribed else unsubscribed
        }
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = Messages[user.settings.lang].menuReceiptActivateSubscribeMessage,
            keyboard = BotKeyboard {
                if (chats.isNotEmpty()) {
                    for ((chat, subscription) in map) {
                        row {
                            linkButton(
                                Messages[user.settings.lang].menuReceiptActivateSubscribeEntry.format(
                                    subscription,
                                    chat.title
                                ),
                                "https://t.me/" + chat.username,
                                ButtonPayload.serializer(),
                                ButtonPayload.Back
                            )
                        }
                    }
                }
                row {
                    linkButton(
                        Messages[user.settings.lang].menuButtonCheck,
                        tgLink,
                        ButtonPayload.serializer(),
                        ButtonPayload.Back
                    )
                }
                row {
                    button(
                        Messages[user.settings.lang].menuButtonMenu,
                        ButtonPayload.serializer(),
                        ButtonPayload.Back
                    )
                }
            }
        )
    }

    override suspend fun handleMessage(bot: Bot, message: BotMessage): Boolean {
        val payload = message.payload ?: return false
        when (Json.decodeFromString<ButtonPayload>(payload)) {
            is ButtonPayload.Back -> {
                user.setMenu(bot, parentMenu, message.lastMenuMessageId)
            }
        }
        return true
    }

    @Serializable
    private sealed class ButtonPayload {
        @Serializable
        @SerialName("back")
        object Back : ButtonPayload()
    }
}