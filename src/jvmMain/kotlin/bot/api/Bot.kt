package bot.api

import bot.objects.keyboard.BotKeyboard
import java.io.File
import java.io.InputStream

sealed interface Bot {
    suspend fun sendMessage(to: Long, message: String)
    suspend fun sendMessageKeyboard(to: Long, message: String, keyboard: BotKeyboard)
    suspend fun updateKeyboard(to: Long, lastMenuMessageId: Long?, message: String, keyboard: BotKeyboard)
    suspend fun sendPhoto(to: Long, message: String, file: File, keyboard: BotKeyboard?)
    suspend fun sendPhoto(to: Long, message: String, inputStream: InputStream, filename: String, keyboard: BotKeyboard?)
}