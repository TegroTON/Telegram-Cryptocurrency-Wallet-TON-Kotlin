package money.tegro.bot.objects.keyboard

import kotlinx.serialization.Serializable

@Serializable
class TextButton(
    private val text: String,
    private val payload: String
) : Button {
    internal fun build(): Button = TextButton(
        text = text,
        payload = payload
    )

    override fun getText(): String {
        return text
    }

    override fun getLink(): String {
        return ""
    }

    override fun getPayload(): String {
        return payload
    }
}