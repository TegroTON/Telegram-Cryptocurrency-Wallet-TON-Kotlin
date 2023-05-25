package money.tegro.bot.objects

import kotlinx.serialization.Serializable
import money.tegro.bot.utils.UUIDSerializer
import java.util.*

@Serializable
data class UserSettings(
    @Serializable(UUIDSerializer::class)
    val userId: UUID,
    val lang: Language,
    val localCurrency: LocalCurrency,
    @Serializable(UUIDSerializer::class)
    val referralId: UUID?,
    val address: String = "",
    val nfts: List<Nft> = emptyList()
)