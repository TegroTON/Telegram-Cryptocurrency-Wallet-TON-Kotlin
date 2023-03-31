package money.tegro.bot.eth

import kotlinx.serialization.Serializable

@Serializable
data class EthRequest<T>(
    val id: Int = 1,
    val jsonrpc: String = "2.0",
    val params: T,
    val method: String,
)