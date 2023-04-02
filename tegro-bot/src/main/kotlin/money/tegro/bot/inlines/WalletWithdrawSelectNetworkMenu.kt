package money.tegro.bot.inlines

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
class WalletWithdrawSelectNetworkMenu(
    val user: User,
    val currency: CryptoCurrency,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        println("send select network: $currency")
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = Messages[user.settings.lang].menuWalletWithdrawSelectMessage,
            keyboard = BotKeyboard {
                if (currency.nativeBlockchainType != null) {
                    val networkName = currency.nativeBlockchainType.displayName
                    row {
                        button(networkName, ButtonPayload.serializer(), Network(currency.nativeBlockchainType))
                    }
                } else {
                    for (tokenContract: Pair<BlockchainType, String> in currency.tokenContracts) {
                        val networkName = tokenContract.first.displayName
                        row {
                            button(networkName, ButtonPayload.serializer(), Network(tokenContract.first))
                        }
                    }
                }
                row {
                    button(
                        Messages[user.settings.lang].menuButtonBack,
                        ButtonPayload.serializer(),
                        Back
                    )
                }
            }
        )
    }

    override suspend fun handleMessage(bot: Bot, message: BotMessage): Boolean {
        val payload = message.payload ?: return false
        when (val blockchainType = Json.decodeFromString<ButtonPayload>(payload)) {
            Back -> user.setMenu(bot, parentMenu, message.lastMenuMessageId)
            is Network -> user.setMenu(
                bot,
                WalletWithdrawSelectAmountMenu(user, CryptoCurrency.TON, blockchainType.blockchainType, this),
                message.lastMenuMessageId
            )
        }
        return true
    }

    @Serializable
    sealed class ButtonPayload

    @Serializable
    object Back : ButtonPayload()

    @Serializable
    class Network(val blockchainType: BlockchainType) : ButtonPayload()
}