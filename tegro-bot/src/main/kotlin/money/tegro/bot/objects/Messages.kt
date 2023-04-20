package money.tegro.bot.objects

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromMap
import java.io.InputStream

@Serializable
data class Messages(
    /*************************
     *   Main menu section   *
     *************************/
    val mainMenuMessage: String = "mainMenuMessage",
    val mainMenuMessageTg: String = "mainMenuMessageTg",
    val mainMenuTestnetWarning: String = "mainMenuTestnetWarning",
    val mainMenuButtonWallet: String = "mainMenuButtonWallet",
    val mainMenuButtonReceipts: String = "mainMenuButtonReceipts",
    val mainMenuButtonAccounts: String = "mainMenuButtonAccounts",
    val mainMenuButtonSettings: String = "mainMenuButtonSettings",
    val mainMenuButtonDeposits: String = "mainMenuButtonDeposits",
    val mainMenuButtonNFT: String = "mainMenuButtonNFT",
    val mainMenuButtonDex: String = "mainMenuButtonDex",
    val mainMenuButtonExchange: String = "mainMenuButtonExchange",
    val mainMenuButtonStock: String = "mainMenuButtonStock",
    val mainMenuButtonMarket: String = "mainMenuButtonMarket",
    val mainMenuButtonDeals: String = "mainMenuButtonDeals",

    /***************************
     *   Wallet menu section   *
     ***************************/
    val walletMenuTitle: String = "walletMenuTitle",
    val menuWalletButtonDeposit: String = "menuWalletButtonDeposit",
    val menuWalletButtonWithdraw: String = "menuWalletButtonWithdraw",
    val menuWalletButtonTransfer: String = "menuWalletButtonTransfer",
    val menuWalletButtonHistory: String = "menuWalletButtonHistory",
    val menuWalletFrozenTitle: String = "menuWalletFrozenTitle",
    val menuWalletDepositSelectMessage: String = "menuWalletDepositSelectMessage",
    val menuWalletDepositSelectNetworkMessage: String = "menuWalletDepositSelectNetworkMessage",
    val menuWalletWithdrawSelectMessage: String = "menuWalletWithdrawSelectMessage",
    val menuWalletWithdrawSelectNetworkMessage: String = "menuWalletWithdrawSelectNetworkMessage",
    val menuWalletWithdrawMessage: String = "menuWalletWithdrawMessage",
    val menuWalletWithdrawTON: String = "menuWalletWithdrawTON",
    val menuWalletWithdrawTGR: String = "menuWalletWithdrawTGR",
    val menuWalletWithdrawUSDT: String = "menuWalletWithdrawUSDT",
    val menuWalletDepositMessage: String = "menuWalletDepositMessage",
    val menuWalletDepositLink: String = "menuWalletDepositLink",
    val menuWalletDepositQR: String = "menuWalletDepositQR",
    val walletMenuDepositMessage: String = "walletMenuDepositMessage",
    val menuWalletWithdrawSelectAmountMessage: String = "menuWalletWithdrawSelectAmountMessage",
    val walletMenuWithdrawMessage: String = "walletMenuWithdrawMessage",
    val walletMenuWithdrawInvalidAddress: String = "walletMenuWithdrawInvalidAddress",


    /*****************************
     *   Receipts menu section   *
     *****************************/
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
    val menuReceiptRecipientNotFound: String = "menuReceiptRecipientNotFound",
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
    val receiptActivated: String = "receiptActivated",
    val multireceiptActivated: String = "multireceiptActivated",
    val receiptMoneyReceived: String = "receiptMoneyReceived",
    val receiptIssuerActivationException: String = "receiptIssuerActivationException",
    val receiptNotActiveException: String = "receiptNotActiveException",
    val illegalRecipientException: String = "illegalRecipientException",

    /*****************************
     *   Deposits menu section   *
     *****************************/
    val menuDepositsMessage: String = "menuDepositsMessage",
    val menuDepositsNew: String = "menuDepositsNew",
    val menuDepositsCurrent: String = "menuDepositsCurrent",
    val menuDepositsCalculator: String = "menuDepositsCalculator",
    val menuDepositCalculatorButton: String = "menuDepositCalculatorButton",
    val menuDepositSelectAmountMessage: String = "menuDepositSelectAmountMessage",
    val menuDepositSelectAmountMessageCalc: String = "menuDepositSelectAmountMessageCalc",
    val menuDepositSelectPeriodMessage: String = "menuDepositSelectPeriodMessage",
    val menuDepositSelectPeriod: String = "menuDepositSelectPeriod",
    val menuDepositApproveMessage: String = "menuDepositApproveMessage",
    val menuDepositApproveMessageCalc: String = "menuDepositApproveMessageCalc",
    val menuDepositApproveButton: String = "menuDepositApproveButton",
    val menuDepositReadyMessage: String = "menuDepositReadyMessage",
    val menuDepositsListMessage: String = "menuDepositsListMessage",
    val menuDepositsListEmpty: String = "menuDepositsListEmpty",
    val menuDepositsListEntry: String = "menuDepositsListEntry",
    val menuDepositPaid: String = "menuDepositPaid",

    /*****************************
     *   Accounts menu section   *
     *****************************/
    val menuAccountsMessage: String = "menuAccountsMessage",
    val menuAccountsCreate: String = "menuAccountsCreate",
    val menuAccountsList: String = "menuAccountsList",
    val menuAccountReadyMessage: String = "menuAccountReadyMessage",
    val menuAccountReadyMinAmountButton: String = "menuAccountReadyMinAmountButton",
    val menuAccountReadyActivationsButton: String = "menuAccountReadyActivationsButton",
    val menuAccountSelectTypeMessage: String = "menuAccountSelectTypeMessage",
    val menuAccountSelectTypeOneTime: String = "menuAccountSelectTypeOneTime",
    val menuAccountSelectTypeNotOneTime: String = "menuAccountSelectTypeNotOneTime",
    val menuAccountSelectActivationsMessage: String = "menuAccountSelectActivationsMessage",
    val menuAccountSelectActivationsSkip: String = "menuAccountSelectActivationsSkip",
    val menuAccountSelectCurrencyMessage: String = "menuAccountSelectCurrencyMessage",
    val menuAccountSelectAmountMessage: String = "menuAccountSelectAmountMessage",
    val menuAccountsListMessage: String = "menuAccountsListMessage",
    val menuAccountsListEmpty: String = "menuAccountsListEmpty",
    val menuAccountsListEntry: String = "menuAccountsListEntry",
    val menuAccountsListEntryOpen: String = "menuAccountsListEntryOpen",
    val menuAccountSelectMinAmountMessage: String = "menuAccountSelectMinAmountMessage",
    val menuAccountChangeActivationsMessage: String = "menuAccountChangeActivationsMessage",
    val menuAccountChangeActivationsOneTimeButton: String = "menuAccountChangeActivationsOneTimeButton",
    val menuAccountChangeActivationsNotSetButton: String = "menuAccountChangeActivationsNotSetButton",
    val menuAccountReadyOneTimePopup: String = "menuAccountReadyOneTimePopup",
    val menuAccountPaySelectAmountMessage: String = "menuAccountPaySelectAmountMessage",
    val menuAccountPaySelectAmountErrorMinAmount: String = "menuAccountPaySelectAmountErrorMinAmount",
    val menuAccountPaySelectAmountErrorMaxCoins: String = "menuAccountPaySelectAmountErrorMaxCoins",
    val menuAccountPayMessage: String = "menuAccountPayMessage",
    val menuAccountPayButton: String = "menuAccountPayButton",
    val menuAccountPayDeclineButton: String = "menuAccountPayDeclineButton",
    val menuAccountPaySuccess: String = "menuAccountPaySuccess",
    val accountMoneyReceived: String = "accountMoneyReceived",
    val accountIssuerActivationException: String = "accountIssuerActivationException",
    val accountNotActiveException: String = "accountNotActiveException",
    val accountMinAmountException: String = "accountMinAmountException",
    val accountOverdraftException: String = "accountOverdraftException",

    /*****************************
     *   Settings menu section   *
     *****************************/
    val menuSettingsMessage: String = "menuSettingsMessage",
    val menuSettingsRefs: String = "menuSettingsRefs",
    val menuSettingsLang: String = "menuSettingsLang",
    val menuSettingsCurrency: String = "menuSettingsCurrency",
    val menuSettingsHints: String = "menuSettingsHints",
    val menuSettingsHelp: String = "menuSettingsHelp",
    val menuReferralsMessage: String = "menuReferralsMessage",

    /*****************************
     *   Exchange menu section   *
     *****************************/
    val menuExchangeMessage: String = "menuExchangeMessage",
    val menuExchangeToMessage: String = "menuExchangeToMessage",

    /**************************
     *   Stock menu section   *
     **************************/
    val menuStockMessage: String = "menuStockMessage",
    val menuStockStart: String = "menuStockStart",
    val menuStockHistory: String = "menuStockHistory",

    /**************
     *   Others   *
     **************/
    val menuSelectInvalidAmount: String = "menuSelectInvalidAmount",
    val menuButtonBack: String = "menuButtonBack",
    val monthOne: String = "monthOne",
    val monthTwo: String = "monthTwo",
    val monthThree: String = "monthThree",
    val oneTime: String = "oneTime",
    val notOneTime: String = "notOneTime",
    val open: String = "open",
    val notSet: String = "notSet",
    val soon: String = "soon",
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
