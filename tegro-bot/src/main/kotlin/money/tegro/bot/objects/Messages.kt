package money.tegro.bot.objects

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromMap
import java.io.InputStream

@Serializable
data class Messages(
    val mainMenuMessage: String = "mainMenuMessage",
    val mainMenuButtonWallet: String = "mainMenuButtonWallet",
    val mainMenuButtonReceipts: String = "mainMenuButtonReceipts",
    val mainMenuButtonAccounts: String = "mainMenuButtonAccounts",
    val mainMenuButtonSettings: String = "mainMenuButtonSettings",
    val mainMenuButtonExchange: String = "mainMenuButtonExchange",
    val mainMenuButtonStock: String = "mainMenuButtonStock",
    val mainMenuButtonMarket: String = "mainMenuButtonMarket",
    val mainMenuButtonDeals: String = "mainMenuButtonDeals",
    val mainMenuButtonDeposits: String = "mainMenuButtonDeposits",
    val mainMenuButtonNFT: String = "mainMenuButtonNFT",
    val walletMenuTitle: String = "walletMenuTitle",
    val menuButtonBack: String = "menuButtonBack",
    val menuWalletButtonDeposit: String = "menuWalletButtonDeposit",
    val menuWalletButtonWithdraw: String = "menuWalletButtonWithdraw",
    val menuWalletButtonTransfer: String = "menuWalletButtonTransfer",
    val menuWalletButtonHistory: String = "menuWalletButtonHistory",
    val menuWalletFrozenTitle: String = "menuWalletFrozenTitle",
    val menuReceiptsMessage: String = "menuReceiptsMessage",
    val menuReceiptsCreate: String = "menuReceiptsCreate",
    val menuReceiptsSelectCurrencyMessage: String = "menuReceiptsSelectCurrencyMessage",
    val menuReceiptsSelectAmountNoMoney: String = "menuReceiptsSelectAmountNoMoney",
    val menuReceiptsSelectAmountMessage: String = "menuReceiptsSelectAmountMessage",
    val menuReceiptsSelectAmountMin: String = "menuReceiptsSelectAmountMin",
    val menuReceiptsSelectAmountMax: String = "menuReceiptsSelectAmountMax",
    val menuReceiptsSelectActivationsMessage: String = "menuReceiptsSelectActivationsMessage",
    val menuReceiptsSelectActivationsSkip: String = "menuReceiptsSelectActivationsSkip",
    val menuReceiptsSelectActivationsMax: String = "menuReceiptsSelectActivationsMax",
    val menuReceiptNotFound: String = "menuReceiptNotFound",
    val menuReceiptDeleted: String = "menuReceiptDeleted",
    val menuReceiptReadyMessage: String = "menuReceiptReadyMessage",
    val menuReceiptReadyShare: String = "menuReceiptReadyShare",
    val menuReceiptReadyQr: String = "menuReceiptReadyQr",
    val menuReceiptReadyLimitations: String = "menuReceiptReadyLimitations",
    val menuReceiptReadyDelete: String = "menuReceiptReadyDelete",
    val menuReceiptLimitationsMessage: String = "menuReceiptLimitationsMessage",
    val menuReceiptLimitationsRef: String = "menuReceiptLimitationsRef",
    val menuReceiptLimitationsSub: String = "menuReceiptLimitationsSub",
    val menuReceiptLimitationsUser: String = "menuReceiptLimitationsUser",
    val menuReceiptLimitationsCaptcha: String = "menuReceiptLimitationsCaptcha",
    val menuReceiptRecipientMessage: String = "menuReceiptRecipientMessage",
    val menuReceiptRecipientSetMessage: String = "menuReceiptRecipientSetMessage",
    val menuReceiptRecipientUnattach: String = "menuReceiptRecipientUnattach",
    val menuReceiptsList: String = "menuReceiptsList",
    val menuReceiptsListMessage: String = "menuReceiptsListMessage",
    val menuReceiptsListEmpty: String = "menuReceiptsListEmpty",
    val menuReceiptsListEntry: String = "menuReceiptsListEntry",
    val menuReceiptsListKeyboardEntry: String = "menuReceiptsListKeyboardEntry",
    val menuReceiptsListWithRecipient: String = "menuReceiptsListWithRecipient",
    val menuReceiptsListWithoutRecipient: String = "menuReceiptsListWithoutRecipient",
    val menuReceiptsListBack: String = "menuReceiptsListBack",
    val menuAccountsMessage: String = "menuAccountsMessage",
    val menuAccountsCreate: String = "menuAccountsCreate",
    val menuAccountsList: String = "menuAccountsList",
    val menuSettingsMessage: String = "menuSettingsMessage",
    val menuSettingsRefs: String = "menuSettingsRefs",
    val menuSettingsLang: String = "menuSettingsLang",
    val menuSettingsCurrency: String = "menuSettingsCurrency",
    val menuSettingsHints: String = "menuSettingsHints",
    val menuSettingsHelp: String = "menuSettingsHelp",
    val menuWalletDepositSelectMessage: String = "menuWalletDepositSelectMessage",
    val menuWalletWithdrawSelectMessage: String = "menuWalletWithdrawSelectMessage",
    val menuWalletWithdrawMessage: String = "menuWalletWithdrawMessage",
    val menuWalletWithdrawTON: String = "menuWalletWithdrawTON",
    val menuWalletWithdrawTGR: String = "menuWalletWithdrawTGR",
    val menuWalletWithdrawUSDT: String = "menuWalletWithdrawUSDT",
    val menuWalletDepositMessage: String = "menuWalletDepositMessage",
    val menuWalletDepositLink: String = "menuWalletDepositLink",
    val menuWalletDepositQR: String = "menuWalletDepositQR",
    val menuExchangeMessage: String = "menuExchangeMessage",
    val menuExchangeToMessage: String = "menuExchangeToMessage",
    val menuStockMessage: String = "menuStockMessage",
    val menuStockStart: String = "menuStockStart",
    val menuStockHistory: String = "menuStockHistory",
    val menuDepositsMessage: String = "menuDepositsMessage",
    val menuDepositsNew: String = "menuDepositsNew",
    val menuDepositsCurrent: String = "menuDepositsCurrent",
    val menuReferralsMessage: String = "menuReferralsMessage",
    val menuSelectInvalidAmount: String = "menuSelectInvalidAmount",
    val walletMenuDepositMessage: String = "walletMenuDepositMessage",
    val menuWalletWithdrawSelectAmountMessage: String = "menuWalletWithdrawSelectAmountMessage",
    val walletMenuWithdrawMessage: String = "walletMenuWithdrawMessage",
    val walletMenuWithdrawInvalidAddress: String = "walletMenuWithdrawInvalidAddress",
    val walletSoon: String = "walletSoon",
) {
    companion object {

        val messages = Language.values()
            .map {
                loadFromResources(it)
            }

        operator fun get(language: Language): Messages {
            return messages[language.ordinal]
        }

        operator fun get(user: User): Messages {
            return get(user.settings.lang)
        }

        fun loadFromResources(language: Language): Messages {
            val classloader = Thread.currentThread().contextClassLoader
            return classloader.getResourceAsStream("messages_${language.short.lowercase()}.properties")?.use {
                load(it)
            } ?: Messages()
        }

        @OptIn(ExperimentalSerializationApi::class)
        fun load(inputStream: InputStream): Messages {
            val properties = java.util.Properties().apply {
                load(inputStream.bufferedReader())
            }.map { it.key.toString() to it.value }.toMap()
            return Properties.decodeFromMap<Messages>(properties)
        }
    }
}
