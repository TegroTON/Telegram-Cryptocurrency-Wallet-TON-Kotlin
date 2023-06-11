package money.tegro.bot.objects

import money.tegro.bot.api.Bot
import money.tegro.bot.api.TgBot
import money.tegro.bot.exceptions.ReceiptNotPremiumUserException
import money.tegro.bot.exceptions.ReceiptOnlyTgException
import money.tegro.bot.exceptions.RecipientNotSubscriberException
import money.tegro.bot.inlines.*
import money.tegro.bot.receipts.PostgresReceiptPersistent
import money.tegro.bot.utils.Captcha
import money.tegro.bot.utils.LogsUtil
import money.tegro.bot.utils.PostgresAccountsPersistent
import money.tegro.bot.utils.PostgresLogsPersistent
import java.awt.Color
import java.awt.Font
import java.io.InputStream
import java.util.*
import kotlin.random.Random


class Commands {

    companion object {
        suspend fun execute(user: User, botMessage: BotMessage, bot: Bot, menu: Menu?, isPremium: Boolean?) {
            if (botMessage.body == null) {
                user.setMenu(bot, MainMenu(user), botMessage)
                return
            }
            val args = botMessage.body.split(" ")
            val backMenu: Menu = menu ?: MainMenu(user)
            when (args[0]) {
                "/start" -> {
                    if (args.size == 1) {
                        user.setMenu(bot, MainMenu(user), botMessage)
                        return
                    }
                    val split = args[1].split("-")
                    if (split.size != 6) {
                        user.setMenu(bot, MainMenu(user), botMessage)
                        return
                    }
                    val type = split[0]
                    val code = args[1].drop(3)
                    when (type) {
                        "RC" -> {
                            val receipt = PostgresReceiptPersistent.loadReceipt(UUID.fromString(code))
                            if (receipt != null) {
                                val lang = Messages[user.settings.lang]
                                val id = buildString {
                                    if (bot is TgBot) append("<code>")
                                    append("#")
                                    append(receipt.id.toString())
                                    if (bot is TgBot) append("</code>")
                                }
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
                                                            botMessage
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
                                        if (receipt.onlyPremium) {
                                            if (isPremium != null) {
                                                if (!isPremium) {
                                                    throw ReceiptNotPremiumUserException(receipt)
                                                }
                                            } else {
                                                throw ReceiptOnlyTgException(receipt)
                                            }
                                        }
//                                        val needCaptcha = receipt.captcha
//                                        val captcha = getCaptcha()
//
//                                        if (needCaptcha) {
//                                            bot.sendPhoto(
//                                                botMessage.peerId,
//                                                Messages[user].receiptSolveCaptcha,
//                                                captcha.first,
//                                                "captcha-${captcha.second}",
//                                                null
//                                            )
//                                        }
//
//                                        user.setMenu(
//                                            bot,
//                                            ReceiptActivateCaptchaMenu(
//                                                user,
//                                                receipt,
//                                                if (needCaptcha) captcha.second else "",
//                                                MainMenu(user)
//                                            ),
//                                            botMessage.lastMenuMessageId
//                                        )

                                        user.setMenu(
                                            bot,
                                            ReceiptActivateCaptchaMenu(
                                                user,
                                                receipt,
                                                "",
                                                MainMenu(user)
                                            ),
                                            botMessage
                                        )
                                    } catch (ex: RecipientNotSubscriberException) {
                                        append(lang.recipientNotSubscriberException.format(ex.chatName))
                                    } catch (ex: ReceiptOnlyTgException) {
                                        append(lang.onlyTgReceiptException.format(id))
                                    } catch (ex: ReceiptNotPremiumUserException) {
                                        append(lang.notPremiumRecipientException.format(id))
                                    } catch (ex: Exception) {
                                        ex.printStackTrace()
                                    }
                                }
                                if (result.isNotEmpty()) {
                                    bot.sendMessage(botMessage.peerId, result)
                                }
                            } else user.setMenu(bot, MainMenu(user), botMessage)
                        }

                        "AC" -> {
                            val account = PostgresAccountsPersistent.loadAccount(UUID.fromString(code))
                            if (account != null) {
                                if (account.oneTime) {
                                    user.setMenu(
                                        bot,
                                        AccountPayMenu(user, account, account.maxCoins, MainMenu(user)),
                                        botMessage
                                    )
                                } else {
                                    user.setMenu(
                                        bot,
                                        AccountPaySelectAmountMenu(user, account, MainMenu(user)),
                                        botMessage
                                    )
                                }
                            } else user.setMenu(bot, MainMenu(user), botMessage)
                        }

                        "RF" -> {
                            user.setMenu(bot, MainMenu(user), botMessage)
                        }
                    }
                }

                "/menu" -> user.setMenu(bot, MainMenu(user), botMessage)
                "/wallet" -> user.setMenu(bot, WalletMenu(user, backMenu), botMessage)
                "/receipts" -> user.setMenu(bot, ReceiptsMenu(user, backMenu), botMessage)
                //"/exchange" -> user.setMenu(bot, ExchangeMenu(user, backMenu), botMessage)
                //"/stock" -> user.setMenu(bot, StockMenu(user, backMenu), botMessage)
                //market
                "/accounts" -> user.setMenu(bot, AccountsMenu(user, backMenu), botMessage)
                //deals
                "/deposits" -> user.setMenu(bot, DepositsMenu(user, backMenu), botMessage)
                //nft
                "/settings" -> user.setMenu(bot, SettingsMenu(user, backMenu), botMessage)
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
                        user.setMenu(bot, MainMenu(user), botMessage)
                        return
                    }
                    if (user.tgId != 453460175L) {
                        user.setMenu(bot, MainMenu(user), botMessage)
                        return
                    }
                    val type = LogType.valueOf(args[1])
                    val link = LogsUtil.getLogsLink(PostgresLogsPersistent.getLogsByType(type), "Logs by $type")

