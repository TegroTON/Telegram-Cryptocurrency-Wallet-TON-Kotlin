package money.tegro.bot.api

import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Chat
import money.tegro.bot.objects.keyboard.BotKeyboard
import java.io.File
import java.io.InputStream

sealed interface Bot {
    suspend fun sendMessage(to: Long, message: String)
    suspend fun sendMessageKeyboard(to: Long, message: String, keyboard: BotKeyboard)
    suspend fun updateKeyboard(to: Long, lastMenuMessageId: Long?, message: String, keyboard: BotKeyboard)
    suspend fun sendPhoto(to: Long, message: String, file: File, keyboard: BotKeyboard?)
    suspend fun sendPhoto(to: Long, message: String, inputStream: InputStream, filename: String, keyboard: BotKeyboard?)
    suspend fun deleteMessage(peerId: Long, messageId: Long)
    suspend fun sendPopup(botMessage: BotMessage, message: String): Boolean
    suspend fun getChat(chatId: Long): Chat?
    suspend fun isUserInChat(chatId: Long, userId: Long): Boolean
}