package money.tegro.bot.inlines

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.blockchain.BlockchainManager
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.NftsPersistent
import money.tegro.bot.utils.button
import money.tegro.bot.wallet.BlockchainType
import money.tegro.bot.wallet.Coins

@Serializable
class WalletWithdrawSelectAddressMenu(
    val user: User,
    val network: BlockchainType,
    val coins: Coins,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, botMessage: BotMessage) {
        val message = buildString {
            appendLine(
                String.format(
                    Messages[user].menuWalletWithdrawSelectAddressMessage,
                    coins.currency.ticker,
                    network.displayName,
                    coins,
                    Coins(coins.currency, NftsPersistent.countBotFee(user, coins.currency)),
                )
            )
        }
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
            to = botMessage.peerId,
            lastMenuMessageId = botMessage.lastMenuMessageId,
            message = message,
            keyboard = BotKeyboard {
                if (address != "" && network == BlockchainType.TON) {
                    row {
                        button(
                            displayAddress,
                            ButtonPayload.serializer(),
                            ButtonPayload.MY
                        )
                    }
                }
                row {
                    button(
                        Messages[user].menuButtonBack,
                        ButtonPayload.serializer(),
                        ButtonPayload.BACK
                    )
                }
            }
        )
    }

    override suspend fun handleMessage(bot: Bot, botMessage: BotMessage): Boolean {
        val messageBody = botMessage.body
        if (botMessage.payload != null) {
            val payload = botMessage.payload
            when (Json.decodeFromString<ButtonPayload>(payload)) {
                ButtonPayload.BACK -> {
                    user.setMenu(bot, parentMenu, botMessage)
                }

                ButtonPayload.MY -> {
                    user.setMenu(
                        bot,
                        WalletWithdrawApproveMenu(user, user.settings.address, network, coins, this),
                        botMessage
                    )
                }
            }
        } else if (messageBody != null) {
            val blockchainManager = BlockchainManager[network]
            val withdrawAddress: String = messageBody
            if (!blockchainManager.isValidAddress(messageBody)) {
                bot.sendMessage(
                    botMessage.peerId,
                    Messages[user].walletMenuWithdrawInvalidAddress
                )
                return true
            }
            user.setMenu(
                bot,
                WalletWithdrawApproveMenu(user, withdrawAddress, network, coins, this),
                botMessage
            )
        }
        return true
    }

    @Serializable
    private enum class ButtonPayload {
        MY,
        BACK
    }
}
