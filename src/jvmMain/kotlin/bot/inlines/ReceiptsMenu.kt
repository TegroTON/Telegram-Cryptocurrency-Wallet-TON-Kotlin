package bot.inlines

import bot.api.Bot
import bot.objects.BotMessage
import bot.objects.MessagesContainer
import bot.objects.User
import bot.objects.keyboard.BotKeyboard
import bot.receipts.PostgresReceiptPersistent
import bot.utils.button
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
class ReceiptsMenu(
    val user: User,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = MessagesContainer[user.settings.lang].menuReceiptsMessage,
            keyboard = BotKeyboard {
                row {
                    button(
                        MessagesContainer[user.settings.lang].menuReceiptsCreate,
                        ButtonPayload.serializer(),
                        ButtonPayload.CREATE
                    )
                }
                row {
                    button(
                        MessagesContainer[user.settings.lang].menuReceiptsList,
                        ButtonPayload.serializer(),
                        ButtonPayload.LIST
                    )
                }
                row {
                    button(
                        MessagesContainer[user.settings.lang].menuButtonBack,
                        WalletMenu.ButtonPayload.serializer(),
                        WalletMenu.ButtonPayload.BACK
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
    enum class ButtonPayload {
        CREATE,
        LIST,
        BACK
    }
}