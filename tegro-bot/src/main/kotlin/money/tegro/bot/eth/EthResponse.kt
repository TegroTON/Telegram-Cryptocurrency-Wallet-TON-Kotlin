package money.tegro.bot.eth

import kotlinx.serialization.Serializable

@Serializable
data class EthResponse<T>(
    val jsonrpc: String = "2.0",
    val id: Int,
    val result: T? = null,
    val error: EthError? = null
)