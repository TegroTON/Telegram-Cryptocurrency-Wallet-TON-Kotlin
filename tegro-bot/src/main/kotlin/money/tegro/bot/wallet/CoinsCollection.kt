package money.tegro.bot.wallet

import kotlinx.serialization.Serializable

@Serializable
data class CoinsCollection(
    val coins: Collection<Coins>
) : Collection<Coins> by coins {
    operator fun get(currency: CryptoCurrency): Coins = coins.find {
        it.currency == currency
    } ?: currency.ZERO

    fun withCoins(coins: Coins): CoinsCollection {
        val newCoinsCollection = this.coins.toMutableList()
        newCoinsCollection.removeIf { it.currency == coins.currency }
        newCoinsCollection.add(coins)
        return CoinsCollection(newCoinsCollection)
    }

    companion object {
        val ZERO = CoinsCollection(
            CryptoCurrency.values().map {
                it.ZERO
            }
        )
    }
}