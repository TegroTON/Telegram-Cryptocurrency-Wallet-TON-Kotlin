package money.tegro.bot.wallet

import java.math.BigDecimal
import java.math.BigInteger

enum class CryptoCurrency(
    val displayName: String,
    val ticker: String,
    val decimals: Int,
    val minAmount: Long,
    val feeReserve: BigInteger = BigInteger.ZERO
) {
    TON("Toncoin", "TON", 9, 10000000, feeReserve = 100000000.toBigInteger()),
    TGR("Tegro", "TGR", 9, 10000000),
    USDT("Tether USD", "USDT", 2, 2);

    val ZERO = Coins(this, BigInteger.ZERO)

    private val factor = BigDecimal.TEN.pow(decimals)

    fun fromNano(value: String): BigDecimal =
        fromNano(BigInteger(value))

    fun fromNano(value: BigInteger): BigDecimal =
        value.toBigDecimal().divide(factor)

    fun toNano(string: String): BigInteger =
        toNano(BigDecimal(string))

    fun toNano(value: BigDecimal): BigInteger =
        value.multiply(factor).toBigInteger()

    fun toString(value: BigInteger): String =
        fromNano(value).toString()
}
