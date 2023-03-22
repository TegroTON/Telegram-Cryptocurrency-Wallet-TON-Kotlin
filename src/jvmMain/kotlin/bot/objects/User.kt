package bot.objects

import bot.api.Bot
import bot.inlines.Menu
import bot.menuPersistent
import bot.utils.UUIDSerializer
import bot.wallet.Coins
import bot.walletPersistent
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class User(
    @Serializable(UUIDSerializer::class)
    val id: UUID,
    val tgId: Long?,
    val vkId: Long?,
    var settings: UserSettings
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