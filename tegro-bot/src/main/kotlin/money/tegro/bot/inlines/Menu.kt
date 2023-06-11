package money.tegro.bot.inlines

import kotlinx.serialization.Serializable
import money.tegro.bot.api.Bot
import money.tegro.bot.objects.BotMessage

@Serializable
sealed interface Menu {
    suspend fun sendKeyboard(bot: Bot, botMessage: BotMessage)
    suspend fun handleMessage(bot: Bot, botMessage: BotMessage): Boolean
}

