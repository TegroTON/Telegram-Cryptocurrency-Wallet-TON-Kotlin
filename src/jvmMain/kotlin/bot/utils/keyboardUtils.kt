package bot.utils

import bot.objects.keyboard.OpenLinkButton
import bot.objects.keyboard.RowDslBuilder
import bot.objects.keyboard.TextButton
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

fun <T> RowDslBuilder.button(
    label: String,
    serializer: KSerializer<T>,
    payload: T,
    block: TextButton.() -> Unit = { }
) {
    val jsonElement = Json.encodeToString(serializer, payload)
    addButton(label, jsonElement, block)
}

fun <T> RowDslBuilder.linkButton(
    label: String,
    link: String,
    serializer: KSerializer<T>,
    payload: T,
    block: OpenLinkButton.() -> Unit = { }
) {
    val jsonElement = Json.encodeToString(serializer, payload)
    addLinkButton(label, link, jsonElement, block)
}
