package money.tegro.bot.inlines

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.MASTER_KEY
import money.tegro.bot.api.Bot
import money.tegro.bot.api.TgBot
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
import money.tegro.bot.walletPersistent
import java.util.*

@Serializable
class WalletWithdrawApproveMenu(
    val user: User,
    val withdrawAddress: String,
    val network: BlockchainType,
    val coins: Coins,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        val displayAddress = buildString {
            if (bot is TgBot) append("<code>")
            append(withdrawAddress)
            if (bot is TgBot) append("</code>")
        }
        val message = buildString {
            appendLine(
                String.format(
                    Messages[user].menuWalletWithdrawApproveMessage,
                    network.displayName,
                    coins,
                    Coins(coins.currency, coins.currency.botFee),
                    displayAddress
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
                        Messages[user].menuButtonApprove,
                        ButtonPayload.serializer(),
                        ButtonPayload.APPROVE
                    )
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
        val payload = message.payload ?: return false
        when (Json.decodeFromString<ButtonPayload>(payload)) {
            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, message.lastMenuMessageId)
            }

            ButtonPayload.APPROVE -> {
                user.setMenu(
                    bot,
                    WalletWithdrawMenu(user, withdrawAddress, network, coins, this),
                    message.lastMenuMessageId
                )

                val blockchainManager = BlockchainManager[network]

                val active = walletPersistent.loadWalletState(user).active[coins.currency]
                if (active < coins) return false
                val fee = Coins(coins.currency, coins.currency.botFee)
                val amountWithFee = coins + fee

                walletPersistent.freeze(user, amountWithFee)
                try {
                    val pk = UserPrivateKey(UUID(0, 0), MASTER_KEY)
                    if (coins.currency.isNative) {
                        blockchainManager.transfer(
                            pk.key.toByteArray(),
                            withdrawAddress,
                            coins
                        )
                    } else {
                        blockchainManager.transferToken(
                            pk.key.toByteArray(),
                            coins.currency,
                            withdrawAddress,
                            coins
                        )
                    }
                    val oldFreeze = walletPersistent.loadWalletState(user).frozen[coins.currency]
                    walletPersistent.updateFreeze(user, coins.currency) {
                        (it - amountWithFee).also {
                            println(
                                "Remove from freeze:\n" +
                                        " old freeze: $oldFreeze\n" +
                                        " amount    : $amountWithFee\n" +
                                        " new freeze: $it"
                            )
                        }
                    }
                    LogsUtil.log(user, "$coins", LogType.WITHDRAW)
                    LogsUtil.log(
                        user,
                        "$coins (fee: ${coins.currency.botFee}), balance ${active - amountWithFee}",
                        LogType.WITHDRAW_ADMIN
                    )
                } catch (e: Throwable) {
                    walletPersistent.unfreeze(user, amountWithFee)
                    throw e
                }
            }
        }
        return true
    }

    @Serializable
    private enum class ButtonPayload {
        APPROVE,
        BACK
    }
}