package bot.objects.keyboard


/**
 * Use keyboard DSL to build the keyboard
 */


class KeyboardDslBuilder {
    internal val rows: MutableList<BotKeyboardRow> = mutableListOf()

    fun row(block: RowDslBuilder.() -> Unit) {
        rows.add(RowDslBuilder().apply(block).buttons)
    }

    fun openLinkButton(
        label: String,
        link: String,
        payload: String,
        block: OpenLinkButton.() -> Unit = { }
    ) {
        val buttons = BotKeyboardRow()
        buttons.addButton(OpenLinkButton(label, link, payload).apply(block).build())
        rows.add(buttons)
    }
}

class RowDslBuilder {
    internal val buttons = BotKeyboardRow()
    fun addButton(label: String, payload: String?, block: TextButton.() -> Unit = {}) {
        buttons.addButton(TextButton(label, payload ?: "").apply(block).build())
    }

    fun addLinkButton(label: String, link: String, payload: String?, block: OpenLinkButton.() -> Unit = {}) {
        buttons.addButton(OpenLinkButton(label, link, payload ?: "").apply(block).build())
    }
}