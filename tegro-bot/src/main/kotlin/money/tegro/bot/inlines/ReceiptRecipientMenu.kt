package money.tegro.bot.inlines

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.api.TgBot
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.PostgresUserPersistent
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.receipts.PostgresReceiptPersistent
import money.tegro.bot.receipts.Receipt
import money.tegro.bot.utils.button

@Serializable
data class ReceiptRecipientMenu(
    val user: User,
    val receipt: Receipt,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, botMessage: BotMessage) {
        bot.updateKeyboard(
            to = botMessage.peerId,
            lastMenuMessageId = botMessage.lastMenuMessageId,
            message = if (receipt.recipient == null) Messages[user.settings.lang].menuReceiptRecipientMessage else Messages[user.settings.lang].menuReceiptRecipientSetMessage,
            keyboard = BotKeyboard {
                if (receipt.recipient != null) {
                    row {
                        button(
                            Messages[user.settings.lang].menuReceiptRecipientUnattach,
                            ButtonPayload.serializer(),
                            ButtonPayload.UNATTACH
                        )
                    }
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
        if (botMessage.payload != null) {
            val payload = botMessage.payload
            when (Json.decodeFromString<ButtonPayload>(payload)) {
                ButtonPayload.UNATTACH -> {
                    val newReceipt = receipt.copy(recipient = null)
                    PostgresReceiptPersistent.saveReceipt(newReceipt)
                    user.setMenu(
                        bot,
                        ReceiptReadyMenu(user, newReceipt, ReceiptsMenu(user, MainMenu(user))),
                        botMessage
                    )
                }

                ButtonPayload.BACK -> {
                    user.setMenu(bot, parentMenu, botMessage)
                }
            }
        } else if (botMessage.forwardMessages.isNotEmpty()) {
            val newRecipient: User? = if (bot is TgBot) {
                PostgresUserPersistent.loadByTg(botMessage.forwardMessages[0].userId)
            } else {
                PostgresUserPersistent.loadByVk(botMessage.forwardMessages[0].userId)

            }
            if (newRecipient == null) {
                bot.sendMessage(botMessage.peerId, Messages[user.settings.lang].menuReceiptRecipientNotRegistered)
                user.setMenu(bot, parentMenu, botMessage)
                return true
            }
            val newReceipt = receipt.copy(recipient = newRecipient)
            PostgresReceiptPersistent.saveReceipt(newReceipt)
            user.setMenu(
                bot,
                ReceiptReadyMenu(user, newReceipt, ReceiptsMenu(user, MainMenu(user))),
                botMessage
            )
        } else {
            //TODO get id from mention
            bot.sendMessage(botMessage.peerId, Messages[user.settings.lang].menuReceiptRecipientNotFound)
            user.setMenu(bot, parentMenu, botMessage)
            return true
        }
        return true
    }

    @Serializable
    private enum class ButtonPayload {
        UNATTACH,
        BACK
    }
}