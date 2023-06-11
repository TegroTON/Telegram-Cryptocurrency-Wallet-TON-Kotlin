package money.tegro.bot.inlines

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.button
import money.tegro.bot.wallet.BlockchainType
import money.tegro.bot.wallet.CryptoCurrency

@Serializable
class WalletDepositSelectNetworkMenu(
    val user: User,
    val currency: CryptoCurrency,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, botMessage: BotMessage) {
        bot.updateKeyboard(
            to = botMessage.peerId,
            lastMenuMessageId = botMessage.lastMenuMessageId,
            message = Messages[user.settings.lang].menuWalletDepositSelectNetworkMessage.format(currency.ticker),
            keyboard = BotKeyboard {
                if (currency.nativeBlockchainType != null) {
                    val networkName = currency.nativeBlockchainType.displayName
                    row {
                        button(
                            networkName, ButtonPayload.serializer(),
                            ButtonPayload.Network(currency.nativeBlockchainType)
                        )
                    }
                } else {
                    for (tokenContract: Pair<BlockchainType, String> in currency.tokenContracts) {
                        val networkName = tokenContract.first.displayName
                        row {
                            button(networkName, ButtonPayload.serializer(), ButtonPayload.Network(tokenContract.first))
                        }
                    }
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
            ButtonPayload.Back -> user.setMenu(bot, parentMenu, botMessage)
            is ButtonPayload.Network -> user.setMenu(
                bot,
                WalletDepositMenu(user, currency, payloadValue.blockchainType, this),
                botMessage
            )
        }
        return true
    }

    @Serializable
    private sealed class ButtonPayload {
        @Serializable
        @SerialName("back")
        object Back : ButtonPayload()

        @Serializable
        @SerialName("network")
        data class Network(val blockchainType: BlockchainType) : ButtonPayload()
    }
}