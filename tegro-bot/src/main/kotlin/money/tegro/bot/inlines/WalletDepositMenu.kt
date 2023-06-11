package money.tegro.bot.inlines

import kotlinx.coroutines.*
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.MASTER_KEY
import money.tegro.bot.api.Bot
import money.tegro.bot.api.TgBot
import money.tegro.bot.blockchain.BlockchainManager
import money.tegro.bot.objects.*
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.LogsUtil
import money.tegro.bot.utils.UserPrivateKey
import money.tegro.bot.utils.button
import money.tegro.bot.utils.linkButton
import money.tegro.bot.wallet.BlockchainType
import money.tegro.bot.wallet.Coins
import money.tegro.bot.wallet.CryptoCurrency
import money.tegro.bot.wallet.WalletObserver
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.minutes

@Serializable
class WalletDepositMenu(
    val user: User,
    val currency: CryptoCurrency,
    val network: BlockchainType,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, botMessage: BotMessage) {
        val privateKey = UserPrivateKey(user.id, MASTER_KEY)
        val userAddress = BlockchainManager[network].getAddress(privateKey.key.toByteArray())
        val displayAddress = buildString {
            if (bot is TgBot) append("<code>")
            append(userAddress)
            if (bot is TgBot) append("</code>")
        }
        val feeInfo = Messages[user].menuWalletDepositFee.format(Coins(currency, currency.networkFeeReserve))
        val checkInfo = Messages[user].menuWalletDepositCheckInfo
        bot.updateKeyboard(
            to = botMessage.peerId,
            lastMenuMessageId = botMessage.lastMenuMessageId,
            message = String.format(
                Messages[user].menuWalletDepositMessage,
                currency.ticker,
                network.displayName,
                displayAddress,
                if (currency.isNative) feeInfo else checkInfo
            ),
            keyboard = BotKeyboard {
                if (currency == CryptoCurrency.TON && bot is TgBot) {
                    row {
                        linkButton(
                            Messages[user].menuWalletDepositLink,
                            "ton://transfer/${userAddress}",
                            ButtonPayload.serializer(),
                            ButtonPayload.LINK
                        )
                    }
                }
                /*
                row {
                    button(
                        Messages[user.settings.lang].menuWalletDepositQR,
                        ButtonPayload.serializer(),
                        ButtonPayload.QR
                    )
                }
                 */
                if (currency == CryptoCurrency.TGR) {
                    row {
                        button(
                            Messages[user].menuWalletDepositCheck,
                            ButtonPayload.serializer(),
                            ButtonPayload.CHECK
                        )
                    }
                }
                row {
                    button(
                        Messages[user].menuButtonBack,
                        ButtonPayload.serializer(),
                        ButtonPayload.BACK
                    )
                    button(
                        Messages[user].menuButtonMenu,
                        ButtonPayload.serializer(),
                        ButtonPayload.MENU
                    )
                }
            }
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun handleMessage(bot: Bot, botMessage: BotMessage): Boolean {
        val payload = botMessage.payload ?: return false
        when (Json.decodeFromString<ButtonPayload>(payload)) {

            ButtonPayload.LINK -> TODO()
            ButtonPayload.QR -> TODO()

            ButtonPayload.CHECK -> {
                if (PostgresUserPersistent.checkCooldown(user, CooldownType.DEPOSIT_CHECK)) {
                    PostgresUserPersistent.addCooldown(user, CooldownType.DEPOSIT_CHECK, 5.minutes)
                    GlobalScope.launch {
                        listOf(
                            async {
                                WalletObserver.checkForNewDeposits(user, BlockchainManager[network], currency)
                            },
                        ).awaitAll().filter { it.amount > BigInteger.ZERO }.forEach { coins ->
                            bot.sendMessage(
                                botMessage.peerId,
                                Messages[user].walletMenuDepositMessage.format(
                                    coins,
                                    Coins(coins.currency, coins.currency.networkFeeReserve)
                                )
                            )
                            LogsUtil.log(user, "$coins", LogType.DEPOSIT)
                        }
                    }
                    bot.sendMessage(botMessage.peerId, "⌛\uFE0F")
                    user.setMenu(bot, MainMenu(user), botMessage)
                } else {
                    val time = PostgresUserPersistent.getCooldown(user, CooldownType.DEPOSIT_CHECK)
                    val date = Date.from(time.toJavaInstant())
                    val timeDisplay = SimpleDateFormat("HH:mm").format(date)
                    bot.sendPopup(botMessage, Messages[user].menuWalletDepositCooldown.format(timeDisplay))
                    return true
                }

            }

            ButtonPayload.MENU -> {
                user.setMenu(bot, MainMenu(user), botMessage)
            }

            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, botMessage)
            }
        }
        return true
    }

    @Serializable
    private enum class ButtonPayload {
        LINK,
        QR,
        CHECK,
        MENU,
        BACK
    }
}
