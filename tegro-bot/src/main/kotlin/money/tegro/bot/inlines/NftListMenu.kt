package money.tegro.bot.inlines

import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.objects.*
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.NftsPersistent
import money.tegro.bot.utils.PostgresNftsPersistent
import money.tegro.bot.utils.button
import money.tegro.bot.utils.linkButton
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.hours

@Serializable
class NftListMenu(
    val user: User,
    val nfts: MutableList<Nft>,
    val page: Int,
    val parentMenu: Menu
) : Menu {

    private var maxPages: Int = max(nfts.size - 1, 0) / 6 + 1
    private var start = if (page == 1) 0 else (page - 1) * 6
    override suspend fun sendKeyboard(bot: Bot, botMessage: BotMessage) {
        if (page > maxPages) {
            user.setMenu(bot, NftListMenu(user, nfts, 1, parentMenu), botMessage)
            return
        }
        bot.updateKeyboard(
            to = botMessage.peerId,
            lastMenuMessageId = botMessage.lastMenuMessageId,
            message = buildString {
                appendLine(
                    Messages[user.settings.lang].menuNftListMessage.format(
                        nfts.size,
                        NftsPersistent.getUserProfitStacking(user),
                        NftsPersistent.getUserProfitBotFee(user)
                    )
                )
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

                row {
                    button(
                        Messages[user.settings.lang].menuNftListUpdate,
                        ButtonPayload.serializer(),
                        ButtonPayload.UPDATE
                    )
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

    override suspend fun handleMessage(bot: Bot, botMessage: BotMessage): Boolean {
        val payload = botMessage.payload ?: return false
        when (Json.decodeFromString<ButtonPayload>(payload)) {

            ButtonPayload.PREVIOUS_PAGE -> user.setMenu(
                bot,
                NftListMenu(user, nfts, page - 1, parentMenu),
                botMessage
            )

            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, botMessage)
            }

            ButtonPayload.NEXT_PAGE -> user.setMenu(
                bot,
                NftListMenu(user, nfts, page + 1, parentMenu),
                botMessage
            )

            ButtonPayload.UPDATE -> {
                val time = PostgresUserPersistent.getCooldown(user, CooldownType.NFT_UPDATE)
                val date = Date.from(time.toJavaInstant())
                val timeDisplay = SimpleDateFormat("dd.MM HH:mm").format(date)
                if (PostgresUserPersistent.checkCooldown(user, CooldownType.NFT_UPDATE)) {
                    val nfts = PostgresNftsPersistent.getNftsByUser(user, true)
                    PostgresUserPersistent.addCooldown(user, CooldownType.NFT_UPDATE, 1.hours)
                    val userSettings = user.settings.copy(nfts = nfts)

                    val userCopy = user.copy(
                        settings = userSettings
                    )
                    if (user.settings.nfts == nfts) {
                        user.setMenu(bot, parentMenu, botMessage)
                    } else {
                        userCopy.setMenu(
                            bot,
                            NftListMenu(userCopy, nfts.toMutableList(), 1, parentMenu),
                            botMessage
                        )
                    }
                } else {
                    bot.sendPopup(botMessage, Messages[user].menNftListCooldown.format(timeDisplay))
                    return true
                }
            }
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
        UPDATE,
        BACK
    }
}