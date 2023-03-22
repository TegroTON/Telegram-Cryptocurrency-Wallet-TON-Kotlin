package bot.inlines

import bot.api.Bot
import bot.objects.BotMessage
import bot.objects.Messages
import bot.objects.MessagesContainer
import bot.objects.User
import bot.objects.keyboard.BotKeyboard
import bot.utils.button
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

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
            button(Messages.mainMenuButtonReceipts, CommandPayload.serializer(), CommandPayload.RECEIPTS)
        }
        row {
            button(Messages.mainMenuButtonExchange, CommandPayload.serializer(), CommandPayload.EXCHANGE)
            button(Messages.mainMenuButtonStock, CommandPayload.serializer(), CommandPayload.STOCK)
        }
        row {
            button(Messages.mainMenuButtonMarket, CommandPayload.serializer(), CommandPayload.MARKET)
            button(Messages.mainMenuButtonAccounts, CommandPayload.serializer(), CommandPayload.ACCOUNTS)
        }
        row {
            button(Messages.mainMenuButtonDeals, CommandPayload.serializer(), CommandPayload.DEALS)
            button(Messages.mainMenuButtonDeposits, CommandPayload.serializer(), CommandPayload.DEPOSITS)
        }
        row {
            button(Messages.mainMenuButtonNFT, CommandPayload.serializer(), CommandPayload.NFT)
        }
        row {
            button(Messages.mainMenuButtonSettings, CommandPayload.serializer(), CommandPayload.SETTINGS)
        }
    }

    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        val keyboard = createKeyboard()
        bot.updateKeyboard(user.vkId ?: user.tgId ?: 0, lastMenuMessageId, Messages.mainMenuMessage, keyboard)
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