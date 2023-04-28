package money.tegro.bot.objects

import kotlinx.serialization.Serializable

@Serializable
data class Chat(
    val id: Long,
    val title: String,
    val username: String
)