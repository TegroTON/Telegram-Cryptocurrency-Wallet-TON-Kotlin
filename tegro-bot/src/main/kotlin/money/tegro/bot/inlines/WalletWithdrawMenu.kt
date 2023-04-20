package money.tegro.bot.inlines

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.MASTER_KEY
import money.tegro.bot.api.Bot
import money.tegro.bot.blockchain.BlockchainManager
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.LogType
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.LogsUtil
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
                    amount,
                    Coins(currency, currency.botFee),
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
            val active = walletPersistent.loadWalletState(user).active[currency]
            if (active < amount) return false
            val fee = Coins(amount.currency, currency.botFee)
            val amountWithFee = amount + fee

            bot.sendMessage(
                message.peerId,
                Messages[user].walletMenuWithdrawMessage.format(amount, fee)
            )
            walletPersistent.freeze(user, amountWithFee)
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
                val oldFreeze = walletPersistent.loadWalletState(user).frozen[amount.currency]
                walletPersistent.updateFreeze(user, amount.currency) {
                    (it - amountWithFee).also {
                        println(
                            "Remove from freeze:\n" +
                                    " old freeze: $oldFreeze\n" +
                                    " amount    : $amountWithFee\n" +
                                    " new freeze: $it"
                        )
                    }
                }
                LogsUtil.log(user, "$amount", LogType.WITHDRAW)
                LogsUtil.log(
                    user,
                    "$amount (fee: ${currency.botFee}), balance ${active - amount}",
                    LogType.WITHDRAW_ADMIN
                )
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
