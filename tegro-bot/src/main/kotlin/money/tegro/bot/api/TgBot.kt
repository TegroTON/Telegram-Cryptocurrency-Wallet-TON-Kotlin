package money.tegro.bot.api

import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import kotlinx.datetime.Clock
import money.tegro.bot.inlines.MainMenu
import money.tegro.bot.menuPersistent
import money.tegro.bot.objects.*
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.receipts.PostgresReceiptPersistent
import money.tegro.bot.testnet
import money.tegro.bot.utils.*
import money.tegro.bot.wallet.*
import money.tegro.bot.walletPersistent
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import java.io.File
import java.io.InputStream
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.hours

@OptIn(DelicateCoroutinesApi::class)
class TgBot(
    val apiToken: String
) : Bot, TelegramLongPollingBot(apiToken), CoroutineScope {
    private val job = SupervisorJob()

    override val coroutineContext: CoroutineContext = Dispatchers.Default + job

    private var botUsername: String = ""

    fun start(username: String) {
        val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
        botsApi.registerBot(this)
        botUsername = username

        launch {
            RatePersistent.start()
            while (true) {
                val result = PostgresDepositsPersistent.check()
                for (deposit in result) {
                    if (deposit.issuer.tgId == null) continue
                    if (deposit.finishDate <= Clock.System.now()) {
                        PostgresDepositsPersistent.payDeposit(deposit, this@TgBot)
                    }
                }
                delay(1.hours)
            }
        }

        SecurityPersistent.informAdmins(this@TgBot, "Bot logger init")
    }

    override fun sendMessage(to: Long, message: String) {
        sendMessage(to, message, true)
    }

    fun sendMessage(to: Long, message: String, html: Boolean) {
        val sendMessage = SendMessage().apply {
            chatId = to.toString()
            enableHtml(html)
            text = message
        }
        executeAsync(sendMessage)
    }

    override suspend fun sendPhoto(to: Long, message: String, file: File, keyboard: BotKeyboard?) {
        val sendPhoto = SendPhoto().apply {
            chatId = to.toString()
            caption = message
            photo = InputFile(file)
            if (keyboard != null) replyMarkup = keyboard.toTg()
        }
        executeAsync(sendPhoto).await()
    }

    override suspend fun sendPhoto(
        to: Long,
        message: String,
        inputStream: InputStream,
        filename: String,
        keyboard: BotKeyboard?
    ) {
        sendPhoto(to, message, true, inputStream, filename, keyboard)
    }

    suspend fun sendPhoto(
        to: Long,
        message: String,
        html: Boolean,
        inputStream: InputStream,
        filename: String,
        keyboard: BotKeyboard?
    ) {
        val sendPhoto = SendPhoto().apply {
            chatId = to.toString()
            if (html) parseMode = "HTML"
            caption = message
            photo = InputFile(inputStream, filename)
            if (keyboard != null) replyMarkup = keyboard.toTg()
        }
        executeAsync(sendPhoto).await()
    }

    override suspend fun sendMessageKeyboard(
        to: Long,
        message: String,
        keyboard: BotKeyboard
    ) {
        sendMessageKeyboard(to, message, true, keyboard)
    }

    suspend fun sendMessageKeyboard(
        to: Long,
        message: String,
        html: Boolean,
        keyboard: BotKeyboard
    ) {
        val sendMessage = SendMessage().apply {
            chatId = to.toString()
            enableHtml(html)
            text = message
            replyMarkup = keyboard.toTg()
            disableWebPagePreview = true
        }
        executeAsync(sendMessage).await()
    }

    override suspend fun updateKeyboard(to: Long, lastMenuMessageId: Long?, message: String, keyboard: BotKeyboard) {
        updateKeyboard(to, lastMenuMessageId, message, true, keyboard)
    }

    suspend fun updateKeyboard(
        to: Long,
        lastMenuMessageId: Long?,
        message: String,
        html: Boolean,
        keyboard: BotKeyboard
    ) {
        if (lastMenuMessageId != null) {
            val updated = EditMessageText().apply {
                chatId = to.toString()
                messageId = lastMenuMessageId.toInt()
                enableHtml(html)
                text = message
                replyMarkup = keyboard.toTg()
                disableWebPagePreview = true
            }
            executeAsync(updated).await()
        } else {
            sendMessageKeyboard(to, message, keyboard)
        }
    }

    override suspend fun deleteMessage(peerId: Long, messageId: Long) {
        val deleteMessage = DeleteMessage().apply {
            chatId = peerId.toString()
            this.messageId = messageId.toInt()
        }
        executeAsync(deleteMessage)
    }

    override suspend fun sendPopup(botMessage: BotMessage, message: String): Boolean {
        val id = botMessage.otherData["callbackQueryId"] as? String
        if (id == null) {
            sendMessage(botMessage.peerId, "sendPopup callbackQueryId null, inform admin")
            return false
        }
        val popup = AnswerCallbackQuery().apply {
            showAlert = true
            text = message
            callbackQueryId = id
        }
        executeAsync(popup).await()
        return true
    }

    override suspend fun getChat(chatId: Long): Chat? {
        return try {
            val request = GetChat().apply {
                this.chatId = chatId.toString()
            }
            val result = executeAsync(request).await()
            Chat(
                result.id,
                result.title,
                result.userName
            )
        } catch (ignored: Exception) {
            null
        }
    }

    override suspend fun isUserInChat(chatId: Long, userId: Long): Boolean {
        return try {
            val request = GetChatMember().apply {
                this.chatId = chatId.toString()
                this.userId = userId
            }
            val result = executeAsync(request).await()
            return !result.status.equals("kicked") && !result.status.equals("left")
        } catch (ignored: Exception) {
            false
        }
    }

    override fun getBotUsername(): String {
        return System.getenv("TG_USER_NAME")
    }

    override fun onUpdateReceived(update: Update) {
        //println("$update")
        if (!update.hasMessage() && !update.hasCallbackQuery() && !update.hasInlineQuery()) {
            println("unknown update type: $update")
            return
        }
        launch {
            val userTgId = update.message?.from?.id ?: update.callbackQuery?.from?.id ?: update.inlineQuery.from.id
            val randomUUID = UUID.nameUUIDFromBytes("tg_$userTgId".toByteArray())
            val ref = getRefId(update.message?.text)
            val user =
                PostgresUserPersistent.loadByTg(userTgId) ?: PostgresUserPersistent.save(
                    User(
                        randomUUID, userTgId, null, UserSettings(
                            randomUUID,
                            Language.RU,
                            LocalCurrency.RUB,
                            ref
                        )
                    )
                )
            if (update.hasInlineQuery()) {
                val iq = update.inlineQuery
                InlineQueryManager.answer(this@TgBot, user, iq)
                return@launch
            }
            val menu = menuPersistent.loadMenu(user)
            val message: Message = update.message ?: update.callbackQuery.message
            val fwdMessages = emptyList<BotMessage>().toMutableList()
            if (message.forwardFrom != null) {
                val fwdBotMessage = BotMessage(
                    message.messageId.toLong(),
                    message.forwardFrom.id,
                    userTgId,
                    false,
                    message.text,
                    null,
                    null,
                    fwdMessages,
                    emptyMap()
                )
                fwdMessages.add(fwdBotMessage)
            }
            if (message.forwardFromChat != null) {
                val fwdBotMessage = BotMessage(
                    message.messageId.toLong(),
                    message.forwardFromChat.id,
                    userTgId,
                    false,
                    message.text,
                    null,
                    null,
                    fwdMessages,
                    emptyMap()
                )
                fwdMessages.add(fwdBotMessage)
            }
            val botMessage = BotMessage(
                message.messageId.toLong(),
                userTgId,
                message.chat.id,
                !message.isUserMessage,
                message.text,
                update.callbackQuery?.data,
                update.callbackQuery?.message?.messageId?.toLong(),
                fwdMessages,
                if (update.callbackQuery != null) mapOf("callbackQueryId" to update.callbackQuery.id) else emptyMap()
            )
            GlobalScope.launch {
                repeat(6) {
                    WalletObserver.checkDeposit(user).forEach { coins ->
                        walletPersistent.updateActive(user, coins.currency) { oldCoins ->
                            (oldCoins + coins).also { newCoins ->
                                println(
                                    "New deposit: $user\n" +
                                            "     old coins: $oldCoins\n" +
                                            " deposit coins: $coins\n" +
                                            "     new coins: $newCoins"
                                )
                            }
                        }
                        sendMessage(
                            botMessage.peerId,
                            Messages[user].walletMenuDepositMessage.format(
                                coins.toStringWithRate(user.settings.localCurrency),
                                Coins(coins.currency, coins.currency.networkFeeReserve)
                            )
                        )
                        val active = PostgresWalletPersistent.loadWalletState(user).active[CryptoCurrency.TON]
                        SecurityPersistent.log(user, coins, "$coins", LogType.DEPOSIT)
                        SecurityPersistent.log(user, coins, "$coins, balance $active", LogType.DEPOSIT_ADMIN)
                    }
                    WalletObserver.checkDepositBnb(user).forEach { coins ->
                        PostgresSecurityPersistent.addFinanceRequest(
                            user,
                            LogType.DEPOSIT,
                            coins,
                            BlockchainType.BSC,
                            ""
                        )
                        sendMessage(
                            botMessage.peerId,
                            Messages[user].menuWalletDepositCheckMessage.format(coins)
                        )
                    }
                    if (!testnet) {
//                        listOf(
//                            async {
//                                WalletObserver.checkForNewDeposits(
//                                    user,
//                                    EthBlockchainManager,
//                                    CryptoCurrency.TGR
//                                )
//                            },
//                        ).awaitAll().filter { it.amount > BigInteger.ZERO }.forEach { coins ->
//                            sendMessage(
//                                botMessage.peerId,
//                                Messages[user].walletMenuDepositMessage.format(
//                                    coins,
//                                    Coins(coins.currency, coins.currency.networkFeeReserve)
//                                )
//                            )
//                            LogsUtil.log(user, "$coins", LogType.DEPOSIT)
//                        }
                    }
                    delay(15_000)
                }
            }
            if (message.text != null && message.text.startsWith("/")) {
                println("Got command from ${user.id}: ${message.text}")
                Commands.execute(user, botMessage, this@TgBot, menu, message.from.isPremium)
                return@launch
            }
            if (botMessage.isFromChat) return@launch
            try {
                println("Open menu $menu for user ${user.id}")
                if (menu?.handleMessage(this@TgBot, botMessage) == true) {
                    return@launch
                }
            } catch (e: Throwable) {
                user.setMenu(this@TgBot, MainMenu(user), botMessage)
                throw RuntimeException("Failed handle message for user $user in menu: $menu", e)
            }
            println("Open main menu for user ${user.id}")
            user.setMenu(this@TgBot, MainMenu(user), botMessage)
        }
    }

    private suspend fun getRefId(message: String?): UUID? {
        if (message == null) return null
        if (!message.startsWith("/start")) return null
        if (message.split(" ").size == 1) return null
        val ref = message.split(" ")[1]
        val split = ref.split("-")
        if (split.size != 6) {
            return null
        }
        when (split[0]) {
            "RF" -> {
                return UUID.fromString(ref.drop(3))
            }

            "RC" -> {
                val receipt = PostgresReceiptPersistent.loadReceipt(UUID.fromString(ref.drop(3)))
                return receipt?.issuer?.id
            }

            "AC" -> {
                val account = PostgresAccountsPersistent.loadAccount(UUID.fromString(ref.drop(3)))
                return account?.issuer?.id
            }
        }
        return null
    }
}
