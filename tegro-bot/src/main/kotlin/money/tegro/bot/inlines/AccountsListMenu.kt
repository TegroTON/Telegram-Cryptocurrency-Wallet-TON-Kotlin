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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

@Serializable
class AccountsListMenu(
    val user: User,
    val accounts: MutableList<Account>,
    val page: Int,
    val parentMenu: Menu
) : Menu {

    private var maxPages: Int = max(accounts.size - 1, 0) / 6 + 1
    private var start = if (page == 1) 0 else (page - 1) * 6
    override suspend fun sendKeyboard(bot: Bot, botMessage: BotMessage) {
        if (page > maxPages) {
            user.setMenu(bot, AccountsListMenu(user, accounts, 1, this), botMessage)
            return
        }
        bot.updateKeyboard(
            to = botMessage.peerId,
            lastMenuMessageId = botMessage.lastMenuMessageId,
            message = buildString {
                appendLine(Messages[user.settings.lang].menuAccountsListMessage)
                if (accounts.isNotEmpty()) {
                    var count = 1
                    for (account in accounts.subList(start, min(accounts.size, start + 6))) {
                        val date = Date.from(account.issueTime.toJavaInstant())
                        val time =
                            SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(date)
                        appendLine()
                        if (account.maxCoins.amount > 0.toBigInteger()) {
                            appendLine(
                                String.format(
                                    Messages[user.settings.lang].menuAccountsListEntry,
                                    count++.toString(),
                                    time,
                                    account.maxCoins
                                )
                            )
                        } else {
                            appendLine(
                                String.format(
                                    Messages[user.settings.lang].menuAccountsListEntryOpen,
                                    count++.toString(),
                                    time
                                )
                            )
                        }
                    }
                } else {
                    appendLine()
                    appendLine(Messages[user.settings.lang].menuAccountsListEmpty)
                }
            },
            keyboard = BotKeyboard {
                if (accounts.size < 3) {
                    row {
                        var first = true
                        for (account: Account in accounts) {
                            if (first) {
                                button(getText(account, "1"), ButtonPayload.serializer(), ButtonPayload.ONE_ONE)
                                first = false
                            } else {
                                button(getText(account, "2"), ButtonPayload.serializer(), ButtonPayload.ONE_TWO)
                            }
                        }
                    }
                } else {
                    row {
                        var first = true
                        for (account: Account in accounts.subList(start, min(accounts.size, start + 2))) {
                            if (first) {
                                button(getText(account, "1"), ButtonPayload.serializer(), ButtonPayload.ONE_ONE)
                                first = false
                            } else {
                                button(getText(account, "2"), ButtonPayload.serializer(), ButtonPayload.ONE_TWO)
                            }
                        }
                    }
                    if (start - accounts.size > 2 || start == 0) {
                        row {
                            var first = true
                            for (account: Account in accounts.subList(start + 2, min(accounts.size, start + 4))) {
                                if (first) {
                                    button(getText(account, "3"), ButtonPayload.serializer(), ButtonPayload.TWO_ONE)
                                    first = false
                                } else {
                                    button(getText(account, "4"), ButtonPayload.serializer(), ButtonPayload.TWO_TWO)
                                }
                            }
                        }
                    }
                    if (start == 0 && accounts.size > 4) {
                        row {
                            var first = true
                            for (account: Account in accounts.subList(start + 4, min(accounts.size, start + 6))) {
                                if (first) {
                                    button(getText(account, "5"), ButtonPayload.serializer(), ButtonPayload.THREE_ONE)
                                    first = false
                                } else {
                                    button(getText(account, "6"), ButtonPayload.serializer(), ButtonPayload.THREE_TWO)
                                }
                            }
                        }
                    } else {
                        if (start - accounts.size > 4) {
                            row {
                                var first = true
                                for (account: Account in accounts.subList(start + 4, min(accounts.size, start + 6))) {
                                    if (first) {
                                        button(
                                            getText(account, "5"),
                                            ButtonPayload.serializer(),
                                            ButtonPayload.THREE_ONE
                                        )
                                        first = false
                                    } else {
                                        button(
                                            getText(account, "6"),
                                            ButtonPayload.serializer(),
                                            ButtonPayload.THREE_TWO
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (accounts.size > 6) {
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

    override suspend fun handleMessage(bot: Bot, botMessage: BotMessage): Boolean {
        val payload = botMessage.payload ?: return false
        when (Json.decodeFromString<ButtonPayload>(payload)) {

            ButtonPayload.ONE_ONE -> user.setMenu(
                bot,
                AccountReadyMenu(user, accounts[start], AccountsMenu(user, MainMenu(user))),
                botMessage
            )

            ButtonPayload.ONE_TWO -> user.setMenu(
                bot,
                AccountReadyMenu(user, accounts[start + 1], AccountsMenu(user, MainMenu(user))),
                botMessage
            )

            ButtonPayload.TWO_ONE -> user.setMenu(
                bot,
                AccountReadyMenu(user, accounts[start + 2], AccountsMenu(user, MainMenu(user))),
                botMessage
            )

            ButtonPayload.TWO_TWO -> user.setMenu(
                bot,
                AccountReadyMenu(user, accounts[start + 3], AccountsMenu(user, MainMenu(user))),
                botMessage
            )

            ButtonPayload.THREE_ONE -> user.setMenu(
                bot,
                AccountReadyMenu(user, accounts[start + 4], AccountsMenu(user, MainMenu(user))),
                botMessage
            )

            ButtonPayload.THREE_TWO -> user.setMenu(
                bot,
                AccountReadyMenu(user, accounts[start + 5], AccountsMenu(user, MainMenu(user))),
                botMessage
            )

            ButtonPayload.PREVIOUS_PAGE -> user.setMenu(
                bot,
                AccountsListMenu(user, accounts, page - 1, parentMenu),
                botMessage
            )

            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, botMessage)
            }

            ButtonPayload.NEXT_PAGE -> user.setMenu(
                bot,
                AccountsListMenu(user, accounts, page + 1, parentMenu),
                botMessage
            )
        }
        return true
    }

    private fun getText(account: Account, id: String): String {
        val valueOrOpen =
            if (account.maxCoins.amount > 0.toBigInteger()) account.maxCoins else Messages[user.settings.lang].open
        return buildString {
            append(Messages[user.settings.lang].menuReceiptsListKeyboardEntry.format(id, valueOrOpen))
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