package money.tegro.bot.inlines

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.MessagesContainer
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.button

@Serializable
data class MainMenu(
    private val user: User
) : Menu {
    private fun createKeyboard(): BotKeyboard = BotKeyboard {
        row {
            button(
                MessagesContainer[user.settings.lang].mainMenuButtonWallet,
                CommandPayload.serializer(),
                CommandPayload.WALLET
            )
            button(
                MessagesContainer[user.settings.lang].mainMenuButtonReceipts,
                CommandPayload.serializer(),
                CommandPayload.RECEIPTS
            )
        }
        row {
            button(
                MessagesContainer[user.settings.lang].mainMenuButtonExchange,
                CommandPayload.serializer(),
                CommandPayload.EXCHANGE
            )
            button(
                MessagesContainer[user.settings.lang].mainMenuButtonStock,
                CommandPayload.serializer(),
                CommandPayload.STOCK
            )
        }
        row {
            button(
                MessagesContainer[user.settings.lang].mainMenuButtonMarket,
                CommandPayload.serializer(),
                CommandPayload.MARKET
            )
            button(
                MessagesContainer[user.settings.lang].mainMenuButtonAccounts,
                CommandPayload.serializer(),
                CommandPayload.ACCOUNTS
            )
        }
        row {
            button(
                MessagesContainer[user.settings.lang].mainMenuButtonDeals,
                CommandPayload.serializer(),
                CommandPayload.DEALS
            )
            button(
                MessagesContainer[user.settings.lang].mainMenuButtonDeposits,
                CommandPayload.serializer(),
                CommandPayload.DEPOSITS
            )
        }
        row {
            button(
                MessagesContainer[user.settings.lang].mainMenuButtonNFT,
                CommandPayload.serializer(),
                CommandPayload.NFT
            )
        }
        row {
            button(
                MessagesContainer[user.settings.lang].mainMenuButtonSettings,
                CommandPayload.serializer(),
                CommandPayload.SETTINGS
            )
        }
    }

    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        val keyboard = createKeyboard()
        bot.updateKeyboard(
            user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId,
            MessagesContainer[user.settings.lang].mainMenuMessage,
            keyboard
        )
    }

    override suspend fun handleMessage(bot: Bot, message: BotMessage): Boolean {
        val payload = message.payload ?: return false
        when (Json.decodeFromString<CommandPayload>(payload)) {
            CommandPayload.WALLET -> user.setMenu(bot, WalletMenu(user, this), message.lastMenuMessageId)
            CommandPayload.RECEIPTS -> user.setMenu(bot, ReceiptsMenu(user, this), message.lastMenuMessageId)
            CommandPayload.EXCHANGE -> user.setMenu(bot, ExchangeMenu(user, this), message.lastMenuMessageId)
            CommandPayload.STOCK -> user.setMenu(bot, StockMenu(user, this), message.lastMenuMessageId)
            CommandPayload.MARKET -> TODO()
            CommandPayload.ACCOUNTS -> user.setMenu(bot, AccountsMenu(user, this), message.lastMenuMessageId)
            CommandPayload.DEALS -> TODO()
            CommandPayload.DEPOSITS -> user.setMenu(bot, DepositsMenu(user, this), message.lastMenuMessageId)
            CommandPayload.SETTINGS -> user.setMenu(bot, SettingsMenu(user, this), message.lastMenuMessageId)
            CommandPayload.NFT -> TODO()
        }
        return true
    }

    @Serializable
    enum class CommandPayload {
        WALLET, RECEIPTS, EXCHANGE, STOCK, MARKET, ACCOUNTS, DEALS, DEPOSITS, NFT, SETTINGS
    }
}