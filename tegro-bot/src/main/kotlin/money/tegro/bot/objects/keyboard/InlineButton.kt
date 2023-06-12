package money.tegro.bot.objects.keyboard

import kotlinx.serialization.Serializable

@Serializable
class InlineButton(
    private val text: String,
    private val query: String,
    private val payload: String
) : Button {
    internal fun build(): Button = InlineButton(
        text = text,
        query = query,
        payload = payload
    )

    override fun getText(): String {
        return text
    }

    override fun getLink(): String {
        return query
    }

    override fun getPayload(): String {
        return payload
    }
}