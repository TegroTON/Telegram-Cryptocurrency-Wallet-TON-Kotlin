package money.tegro.bot.eth

import kotlinx.serialization.Serializable

@Serializable
data class EthError(
    val code: Int,
    val message: String
)