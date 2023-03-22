package bot.exceptions

import bot.wallet.CryptoCurrency
import java.math.BigInteger

class NegativeCoinsException(
    val currency: CryptoCurrency,
    val amount: BigInteger
) : RuntimeException("Negative ${currency.ticker} amount: $amount")