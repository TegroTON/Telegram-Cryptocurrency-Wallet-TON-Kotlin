package money.tegro.bot.objects

import money.tegro.bot.api.Bot
import money.tegro.bot.api.TgBot
import money.tegro.bot.exceptions.RecipientNotSubscriberException
import money.tegro.bot.inlines.*
import money.tegro.bot.receipts.PostgresReceiptPersistent
import money.tegro.bot.utils.Captcha
import money.tegro.bot.utils.LogsUtil
import money.tegro.bot.utils.PostgresLogsPersistent
import money.tegro.bot.wallet.PostgresAccountsPersistent
import java.awt.Color
import java.awt.Font
import java.util.*

class Commands {

    companion object {
        suspend fun execute(user: User, botMessage: BotMessage, bot: Bot, menu: Menu?) {
            if (botMessage.body == null) {
                user.setMenu(bot, MainMenu(user), null)
                return
            }
            val args = botMessage.body.split(" ")
            val backMenu: Menu = menu ?: MainMenu(user)
            when (args[0]) {
                "/start" -> {
                    if (args.size == 1) {
                        user.setMenu(bot, MainMenu(user), null)
                        return
                    }
                    val split = args[1].split("-")
                    if (split.size != 6) {
                        user.setMenu(bot, MainMenu(user), null)
                        return
                    }
                    val type = split[0]
                    val code = args[1].drop(3)
                    when (type) {
                        "RC" -> {
                            val receipt = PostgresReceiptPersistent.loadReceipt(UUID.fromString(code))
                            if (receipt != null) {
                                val lang = Messages[user.settings.lang]
                                val result = buildString {
                                    try {
                                        val chatIds = PostgresReceiptPersistent.getChatsByReceipt(receipt)
                                        if (chatIds.isNotEmpty()) {
                                            for (chatId: Long in chatIds) {
                                                val isUserInChat = bot.isUserInChat(
                                                    chatId,
                                                    (if (bot is TgBot) user.tgId else user.vkId) ?: 0
                                                )
                                                if (!isUserInChat) {
                                                    if (bot is TgBot) {
                                                        user.setMenu(
                                                            bot,
                                                            ReceiptActivateSubscribeMenu(user, receipt, MainMenu(user)),
                                                            null
                                                        )
                                                        return
                                                    } else {
                                                        val chat = bot.getChat(chatId)
                                                        if (chat != null) {
                                                            throw RecipientNotSubscriberException(
                                                                receipt,
                                                                "${chat.title} (@${chat.username})"
                                                            )
                                                        } else {
                                                            throw RecipientNotSubscriberException(
                                                                receipt,
                                                                "Chat not found!"
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        val captcha = Captcha().builder(350, 100, "./", Color.white)
                                            .addLines(10, 10, 1, Color.black)
                                            .addNoise(false, Color.black)
                                            .setFont("Default", 45, Font.BOLD)
                                            .setText(6, Color.black)
                                            .build()

                                        if (captcha.image != null) {
                                            bot.sendPhoto(
                                                botMessage.peerId,
                                                Messages[user].receiptSolveCaptcha,
                                                captcha.image!!,
                                                null
                                            )

                                            user.setMenu(
                                                bot,
                                                ReceiptActivateCaptchaMenu(
                                                    user,
                                                    receipt,
                                                    captcha.answer,
                                                    MainMenu(user)
                                                ),
                                                botMessage.lastMenuMessageId
                                            )
                                        }
                                    } catch (ex: RecipientNotSubscriberException) {
                                        append(lang.recipientNotSubscriberException.format(ex.chatName))
                                    } catch (ex: Exception) {
                                        ex.printStackTrace()
                                    }
                                }
                                if (result.isNotEmpty()) {
                                    bot.sendMessage(botMessage.peerId, result)
                                }
                            } else user.setMenu(bot, MainMenu(user), null)
                        }

                        "AC" -> {
                            val account = PostgresAccountsPersistent.loadAccount(UUID.fromString(code))
                            if (account != null) {
                                if (account.oneTime) {
                                    user.setMenu(
                                        bot,
                                        AccountPayMenu(user, account, account.maxCoins, MainMenu(user)),
                                        null
                                    )
                                } else {
                                    user.setMenu(bot, AccountPaySelectAmountMenu(user, account, MainMenu(user)), null)
                                }
                            } else user.setMenu(bot, MainMenu(user), null)
                        }

                        "RF" -> {
                            user.setMenu(bot, MainMenu(user), null)
                        }
                    }
                }

                "/menu" -> user.setMenu(bot, MainMenu(user), null)
                "/wallet" -> user.setMenu(bot, WalletMenu(user, backMenu), botMessage.lastMenuMessageId)
                "/receipts" -> user.setMenu(bot, ReceiptsMenu(user, backMenu), botMessage.lastMenuMessageId)
                //"/exchange" -> user.setMenu(bot, ExchangeMenu(user, backMenu), botMessage.lastMenuMessageId)
                //"/stock" -> user.setMenu(bot, StockMenu(user, backMenu), botMessage.lastMenuMessageId)
                //market
                "/accounts" -> user.setMenu(bot, AccountsMenu(user, backMenu), botMessage.lastMenuMessageId)
                //deals
                "/deposits" -> user.setMenu(bot, DepositsMenu(user, backMenu), botMessage.lastMenuMessageId)
                //nft
                "/settings" -> user.setMenu(bot, SettingsMenu(user, backMenu), botMessage.lastMenuMessageId)
                "/whoami" -> {
                    val userDisplay = buildString {
                        if (bot is TgBot) append("<code>")
                        append(user.id)
                        if (bot is TgBot) append("</code>")
                    }
                    bot.sendMessage(botMessage.peerId, "By admin request forward this message\n$userDisplay")
                }

                "/logsbytype" -> {
                    if (user.tgId == null) {
                        user.setMenu(bot, MainMenu(user), null)
                        return
                    }
                    if (user.tgId != 453460175L) {
                        user.setMenu(bot, MainMenu(user), null)
                        return
                    }
                    val type = LogType.valueOf(args[1])
                    val link = LogsUtil.getLogsLink(PostgresLogsPersistent.getLogsByType(type), "Logs by $type")

                    bot.sendMessage(botMessage.peerId, link)
                }

                "/logsbyuserid" -> {
                    if (user.tgId == null) {
                        user.setMenu(bot, MainMenu(user), null)
                        return
                    }
                    if (user.tgId != 453460175L) {
                        user.setMenu(bot, MainMenu(user), null)
                        return
                    }
                    val targetUser = PostgresUserPersistent.load(UUID.fromString(args[1]))
                    if (targetUser == null) {
                        bot.sendMessage(botMessage.peerId, "User not found")
                        return
                    }
                    val link =
                        LogsUtil.getLogsLink(PostgresLogsPersistent.getLogsByUser(targetUser), "Logs by ${user.id}")

                    bot.sendMessage(botMessage.peerId, link)
                }

                "/logsbyusertg" -> {
                    if (user.tgId == null) {
                        user.setMenu(bot, MainMenu(user), null)
                        return
                    }
                    if (user.tgId != 453460175L) {
                        user.setMenu(bot, MainMenu(user), null)
                        return
                    }
                    val targetUser = PostgresUserPersistent.loadByTg(args[1].toLong())
                    if (targetUser == null) {
                        bot.sendMessage(botMessage.peerId, "User not found")
                        return
                    }
                    val link =
                        LogsUtil.getLogsLink(PostgresLogsPersistent.getLogsByUser(targetUser), "Logs by ${user.id}")

                    bot.sendMessage(botMessage.peerId, link)
                }

                else -> user.setMenu(bot, MainMenu(user), null)
            }
        }
    }
}