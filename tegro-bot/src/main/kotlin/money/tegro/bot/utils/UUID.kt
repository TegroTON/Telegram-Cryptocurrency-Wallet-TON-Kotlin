package money.tegro.bot.utils

import io.ktor.utils.io.core.*
import java.util.*

fun UUID.toByteArray(): ByteArray = buildPacket {
    writeLong(this@toByteArray.mostSignificantBits)
    writeLong(this@toByteArray.leastSignificantBits)
}.readBytes()

fun UUID(byteArray: ByteArray) = with(ByteReadPacket(byteArray)) {
    val most = readLong()
    val least = readLong()
    UUID(most, least)
}
