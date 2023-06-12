package money.tegro.bot.wallet

import kotlinx.serialization.Serializable
import money.tegro.bot.exceptions.NegativeCoinsException
import money.tegro.bot.objects.LocalCurrency
import money.tegro.bot.objects.User
import money.tegro.bot.utils.BigIntegerSerializer
import money.tegro.bot.utils.RatePersistent
import java.math.BigDecimal
import java.math.BigInteger

@Serializable
data class Coins(
    val currency: CryptoCurrency,
    @Serializable(BigIntegerSerializer::class)
    val amount: BigInteger
) : Comparable<Coins> {
    constructor(currency: CryptoCurrency, amount: BigDecimal) : this(
        currency,
        currency.toNano(amount)
    )

    init {
        require(amount >= BigInteger.ZERO) {
            throw NegativeCoinsException(currency, amount)
        }
    }

    fun toBigInteger(): BigInteger = amount
    fun toBigDecimal(): BigDecimal = currency.fromNano(amount)

    override fun compareTo(other: Coins): Int = amount.compareTo(other.amount)

    operator fun plus(other: Coins): Coins {
        require(currency == other.currency) { "expected: $currency, actual: ${other.currency}" }
        return Coins(currency, amount + other.amount)
    }

    operator fun plus(other: BigInteger): Coins = plus(Coins(currency, other))

    operator fun minus(other: Coins): Coins {
        require(currency == other.currency) { "expected: $currency, actual: ${other.currency}" }
        val newAmount = amount - other.amount
        return Coins(currency, newAmount)
    }

    operator fun minus(other: BigInteger): Coins = minus(Coins(currency, other))

    override fun toString(): String = "${currency.fromNano(amount)} ${currency.ticker}"

    fun toStringWithRate(user: User): String {
        return toStringWithRate(user.settings.localCurrency)
    }

    fun toStringWithRate(localCurrency: LocalCurrency): String {
        val rate = RatePersistent.getRate(currency, localCurrency)
        val result =
            if (amount == 0.toBigInteger()) 0.toBigDecimal() else currency.fromNano(amount) * rate.toBigDecimal()
        return "${currency.fromNano(amount)} ${currency.ticker} ($result ${localCurrency.ticker})"
    }

    companion object {
        fun fromDecimal(currency: CryptoCurrency, decimal: BigDecimal) =
            Coins(currency, decimal)

        fun fromDecimal(currency: CryptoCurrency, decimal: Double) =
            fromDecimal(currency, decimal.toBigDecimal())

        fun fromDecimal(currency: CryptoCurrency, decimal: String) =
            Coins(currency, decimal.toBigDecimal())

        fun fromNano(currency: CryptoCurrency, nano: BigInteger) =
            Coins(currency, nano)

        fun fromNano(currency: CryptoCurrency, nano: Long) =
            fromNano(currency, nano.toBigInteger())

        fun fromNano(currency: CryptoCurrency, nano: String) =
            Coins(currency, nano.toBigDecimal())
    }
}
