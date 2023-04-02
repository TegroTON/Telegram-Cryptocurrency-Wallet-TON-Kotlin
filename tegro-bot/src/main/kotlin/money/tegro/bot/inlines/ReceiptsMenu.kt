package money.tegro.bot.inlines

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.receipts.PostgresReceiptPersistent
import money.tegro.bot.utils.button

@Serializable
class ReceiptsMenu(
    val user: User,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = Messages[user.settings.lang].menuReceiptsMessage,
            keyboard = BotKeyboard {
                row {
                    button(
                        Messages[user.settings.lang].menuReceiptsCreate,
                        ButtonPayload.serializer(),
                        ButtonPayload.CREATE
                    )
                }
                row {
                    button(
                        Messages[user.settings.lang].menuReceiptsList,
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
            ButtonPayload.CREATE -> user.setMenu(bot, ReceiptSelectCurrencyMenu(user, this), message.lastMenuMessageId)

            ButtonPayload.LIST -> {
                val list = PostgresReceiptPersistent.loadReceipts(user).filter { it.isActive }
                user.setMenu(bot, ReceiptsListMenu(user, list.toMutableList(), 1, this), message.lastMenuMessageId)
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