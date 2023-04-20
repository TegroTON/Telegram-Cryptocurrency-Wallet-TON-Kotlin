package money.tegro.bot.objects.keyboard

import kotlinx.serialization.Serializable
import lombok.ToString

@ToString
@Serializable
sealed interface Button {
    fun getText(): String
    fun getLink(): String
    fun getPayload(): String
}