package money.tegro.bot.wallet

import java.math.BigDecimal
import java.math.BigInteger

enum class CryptoCurrency(
    val displayName: String,
    val ticker: String,
    val decimals: Int,
    val minAmount: BigInteger,
    val feeReserve: BigInteger = BigInteger.ZERO,
    val tokenContracts: List<Pair<BlockchainType, String>> = emptyList(),
    val nativeBlockchainType: BlockchainType? = null
) {
    TON(
        displayName = "Toncoin",
        ticker = "TON",
        decimals = 9,
        minAmount = 10000000.toBigInteger(),
        feeReserve = 100000000.toBigInteger(),
        tokenContracts = emptyList(),
        nativeBlockchainType = BlockchainType.TON
    ),
    TGR(
        displayName = "Tegro",
        ticker = "TGR",
        decimals = 9,
        minAmount = 10000000.toBigInteger(),
        tokenContracts = listOf(
            BlockchainType.TON to "0:2f0df5851b4a185f5f63c0d0cd0412f5aca353f577da18ff47c936f99dbd849a",
            BlockchainType.BSC to ""
        )
    ),
    USDT(
        displayName = "Tether USD",
        ticker = "USDT",
        decimals = 6,
        minAmount = 2.toBigInteger(),
        tokenContracts = listOf(
            BlockchainType.TON to "0:bfd58a0cf11062c4df79973ee875c17756b91cfcdf0d7bb8108bb01bb657adfc"
        )
    );

    val ZERO = Coins(this, BigInteger.ZERO)

    private val factor = BigDecimal.TEN.pow(decimals)

    fun getTokenContractAddress(
        blockchainType: BlockchainType
    ): String? = tokenContracts.find { it.first == blockchainType }?.second

    fun fromNano(value: String): BigDecimal =
        fromNano(value.toBigInteger())

    fun fromNano(value: Long): BigDecimal =
        fromNano(value.toBigInteger())

    fun fromNano(value: BigInteger): BigDecimal =
        value.toBigDecimal().divide(factor)

    fun toNano(string: String): BigInteger =
        toNano(string.toBigDecimal())

    fun toNano(value: Double): BigInteger =
        toNano(value.toBigDecimal())

    fun toNano(value: BigDecimal): BigInteger =
        value.multiply(factor).toBigInteger()
}
