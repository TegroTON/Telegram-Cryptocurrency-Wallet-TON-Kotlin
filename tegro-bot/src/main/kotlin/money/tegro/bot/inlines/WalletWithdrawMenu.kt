package money.tegro.bot.inlines

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.MASTER_KEY
import money.tegro.bot.api.Bot
import money.tegro.bot.blockchain.BlockchainManager
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.UserPrivateKey
import money.tegro.bot.utils.button
import money.tegro.bot.wallet.BlockchainType
import money.tegro.bot.wallet.Coins
import money.tegro.bot.wallet.CryptoCurrency
import money.tegro.bot.walletPersistent
import java.util.*

@Serializable
class WalletWithdrawMenu(
    val user: User,
    val currency: CryptoCurrency,
    val network: BlockchainType,
    val amount: Coins,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        val message = buildString {
            appendLine(
                String.format(
                    Messages[user].menuWalletWithdrawMessage,
                    currency.ticker,
                    network.displayName
                )
            )
        }
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = message,
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

    override suspend fun handleMessage(bot: Bot, message: BotMessage): Boolean {
        val messageBody = message.body
        if (message.payload != null) {
            val payload = message.payload
            when (Json.decodeFromString<ButtonPayload>(payload)) {
                ButtonPayload.BACK -> {
                    user.setMenu(bot, parentMenu, message.lastMenuMessageId)
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

            bot.sendMessage(
                message.peerId,
                Messages[user].walletMenuWithdrawMessage.format(amount)
            )
            walletPersistent.freeze(user, amount + currency.botFee)
            try {
                val pk = UserPrivateKey(UUID(0, 0), MASTER_KEY)
                if (amount.currency.isNative) {
                    blockchainManager.transfer(
                        pk.key.toByteArray(),
                        withdrawAddress,
                        amount
                    )
                } else {
                    blockchainManager.transferToken(
                        pk.key.toByteArray(),
                        amount.currency,
                        withdrawAddress,
                        amount
                    )
                }
                walletPersistent.updateFreeze(user, amount.currency) {
                    (it - (amount + currency.botFee)).also {
                        println(
                            "Remove from freeze:\n" +
                                    " old freeze: $it\n" +
                                    " amount    : ${amount + currency.botFee}\n" +
                                    " new freeze: ${it - amount.currency.botFee}"
                        )
                    }
                }
            } catch (e: Throwable) {
                walletPersistent.unfreeze(user, amount + currency.botFee)
                throw e
            }
        }
        return true
    }

    @Serializable
    private enum class ButtonPayload {
        BACK
    }
}
