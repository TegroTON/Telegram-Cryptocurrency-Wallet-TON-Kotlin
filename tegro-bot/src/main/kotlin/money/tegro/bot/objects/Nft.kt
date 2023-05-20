package money.tegro.bot.objects

import kotlinx.serialization.Serializable
import lombok.ToString
import money.tegro.bot.utils.UUIDSerializer
import java.util.*

@ToString
@Serializable
data class Nft(
    @Serializable(UUIDSerializer::class)
    val id: UUID,
    val name: String,
    val address: String,
    @Serializable(UUIDSerializer::class)
    val ownerId: UUID,
    val ownerAddress: String,
    val imageLink: String,
    val collection: NftCollection,
)