package money.tegro.bot.inlines

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.objects.*
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.receipts.PostgresReceiptPersistent
import money.tegro.bot.receipts.Receipt
import money.tegro.bot.utils.button
import java.util.*

@Serializable
data class ReceiptRecipientMenu(
    val user: User,
    val receipt: Receipt,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = if (receipt.recipient == null) MessagesContainer[user.settings.lang].menuReceiptRecipientMessage else MessagesContainer[user.settings.lang].menuReceiptRecipientSetMessage,
            keyboard = BotKeyboard {
                if (receipt.recipient != null) {
                    row {
                        button(
                            MessagesContainer[user.settings.lang].menuReceiptRecipientUnattach,
                            ButtonPayload.serializer(),
                            ButtonPayload.UNATTACH
                        )
                    }
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
        if (message.payload != null) {
            val payload = message.payload
            when (Json.decodeFromString<ButtonPayload>(payload)) {
                ButtonPayload.UNATTACH -> {
                    receipt.recipient = null
                    PostgresReceiptPersistent.saveReceipt(receipt)
                    user.setMenu(bot, parentMenu, message.lastMenuMessageId)
                }

                ButtonPayload.BACK -> {
                    user.setMenu(bot, parentMenu, message.lastMenuMessageId)
                }
            }
        } else if (message.forwardMessages.isNotEmpty()) {
            receipt.recipient = User(
                UUID.randomUUID(), message.forwardMessages[0].userId, null, UserSettings(
                    UUID.randomUUID(),
                    Language.RU,
                    LocalCurrency.RUB,
                    null
                )
            )
        } else {
            //TODO get id from mention
        }
        return true
    }

    @Serializable
    enum class ButtonPayload {
        UNATTACH,
        BACK
    }
}