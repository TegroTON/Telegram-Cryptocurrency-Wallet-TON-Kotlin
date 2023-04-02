package money.tegro.bot.objects

import kotlinx.serialization.Serializable
import money.tegro.bot.api.Bot
import money.tegro.bot.inlines.Menu
import money.tegro.bot.menuPersistent
import money.tegro.bot.utils.UUIDSerializer
import money.tegro.bot.wallet.Coins
import money.tegro.bot.walletPersistent
import java.util.*

@Serializable
data class User(
    @Serializable(UUIDSerializer::class)
    val id: UUID,
    val tgId: Long?,
    val vkId: Long?,
    val settings: UserSettings
) {

    suspend fun setMenu(bot: Bot, menu: Menu, lastMenuMessageId: Long?) {
        menuPersistent.saveMenu(this, menu)
        menu.sendKeyboard(bot, lastMenuMessageId)
    }

    suspend fun transfer(receiver: User, coins: Coins) {
        walletPersistent.transfer(this, receiver, coins)
    }

    suspend fun freeze(coins: Coins) {
        walletPersistent.freeze(this, coins)
    }

    suspend fun unfreeze(coins: Coins) {
        walletPersistent.unfreeze(this, coins)
    }
}