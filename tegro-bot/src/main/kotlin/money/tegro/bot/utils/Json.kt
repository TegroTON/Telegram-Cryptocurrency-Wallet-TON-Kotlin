package money.tegro.bot.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

val JSON = Json {
    prettyPrint = true
    serializersModule = SerializersModule {
        contextual(UUIDSerializer)
    }
}