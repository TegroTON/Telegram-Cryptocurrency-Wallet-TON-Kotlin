package money.tegro.bot.inlines

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.api.TgBot
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.testnet
import money.tegro.bot.utils.button
import money.tegro.bot.utils.linkButton

@Serializable
data class MainMenu(
    private val user: User
) : Menu {
    private fun createKeyboard(): BotKeyboard = BotKeyboard {
        row {
            button(
                Messages[user.settings.lang].mainMenuButtonWallet,
                CommandPayload.serializer(),
                CommandPayload.WALLET
            )
            button(
                Messages[user.settings.lang].mainMenuButtonReceipts,
                CommandPayload.serializer(),
                CommandPayload.RECEIPTS
            )
        }
        /*
        row {
            button(
                Messages[user.settings.lang].mainMenuButtonExchange,
                CommandPayload.serializer(),
                CommandPayload.EXCHANGE
            )
            button(
                Messages[user.settings.lang].mainMenuButtonStock,
                CommandPayload.serializer(),
                CommandPayload.STOCK
            )
        }
         */
        row {
            /*
            button(
                Messages[user.settings.lang].mainMenuButtonMarket,
                CommandPayload.serializer(),
                CommandPayload.MARKET
            )
             */
            button(
                Messages[user.settings.lang].mainMenuButtonDeposits,
                CommandPayload.serializer(),
                CommandPayload.DEPOSITS
            )
            button(
                Messages[user.settings.lang].mainMenuButtonAccounts,
                CommandPayload.serializer(),
                CommandPayload.ACCOUNTS
            )
        }
        /*
        row {
            button(
                Messages[user.settings.lang].mainMenuButtonDeals,
                CommandPayload.serializer(),
                CommandPayload.DEALS
            )
            button(
                Messages[user.settings.lang].mainMenuButtonDeposits,
                CommandPayload.serializer(),
                CommandPayload.DEPOSITS
            )
        }
         */
        row {
//            linkButton(
//                Messages[user.settings.lang].mainMenuButtonNFT,
//                "https://libermall.com/",
//                CommandPayload.serializer(),
//                CommandPayload.NFT
//            )
            button(
                Messages[user.settings.lang].mainMenuButtonNFT,
                CommandPayload.serializer(),
                CommandPayload.NFT
            )
            linkButton(
                Messages[user.settings.lang].mainMenuButtonDex,
                "https://tegro.finance/",
                CommandPayload.serializer(),
                CommandPayload.DEX
            )
        }
        row {
            button(
                Messages[user.settings.lang].mainMenuButtonSettings,
                CommandPayload.serializer(),
                CommandPayload.SETTINGS
            )
        }
    }

    override suspend fun sendKeyboard(bot: Bot, botMessage: BotMessage) {
        val keyboard = createKeyboard()
        var message =
            if (bot is TgBot) Messages[user.settings.lang].mainMenuMessageTg else Messages[user.settings.lang].mainMenuMessage
        message = message.format(if (testnet) Messages[user.settings.lang].mainMenuTestnetWarning else "")
        bot.updateKeyboard(
            botMessage.peerId,
            botMessage.lastMenuMessageId,
            message,
            keyboard
        )
    }

    override suspend fun handleMessage(bot: Bot, botMessage: BotMessage): Boolean {
        val payload = botMessage.payload ?: return false
        when (Json.decodeFromString<CommandPayload>(payload)) {
            CommandPayload.WALLET -> user.setMenu(bot, WalletMenu(user, this), botMessage)
            CommandPayload.RECEIPTS -> user.setMenu(bot, ReceiptsMenu(user, this), botMessage)
            CommandPayload.EXCHANGE -> user.setMenu(bot, ExchangeMenu(user, this), botMessage)
            CommandPayload.STOCK -> user.setMenu(bot, StockMenu(user, this), botMessage)
            CommandPayload.MARKET -> TODO()
            CommandPayload.ACCOUNTS -> user.setMenu(bot, AccountsMenu(user, this), botMessage)
            CommandPayload.DEALS -> TODO()
            CommandPayload.DEPOSITS -> user.setMenu(bot, DepositsMenu(user, this), botMessage)
            CommandPayload.SETTINGS -> user.setMenu(bot, SettingsMenu(user, this), botMessage)
            CommandPayload.NFT -> user.setMenu(bot, NftMenu(user, this), botMessage)
            CommandPayload.DEX -> TODO()
        }
        return true
    }

    @Serializable
    private enum class CommandPayload {
        WALLET, RECEIPTS, EXCHANGE, STOCK, MARKET, ACCOUNTS, DEALS, DEPOSITS, NFT, DEX, SETTINGS
    }
}