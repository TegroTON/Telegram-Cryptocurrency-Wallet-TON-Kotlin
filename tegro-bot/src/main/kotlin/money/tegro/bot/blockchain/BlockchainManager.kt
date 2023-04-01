package money.tegro.bot.blockchain

import money.tegro.bot.wallet.Coins

interface BlockchainManager {
    fun getAddress(privateKey: ByteArray): String

    suspend fun getBalance(address: String): Coins

    suspend fun transfer(
        privateKey: ByteArray,
        address: String,
        value: Coins
    )
}
