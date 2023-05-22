package money.tegro.bot.inlines

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.Nft
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.button
import money.tegro.bot.utils.linkButton
import kotlin.math.max
import kotlin.math.min

@Serializable
class NftListMenu(
    val user: User,
    val nfts: MutableList<Nft>,
    val page: Int,
    val parentMenu: Menu
) : Menu {

    private var maxPages: Int = max(nfts.size - 1, 0) / 6 + 1
    private var start = if (page == 1) 0 else (page - 1) * 6
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        if (page > maxPages) {
            user.setMenu(bot, NftListMenu(user, nfts, 1, parentMenu), lastMenuMessageId)
            return
        }
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = buildString {
                appendLine(Messages[user.settings.lang].menuNftListMessage.format(nfts.size))
                if (nfts.isNotEmpty()) {
                    var count = 1
                    for (nft in nfts.subList(start, min(nfts.size, start + 6))) {
                        appendLine()
                        appendLine(
                            String.format(
                                Messages[user.settings.lang].menuNftListEntry,
                                count++.toString(),
                                nft.name
                            )
                        )
                    }
                } else {
                    appendLine()
                    appendLine(Messages[user.settings.lang].menuNftListEmpty)
                }
            },
            keyboard = BotKeyboard {
                if (nfts.size < 3) {
                    row {
                        var first = true
                        for (nft in nfts) {
                            if (first) {
                                linkButton(
                                    getText(nft, "1"),
                                    "https://explorer.tonnft.tools/nft/" + nft.address,
                                    ButtonPayload.serializer(),
                                    ButtonPayload.BACK
                                )
                                first = false
                            } else {
                                linkButton(
                                    getText(nft, "2"),
                                    "https://explorer.tonnft.tools/nft/" + nft.address,
                                    ButtonPayload.serializer(),
                                    ButtonPayload.BACK
                                )
                            }
                        }
                    }
                } else {
                    row {
                        var first = true
                        for (nft in nfts.subList(start, min(nfts.size, start + 2))) {
                            if (first) {
                                linkButton(
                                    getText(nft, "1"),
                                    "https://explorer.tonnft.tools/nft/" + nft.address,
                                    ButtonPayload.serializer(),
                                    ButtonPayload.BACK
                                )
                                first = false
                            } else {
                                linkButton(
                                    getText(nft, "2"),
                                    "https://explorer.tonnft.tools/nft/" + nft.address,
                                    ButtonPayload.serializer(),
                                    ButtonPayload.BACK
                                )
                            }
                        }
                    }
                    if (start - nfts.size > 2 || start == 0) {
                        row {
                            var first = true
                            for (nft in nfts.subList(start + 2, min(nfts.size, start + 4))) {
                                if (first) {
                                    linkButton(
                                        getText(nft, "3"),
                                        "https://explorer.tonnft.tools/nft/" + nft.address,
                                        ButtonPayload.serializer(),
                                        ButtonPayload.BACK
                                    )
                                    first = false
                                } else {
                                    linkButton(
                                        getText(nft, "4"),
                                        "https://explorer.tonnft.tools/nft/" + nft.address,
                                        ButtonPayload.serializer(),
                                        ButtonPayload.BACK
                                    )
                                }
                            }
                        }
                    }
                    if (start == 0 && nfts.size > 4) {
                        row {
                            var first = true
                            for (nft in nfts.subList(start + 4, min(nfts.size, start + 6))) {
                                if (first) {
                                    linkButton(
                                        getText(nft, "5"),
                                        "https://explorer.tonnft.tools/nft/" + nft.address,
                                        ButtonPayload.serializer(),
                                        ButtonPayload.BACK
                                    )
                                    first = false
                                } else {
                                    linkButton(
                                        getText(nft, "6"),
                                        "https://explorer.tonnft.tools/nft/" + nft.address,
                                        ButtonPayload.serializer(),
                                        ButtonPayload.BACK
                                    )
                                }
                            }
                        }
                    } else {
                        if (start - nfts.size > 4) {
                            row {
                                var first = true
                                for (nft in nfts.subList(start + 4, min(nfts.size, start + 6))) {
                                    if (first) {
                                        linkButton(
                                            getText(nft, "5"),
                                            "https://explorer.tonnft.tools/nft/" + nft.address,
                                            ButtonPayload.serializer(),
                                            ButtonPayload.BACK
                                        )
                                        first = false
                                    } else {
                                        linkButton(
                                            getText(nft, "6"),
                                            "https://explorer.tonnft.tools/nft/" + nft.address,
                                            ButtonPayload.serializer(),
                                            ButtonPayload.BACK
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (nfts.size > 6) {
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

            ButtonPayload.PREVIOUS_PAGE -> user.setMenu(
                bot,
                NftListMenu(user, nfts, page - 1, parentMenu),
                message.lastMenuMessageId
            )

            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, message.lastMenuMessageId)
            }

            ButtonPayload.NEXT_PAGE -> user.setMenu(
                bot,
                NftListMenu(user, nfts, page + 1, parentMenu),
                message.lastMenuMessageId
            )
        }
        return true
    }

    private fun getText(nft: Nft, id: String): String {
        return buildString {
            append(Messages[user.settings.lang].menuNftListKeyboardEntry.format(id, nft.name))
        }
    }

    @Serializable
    private enum class ButtonPayload {
        NEXT_PAGE,
        PREVIOUS_PAGE,
        BACK
    }
}