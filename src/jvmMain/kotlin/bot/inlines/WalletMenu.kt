package bot.inlines

import bot.api.Bot
import bot.objects.BotMessage
import bot.objects.MessagesContainer
import bot.objects.User
import bot.objects.keyboard.BotKeyboard
import bot.utils.button
import bot.wallet.CryptoCurrency
import bot.walletPersistent
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.math.BigInteger

@Serializable
class WalletMenu(
    val user: User,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        val walletState = walletPersistent.loadWalletState(user)
        walletPersistent.saveWalletState(user, walletState)
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = buildString {
                appendLine(MessagesContainer[user.settings.lang].walletMenuTitle)
                appendLine()
                val values = CryptoCurrency.values().toMutableList()
                walletState.active.forEach {
                    appendLine("· ${it.currency.displayName}: $it")
                    values.remove(it.currency)
                }
                values.forEach {
                    appendLine("· ${it.displayName}: 0 ${it.ticker}")
                }
                val frozen = walletState.frozen.filter { it.amount > BigInteger.ZERO }
                if (frozen.isNotEmpty()) {
                    appendLine()
                    appendLine(MessagesContainer[user.settings.lang].menuWalletFrozenTitle)
                    appendLine()
                    frozen.forEach {
                        appendLine("· ${it.currency.displayName}: $it")
                    }
                }
            },
            keyboard = BotKeyboard {
                row {
                    button(
                        MessagesContainer[user.settings.lang].menuWalletButtonDeposit,
                        ButtonPayload.serializer(),
                        ButtonPayload.DEPOSIT
                    )
                    button(
                        MessagesContainer[user.settings.lang].menuWalletButtonWithdraw,
                        ButtonPayload.serializer(),
                        ButtonPayload.WITHDRAW
                    )
                }
                row {
                    button(
                        MessagesContainer[user.settings.lang].menuWalletButtonTransfer,
                        ButtonPayload.serializer(),
                        ButtonPayload.TRANSFER
                    )
                    button(
                        MessagesContainer[user.settings.lang].menuWalletButtonHistory,
                        ButtonPayload.serializer(),
                        ButtonPayload.HISTORY
                    )
                }
                row {
                    button(
                        MessagesContainer[user.settings.lang].menuButtonBack,
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
            ButtonPayload.DEPOSIT -> {
                user.setMenu(bot, WalletDepositSelectMenu(user, this), message.lastMenuMessageId)
            }

            ButtonPayload.WITHDRAW -> {
                user.setMenu(bot, WalletWithdrawSelectMenu(user, this), message.lastMenuMessageId)
            }

            ButtonPayload.TRANSFER -> TODO()
            ButtonPayload.HISTORY -> TODO()
            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, message.lastMenuMessageId)
            }


        }
        return true
    }

    @Serializable
    enum class ButtonPayload {
        DEPOSIT,
        WITHDRAW,
        TRANSFER,
        HISTORY,
        BACK
    }
}