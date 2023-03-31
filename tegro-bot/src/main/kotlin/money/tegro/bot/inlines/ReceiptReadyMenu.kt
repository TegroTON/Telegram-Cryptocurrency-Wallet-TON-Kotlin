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
import money.tegro.bot.objects.MessagesContainer
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
        val code = receipt.id.toString()
        val tgLink = String.format("t.me/%s?start=RC-%s", System.getenv("TG_USER_NAME"), code)
        val vkLink = String.format("https://vk.com/write-%s?ref=RC-%s", System.getenv("VK_GROUP_ID"), code)
        val date = Date.from(receipt.issueTime.toJavaInstant())
        val time =
            SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(date)
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = String.format(
                MessagesContainer[user.settings.lang].menuReceiptReadyMessage,
                receipt.coins,
                receipt.activations,
                time
            ),
            keyboard = BotKeyboard {
                row {
                    linkButton(
                        MessagesContainer[user.settings.lang].menuReceiptReadyShare,
                        if (bot is TgBot) tgLink else vkLink,
                        ButtonPayload.serializer(),
                        ButtonPayload.SHARE
                    )
                }
                row {
                    button(
                        MessagesContainer[user.settings.lang].menuReceiptReadyQr,
                        ButtonPayload.serializer(),
                        ButtonPayload.QR
                    )
                }
                row {
                    button(
                        MessagesContainer[user.settings.lang].menuReceiptReadyLimitations,
                        ButtonPayload.serializer(),
                        ButtonPayload.LIMITATIONS
                    )
                }
                row {
                    button(
                        MessagesContainer[user.settings.lang].menuReceiptReadyDelete,
                        ButtonPayload.serializer(),
                        ButtonPayload.DELETE
                    )
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

                user.setMenu(
                    bot,
                    ReceiptReadyMenu(user, receipt, ReceiptsMenu(user, MainMenu(user))),
                    message.lastMenuMessageId
                )
                bot.sendPhoto(
                    message.peerId,
                    if (bot is TgBot) tgLink else vkLink,
                    ByteArrayInputStream(imageBytes),
                    filename,
                    null
                )
            }

            ButtonPayload.LIMITATIONS -> user.setMenu(
                bot,
                ReceiptLimitationsMenu(user, receipt, this),
                message.lastMenuMessageId
            )

            ButtonPayload.DELETE -> {
                PostgresReceiptPersistent.deleteReceipt(receipt)
                bot.sendMessage(message.peerId, MessagesContainer[user.settings.lang].menuReceiptDeleted)
                user.setMenu(bot, ReceiptsMenu(user, MainMenu(user)), message.lastMenuMessageId)
            }

            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, message.lastMenuMessageId)
            }
        }
        return true
    }

    @Serializable
    enum class ButtonPayload {
        SHARE,
        QR,
        LIMITATIONS,
        DELETE,
        BACK
    }
}