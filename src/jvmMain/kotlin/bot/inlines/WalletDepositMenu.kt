package bot.inlines

import bot.api.Bot
import bot.api.TgBot
import bot.objects.BotMessage
import bot.objects.MessagesContainer
import bot.objects.User
import bot.objects.keyboard.BotKeyboard
import bot.ton.TonUtils
import bot.tonMasterKey
import bot.utils.button
import bot.utils.linkButton
import bot.wallet.CryptoCurrency
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
class WalletDepositMenu(
    val user: User,
    val currency: CryptoCurrency,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = String.format(MessagesContainer[user.settings.lang].menuWalletDepositMessage, currency.ticker),
            keyboard = BotKeyboard {
                if (currency == CryptoCurrency.TON && bot is TgBot) {
                    val userTonAddress = TonUtils.getUserAddress(user.id, tonMasterKey)
                    row {
                        linkButton(
                            MessagesContainer[user.settings.lang].menuWalletDepositLink,
                            "ton://transfer/${userTonAddress.toString(userFriendly = true, bounceable = false)}",
                            ButtonPayload.serializer(),
                            ButtonPayload.LINK
                        )
                    }
                }
                row {
                    button(
                        MessagesContainer[user.settings.lang].menuWalletDepositQR,
                        ButtonPayload.serializer(),
                        ButtonPayload.QR
                    )
                }
                row {
                    button(
                        MessagesContainer[user.settings.lang].menuButtonBack,
                        WalletMenu.ButtonPayload.serializer(),
                        WalletMenu.ButtonPayload.BACK
                    )
                }
            }
        )
    }

    override suspend fun handleMessage(bot: Bot, message: BotMessage): Boolean {
        val payload = message.payload ?: return false
        when (Json.decodeFromString<ButtonPayload>(payload)) {

            ButtonPayload.LINK -> TODO()
            ButtonPayload.QR -> TODO()

            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, message.lastMenuMessageId)
            }
        }
        return true
    }

    @Serializable
    enum class ButtonPayload {
        LINK,
        QR,
        BACK
    }
}