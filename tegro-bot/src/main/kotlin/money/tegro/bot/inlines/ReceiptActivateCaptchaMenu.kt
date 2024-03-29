package money.tegro.bot.inlines

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.api.TgBot
import money.tegro.bot.exceptions.*
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.receipts.PostgresReceiptPersistent
import money.tegro.bot.receipts.Receipt
import money.tegro.bot.utils.button

@Serializable
data class ReceiptActivateCaptchaMenu(
    val user: User,
    val receipt: Receipt,
    val answer: String,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, botMessage: BotMessage) {
        if (answer == "") {
            val id = buildString {
                if (bot is TgBot) append("<code>")
                append("#")
                append(receipt.id.toString())
                if (bot is TgBot) append("</code>")
            }
            bot.updateKeyboard(
                to = botMessage.peerId,
                lastMenuMessageId = botMessage.lastMenuMessageId,
                message = Messages[user].receiptActivateMessage.format(id, receipt.coins),
                keyboard = BotKeyboard {
                    row {
                        button(
                            Messages[user].receiptActivate,
                            ButtonPayload.serializer(),
                            ButtonPayload.Activate
                        )
                    }
                    row {
                        button(
                            Messages[user].menuButtonMenu,
                            ButtonPayload.serializer(),
                            ButtonPayload.Back
                        )
                    }
                }
            )
        }
    }

    override suspend fun handleMessage(bot: Bot, botMessage: BotMessage): Boolean {
        val payload = botMessage.payload
        if (payload != null) {
            when (Json.decodeFromString<ButtonPayload>(payload)) {
                is ButtonPayload.Activate -> {
                    activate(bot, user, botMessage)
                    return false
                }

                is ButtonPayload.Back -> {
                    user.setMenu(bot, parentMenu, botMessage)
                }
            }
        } else {
            if (botMessage.body != null && botMessage.body == answer) {
                activate(bot, user, botMessage)
                return false
            }
            bot.sendMessage(botMessage.peerId, Messages[user].receiptCaptchaIncorrect)
        }
        return true
    }

    private suspend fun activate(bot: Bot, user: User, botMessage: BotMessage) {
        val lang = Messages[user.settings.lang]
        val id = buildString {
            if (bot is TgBot) append("<code>")
            append("#")
            append(receipt.id.toString())
            if (bot is TgBot) append("</code>")
        }
        val result = buildString {
            try {
                PostgresReceiptPersistent.activateReceipt(receipt, user)
                append(lang.receiptMoneyReceived.format(receipt.coins))
                val issuer = receipt.issuer
                if (receipt.activations > 1) {
                    val updatedReceipt = PostgresReceiptPersistent.loadReceipt(receipt.id)
                    if (updatedReceipt != null)
                        bot.sendMessage(
                            issuer.tgId ?: issuer.vkId ?: 0,
                            Messages[issuer.settings.lang].multireceiptActivated.format(
                                updatedReceipt.coins,
                                updatedReceipt.activations
                            )
                        )
                } else {
                    bot.sendMessage(
                        issuer.tgId ?: issuer.vkId ?: 0,
                        Messages[issuer.settings.lang].receiptActivated.format(receipt.coins)
                    )
                }
            } catch (ex: ReceiptIssuerActivationException) {
                append(lang.receiptIssuerActivationException)
            } catch (ex: InvalidRecipientException) {
                append(lang.illegalRecipientException.format(id))
            } catch (ex: ReceiptNotActiveException) {
                append(lang.receiptNotActiveException.format(id))
            } catch (ex: ReceiptNotNewUserException) {
                append(lang.notNewRecipientException.format(id))
            } catch (ex: RecipientNotSubscriberException) {
                append(lang.recipientNotSubscriberException.format(ex.chatName))
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        bot.sendMessage(botMessage.peerId, result)
    }

    @Serializable
    private sealed class ButtonPayload {
        @Serializable
        @SerialName("activate")
        object Activate : ButtonPayload()

        @Serializable
        @SerialName("back")
        object Back : ButtonPayload()
    }
}