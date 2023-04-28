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
import money.tegro.bot.utils.linkButton
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class ReceiptReadyMenu(
    val user: User,
    val receipt: Receipt,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = getBody(bot),
            keyboard = getKeyboard(bot, true)
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
        println(chatIds)
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
        return Messages[user.settings.lang].menuReceiptReadyMessage.format(
            receipt.coins,
            receipt.activations,
            time,
            limitation,
            if (bot is TgBot) tgLink else vkLink
        )
    }

    private fun getKeyboard(bot: Bot, qrButton: Boolean): BotKeyboard {
        val code = receipt.id.toString()
        val tgLink = String.format("t.me/%s?start=RC-%s", System.getenv("TG_USER_NAME"), code)
        val vkLink = String.format("https://vk.com/write-%s?ref=RC-%s", System.getenv("VK_GROUP_ID"), code)
        return BotKeyboard {
            row {
                linkButton(
                    Messages[user.settings.lang].menuReceiptReadyShare,
                    if (bot is TgBot) tgLink else vkLink,
                    ButtonPayload.serializer(),
                    ButtonPayload.SHARE
                )
            }
            if (qrButton) {
                row {
                    button(
                        Messages[user.settings.lang].menuReceiptReadyQr,
                        ButtonPayload.serializer(),
                        ButtonPayload.QR
                    )
                }
            }
            row {
                button(
                    Messages[user.settings.lang].menuReceiptReadyLimitations,
                    ButtonPayload.serializer(),
                    ButtonPayload.LIMITATIONS
                )
            }
            row {
                button(
                    Messages[user.settings.lang].menuReceiptReadyDelete,
                    ButtonPayload.serializer(),
                    ButtonPayload.DELETE
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
    }

    override suspend fun handleMessage(bot: Bot, message: BotMessage): Boolean {
        val payload = message.payload ?: return false
        val code = receipt.id.toString()
        val tgLink = String.format("t.me/%s?start=RC-%s", System.getenv("TG_USER_NAME"), code)
        val vkLink = String.format("https://vk.com/write-%s?ref=RC-%s", System.getenv("VK_GROUP_ID"), code)
        when (Json.decodeFromString<ButtonPayload>(payload)) {

            ButtonPayload.SHARE -> TODO()
            ButtonPayload.QR -> {

                val filename = "qr-$code.png"

                val darkColor = Colors.css("#0D1117")
                val lightColor = Colors.css("#8B949E")

                val qrCodeCanvas =
                    QRCode(if (bot is TgBot) tgLink else vkLink).render(darkColor = lightColor, brightColor = darkColor)
                val imageBytes = qrCodeCanvas.getBytes()

                bot.sendPhoto(
                    message.peerId,
                    getBody(bot),
                    ByteArrayInputStream(imageBytes),
                    filename,
                    null
                )

                val list = PostgresReceiptPersistent.loadReceipts(user).filter { it.isActive }
                user.setMenu(
                    bot,
                    ReceiptsListMenu(user, list.toMutableList(), 1, ReceiptsMenu(user, MainMenu(user))),
                    message.lastMenuMessageId
                )
            }

            ButtonPayload.LIMITATIONS -> user.setMenu(
                bot,
                ReceiptLimitationsMenu(user, receipt, this),
                message.lastMenuMessageId
            )

            ButtonPayload.DELETE -> {
                for (chatId: Long in PostgresReceiptPersistent.getChatsByReceipt(receipt)) {
                    PostgresReceiptPersistent.deleteChatFromReceipt(receipt, chatId)
                }
                PostgresReceiptPersistent.deleteReceipt(receipt)
                bot.sendMessage(message.peerId, Messages[user.settings.lang].menuReceiptDeleted)
                val list = PostgresReceiptPersistent.loadReceipts(user).filter { it.isActive }
                user.setMenu(
                    bot,
                    ReceiptsListMenu(user, list.toMutableList(), 1, ReceiptsMenu(user, MainMenu(user))),
                    message.lastMenuMessageId
                )
            }

            ButtonPayload.BACK -> {
                val list = PostgresReceiptPersistent.loadReceipts(user).filter { it.isActive }
                user.setMenu(
                    bot,
                    ReceiptsListMenu(user, list.toMutableList(), 1, ReceiptsMenu(user, MainMenu(user))),
                    message.lastMenuMessageId
                )
            }
        }
        return true
    }

    @Serializable
    private enum class ButtonPayload {
        SHARE,
        QR,
        LIMITATIONS,
        DELETE,
        BACK
    }
}