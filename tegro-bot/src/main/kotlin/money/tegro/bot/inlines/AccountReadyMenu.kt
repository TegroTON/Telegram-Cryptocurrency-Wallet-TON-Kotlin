package money.tegro.bot.inlines

import io.github.g0dkar.qrcode.QRCode
import io.github.g0dkar.qrcode.render.Colors
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.api.TgBot
import money.tegro.bot.objects.Account
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.PostgresAccountsPersistent
import money.tegro.bot.utils.button
import money.tegro.bot.utils.linkButton
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class AccountReadyMenu(
    val user: User,
    val account: Account,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        val code = account.id.toString()
        val tgLink = String.format("t.me/%s?start=AC-%s", System.getenv("TG_USER_NAME"), code)
        val vkLink = String.format("https://vk.com/write-%s?ref=AC-%s", System.getenv("VK_GROUP_ID"), code)
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = getBody(bot),
            keyboard = BotKeyboard {
                row {
                    linkButton(
                        Messages[user.settings.lang].menuReceiptReadyShare,
                        if (bot is TgBot) tgLink else vkLink,
                        ButtonPayload.serializer(),
                        ButtonPayload.SHARE
                    )
                }
                row {
                    button(
                        Messages[user.settings.lang].menuReceiptReadyQr,
                        ButtonPayload.serializer(),
                        ButtonPayload.QR
                    )
                }
                if (account.maxCoins.amount > 0.toBigInteger()) {
                    row {
                        button(
                            Messages[user.settings.lang].menuAccountReadyMinAmountButton,
                            ButtonPayload.serializer(),
                            ButtonPayload.MINAMOUNT
                        )
                        button(
                            Messages[user.settings.lang].menuAccountReadyActivationsButton,
                            ButtonPayload.serializer(),
                            ButtonPayload.ACTIVATIONS
                        )
                    }
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
        )
    }

    private fun getBody(bot: Bot): String {
        val code = account.id.toString()
        val tgLink = String.format("t.me/%s?start=AC-%s", System.getenv("TG_USER_NAME"), code)
        val vkLink = String.format("https://vk.com/write-%s?ref=AC-%s", System.getenv("VK_GROUP_ID"), code)
        val date = Date.from(account.issueTime.toJavaInstant())
        val time =
            SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(date)
        val lang = user.settings.lang
        return Messages[lang].menuAccountReadyMessage.format(
            time,
            Account.getTypeDisplay(account, lang),
            account.coins.toStringWithRate(user.settings.localCurrency),
            Account.getProgress(bot, account, lang),
            Account.getActivations(bot, account, lang),
            Account.getMinAmount(bot, account, lang),
            account.coins.currency.ticker,
            if (bot is TgBot) tgLink else vkLink
        )
    }

    override suspend fun handleMessage(bot: Bot, message: BotMessage): Boolean {
        val payload = message.payload ?: return false
        val code = account.id.toString()
        val tgLink = String.format("t.me/%s?start=AC-%s", System.getenv("TG_USER_NAME"), code)
        val vkLink = String.format("https://vk.com/write-%s?ref=AC-%s", System.getenv("VK_GROUP_ID"), code)
        when (Json.decodeFromString<ButtonPayload>(payload)) {
            ButtonPayload.BACK -> {
                val list = PostgresAccountsPersistent.loadAccounts(user).filter { it.isActive }
                user.setMenu(
                    bot,
                    AccountsListMenu(user, list.toMutableList(), 1, AccountsMenu(user, MainMenu(user))),
                    message.lastMenuMessageId
                )
            }

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

                val list = PostgresAccountsPersistent.loadAccounts(user).filter { it.isActive }
                user.setMenu(
                    bot,
                    AccountsListMenu(user, list.toMutableList(), 1, AccountsMenu(user, MainMenu(user))),
                    message.lastMenuMessageId
                )
            }

            ButtonPayload.MINAMOUNT -> {
                if (account.oneTime) {
                    return bot.sendPopup(message, Messages[user.settings.lang].menuAccountReadyOneTimePopup)
                } else {
                    user.setMenu(
                        bot,
                        AccountChangeMinAmountMenu(user, account, this),
                        message.lastMenuMessageId
                    )
                }
            }

            ButtonPayload.ACTIVATIONS -> user.setMenu(
                bot,
                AccountChangeActivationsMenu(user, account, this),
                message.lastMenuMessageId
            )

            ButtonPayload.DELETE -> {
                PostgresAccountsPersistent.deleteAccount(account)
                val list = PostgresAccountsPersistent.loadAccounts(user).filter { it.isActive }
                user.setMenu(
                    bot,
                    AccountsListMenu(user, list.toMutableList(), 1, AccountsMenu(user, MainMenu(user))),
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
        MINAMOUNT,
        ACTIVATIONS,
        DELETE,
        BACK
    }
}