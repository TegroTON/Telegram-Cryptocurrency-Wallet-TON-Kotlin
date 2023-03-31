package money.tegro.bot.objects

import money.tegro.bot.utils.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class UserSettings(
    @Serializable(UUIDSerializer::class)
    val userId: UUID,
    val lang: Language,
    val localCurrency: LocalCurrency,
    @Serializable(UUIDSerializer::class)
    val referralId: UUID?
)