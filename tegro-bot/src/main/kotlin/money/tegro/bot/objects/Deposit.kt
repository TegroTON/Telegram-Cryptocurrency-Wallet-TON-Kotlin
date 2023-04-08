package money.tegro.bot.objects

import money.tegro.bot.utils.UUIDSerializer
import kotlinx.serialization.Serializable
import money.tegro.bot.wallet.Coins
import money.tegro.bot.wallet.CryptoCurrency
import java.util.*

@Serializable
data class Deposit(
    @Serializable(UUIDSerializer::class)
    val userId: UUID,
    val depositPeriod: DepositPeriod,
    val coins: Coins,
)