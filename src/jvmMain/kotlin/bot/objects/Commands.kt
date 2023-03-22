package bot.objects

import bot.api.Bot
import bot.exceptions.IllegalRecipientException
import bot.exceptions.ReceiptIssuerActivationException
import bot.exceptions.ReceiptNotActiveException
import bot.inlines.*
import bot.receipts.PostgresReceiptPersistent
import java.util.*

class Commands {

    companion object {
        suspend fun execute(user: User, botMessage: BotMessage, bot: Bot, menu: Menu?) {
            if (botMessage.body == null) {
                user.setMenu(bot, MainMenu(user), null)
                return
            }
            println(botMessage.body)
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
                                val result = buildString {
                                    try {
                                        PostgresReceiptPersistent.activateReceipt(receipt, user)
                                        appendLine("Вы получили ${receipt.coins}.")
                                    } catch (ex: ReceiptIssuerActivationException) {
                                        appendLine("Вы не можете активировать свой же чек")
                                    } catch (ex: IllegalRecipientException) {
                                        appendLine("Ошибка: Чек не найден")
                                    } catch (ex: ReceiptNotActiveException) {
                                        appendLine("Ошибка: Чек уже неактивен: удален или активирован")
                                    }
                                }
                                bot.sendMessage(botMessage.peerId, result)
                            } else
                                user.setMenu(bot, MainMenu(user), null)
                        }

                        "RF" -> {
                            //ref
                        }
                    }
                }

                "/menu" -> user.setMenu(bot, MainMenu(user), null)
                "/wallet" -> user.setMenu(bot, WalletMenu(user, backMenu), botMessage.lastMenuMessageId)
                "/receipts" -> user.setMenu(bot, ReceiptsMenu(user, backMenu), botMessage.lastMenuMessageId)
                "/exchange" -> user.setMenu(bot, ExchangeMenu(user, backMenu), botMessage.lastMenuMessageId)
                "/stock" -> user.setMenu(bot, StockMenu(user, backMenu), botMessage.lastMenuMessageId)
                //market
                "/accounts" -> user.setMenu(bot, AccountsMenu(user, backMenu), botMessage.lastMenuMessageId)
                //deals
                "/deposits" -> user.setMenu(bot, DepositsMenu(user, backMenu), botMessage.lastMenuMessageId)
                //nft
                "/settings" -> user.setMenu(bot, SettingsMenu(user, backMenu), botMessage.lastMenuMessageId)
            }
        }
    }
}