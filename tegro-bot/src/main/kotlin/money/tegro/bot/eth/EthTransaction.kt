package money.tegro.bot.eth

import kotlinx.serialization.Serializable

@Serializable
data class EthTransaction(
    val from: String,
    val to: String? = null,
    val gas: String? = null,
    val gasPrice: String? = null,
    val value: String? = null,
    val data: String? = null,
    val nonce: String? = null,
)