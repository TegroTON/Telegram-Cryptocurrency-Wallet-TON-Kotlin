package money.tegro.bot.inlines

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.receipts.Receipt
import money.tegro.bot.utils.button

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
            message = Messages[user.settings.lang].menuReceiptLimitationsMessage,
            keyboard = BotKeyboard {
                row {
                    //TODO: ref
                    button(
                        Messages[user.settings.lang].menuReceiptLimitationsRef,
                        ButtonPayload.serializer(),
                        ButtonPayload.REF
                    )
                }
                row {
                    button(
                        Messages[user.settings.lang].menuReceiptLimitationsSub,
                        ButtonPayload.serializer(),
                        ButtonPayload.SUB
                    )
                }
                row {
                    button(
                        if (receipt.recipient == null)
                            Messages[user.settings.lang].menuReceiptLimitationsUser
                        else
                            Messages[user.settings.lang].menuReceiptLimitationsUserUnattach,
                        ButtonPayload.serializer(),
                        ButtonPayload.USER
                    )
                }
                row {
                    button(
                        Messages[user.settings.lang].menuReceiptLimitationsCaptcha,
                        ButtonPayload.serializer(),
                        ButtonPayload.CAPTCHA
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
            ButtonPayload.REF -> TODO()
            ButtonPayload.SUB -> user.setMenu(
                bot,
                ReceiptSubscriberMenu(user, receipt, this),
                message.lastMenuMessageId
            )

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
    private enum class ButtonPayload {
        REF,
        SUB,
        USER,
        CAPTCHA,
        BACK
    }
}