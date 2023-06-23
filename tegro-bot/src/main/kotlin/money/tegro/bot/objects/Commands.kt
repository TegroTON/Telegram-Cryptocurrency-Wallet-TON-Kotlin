package money.tegro.bot.objects

import money.tegro.bot.MASTER_KEY
import money.tegro.bot.api.Bot
import money.tegro.bot.api.TgBot
import money.tegro.bot.blockchain.BlockchainManager
import money.tegro.bot.exceptions.ReceiptNotPremiumUserException
import money.tegro.bot.exceptions.ReceiptOnlyTgException
import money.tegro.bot.exceptions.RecipientNotSubscriberException
import money.tegro.bot.inlines.*
import money.tegro.bot.receipts.PostgresReceiptPersistent
import money.tegro.bot.utils.*
import money.tegro.bot.wallet.Coins
import money.tegro.bot.wallet.CryptoCurrency
import money.tegro.bot.wallet.PostgresWalletPersistent
import money.tegro.bot.walletPersistent
import java.awt.Color
import java.awt.Font
import java.io.InputStream
import java.util.*
import kotlin.random.Random


class Commands {

    companion object {
        private val admins = listOf(453460175L to "Антон SPY_me")

        suspend fun execute(user: User, botMessage: BotMessage, bot: Bot, menu: Menu?, isPremium: Boolean?) {
            if (botMessage.isFromChat) {
                if (botMessage.body == null) return
                val args = botMessage.body.split(" ")
                when (args[0]) {
                    "/logs" -> {
                        logsCommand(bot, user, botMessage)
                    }

                    "/whoami" -> {
                        whoAmICommand(bot, user, botMessage)
                    }

                    "/accept" -> {
                        acceptCommand(bot, user, botMessage)
                    }
                }
                return
            }
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
                    whoAmICommand(bot, user, botMessage)
                }

                "/logs" -> {
                    logsCommand(bot, user, botMessage)
                }

                "/accept" -> {
                    acceptCommand(bot, user, botMessage)
                }

                else -> user.setMenu(bot, MainMenu(user), botMessage)
            }
        }

        private suspend fun acceptCommand(bot: Bot, user: User, botMessage: BotMessage) {
            if (user.tgId == null) {
                return
            }
            val admin = admins.find { it.first == user.tgId } ?: return
            val code = botMessage.body!!.split(" ")[1]
            val financeRequest = PostgresSecurityPersistent.loadFinanceRequest(UUID.fromString(code))
            val targetUser = PostgresUserPersistent.load(financeRequest.userId)
            if (targetUser == null) {
                bot.sendMessage(botMessage.peerId, "User not found")
                return
            }
            val coins = financeRequest.coins
            when (financeRequest.logType) {
                LogType.DEPOSIT -> {
                    walletPersistent.updateActive(targetUser, coins.currency) { oldCoins ->
                        (oldCoins + coins).also { newCoins ->
                            println(
                                "New deposit: $targetUser\n" +
                                        "     old coins: $oldCoins\n" +
                                        " deposit coins: $coins\n" +
                                        "     new coins: $newCoins"
                            )
                        }
                    }
                    bot.sendMessage(
                        targetUser.tgId ?: targetUser.vkId ?: 0,
                        Messages[targetUser].walletMenuDepositMessage.format(
                            coins.toStringWithRate(targetUser.settings.localCurrency),
                            Coins(coins.currency, coins.currency.networkFeeReserve)
                        )
                    )
                    val active = PostgresWalletPersistent.loadWalletState(targetUser).active[CryptoCurrency.TON]
                    SecurityPersistent.log(targetUser, coins, "$coins", LogType.DEPOSIT)
                    SecurityPersistent.log(
                        targetUser,
                        coins,
                        "$coins, balance $active, approved by ${admin.second}(${admin.first})",
                        LogType.DEPOSIT_ADMIN
                    )
                }

                LogType.WITHDRAW -> {
                    val blockchainManager = BlockchainManager[financeRequest.blockchainType]

                    val active = walletPersistent.loadWalletState(targetUser).active[coins.currency]
                    if (active < coins) return
                    val fee = Coins(coins.currency, NftsPersistent.countBotFee(targetUser, coins.currency))
                    val amountWithFee = coins + fee

                    walletPersistent.freeze(targetUser, amountWithFee)
                    try {
                        val pk = UserPrivateKey(UUID(0, 0), MASTER_KEY)
                        if (coins.currency.isNative) {
                            blockchainManager.transfer(
                                pk.key.toByteArray(),
                                financeRequest.address,
                                coins
                            )
                        } else {
                            blockchainManager.transferToken(
                                pk.key.toByteArray(),
                                coins.currency,
                                financeRequest.address,
                                coins
                            )
                        }
                        val oldFreeze = walletPersistent.loadWalletState(targetUser).frozen[coins.currency]
                        walletPersistent.updateFreeze(targetUser, coins.currency) { updated ->
                            (updated - amountWithFee).also {
                                println(
                                    "Remove from freeze:\n" +
                                            " old freeze: $oldFreeze\n" +
                                            " amount    : $amountWithFee\n" +
                                            " new freeze: $it"
                                )
                            }
                        }
                        SecurityPersistent.log(targetUser, coins, "$coins", LogType.WITHDRAW)
                        SecurityPersistent.log(
                            targetUser,
                            coins,
                            "$coins (fee: $fee) to ${financeRequest.address}, balance ${active - amountWithFee}, approved by ${admin.second}(${admin.first})",
                            LogType.WITHDRAW_ADMIN
                        )
                    } catch (e: Throwable) {
                        walletPersistent.unfreeze(targetUser, amountWithFee)
                        throw e
                    }
                }

                else -> {

                }
            }
        }

        private fun whoAmICommand(bot: Bot, user: User, botMessage: BotMessage) {
            val userDisplay = buildString {
                if (bot is TgBot) append("<code>")
                append(user.id)
                if (bot is TgBot) append("</code>")
            }
            println("whoami from ${botMessage.peerId}")
            bot.sendMessage(botMessage.peerId, "By admin request forward this message\n$userDisplay")
        }

        private suspend fun logsCommand(bot: Bot, user: User, botMessage: BotMessage) {
            if (user.tgId == null) {
                user.setMenu(bot, MainMenu(user), botMessage)
                return
            }
            if (user.tgId != 453460175L) {
                user.setMenu(bot, MainMenu(user), botMessage)
                return
            }
            val args = botMessage.body!!.split(" ")
            if (args.size < 3) return
            when (args[1]) {
                "type" -> {
                    val type = LogType.valueOf(args[2])
                    val link = LogsUtil.getLogsLink(PostgresLogsPersistent.getLogsByType(type), "Logs by $type")

                    bot.sendMessage(botMessage.peerId, link)
                }

                "userid" -> {
                    val targetUser = PostgresUserPersistent.load(UUID.fromString(args[2]))
                    if (targetUser == null) {
                        bot.sendMessage(botMessage.peerId, "User not found")
                        return
                    }

                    bot.sendMessage(botMessage.peerId, LogsUtil.logsByUser(targetUser))
                }

                "tgid" -> {
                    val targetUser = PostgresUserPersistent.loadByTg(args[2].toLong())
                    if (targetUser == null) {
                        bot.sendMessage(botMessage.peerId, "User not found")
                        return
                    }
                    val link =
                        LogsUtil.getLogsLink(PostgresLogsPersistent.getLogsByUser(targetUser), "Logs by ${user.id}")

                    bot.sendMessage(botMessage.peerId, link)
                }
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