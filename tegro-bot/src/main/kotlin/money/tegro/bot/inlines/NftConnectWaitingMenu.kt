package money.tegro.bot.inlines

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.api.TgBot
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.PostgresUserPersistent
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.PostgresNftsPersistent
import money.tegro.bot.utils.button
import money.tegro.bot.wallet.Coins
import money.tegro.bot.wallet.CryptoCurrency

@Serializable
class NftConnectWaitingMenu(
    val user: User,
    val address: String,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, botMessage: BotMessage) {
        val chars = ('a'..'z').toList() + ('0'..'9').toList()
        val verifyCode = (0 until 6).map { chars.random() }.joinToString("")
        val displayMasterAddress = buildString {
            if (bot is TgBot) append("<code>")
            append("EQA1Mg34Zy5nLWfXHocsuuZo911Wi5faf-iGoM-_A8X-9z0e")
            if (bot is TgBot) append("</code>")
        }
        val displayAddress = buildString {
            if (bot is TgBot) append("<code>")
            append(address)
            if (bot is TgBot) append("</code>")
        }
        val message = buildString {
            if (bot is TgBot) append("<code>")
            append("Connect to TegroWalletBot @$verifyCode")
            if (bot is TgBot) append("</code>")
        }
        bot.updateKeyboard(
            to = botMessage.peerId,
            lastMenuMessageId = botMessage.lastMenuMessageId,
            message = Messages[user.settings.lang].menuNftConnectWaitingMessage.format(
                Coins(CryptoCurrency.TON, CryptoCurrency.TON.botFee),
                displayMasterAddress,
                message,
                displayAddress
            ),
            keyboard = BotKeyboard {
                row {
                    button(
                        Messages[user.settings.lang].menuNftConnectWaitingCheck,
                        ButtonPayload.serializer(),
                        ButtonPayload.Check(verifyCode)
                    )
                }
                row {
                    button(
                        Messages[user.settings.lang].menuButtonBack,
                        ButtonPayload.serializer(),
                        ButtonPayload.Back
                    )
                }
            }
        )
    }

    override suspend fun handleMessage(bot: Bot, botMessage: BotMessage): Boolean {
        val payload = botMessage.payload ?: return false
        when (val payloadValue = Json.decodeFromString<ButtonPayload>(payload)) {
            ButtonPayload.Back -> {
                user.setMenu(bot, parentMenu, botMessage)
            }

            is ButtonPayload.Check -> {
                val verifyCode = payloadValue.verifyCode
                val result = PostgresNftsPersistent.checkTransactions(address, verifyCode)
                if (result) {
                    val userSettings = user.settings.copy(address = address)
                    PostgresUserPersistent.saveSettings(userSettings)
                    val newUser = user.copy(
                        settings = userSettings
                    )
                    newUser.setMenu(bot, NftMenu(newUser, MainMenu(newUser)), botMessage)
                } else {
                    bot.sendPopup(botMessage, Messages[user].menuNftConnectWaitingCheckFailed)
                    return true
                }
            }
        }
        return true
    }

    @Serializable
    private sealed class ButtonPayload {
        @Serializable
        @SerialName("back")
        object Back : ButtonPayload()

        @Serializable
        @SerialName("check")
        data class Check(val verifyCode: String) : ButtonPayload()
    }
}