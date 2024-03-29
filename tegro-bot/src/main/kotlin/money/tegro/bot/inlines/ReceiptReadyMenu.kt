package money.tegro.bot.inlines

import io.github.g0dkar.qrcode.QRCode
import io.github.g0dkar.qrcode.render.Colors
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.api.TgBot
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.receipts.PostgresReceiptPersistent
import money.tegro.bot.receipts.Receipt
import money.tegro.bot.utils.button
import money.tegro.bot.utils.inlineButton
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class ReceiptReadyMenu(
    val user: User,
    val receipt: Receipt,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, botMessage: BotMessage) {
        bot.updateKeyboard(
            to = botMessage.peerId,
            lastMenuMessageId = botMessage.lastMenuMessageId,
            message = getBody(bot),
            keyboard = getKeyboard()
        )
    }

    private suspend fun getBody(bot: Bot): String {
        val code = receipt.id.toString()
        val tgLink = String.format("t.me/%s?start=RC-%s", System.getenv("TG_USER_NAME"), code)
        val vkLink = String.format("https://vk.com/write-%s?ref=RC-%s", System.getenv("VK_GROUP_ID"), code)
        val date = Date.from(receipt.issueTime.toJavaInstant())
        val time =
            SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(date)
        val chatIds = PostgresReceiptPersistent.getChatsByReceipt(receipt)
        val recipient = receipt.recipient
        val limitation: String = if (chatIds.isNotEmpty() || recipient != null) {
            if (chatIds.isNotEmpty() && recipient != null) {
                Messages[user.settings.lang].menuReceiptReadySubscriberRecipient
            } else if (chatIds.isNotEmpty()) {
                Messages[user.settings.lang].menuReceiptReadySubscriber
            } else {
                Messages[user.settings.lang].menuReceiptReadyRecipient
            }
        } else {
            Messages[user.settings.lang].menuReceiptReadyAnyone
        }
        val description = if (receipt.description.isEmpty()) Messages[user].notSet else receipt.description
        return Messages[user.settings.lang].menuReceiptReadyMessage.format(
            receipt.coins.toStringWithRate(user.settings.localCurrency),
            receipt.activations,
            time,
            limitation,
            description,
            if (bot is TgBot) tgLink else vkLink
        )
    }

    private fun getKeyboard(): BotKeyboard {
        val code = receipt.id.toString()
        val captchaActivation = if (receipt.captcha) Messages[user].enabled else Messages[user].disabled
        val onlyNewActivation = if (receipt.onlyNew) Messages[user].enabled else Messages[user].disabled
        val onlyPremiumActivation = if (receipt.onlyPremium) Messages[user].enabled else Messages[user].disabled
        return BotKeyboard {
            row {
                inlineButton(
                    Messages[user].menuReceiptReadyShare,
                    "RC-$code",
                    ButtonPayload.serializer(),
                    ButtonPayload.SHARE
                )
                button(
                    Messages[user].menuReceiptReadyQr,
                    ButtonPayload.serializer(),
                    ButtonPayload.QR
                )
            }
            row {
                button(
                    if (receipt.recipient == null)
                        Messages[user].menuReceiptLimitationsUser
                    else
                        Messages[user].menuReceiptLimitationsUserUnattach,
                    ButtonPayload.serializer(),
                    ButtonPayload.USER
                )
            }
            row {
                button(
                    Messages[user].menuReceiptLimitationsRef,
                    ButtonPayload.serializer(),
                    ButtonPayload.REF
                )
                button(
                    Messages[user].menuReceiptLimitationsSub,
                    ButtonPayload.serializer(),
                    ButtonPayload.SUB
                )
            }
            row {
                button(
                    Messages[user].menuReceiptLimitationsDesc,
                    ButtonPayload.serializer(),
                    ButtonPayload.DESC
                )
            }
            row {
                button(
                    Messages[user].menuReceiptLimitationsOnlyNew.format(onlyNewActivation),
                    ButtonPayload.serializer(),
                    ButtonPayload.ONLY_NEW
                )
                button(
                    Messages[user].menuReceiptLimitationsOnlyPremium.format(onlyPremiumActivation),
                    ButtonPayload.serializer(),
                    ButtonPayload.ONLY_PREMIUM
                )
            }
            row {
                button(
                    Messages[user].menuReceiptReadyDelete,
                    ButtonPayload.serializer(),
                    ButtonPayload.DELETE
                )
            }
            row {
                button(
                    Messages[user].menuButtonBack,
                    ButtonPayload.serializer(),
                    ButtonPayload.BACK
                )
            }
        }
    }

    override suspend fun handleMessage(bot: Bot, botMessage: BotMessage): Boolean {
        val payload = botMessage.payload ?: return false
        val code = receipt.id.toString()
        val tgLink = String.format("t.me/%s?start=RC-%s", System.getenv("TG_USER_NAME"), code)
        val vkLink = String.format("https://vk.com/write-%s?ref=RC-%s", System.getenv("VK_GROUP_ID"), code)
        when (Json.decodeFromString<ButtonPayload>(payload)) {
            ButtonPayload.SHARE -> {
                bot.sendPopup(botMessage, Messages[user].onlyTgFunction)
                return true
            }

            ButtonPayload.QR -> {

                val filename = "qr-$code.png"

                val darkColor = Colors.css("#0D1117")
                val lightColor = Colors.css("#8B949E")

                val qrCodeCanvas =
                    QRCode(if (bot is TgBot) tgLink else vkLink).render(darkColor = lightColor, brightColor = darkColor)
                val imageBytes = qrCodeCanvas.getBytes()

                bot.sendPhoto(
                    botMessage.peerId,
                    getBody(bot),
                    ByteArrayInputStream(imageBytes),
                    filename,
                    null
                )

                val list = PostgresReceiptPersistent.loadReceipts(user).filter { it.isActive }
                user.setMenu(
                    bot,
                    ReceiptsListMenu(user, list.toMutableList(), 1, ReceiptsMenu(user, MainMenu(user))),
                    botMessage
                )
            }


            ButtonPayload.CAPTCHA -> {
                val newReceipt = receipt.copy(captcha = receipt.captcha.not())
                PostgresReceiptPersistent.saveReceipt(newReceipt)
                user.setMenu(
                    bot,
                    ReceiptReadyMenu(user, newReceipt, ReceiptsMenu(user, MainMenu(user))),
                    botMessage
                )
            }

            ButtonPayload.ONLY_NEW -> {
                val newReceipt = receipt.copy(onlyNew = receipt.onlyNew.not())
                PostgresReceiptPersistent.saveReceipt(newReceipt)
                user.setMenu(
                    bot,
                    ReceiptReadyMenu(user, newReceipt, ReceiptsMenu(user, MainMenu(user))),
                    botMessage
                )
            }

            ButtonPayload.ONLY_PREMIUM -> {
                val newReceipt = receipt.copy(onlyPremium = receipt.onlyPremium.not())
                PostgresReceiptPersistent.saveReceipt(newReceipt)
                user.setMenu(
                    bot,
                    ReceiptReadyMenu(user, newReceipt, ReceiptsMenu(user, MainMenu(user))),
                    botMessage
                )
            }

            ButtonPayload.USER -> user.setMenu(
                bot,
                ReceiptRecipientMenu(user, receipt, this),
                botMessage
            )

            ButtonPayload.REF -> {
                bot.sendPopup(botMessage, Messages[user].soon)
                return true
            }

            ButtonPayload.SUB -> user.setMenu(
                bot,
                ReceiptSubscriberMenu(user, receipt, this),
                botMessage
            )

            ButtonPayload.DESC -> user.setMenu(
                bot,
                ReceiptSetDescriptionMenu(user, receipt, this),
                botMessage
            )

            ButtonPayload.DELETE -> {
                for (chatId: Long in PostgresReceiptPersistent.getChatsByReceipt(receipt)) {
                    PostgresReceiptPersistent.deleteChatFromReceipt(receipt, chatId)
                }
                PostgresReceiptPersistent.deleteReceipt(receipt)
                bot.sendMessage(botMessage.peerId, Messages[user.settings.lang].menuReceiptDeleted)
                val list = PostgresReceiptPersistent.loadReceipts(user).filter { it.isActive }
                user.setMenu(
                    bot,
                    ReceiptsListMenu(user, list.toMutableList(), 1, ReceiptsMenu(user, MainMenu(user))),
                    botMessage
                )
            }

            ButtonPayload.BACK -> {
                val list = PostgresReceiptPersistent.loadReceipts(user).filter { it.isActive }
                user.setMenu(
                    bot,
                    ReceiptsListMenu(user, list.toMutableList(), 1, ReceiptsMenu(user, MainMenu(user))),
                    botMessage
                )
            }
        }
        return true
    }

    @Serializable
    private enum class ButtonPayload {
        SHARE,
        QR,
        CAPTCHA,
        ONLY_NEW,
        ONLY_PREMIUM,
        USER,
        REF,
        SUB,
        DESC,
        DELETE,
        BACK
    }
}