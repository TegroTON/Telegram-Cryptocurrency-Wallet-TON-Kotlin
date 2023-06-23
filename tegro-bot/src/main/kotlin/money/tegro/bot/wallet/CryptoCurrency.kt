package money.tegro.bot.wallet

import java.math.BigDecimal
import java.math.BigInteger

enum class CryptoCurrency(
    val displayName: String,
    val coingecoId: String,
    val ticker: String,
    val decimals: Int,
    val minAmount: BigInteger,
    val networkFeeReserve: BigInteger = BigInteger.ZERO,
    val botFee: BigInteger = BigInteger.ZERO,
    val tokenContracts: List<Pair<BlockchainType, String>> = emptyList(),
    val nativeBlockchainType: BlockchainType? = null,
    val isEnabled: Boolean = true
) {
    TON(
        displayName = "Toncoin",
        coingecoId = "the-open-network",
        ticker = "TON",
        decimals = 9,
        minAmount = 10_000_000.toBigInteger(),
        networkFeeReserve = 100_000_000.toBigInteger(),
        botFee = 100_000_000.toBigInteger(), // TODO: Calc & check bot fee EVERYWHERE!!!!!!!! EVERY FUCKING WHERE!!!!!!!!
        tokenContracts = emptyList(),
        nativeBlockchainType = BlockchainType.TON,
    ),
    TGR(
        displayName = "Tegro",
        coingecoId = "tegro",
        ticker = "TGR",
        decimals = 9,
        minAmount = 5_000_000_000.toBigInteger(),
        botFee = 500_000_000_000.toBigInteger(),
        tokenContracts = listOf(
            BlockchainType.TON to "0:2f0df5851b4a185f5f63c0d0cd0412f5aca353f577da18ff47c936f99dbd849a"//,
//            BlockchainType.BSC to "0xd9780513292477C4039dFdA1cfCD89Ff111e9DA5"
        ),
        //isEnabled = false
    ),
    USDT(
        displayName = "Tether USD",
        coingecoId = "tether",
        ticker = "USDT",
        decimals = 6,
        minAmount = 1_000_000.toBigInteger(),
        botFee = 100_000.toBigInteger(),
        tokenContracts = listOf(
//            BlockchainType.TON to "0:bfd58a0cf11062c4df79973ee875c17756b91cfcdf0d7bb8108bb01bb657adfc",
            BlockchainType.BSC to "0x55d398326f99059ff775485246999027b3197955"
        ),
        isEnabled = false
    ),
    BNB(
        displayName = "Binance BNB",
        coingecoId = "binancecoin",
        ticker = "BNB",
        decimals = 9,
        minAmount = 10_000_000.toBigInteger(),
        networkFeeReserve = 20_000_000.toBigInteger(),
        botFee = 1_000_000.toBigInteger(),
        nativeBlockchainType = BlockchainType.BSC,
//        isEnabled = false
    );

    private val factor = BigDecimal.TEN.pow(decimals)
    val isNative = nativeBlockchainType != null

    fun getTokenContractAddress(
        blockchainType: BlockchainType
    ): String? = tokenContracts.find { it.first == blockchainType }?.second

    fun fromNano(value: String): BigDecimal =
        fromNano(value.toBigInteger())

    fun fromNano(value: Long): BigDecimal =
        fromNano(value.toBigInteger())

    fun fromNano(value: BigInteger): BigDecimal =
        value.toBigDecimal().divide(factor)

    fun fromNano(value: BigDecimal): BigDecimal =
        value.divide(factor)

    fun toNano(string: String): BigInteger =
        toNano(string.toBigDecimal())

    fun toNano(value: Double): BigInteger =
        toNano(value.toBigDecimal())

    fun toNano(value: BigDecimal): BigInteger =
        value.multiply(factor).toBigInteger()

    companion object {
        val enabledCurrencies: List<CryptoCurrency> = values().filter { it.isEnabled }
    }
}
