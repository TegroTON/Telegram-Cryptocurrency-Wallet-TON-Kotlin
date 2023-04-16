package money.tegro.bot.objects

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import money.tegro.bot.utils.UUIDSerializer
import java.util.*

@Serializable
data class Log(
    @Serializable(UUIDSerializer::class)
    val id: UUID,
    @Serializable(UUIDSerializer::class)
    val userId: UUID,
    val time: Instant,
    val logType: LogType,
    val info: String
)