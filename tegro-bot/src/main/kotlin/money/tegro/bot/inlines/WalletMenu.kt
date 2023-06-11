package money.tegro.bot.inlines

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.testnet
import money.tegro.bot.utils.button
import money.tegro.bot.wallet.Coins
import money.tegro.bot.wallet.CryptoCurrency
import money.tegro.bot.walletPersistent
import java.math.BigDecimal
import java.math.BigInteger

@Serializable
class WalletMenu(
    val user: User,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, botMessage: BotMessage) {
        val walletState = walletPersistent.loadWalletState(user)
        walletPersistent.saveWalletState(user, walletState)
        bot.updateKeyboard(
            to = botMessage.peerId,
            lastMenuMessageId = botMessage.lastMenuMessageId,
            message = buildString {
                appendLine(Messages[user.settings.lang].walletMenuTitle + if (testnet) "   ‼\uFE0FTESTNET‼\uFE0F" else "")
                appendLine()
                val values = CryptoCurrency.values().toMutableList()
                walletState.active.forEach {
                    appendLine("· ${it.currency.displayName}: ${it.toStringWithRate(user.settings.localCurrency)}")
                    values.remove(it.currency)
                }
                values.forEach {
                    appendLine(
                        "· ${it.displayName}: ${
                            Coins(
                                it,
                                BigDecimal.ZERO
                            ).toStringWithRate(user.settings.localCurrency)
                        }"
                    )
                }
                val frozen = walletState.frozen.filter { it.amount > BigInteger.ZERO }
                if (frozen.isNotEmpty()) {
                    appendLine()
                    appendLine(Messages[user.settings.lang].menuWalletFrozenTitle)
                    appendLine()
                    frozen.forEach {
                        appendLine("· ${it.currency.displayName}: $it")
                    }
                }
            },
            keyboard = BotKeyboard {
                row {
                    button(
                        Messages[user.settings.lang].menuWalletButtonDeposit,
                        ButtonPayload.serializer(),
                        ButtonPayload.DEPOSIT
                    )
                    button(
                        Messages[user.settings.lang].menuWalletButtonWithdraw,
                        ButtonPayload.serializer(),
                        ButtonPayload.WITHDRAW
                    )
                }
                /*
                row {
                    button(
                        Messages[user.settings.lang].menuWalletButtonTransfer,
                        ButtonPayload.serializer(),
                        ButtonPayload.TRANSFER
                    )
                    button(
                        Messages[user.settings.lang].menuWalletButtonHistory,
                        ButtonPayload.serializer(),
                        ButtonPayload.HISTORY
                    )
                }
                 */
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

    override suspend fun handleMessage(bot: Bot, botMessage: BotMessage): Boolean {
        val payload = botMessage.payload ?: return false
        when (Json.decodeFromString<ButtonPayload>(payload)) {
            ButtonPayload.DEPOSIT -> {
                user.setMenu(bot, WalletDepositSelectMenu(user, this), botMessage)
            }

            ButtonPayload.WITHDRAW -> {
                bot.sendPopup(botMessage, "Maintenance, please wait...")
                return true
                //user.setMenu(bot, WalletWithdrawSelectMenu(user, this), message.lastMenuMessageId)
            }

            ButtonPayload.TRANSFER -> TODO()
            ButtonPayload.HISTORY -> TODO()
            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, botMessage)
            }


        }
        return true
    }

    @Serializable
    private enum class ButtonPayload {
        DEPOSIT,
        WITHDRAW,
        TRANSFER,
        HISTORY,
        BACK
    }
}