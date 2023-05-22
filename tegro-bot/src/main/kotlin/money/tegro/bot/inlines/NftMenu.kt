package money.tegro.bot.inlines

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.PostgresNftsPersistent
import money.tegro.bot.utils.button
import money.tegro.bot.utils.linkButton

@Serializable
class NftMenu(
    val user: User,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        val address = user.settings.address
        val displayAddress = buildString {
            if (address == "") {
                append("null")
            } else {
                append(address.substring(0, 4))
                append("...")
                append(address.substring(address.length - 5))
            }
        }
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = Messages[user.settings.lang].menuNftMessage,
            keyboard = BotKeyboard {
                row {
                    button(
                        Messages[user.settings.lang].menuNftMy,
                        ButtonPayload.serializer(),
                        ButtonPayload.MY_NFT
                    )
                }
                row {
                    button(
                        if (address == "") Messages[user.settings.lang].menuNftConnect else Messages[user.settings.lang].menuNftDisconnect.format(
                            displayAddress
                        ),
                        ButtonPayload.serializer(),
                        ButtonPayload.CONNECT
                    )
                }
                row {
                    linkButton(
                        Messages[user.settings.lang].menuNftLibermall,
                        "https://libermall.com/?utm_source=telegram&utm_medium=social&utm_campaign=bot&utm_content=telegrambot&utm_term=dex",
                        ButtonPayload.serializer(),
                        ButtonPayload.BACK
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
            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, message.lastMenuMessageId)
            }

            ButtonPayload.CONNECT -> {
                val address = user.settings.address
                if (address == "") {
                    user.setMenu(
                        bot,
                        NftConnectSelectAddressMenu(user, this),
                        message.lastMenuMessageId
                    )
                } else {
                    user.setMenu(
                        bot,
                        NftDisconnectMenu(user, this),
                        message.lastMenuMessageId
                    )
                }
            }

            ButtonPayload.MY_NFT -> {
                val list = PostgresNftsPersistent.getNftsByUser(user)
                user.setMenu(bot, NftListMenu(user, list.toMutableList(), 1, this), message.lastMenuMessageId)
            }
        }
        return true
    }

    @Serializable
    private enum class ButtonPayload {
        MY_NFT,
        CONNECT,
        BACK
    }
}