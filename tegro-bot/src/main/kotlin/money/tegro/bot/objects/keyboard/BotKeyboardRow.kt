package money.tegro.bot.objects.keyboard

import kotlinx.serialization.Serializable

@Serializable
class BotKeyboardRow(
    val buttons: ArrayList<Button> = ArrayList()
) {
    fun addButton(button: Button) {
        buttons.add(button)
    }
}