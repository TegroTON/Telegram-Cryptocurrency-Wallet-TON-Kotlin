package money.tegro.bot.inlines

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.MASTER_KEY
import money.tegro.bot.api.Bot
import money.tegro.bot.api.TgBot
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.ton.TonBlockchainManager
import money.tegro.bot.utils.UserPrivateKey
import money.tegro.bot.utils.button
import money.tegro.bot.utils.linkButton
import money.tegro.bot.wallet.CryptoCurrency

@Serializable
class WalletDepositMenu(
    val user: User,
    val currency: CryptoCurrency,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        val privateKey = UserPrivateKey(user.id, MASTER_KEY)
        val userTonAddress = TonBlockchainManager.getAddress(privateKey)
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = String.format(
                Messages[user.settings.lang].menuWalletDepositMessage,
                currency.ticker,
                userTonAddress
            ),
            keyboard = BotKeyboard {
                if (currency == CryptoCurrency.TON && bot is TgBot) {
                    row {
                        linkButton(
                            Messages[user.settings.lang].menuWalletDepositLink,
                            "ton://transfer/${userTonAddress}",
                            ButtonPayload.serializer(),
                            ButtonPayload.LINK
                        )
                    }
                }
                row {
                    button(
                        Messages[user.settings.lang].menuWalletDepositQR,
                        ButtonPayload.serializer(),
                        ButtonPayload.QR
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
