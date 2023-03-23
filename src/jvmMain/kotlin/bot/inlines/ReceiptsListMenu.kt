package bot.inlines

import bot.api.Bot
import bot.objects.BotMessage
import bot.objects.MessagesContainer
import bot.objects.User
import bot.objects.keyboard.BotKeyboard
import bot.receipts.Receipt
import bot.utils.button
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

@Serializable
class ReceiptsListMenu(
    val user: User,
    val receipts: MutableList<Receipt>,
    val page: Int,
    val parentMenu: Menu
) : Menu {

    private var maxPages: Int = max(receipts.size - 1, 0) / 6 + 1
    private var start = if (page == 1) 0 else (page - 1) * 6
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        if (page > maxPages) {
            user.setMenu(bot, ReceiptsListMenu(user, receipts, 1, this), lastMenuMessageId)
            return
        }
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = buildString {
                appendLine(MessagesContainer[user.settings.lang].menuReceiptsListMessage)
                if (receipts.isNotEmpty()) {
                    var count = 1
                    for (receipt in receipts.subList(start, min(receipts.size, start + 6))) {
                        val date = Date.from(receipt.issueTime.toJavaInstant())
                        val time =
                            SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(date)
                        appendLine()
                        appendLine(
                            String.format(
                                MessagesContainer[user.settings.lang].menuReceiptsListEntry,
                                count++.toString(),
                                time,
                                receipt.coins
                            )
                        )
                        val recipient = receipt.recipient
                        if (recipient != null) {
                            appendLine(MessagesContainer[user.settings.lang].menuReceiptsListWithRecipient)
                        } else {
                            appendLine(MessagesContainer[user.settings.lang].menuReceiptsListWithoutRecipient)
                        }
                    }
                } else {
                    appendLine()
                    appendLine(MessagesContainer[user.settings.lang].menuReceiptsListEmpty)
                }
            },
            keyboard = BotKeyboard {
                if (receipts.size < 3) {
                    row {
                        var first = true
                        for (receipt: Receipt in receipts) {
                            if (first) {
                                button(getText(receipt, "1"), ButtonPayload.serializer(), ButtonPayload.ONE_ONE)
                                first = false
                            } else {
                                button(getText(receipt, "2"), ButtonPayload.serializer(), ButtonPayload.ONE_TWO)
                            }
                        }
                    }
                } else {
                    row {
                        var first = true
                        for (receipt: Receipt in receipts.subList(start, min(receipts.size, start + 2))) {
                            if (first) {
                                button(getText(receipt, "1"), ButtonPayload.serializer(), ButtonPayload.ONE_ONE)
                                first = false
                            } else {
                                button(getText(receipt, "2"), ButtonPayload.serializer(), ButtonPayload.ONE_TWO)
                            }
                        }
                    }
                    if (start - receipts.size > 2 || start == 0) {
                        row {
                            var first = true
                            for (receipt: Receipt in receipts.subList(start + 2, min(receipts.size, start + 4))) {
                                if (first) {
                                    button(getText(receipt, "3"), ButtonPayload.serializer(), ButtonPayload.TWO_ONE)
                                    first = false
                                } else {
                                    button(getText(receipt, "4"), ButtonPayload.serializer(), ButtonPayload.TWO_TWO)
                                }
                            }
                        }
                    }
                    if (start - receipts.size > 4 || start == 0) {
                        row {
                            var first = true
                            for (receipt: Receipt in receipts.subList(start + 4, min(receipts.size, start + 6))) {
                                if (first) {
                                    button(getText(receipt, "5"), ButtonPayload.serializer(), ButtonPayload.THREE_ONE)
                                    first = false
                                } else {
                                    button(getText(receipt, "6"), ButtonPayload.serializer(), ButtonPayload.THREE_TWO)
                                }
                            }
                        }
                    }
                }

                if (receipts.size > 6) {
                    row {
                        if (page != 1) {
                            button("← " + (page - 1), ButtonPayload.serializer(), ButtonPayload.PREVIOUS_PAGE)
                        }
                        button(
                            MessagesContainer[user.settings.lang].menuReceiptsListBack,
                            ButtonPayload.serializer(),
                            ButtonPayload.BACK
                        )
                        if (page != maxPages) {
                            button("" + (page + 1) + " →", ButtonPayload.serializer(), ButtonPayload.NEXT_PAGE)
                        }
                    }
                } else {
                    row {
                        button(
                            MessagesContainer[user.settings.lang].menuReceiptsListBack,
                            ButtonPayload.serializer(),
                            ButtonPayload.BACK
                        )
                    }
                }
            }
        )
    }

    override suspend fun handleMessage(bot: Bot, message: BotMessage): Boolean {
        val payload = message.payload ?: return false
        when (Json.decodeFromString<ButtonPayload>(payload)) {

            ButtonPayload.ONE_ONE -> user.setMenu(
                bot,
                ReceiptReadyMenu(user, receipts[start], ReceiptsMenu(user, MainMenu(user))),
                message.lastMenuMessageId
            )

            ButtonPayload.ONE_TWO -> user.setMenu(
                bot,
                ReceiptReadyMenu(user, receipts[start + 1], ReceiptsMenu(user, MainMenu(user))),
                message.lastMenuMessageId
            )

            ButtonPayload.TWO_ONE -> user.setMenu(
                bot,
                ReceiptReadyMenu(user, receipts[start + 2], ReceiptsMenu(user, MainMenu(user))),
                message.lastMenuMessageId
            )

            ButtonPayload.TWO_TWO -> user.setMenu(
                bot,
                ReceiptReadyMenu(user, receipts[start + 3], ReceiptsMenu(user, MainMenu(user))),
                message.lastMenuMessageId
            )

            ButtonPayload.THREE_ONE -> user.setMenu(
                bot,
                ReceiptReadyMenu(user, receipts[start + 4], ReceiptsMenu(user, MainMenu(user))),
                message.lastMenuMessageId
            )

            ButtonPayload.THREE_TWO -> user.setMenu(
                bot,
                ReceiptReadyMenu(user, receipts[start + 5], ReceiptsMenu(user, MainMenu(user))),
                message.lastMenuMessageId
            )

            ButtonPayload.PREVIOUS_PAGE -> user.setMenu(
                bot,
                ReceiptsListMenu(user, receipts, page - 1, parentMenu),
                message.lastMenuMessageId
            )

            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, message.lastMenuMessageId)
            }

            ButtonPayload.NEXT_PAGE -> user.setMenu(
                bot,
                ReceiptsListMenu(user, receipts, page + 1, parentMenu),
                message.lastMenuMessageId
            )
        }
        return true
    }

    private fun getText(receipt: Receipt, id: String): String {
        return String.format(MessagesContainer[user.settings.lang].menuReceiptsListKeyboardEntry, id, receipt.coins)
    }

    @Serializable
    enum class ButtonPayload {
        ONE_ONE,
        ONE_TWO,
        TWO_ONE,
        TWO_TWO,
        THREE_ONE,
        THREE_TWO,
        NEXT_PAGE,
        PREVIOUS_PAGE,
        BACK
    }
}