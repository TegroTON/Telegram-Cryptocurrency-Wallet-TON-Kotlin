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
import money.tegro.bot.receipts.Receipt
import money.tegro.bot.utils.button

@Serializable
data class ReceiptLimitationsMenu(
    val user: User,
    val receipt: Receipt,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        val captchaActivation = if (receipt.captcha) Messages[user].enabled else Messages[user].disabled
        val onlyNewActivation = if (receipt.onlyNew) Messages[user].enabled else Messages[user].disabled
        val onlyPremiumActivation = if (receipt.onlyPremium) Messages[user].enabled else Messages[user].disabled
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = Messages[user].menuReceiptLimitationsMessage,
            keyboard = BotKeyboard {
                row {
                    //TODO: ref
                    button(
                        Messages[user].menuReceiptLimitationsRef,
                        ButtonPayload.serializer(),
                        ButtonPayload.REF
                    )
                }
                row {
                    button(
                        Messages[user].menuReceiptLimitationsSub,
                        ButtonPayload.serializer(),
                        ButtonPayload.SUB
                    )
                }
                row {
                    button(
                        if (receipt.recipient == null)
                            Messages[user].menuReceiptLimitationsUser
                        else
                            Messages[user].menuReceiptLimitationsUserUnattach,
                        ButtonPayload.serializer(),
                        ButtonPayload.USER
                    )
                }
                row {
                    button(
                        Messages[user].menuReceiptLimitationsCaptcha.format(captchaActivation),
                        ButtonPayload.serializer(),
                        ButtonPayload.CAPTCHA
                    )
                }
                row {
                    button(
                        Messages[user].menuReceiptLimitationsOnlyNew.format(onlyNewActivation),
                        ButtonPayload.serializer(),
                        ButtonPayload.ONLY_NEW
                    )
                }
                row {
                    button(
                        Messages[user].menuReceiptLimitationsOnlyPremium.format(onlyPremiumActivation),
                        ButtonPayload.serializer(),
                        ButtonPayload.ONLY_PREMIUM
                    )
                }
                row {
                    button(
                        Messages[user].menuButtonBack,
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

            ButtonPayload.CAPTCHA -> {
                val newReceipt = receipt.copy(captcha = receipt.captcha.not())
                PostgresReceiptPersistent.saveReceipt(newReceipt)
                user.setMenu(
                    bot, ReceiptLimitationsMenu(
                        user,
                        newReceipt,
                        ReceiptReadyMenu(user, newReceipt, ReceiptsMenu(user, MainMenu(user)))
                    ), message.lastMenuMessageId
                )
            }

            ButtonPayload.ONLY_NEW -> {
                val newReceipt = receipt.copy(onlyNew = receipt.onlyNew.not())
                PostgresReceiptPersistent.saveReceipt(newReceipt)
                user.setMenu(
                    bot, ReceiptLimitationsMenu(
                        user,
                        newReceipt,
                        ReceiptReadyMenu(user, newReceipt, ReceiptsMenu(user, MainMenu(user)))
                    ), message.lastMenuMessageId
                )
            }

            ButtonPayload.ONLY_PREMIUM -> {
                val newReceipt = receipt.copy(onlyPremium = receipt.onlyPremium.not())
                PostgresReceiptPersistent.saveReceipt(newReceipt)
                user.setMenu(
                    bot, ReceiptLimitationsMenu(
                        user,
                        newReceipt,
                        ReceiptReadyMenu(user, newReceipt, ReceiptsMenu(user, MainMenu(user)))
                    ), message.lastMenuMessageId
                )
            }

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
        ONLY_NEW,
        ONLY_PREMIUM,
        BACK
    }
}