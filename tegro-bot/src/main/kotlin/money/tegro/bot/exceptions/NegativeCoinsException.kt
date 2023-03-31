package money.tegro.bot.exceptions

import money.tegro.bot.wallet.CryptoCurrency
import java.math.BigInteger

class NegativeCoinsException(
    val currency: CryptoCurrency,
    val amount: BigInteger
) : RuntimeException("Negative ${currency.ticker} amount: $amount")