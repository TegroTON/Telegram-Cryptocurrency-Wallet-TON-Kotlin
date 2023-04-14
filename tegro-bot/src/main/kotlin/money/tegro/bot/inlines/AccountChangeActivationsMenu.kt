package money.tegro.bot.inlines

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.objects.Account
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.button
import money.tegro.bot.wallet.Coins
import money.tegro.bot.wallet.PostgresAccountsPersistent

@Serializable
class AccountChangeActivationsMenu(
    val user: User,
    val account: Account,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = Messages[user.settings.lang].menuAccountChangeActivationsMessage,
            keyboard = BotKeyboard {
                row {
                    button(
                        Messages[user.settings.lang].menuAccountChangeActivationsOneTimeButton,
                        ButtonPayload.serializer(),
                        ButtonPayload.ONETIME
                    )
                }
                row {
                    button(
                        Messages[user.settings.lang].menuAccountChangeActivationsNotSetButton,
                        ButtonPayload.serializer(),
                        ButtonPayload.NOTSET
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

    override suspend fun handleMessage(bot: Bot, message: BotMessage): Boolean {
        val payload = message.payload
        if (payload != null) {
            when (Json.decodeFromString<ButtonPayload>(payload)) {
                ButtonPayload.BACK -> {
                    user.setMenu(bot, parentMenu, message.lastMenuMessageId)
                }

                ButtonPayload.ONETIME -> {
                    val zero = Coins(account.coins.currency, 0.toBigInteger())
                    val account = Account(
                        account.id,
                        account.issueTime,
                        user,
                        true,
                        account.coins,
                        zero,
                        account.maxCoins,
                        1,
                        account.isActive
                    )
                    PostgresAccountsPersistent.saveAccount(account)
                    user.setMenu(
                        bot,
                        AccountReadyMenu(user, account, AccountsMenu(user, MainMenu(user))),
                        message.lastMenuMessageId
                    )
                }

                ButtonPayload.NOTSET -> {
                    val account = Account(
                        account.id,
                        account.issueTime,
                        user,
                        false,
                        account.coins,
                        account.minAmount,
                        account.maxCoins,
                        Int.MAX_VALUE,
                        account.isActive
                    )
                    PostgresAccountsPersistent.saveAccount(account)
                    user.setMenu(
                        bot,
                        AccountReadyMenu(user, account, AccountsMenu(user, MainMenu(user))),
                        message.lastMenuMessageId
                    )
                }
            }
        } else {
            if (isStringInt(message.body)) {
                val activations = message.body!!.toInt()
                val oneTime = activations == 1
                val zero = Coins(account.coins.currency, 0.toBigInteger())
                val account = Account(
                    account.id,
                    account.issueTime,
                    user,
                    oneTime,
                    account.coins,
                    if (oneTime) zero else account.minAmount,
                    account.maxCoins,
                    activations,
                    account.isActive
                )
                PostgresAccountsPersistent.saveAccount(account)
                user.setMenu(
                    bot,
                    AccountReadyMenu(user, account, AccountsMenu(user, MainMenu(user))),
                    message.lastMenuMessageId
                )
            } else {
                bot.sendMessage(message.peerId, Messages[user.settings.lang].menuSelectInvalidAmount)
                return false
            }
        }
        return true
    }

    private fun isStringInt(s: String?): Boolean {
        if (s == null) return false
        return try {
            s.toInt()
            true
        } catch (ex: NumberFormatException) {
            false
        }
    }

    @Serializable
    private enum class ButtonPayload {
        ONETIME,
        NOTSET,
        BACK
    }
}