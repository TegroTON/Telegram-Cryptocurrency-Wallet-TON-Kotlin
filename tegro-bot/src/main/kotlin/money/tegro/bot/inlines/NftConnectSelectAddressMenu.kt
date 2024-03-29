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
import money.tegro.bot.utils.button
import money.tegro.bot.wallet.BlockchainType

@Serializable
class NftConnectSelectAddressMenu(
    val user: User,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, botMessage: BotMessage) {
        bot.updateKeyboard(
            to = botMessage.peerId,
            lastMenuMessageId = botMessage.lastMenuMessageId,
            message = Messages[user].menuNftConnectSelectAddressMessage,
            keyboard = BotKeyboard {
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
            }
        } else if (messageBody != null) {
            val blockchainManager = BlockchainManager[BlockchainType.TON]
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
                NftConnectWaitingMenu(user, withdrawAddress, this),
                botMessage
            )
        }
        return true
    }

    @Serializable
    private enum class ButtonPayload {
        BACK
    }
}
