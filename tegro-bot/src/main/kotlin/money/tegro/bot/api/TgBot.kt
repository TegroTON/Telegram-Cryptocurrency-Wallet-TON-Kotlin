package money.tegro.bot.api

import money.tegro.bot.menuPersistent
import money.tegro.bot.inlines.MainMenu
import money.tegro.bot.objects.*
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.receipts.PostgresReceiptPersistent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import java.io.File
import java.io.InputStream
import java.util.*
import kotlin.coroutines.CoroutineContext

class TgBot : Bot, TelegramLongPollingBot(System.getenv("TG_API_TOKEN")), CoroutineScope {
    private val job = SupervisorJob()

    override val coroutineContext: CoroutineContext = Dispatchers.Default + job

    private var apiToken: String = ""
    private var botUsername: String = ""


    fun start(username: String, accessToken: String) {
        val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
        botsApi.registerBot(this)
        apiToken = accessToken
        botUsername = username
    }

    override suspend fun sendMessage(to: Long, message: String) {
        val sendMessage = SendMessage().apply {
            chatId = to.toString()
            enableMarkdown(true)
            text = message
        }
        execute(sendMessage)
    }

    override suspend fun sendPhoto(to: Long, message: String, file: File, keyboard: BotKeyboard?) {
        val sendPhoto = SendPhoto().apply {
            chatId = to.toString()
            caption = message
            photo = InputFile(file)
            if (keyboard != null) replyMarkup = keyboard.toTg()
        }
        execute(sendPhoto)
    }

    override suspend fun sendPhoto(
        to: Long,
        message: String,
        inputStream: InputStream,
        filename: String,
        keyboard: BotKeyboard?
    ) {
        val sendPhoto = SendPhoto().apply {
            chatId = to.toString()
            caption = message
            photo = InputFile(inputStream, filename)
            if (keyboard != null) replyMarkup = keyboard.toTg()
        }
        execute(sendPhoto)
    }

    override suspend fun sendMessageKeyboard(
        to: Long,
        message: String,
        keyboard: BotKeyboard
    ) {
        val sendMessage = SendMessage().apply {
            chatId = to.toString()
            enableMarkdown(false)
            text = message
            replyMarkup = keyboard.toTg()
        }
        executeAsync(sendMessage)
    }

    override suspend fun updateKeyboard(to: Long, lastMenuMessageId: Long?, message: String, keyboard: BotKeyboard) {
        if (lastMenuMessageId != null) {
            val deleteMessage = DeleteMessage().apply {
                chatId = to.toString()
                messageId = lastMenuMessageId.toInt()
            }
            executeAsync(deleteMessage)
        }
        sendMessageKeyboard(to, message, keyboard)
    }

    override fun getBotUsername(): String {
        return System.getenv("TG_USER_NAME")
    }

    override fun onUpdateReceived(update: Update) {
        val userTgId = update.message?.from?.id ?: update.callbackQuery.from.id
        launch {
            val randomUUID = UUID.randomUUID()
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
                    fwdMessages
                )
                fwdMessages.add(fwdBotMessage)
            }
            val botMessage = BotMessage(
                message.messageId.toLong(),
                userTgId,
                message.chatId,
                message.isGroupMessage,
                message.text,
                update.callbackQuery?.data,
                update.callbackQuery?.message?.messageId?.toLong(),
                fwdMessages
            )
            if (message.text != null && message.text.startsWith("/")) {
                Commands.execute(user, botMessage, this@TgBot, menu)
                return@launch
            }
            if (menu?.handleMessage(this@TgBot, botMessage) == true) {
                return@launch
            }

            user.setMenu(this@TgBot, MainMenu(user), botMessage.lastMenuMessageId)
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
        if (split[0] == "RF") return UUID.fromString(ref.drop(3))
        if (split[0] == "RC") {
            val receipt = PostgresReceiptPersistent.loadReceipt(UUID.fromString(ref.drop(3)))
            return receipt?.issuer?.id
        }
        return null
    }
}