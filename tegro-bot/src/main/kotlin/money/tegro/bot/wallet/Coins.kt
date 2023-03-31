package money.tegro.bot.wallet

import kotlinx.serialization.Serializable
import money.tegro.bot.exceptions.NegativeCoinsException
import money.tegro.bot.utils.BigIntegerSerializer
import java.math.BigInteger

@Serializable
data class Coins(
    val currency: CryptoCurrency,
    @Serializable(BigIntegerSerializer::class)
    val amount: BigInteger
) : Comparable<Coins> {
    init {
        require(amount >= BigInteger.ZERO) {
            throw NegativeCoinsException(currency, amount)
        }
    }

    override fun compareTo(other: Coins): Int = amount.compareTo(other.amount)

    operator fun plus(other: Coins): Coins {
        require(currency == other.currency) { "expected: $currency, actual: ${other.currency}" }
        return Coins(currency, amount + other.amount)
    }

    operator fun minus(other: Coins): Coins {
        require(currency == other.currency) { "expected: $currency, actual: ${other.currency}" }
        val newAmount = amount - other.amount
        return Coins(currency, newAmount)
    }

    override fun toString(): String = "${currency.toString(amount)} ${currency.ticker}"
}