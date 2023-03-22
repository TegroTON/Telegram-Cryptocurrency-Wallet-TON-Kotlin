package bot.wallet

import java.math.BigInteger

enum class CryptoCurrency(
    val displayName: String,
    val ticker: String,
    val decimals: Int,
    val minAmount: Long
) {
    TON("Toncoin", "TON", 9, 10000000),
    TGR("Tegro", "TGR", 9, 10000000),
    USDT("Tether USD", "USDT", 2, 2);

    val ZERO = Coins(this, BigInteger.ZERO)

    fun toString(value: BigInteger): String {
        return value.toString().let {
            it.dropLast(decimals).ifEmpty { "0" } + it.appendDecimals()
        }
    }

    private fun String.appendDecimals(): String {
        val decimals = takeLast(decimals).padStart(decimals, '0')
        val croppedDecimals = decimals.removeSuffix(takeLastWhile { it == '0' })
        return if (croppedDecimals.isNotEmpty() && croppedDecimals != "0") ".${croppedDecimals}"
        else ""
    }
}