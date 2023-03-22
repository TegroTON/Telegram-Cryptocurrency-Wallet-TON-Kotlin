package bot.inlines

import bot.api.Bot
import bot.objects.BotMessage
import kotlinx.serialization.Serializable

@Serializable
sealed interface Menu {
    suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?)
    suspend fun handleMessage(bot: Bot, message: BotMessage): Boolean
}

