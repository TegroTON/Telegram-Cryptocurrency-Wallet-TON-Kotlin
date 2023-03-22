package bot.objects.keyboard

import kotlinx.serialization.Serializable

@Serializable
sealed interface Button {
    fun getText(): String
    fun getLink(): String
    fun getPayload(): String
}