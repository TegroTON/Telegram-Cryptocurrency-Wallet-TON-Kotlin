package money.tegro.bot.utils

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import money.tegro.bot.objects.LocalCurrency
import money.tegro.bot.wallet.CryptoCurrency
import kotlin.time.Duration.Companion.minutes

interface RatePersistent {

    suspend fun getRate(cryptoCurrency: CryptoCurrency, localCurrency: LocalCurrency): Double

    companion object {

        val cache = emptyMap<Pair<CryptoCurrency, LocalCurrency>, Double>().toMutableMap()

        suspend fun start() {
            while (true) {
                cache.clear()
                for (cc in CryptoCurrency.values()) {
                    for (lc in LocalCurrency.values()) {
                        val rate = HttpRatePersistent.getRate(cc, lc)
                        cache[Pair(cc, lc)] = rate
                    }
                }
                delay(5.minutes)
            }
        }

        fun getRate(cryptoCurrency: CryptoCurrency, localCurrency: LocalCurrency): Double {
            return cache[Pair(cryptoCurrency, localCurrency)] ?: return 0.toDouble()
        }
    }
}

object HttpRatePersistent : RatePersistent {

    private const val endpoint = "https://api.coingecko.com/api/v3/coins/"

    private val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                encodeDefaults = true
                isLenient = true
            })
        }
    }

    override suspend fun getRate(cryptoCurrency: CryptoCurrency, localCurrency: LocalCurrency): Double {

        val response = httpClient.get("$endpoint/${cryptoCurrency.coingecoId}") {
            contentType(ContentType.Application.Json)
        }.body<String>()

        val json = Json.parseToJsonElement(response).jsonObject
        val marketData = json["market_data"]?.jsonObject ?: return 0.toDouble()
        val currentPrice = marketData["current_price"]?.jsonObject ?: return 0.toDouble()

        return currentPrice[localCurrency.ticker.lowercase()]?.jsonPrimitive?.doubleOrNull ?: return 0.toDouble()
    }

}