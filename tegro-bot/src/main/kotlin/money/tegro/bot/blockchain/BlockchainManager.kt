package money.tegro.bot.blockchain

import money.tegro.bot.ton.TonBlockchainManager
import money.tegro.bot.wallet.BlockchainType
import money.tegro.bot.wallet.Coins
import money.tegro.bot.wallet.CryptoCurrency

interface BlockchainManager {
    val type: BlockchainType

    fun getAddress(privateKey: ByteArray): String

    suspend fun getBalance(address: String): Coins

    suspend fun getTokenBalance(cryptoCurrency: CryptoCurrency, ownerAddress: String): Coins

    suspend fun transfer(
        privateKey: ByteArray,
        destinationAddress: String,
        value: Coins
    )

    suspend fun transferToken(
        privateKey: ByteArray,
        cryptoCurrency: CryptoCurrency,
        destinationAddress: String,
        value: Coins
    )

    fun isValidAddress(address: String?): Boolean

    companion object {
        operator fun get(type: BlockchainType): BlockchainManager = when (type) {
            BlockchainType.TON -> TonBlockchainManager
            BlockchainType.BSC -> TODO(type.displayName)
            BlockchainType.ETH -> TODO(type.displayName)
        }
    }
}
