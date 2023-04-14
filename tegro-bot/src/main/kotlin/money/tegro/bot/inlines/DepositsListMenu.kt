package money.tegro.bot.inlines

import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Deposit
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.button
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

@Serializable
class DepositsListMenu(
    val user: User,
    val allDeposits: MutableList<Deposit>,
    val page: Int,
    val parentMenu: Menu
) : Menu {

    private var deposits: MutableList<Deposit> = allDeposits.filter { !it.isPaid }.toMutableList()

    private var maxPages: Int = max(deposits.size - 1, 0) / 6 + 1
    private var start = if (page == 1) 0 else (page - 1) * 6
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        if (page > maxPages) {
            user.setMenu(bot, DepositsListMenu(user, deposits, 1, this), lastMenuMessageId)
            return
        }
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = buildString {
                appendLine(Messages[user.settings.lang].menuDepositsListMessage)
                if (deposits.isNotEmpty()) {
                    var count = 1
                    for (deposit in deposits.subList(start, min(deposits.size, start + 6))) {
                        val date = Date.from(deposit.finishDate.toJavaInstant())
                        val time =
                            SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(date)
                        appendLine()
                        appendLine(
                            String.format(
                                Messages[user.settings.lang].menuDepositsListEntry,
                                count++.toString(),
                                time,
                                deposit.coins
                            )
                        )
                    }
                } else {
                    appendLine()
                    appendLine(Messages[user.settings.lang].menuDepositsListEmpty)
                }
            },
            keyboard = BotKeyboard {
                if (deposits.size < 3) {
                    row {
                        var first = true
                        for (deposit: Deposit in deposits) {
                            if (first) {
                                button(getText(deposit, "1"), ButtonPayload.serializer(), ButtonPayload.ONE_ONE)
                                first = false
                            } else {
                                button(getText(deposit, "2"), ButtonPayload.serializer(), ButtonPayload.ONE_TWO)
                            }
                        }
                    }
                } else {
                    row {
                        var first = true
                        for (deposit: Deposit in deposits.subList(start, min(deposits.size, start + 2))) {
                            if (first) {
                                button(getText(deposit, "1"), ButtonPayload.serializer(), ButtonPayload.ONE_ONE)
                                first = false
                            } else {
                                button(getText(deposit, "2"), ButtonPayload.serializer(), ButtonPayload.ONE_TWO)
                            }
                        }
                    }
                    if (start - deposits.size > 2 || start == 0) {
                        row {
                            var first = true
                            for (deposit: Deposit in deposits.subList(start + 2, min(deposits.size, start + 4))) {
                                if (first) {
                                    button(getText(deposit, "3"), ButtonPayload.serializer(), ButtonPayload.TWO_ONE)
                                    first = false
                                } else {
                                    button(getText(deposit, "4"), ButtonPayload.serializer(), ButtonPayload.TWO_TWO)
                                }
                            }
                        }
                    }
                    if (start == 0 && deposits.size > 4) {
                        row {
                            var first = true
                            for (deposit: Deposit in deposits.subList(start + 4, min(deposits.size, start + 6))) {
                                if (first) {
                                    button(getText(deposit, "5"), ButtonPayload.serializer(), ButtonPayload.THREE_ONE)
                                    first = false
                                } else {
                                    button(getText(deposit, "6"), ButtonPayload.serializer(), ButtonPayload.THREE_TWO)
                                }
                            }
                        }
                    } else {
                        if (start - deposits.size > 4) {
                            row {
                                var first = true
                                for (deposit: Deposit in deposits.subList(start + 4, min(deposits.size, start + 6))) {
                                    if (first) {
                                        button(
                                            getText(deposit, "5"),
                                            ButtonPayload.serializer(),
                                            ButtonPayload.THREE_ONE
                                        )
                                        first = false
                                    } else {
                                        button(
                                            getText(deposit, "6"),
                                            ButtonPayload.serializer(),
                                            ButtonPayload.THREE_TWO
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (deposits.size > 6) {
                    row {
                        if (page != 1) {
                            button("← " + (page - 1), ButtonPayload.serializer(), ButtonPayload.PREVIOUS_PAGE)
                        }
                        button(
                            Messages[user.settings.lang].menuReceiptsListBack,
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
                            Messages[user.settings.lang].menuReceiptsListBack,
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
                DepositReadyMenu(user, deposits[start], ReceiptsMenu(user, MainMenu(user))),
                message.lastMenuMessageId
            )

            ButtonPayload.ONE_TWO -> user.setMenu(
                bot,
                DepositReadyMenu(user, deposits[start + 1], ReceiptsMenu(user, MainMenu(user))),
                message.lastMenuMessageId
            )

            ButtonPayload.TWO_ONE -> user.setMenu(
                bot,
                DepositReadyMenu(user, deposits[start + 2], ReceiptsMenu(user, MainMenu(user))),
                message.lastMenuMessageId
            )

            ButtonPayload.TWO_TWO -> user.setMenu(
                bot,
                DepositReadyMenu(user, deposits[start + 3], ReceiptsMenu(user, MainMenu(user))),
                message.lastMenuMessageId
            )

            ButtonPayload.THREE_ONE -> user.setMenu(
                bot,
                DepositReadyMenu(user, deposits[start + 4], ReceiptsMenu(user, MainMenu(user))),
                message.lastMenuMessageId
            )

            ButtonPayload.THREE_TWO -> user.setMenu(
                bot,
                DepositReadyMenu(user, deposits[start + 5], ReceiptsMenu(user, MainMenu(user))),
                message.lastMenuMessageId
            )

            ButtonPayload.PREVIOUS_PAGE -> user.setMenu(
                bot,
                DepositsListMenu(user, deposits, page - 1, parentMenu),
                message.lastMenuMessageId
            )

            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, message.lastMenuMessageId)
            }

            ButtonPayload.NEXT_PAGE -> user.setMenu(
                bot,
                DepositsListMenu(user, deposits, page + 1, parentMenu),
                message.lastMenuMessageId
            )
        }
        return true
    }

    private fun getText(deposit: Deposit, id: String): String {
        return buildString {
            append(Messages[user.settings.lang].menuReceiptsListKeyboardEntry.format(id, deposit.coins))
        }
    }

    @Serializable
    private enum class ButtonPayload {
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