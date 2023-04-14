package money.tegro.bot.inlines

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.button
import money.tegro.bot.wallet.PostgresAccountsPersistent

@Serializable
class AccountsMenu(
    val user: User,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = Messages[user.settings.lang].menuAccountsMessage,
            keyboard = BotKeyboard {
                row {
                    button(
                        Messages[user.settings.lang].menuAccountsCreate,
                        ButtonPayload.serializer(),
                        ButtonPayload.CREATE
                    )
                }
                row {
                    button(
                        Messages[user.settings.lang].menuAccountsList,
                        ButtonPayload.serializer(),
                        ButtonPayload.LIST
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
        val payload = message.payload ?: return false
        when (Json.decodeFromString<ButtonPayload>(payload)) {
            ButtonPayload.CREATE -> {
                //return false
                user.setMenu(bot, AccountSelectTypeMenu(user, this), message.lastMenuMessageId)
            }

            ButtonPayload.LIST -> {
                val list = PostgresAccountsPersistent.loadAccounts(user).filter { it.isActive }
                user.setMenu(bot, AccountsListMenu(user, list.toMutableList(), 1, this), message.lastMenuMessageId)
            }

            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, message.lastMenuMessageId)
            }
        }
        return true
    }

    @Serializable
    private enum class ButtonPayload {
        CREATE,
        LIST,
        BACK
    }
}