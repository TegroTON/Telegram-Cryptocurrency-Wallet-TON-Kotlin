package bot.objects.keyboard

import kotlinx.serialization.Serializable

@Serializable
class OpenLinkButton(
    private val text: String,
    private val link: String,
    private val payload: String
) : Button {
    internal fun build(): Button = OpenLinkButton(
        text = text,
        link = link,
        payload = payload
    )

    override fun getText(): String {
        return text
    }

    override fun getLink(): String {
        return link
    }

    override fun getPayload(): String {
        return payload
    }
}