package bot.inlines

import bot.api.Bot
import bot.objects.BotMessage
import bot.objects.Messages
import bot.objects.User
import bot.objects.keyboard.BotKeyboard
import bot.receipts.Receipt
import bot.utils.button
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class ReceiptLimitationsMenu(
    val user: User,
    val receipt: Receipt,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = Messages.menuReceiptLimitationsMessage,
            keyboard = BotKeyboard {
                row {
                    //TODO: ref
                    button(Messages.menuReceiptLimitationsRef, ButtonPayload.serializer(), ButtonPayload.REF)
                }
                row {
                    //TODO: sub
                    button(Messages.menuReceiptLimitationsSub, ButtonPayload.serializer(), ButtonPayload.SUB)
                }
                row {
                    button(Messages.menuReceiptLimitationsUser, ButtonPayload.serializer(), ButtonPayload.USER)
                }
                row {
                    button(Messages.menuReceiptLimitationsCaptcha, ButtonPayload.serializer(), ButtonPayload.CAPTCHA)
                }
                row {
                    button(
                        Messages.menuButtonBack,
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
            ButtonPayload.REF -> TODO()
            ButtonPayload.SUB -> TODO()
            ButtonPayload.USER -> user.setMenu(
                bot,
                ReceiptRecipientMenu(user, receipt, this),
                message.lastMenuMessageId
            )

            ButtonPayload.CAPTCHA -> TODO()
            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, message.lastMenuMessageId)
            }
        }
        return true
    }

    @Serializable
    enum class ButtonPayload {
        REF,
        SUB,
        USER,
        CAPTCHA,
        BACK
    }
}