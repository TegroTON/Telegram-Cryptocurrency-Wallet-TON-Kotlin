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
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
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
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = message,
            keyboard = BotKeyboard {
                if (address != "") {
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

    override suspend fun handleMessage(bot: Bot, message: BotMessage): Boolean {
        val messageBody = message.body
        if (message.payload != null) {
            val payload = message.payload
            when (Json.decodeFromString<ButtonPayload>(payload)) {
                ButtonPayload.BACK -> {
                    user.setMenu(bot, parentMenu, message.lastMenuMessageId)
                }

                ButtonPayload.MY -> {
                    user.setMenu(
                        bot,
                        WalletWithdrawApproveMenu(user, user.settings.address, network, coins, this),
                        message.lastMenuMessageId
                    )
                }
            }
        } else if (messageBody != null) {
            val blockchainManager = BlockchainManager[network]
            val withdrawAddress: String = messageBody
            if (!blockchainManager.isValidAddress(messageBody)) {
                bot.sendMessage(
                    message.peerId,
                    Messages[user].walletMenuWithdrawInvalidAddress
                )
                return true
            }
            user.setMenu(
                bot,
                WalletWithdrawApproveMenu(user, withdrawAddress, network, coins, this),
                message.lastMenuMessageId
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
