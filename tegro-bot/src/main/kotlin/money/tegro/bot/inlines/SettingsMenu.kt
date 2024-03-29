package money.tegro.bot.inlines

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.objects.*
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.button
import money.tegro.bot.utils.linkButton

@Serializable
class SettingsMenu(
    val user: User,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, botMessage: BotMessage) {
        bot.updateKeyboard(
            to = botMessage.peerId,
            lastMenuMessageId = botMessage.lastMenuMessageId,
            message = Messages[user.settings.lang].menuSettingsMessage,
            keyboard = BotKeyboard {
                row {
                    button(
                        Messages[user.settings.lang].menuSettingsRefs,
                        ButtonPayload.serializer(),
                        SettingsMenu.ButtonPayload.REFS
                    )
                }
                row {
                    button(
                        String.format(
                            Messages[user.settings.lang].menuSettingsLang,
                            user.settings.lang.displayName
                        ),
                        ButtonPayload.serializer(),
                        ButtonPayload.LANG
                    )
                }
                row {
                    button(
                        String.format(
                            Messages[user.settings.lang].menuSettingsCurrency,
                            user.settings.localCurrency.ticker
                        ),
                        ButtonPayload.serializer(),
                        ButtonPayload.CURRENCY
                    )
                }
                row {
                    linkButton(
                        Messages[user.settings.lang].menuSettingsHints,
                        "https://t.me/TegroForum",
                        ButtonPayload.serializer(),
                        ButtonPayload.HINTS
                    )
                    linkButton(
                        Messages[user.settings.lang].menuSettingsHelp,
                        "https://t.me/TegroLive",
                        ButtonPayload.serializer(),
                        ButtonPayload.HELP
                    )
                }
                row {
                    button(
                        Messages[user.settings.lang].menuButtonBack,
                        ButtonPayload.serializer(),
                        ButtonPayload.BACK
                    )
                }
            }
        )
    }

    override suspend fun handleMessage(bot: Bot, botMessage: BotMessage): Boolean {
        val payload = botMessage.payload ?: return false
        when (Json.decodeFromString<ButtonPayload>(payload)) {
            ButtonPayload.REFS -> user.setMenu(bot, ReferralsMenu(user, this), botMessage)
            ButtonPayload.LANG -> {
                val changeTo = if (user.settings.lang == Language.RU) Language.EN else Language.RU
                val userSettings = user.settings.copy(lang = changeTo)
                PostgresUserPersistent.saveSettings(userSettings)
                val newUser = user.copy(
                    settings = userSettings
                )
                newUser.setMenu(bot, SettingsMenu(newUser, MainMenu(newUser)), botMessage)
            }

            ButtonPayload.CURRENCY -> {
                val changeTo =
                    if (user.settings.localCurrency == LocalCurrency.RUB) LocalCurrency.USD else LocalCurrency.RUB
                val userSettings = user.settings.copy(localCurrency = changeTo)
                PostgresUserPersistent.saveSettings(userSettings)
                val newUser = user.copy(
                    settings = userSettings
                )
                newUser.setMenu(bot, SettingsMenu(newUser, MainMenu(newUser)), botMessage)
            }

            ButtonPayload.HINTS -> TODO()
            ButtonPayload.HELP -> TODO()
            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, botMessage)
            }
        }
        return true
    }

    @Serializable
    private enum class ButtonPayload {
        REFS,
        LANG,
        CURRENCY,
        HINTS,
        HELP,
        BACK
    }
}