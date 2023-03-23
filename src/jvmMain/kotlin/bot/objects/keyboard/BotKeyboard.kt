package bot.objects.keyboard

import com.petersamokhin.vksdk.core.model.objects.Keyboard
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Serializable
data class BotKeyboard(
    val buttons: List<BotKeyboardRow> = emptyList(),
    @SerialName("one_time")
    val oneTime: Boolean = false
) {
    fun toVk(): Keyboard {
        val keyboard = ArrayList<ArrayList<Keyboard.Button>>()
        for (row in buttons) {
            val keyboardRow = ArrayList<Keyboard.Button>()
            for (button in row.buttons) {
                if (button.getLink() == "") {
                    keyboardRow.add(
                        Keyboard.Button(
                            action = Keyboard.Button.Action(
                                type = Keyboard.Button.Action.Type.TEXT,
                                label = button.getText(),
                                payload = button.getPayload()
                            ),
                            color = Keyboard.Button.Color.SECONDARY
                        )
                    )
                } else {
                    keyboardRow.add(
                        Keyboard.Button(
                            action = Keyboard.Button.Action(
                                type = Keyboard.Button.Action.Type.OPEN_LINK,
                                link = button.getLink(),
                                label = button.getText()
                            )
                        )
                    )
                }
            }
            if (keyboardRow.isNotEmpty()) keyboard.add(keyboardRow)
        }
        return Keyboard(buttons = keyboard, oneTime = false, inline = true, authorId = null)
    }

    fun toTg(): InlineKeyboardMarkup {
        val inline = InlineKeyboardMarkup()
        val rows: MutableList<MutableList<InlineKeyboardButton>> = ArrayList()
        for (botRow in buttons) {
            val row: MutableList<InlineKeyboardButton> = ArrayList()
            for (button in botRow.buttons) {
                val ib = InlineKeyboardButton()
                ib.text = button.getText()
                ib.callbackData = button.getPayload()
                if (button.getLink() != "") {
                    ib.url = button.getLink()
                }
                row.add(ib)
            }
            rows.add(row)
        }

        inline.keyboard = rows
        return inline
    }

    companion object {
        operator fun invoke(
            oneTime: Boolean = false,
            builder: KeyboardDslBuilder.() -> Unit
        ): BotKeyboard = BotKeyboard(
            oneTime = oneTime,
            buttons = KeyboardDslBuilder().apply(builder).rows
        )
    }
}