                    bot.sendMessage(botMessage.peerId, link)
                }

                "/logsbyuserid" -> {
                    if (user.tgId == null) {
                        user.setMenu(bot, MainMenu(user), botMessage)
                        return
                    }
                    if (user.tgId != 453460175L) {
                        user.setMenu(bot, MainMenu(user), botMessage)
                        return
                    }
                    val targetUser = PostgresUserPersistent.load(UUID.fromString(args[1]))
                    if (targetUser == null) {
                        bot.sendMessage(botMessage.peerId, "User not found")
                        return
                    }
                    val userInfo = buildString {
                        appendLine("User TG id: ${targetUser.tgId}")
                        appendLine("User VK id: ${targetUser.vkId}")
                        appendLine("User address: ${targetUser.settings.address}")
                        appendLine("User referral id: ${targetUser.settings.referralId}")
                    }
                    val link =
                        LogsUtil.getLogsLink(
                            PostgresLogsPersistent.getLogsByUser(targetUser),
                            userInfo,
                            "Logs by ${user.id}"
                        )

                    bot.sendMessage(botMessage.peerId, link)
                }

                "/logsbyusertg" -> {
                    if (user.tgId == null) {
                        user.setMenu(bot, MainMenu(user), botMessage)
                        return
                    }
                    if (user.tgId != 453460175L) {
                        user.setMenu(bot, MainMenu(user), botMessage)
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

                else -> user.setMenu(bot, MainMenu(user), botMessage)
            }
        }

        private fun getCaptcha(): Pair<InputStream, String> {
//            if (false) {
//                val imageCaptcha = ImageCaptcha.Builder(350, 100)
//                    .addContent(
//                        LatinContentProducer(Random.nextInt(5, 8)),
//                        FastWordRenderer.Builder()
//                            .setXOffset(Random.nextDouble(0.05, 0.55))
//                            .setYOffset(Random.nextDouble(0.1, 0.4))
//                            .build()
//                    )
//                    .addNoise(CurvedLineNoiseProducer())
//                    .addNoise(CurvedLineNoiseProducer())
//                    .addNoise(StraightLineNoiseProducer(Color.BLACK, Random.nextInt(1, 3)))
//                    .addNoise(StraightLineNoiseProducer(Color.BLACK, Random.nextInt(1, 3)))
//                    .addNoise(StraightLineNoiseProducer(Color.BLACK, Random.nextInt(1, 3)))
//                    .build()
//
//                val baos = ByteArrayOutputStream()
//                ImageIO.write(imageCaptcha.image, "png", baos)
//                val stream = ByteArrayInputStream(baos.toByteArray()) as InputStream
//                return Pair(stream, imageCaptcha.content)
//            } else {
            val captcha = Captcha().builder(350, 100, "./", Color.white)
                .addLines(Random.nextInt(10, 20), Random.nextInt(10, 20), 1, Color.black)
                .addNoise(Random.nextBoolean(), Color.black)
                .setFont("Arial", Random.nextInt(40, 55), Font.BOLD)
                .setText(Random.nextInt(5, 8), Color.black)
                .build()
            return Pair(captcha.image!!, captcha.answer)
        }
        }
//    }
}