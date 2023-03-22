package bot.receipts

import kotlinx.serialization.Serializable

@Serializable
data class ReceiptCollection(
    val receipts: Collection<Receipt>
) : Collection<Receipt> by receipts {

}