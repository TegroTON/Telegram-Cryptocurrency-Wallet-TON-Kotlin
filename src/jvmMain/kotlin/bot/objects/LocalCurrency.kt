package bot.objects

import java.math.BigInteger

enum class LocalCurrency(
    val displayName: String,
    val ticker: String,
    val decimals: Int
) {
    RUB("Russian Rouble", "RUB", 2),
    USD("USA Dollar", "USD", 2);

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