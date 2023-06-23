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
class ReceiptSetDescriptionMenu(
    val user: User,
    val receipt: Receipt,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, botMessage: BotMessage) {
        bot.updateKeyboard(
            to = botMessage.peerId,
            lastMenuMessageId = botMessage.lastMenuMessageId,
            message = Messages[user].menuReceiptSetDescriptionMenu,
            keyboard = BotKeyboard {
                if (receipt.description.isNotEmpty()) {
                    row {
                        button(
                            Messages[user.settings.lang].menuReceiptSetDescriptionClear,
                            ButtonPayload.serializer(),
                            ButtonPayload.CLEAR
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
        val payload = botMessage.payload
        if (payload != null) {
            when (Json.decodeFromString<ButtonPayload>(payload)) {
                ButtonPayload.CLEAR -> {
                    val newReceipt = receipt.copy(description = "")
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
        } else {
            if (!botMessage.body.isNullOrEmpty()) {
                val text = botMessage.body
                if (text.length > 1024) {
                    bot.sendMessage(botMessage.peerId, Messages[user.settings.lang].invalidText)
                    return false
                }
                val newReceipt = receipt.copy(description = text)
                PostgresReceiptPersistent.saveReceipt(newReceipt)
                user.setMenu(
                    bot,
                    ReceiptReadyMenu(user, newReceipt, ReceiptsMenu(user, MainMenu(user))),
                    botMessage
                )
                return true
            } else {
                bot.sendMessage(botMessage.peerId, Messages[user.settings.lang].invalidText)
                return false
            }
        }
        return true
    }

    @Serializable
    private enum class ButtonPayload {
        CLEAR,
        BACK
    }
}
