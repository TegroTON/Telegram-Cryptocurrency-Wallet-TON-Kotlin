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
import money.tegro.bot.utils.PostgresAccountsPersistent
import money.tegro.bot.utils.button
import money.tegro.bot.wallet.Coins

@Serializable
class AccountChangeMinAmountMenu(
    val user: User,
    val account: Account,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = Messages[user.settings.lang].menuAccountSelectMinAmountMessage.format(account.coins.currency.ticker),
            keyboard = BotKeyboard {
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
            }
        } else {
            if (isStringLong(message.body)) {
                val currency = account.coins.currency
                val count = (message.body!!.toDouble() * getFactor(currency.decimals)).toLong().toBigInteger()
                val coins = Coins(currency, count)
                val account = Account(
                    account.id,
                    account.issueTime,
                    user,
                    account.oneTime,
                    account.coins,
                    coins,
                    account.maxCoins,
                    account.activations,
                    account.isActive
                )
                PostgresAccountsPersistent.saveAccount(account)
                user.setMenu(
                    bot,
                    AccountReadyMenu(user, account, AccountsMenu(user, MainMenu(user))),
                    message.lastMenuMessageId
                )
                return true
            } else {
                bot.sendMessage(message.peerId, Messages[user.settings.lang].menuSelectInvalidAmount)
                return false
            }
        }
        return true
    }

    private fun getFactor(decimals: Int): Long {
        val string = buildString {
            append("1")
            for (i in 1..decimals) {
                append("0")
            }
        }
        return string.toLong()
    }

    private fun isStringLong(s: String?): Boolean {
        if (s == null) return false
        return try {
            s.toDouble()
            true
        } catch (ex: NumberFormatException) {
            false
        }
    }

    @Serializable
    private enum class ButtonPayload {
        BACK
    }
}