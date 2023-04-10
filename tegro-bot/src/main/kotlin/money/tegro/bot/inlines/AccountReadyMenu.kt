package money.tegro.bot.inlines

import kotlinx.datetime.toJavaInstant
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
import money.tegro.bot.wallet.PostgresDepositsPersistent
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class AccountReadyMenu(
    val user: User,
    val account: Account,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        val date = Date.from(account.issueTime.toJavaInstant())
        val time =
            SimpleDateFormat("dd.MM.yyyy HH:mm").format(date)
        val lang = user.settings.lang
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = Messages[lang].menuAccountReadyMessage.format(
                time,
                Account.getTypeDisplay(account, lang),
                account.coins,
                Account.getProgress(account, lang),
                Account.getMinAmount(account, lang),
                account.coins.currency.ticker
            ),
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
        val payload = message.payload ?: return false
        when (Json.decodeFromString<ButtonPayload>(payload)) {
            ButtonPayload.BACK -> {
                TODO()
                val list = PostgresDepositsPersistent.getAllByUser(user)
                user.setMenu(bot, DepositsListMenu(user, list.toMutableList(), 1, this), message.lastMenuMessageId)
            }
        }
        return true
    }

    @Serializable
    private enum class ButtonPayload {
        BACK
    }
}