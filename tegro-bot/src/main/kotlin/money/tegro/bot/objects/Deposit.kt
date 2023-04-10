package money.tegro.bot.objects

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import money.tegro.bot.utils.UUIDSerializer
import money.tegro.bot.wallet.Coins
import java.util.*

@Serializable
data class Deposit(
    @Serializable(UUIDSerializer::class)
    val userId: UUID,
    val depositPeriod: DepositPeriod,
    val finishDate: Instant,
    val coins: Coins,
)