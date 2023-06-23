package money.tegro.bot.objects

import kotlinx.serialization.Serializable
import money.tegro.bot.utils.UUIDSerializer
import java.util.*

@Serializable
data class BlacklistEntry(
    @Serializable(UUIDSerializer::class)
    val id: UUID,
    @Serializable(UUIDSerializer::class)
    val userId: UUID,
    val address: String?,
    val phone: String?,
    val reason: String?
)