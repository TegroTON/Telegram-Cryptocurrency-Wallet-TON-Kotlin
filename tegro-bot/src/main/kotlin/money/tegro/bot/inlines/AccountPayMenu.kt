package money.tegro.bot.inlines

import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.api.TgBot
import money.tegro.bot.exceptions.*
import money.tegro.bot.objects.Account
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.button
import money.tegro.bot.wallet.Coins
import money.tegro.bot.wallet.PostgresAccountsPersistent
import money.tegro.bot.wallet.PostgresWalletPersistent
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class AccountPayMenu(
    val user: User,
    val account: Account,
    val coins: Coins,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        val available = PostgresWalletPersistent.loadWalletState(user).active[account.coins.currency]
        println("avail: $available")
        println("coins: $coins")
        val id = buildString {
            if (bot is TgBot) append("<code>")
            append("#")
            append(account.id.toString())
            if (bot is TgBot) append("</code>")
        }
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = Messages[user.settings.lang].menuAccountPayMessage.format(
                id,
                coins,
                available
            ),
            keyboard = BotKeyboard {
                row {
                    button(
                        Messages[user.settings.lang].menuAccountPayButton,
                        ButtonPayload.serializer(),
                        ButtonPayload.PAY
                    )
                    button(
                        Messages[user.settings.lang].menuAccountPayDeclineButton,
                        ButtonPayload.serializer(),
                        ButtonPayload.DECLINE
                    )
                }
            }
        )
    }

    override suspend fun handleMessage(bot: Bot, message: BotMessage): Boolean {
        val payload = message.payload ?: return false
        when (Json.decodeFromString<ButtonPayload>(payload)) {

            ButtonPayload.PAY -> {
                val lang = Messages[user.settings.lang]
                val id = buildString {
                    if (bot is TgBot) append("<code>")
                    append("#")
                    append(account.id.toString())
                    if (bot is TgBot) append("</code>")
                }
                val result = buildString {
                    try {
                        PostgresAccountsPersistent.payAccount(account, user, coins)
                        append(lang.menuAccountPaySuccess)
                    } catch (ex: AccountIssuerActivationException) {
                        append(lang.accountIssuerActivationException)
                    } catch (ex: AccountNotActiveException) {
                        append(lang.accountNotActiveException.format(id))
                    } catch (ex: NotEnoughCoinsException) {
                        val available = PostgresWalletPersistent.loadWalletState(ex.user).active[account.coins.currency]
                        append(lang.menuReceiptsSelectAmountNoMoney.format(ex.coins, available))
                    } catch (ex: AccountMinAmountException) {
                        append(lang.accountMinAmountException.format(account.minAmount))
                    } catch (ex: AccountOverdraftException) {
                        append(lang.accountOverdraftException.format(account.maxCoins - account.coins))
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }

                bot.sendMessage(message.peerId, result)

                val issuer = account.issuer
                val date = Date.from(account.issueTime.toJavaInstant())
                val time =
                    SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(date)
                bot.sendMessage(
                    issuer.tgId ?: issuer.vkId ?: 0,
                    Messages[issuer.settings.lang].accountMoneyReceived.format(
                        coins,
                        time
                    )
                )
                return false
            }

            ButtonPayload.DECLINE -> {
                user.setMenu(bot, MainMenu(user), message.lastMenuMessageId)
            }
        }
        return true
    }

    @Serializable
    private enum class ButtonPayload {
        PAY,
        DECLINE
    }
}