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
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = MessagesContainer[user.settings.lang].menuSettingsMessage,
            keyboard = BotKeyboard {
                row {
                    button(
                        MessagesContainer[user.settings.lang].menuSettingsRefs,
                        ButtonPayload.serializer(),
                        SettingsMenu.ButtonPayload.REFS
                    )
                }
                row {
                    button(
                        String.format(
                            MessagesContainer[user.settings.lang].menuSettingsLang,
                            user.settings.lang.displayName
                        ),
                        ButtonPayload.serializer(),
                        ButtonPayload.LANG
                    )
                }
                row {
                    button(
                        String.format(
                            MessagesContainer[user.settings.lang].menuSettingsCurrency,
                            user.settings.localCurrency.ticker
                        ),
                        ButtonPayload.serializer(),
                        ButtonPayload.CURRENCY
                    )
                }
                row {
                    linkButton(
                        MessagesContainer[user.settings.lang].menuSettingsHints,
                        "https://justkiwi.ru",
                        ButtonPayload.serializer(),
                        ButtonPayload.HINTS
                    )
                    linkButton(
                        MessagesContainer[user.settings.lang].menuSettingsHelp,
                        "https://vk.me/justkiwi_bot",
                        ButtonPayload.serializer(),
                        ButtonPayload.HELP
                    )
                }
                row {
                    button(
                        MessagesContainer[user.settings.lang].menuButtonBack,
                        WalletMenu.ButtonPayload.serializer(),
                        WalletMenu.ButtonPayload.BACK
                    )
                }
            }
        )
    }

    override suspend fun handleMessage(bot: Bot, message: BotMessage): Boolean {
        val payload = message.payload ?: return false
        when (Json.decodeFromString<ButtonPayload>(payload)) {
            ButtonPayload.REFS -> user.setMenu(bot, ReferralsMenu(user, this), message.lastMenuMessageId)
            ButtonPayload.LANG -> {
                val changeTo = if (user.settings.lang == Language.RU) Language.EN else Language.RU
                val userSettings = UserSettings(
                    user.id,
                    changeTo,
                    user.settings.localCurrency,
                    user.settings.referralId
                )
                user.settings = userSettings
                PostgresUserPersistent.saveSettings(userSettings)
                user.setMenu(bot, SettingsMenu(user, MainMenu(user)), message.lastMenuMessageId)
            }

            ButtonPayload.CURRENCY -> {
                val changeTo =
                    if (user.settings.localCurrency == LocalCurrency.RUB) LocalCurrency.USD else LocalCurrency.RUB
                val userSettings = UserSettings(
                    user.id,
                    user.settings.lang,
                    changeTo,
                    user.settings.referralId
                )
                user.settings = userSettings
                PostgresUserPersistent.saveSettings(userSettings)
                user.setMenu(bot, SettingsMenu(user, MainMenu(user)), message.lastMenuMessageId)
            }

            ButtonPayload.HINTS -> TODO()
            ButtonPayload.HELP -> TODO()
            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, message.lastMenuMessageId)
            }
        }
        return true
    }

    @Serializable
    enum class ButtonPayload {
        REFS,
        LANG,
        CURRENCY,
        HINTS,
        HELP,
        BACK
    }
}