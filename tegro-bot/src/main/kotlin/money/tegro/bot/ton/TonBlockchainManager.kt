package money.tegro.bot.ton

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import money.tegro.bot.blockchain.BlockchainManager
import money.tegro.bot.utils.base64
import money.tegro.bot.wallet.Coins
import money.tegro.bot.wallet.CryptoCurrency
import org.ton.api.liteserver.LiteServerDesc
import org.ton.api.pk.PrivateKeyEd25519
import org.ton.api.pub.PublicKeyEd25519
import org.ton.block.AddrStd
import org.ton.block.VarUInteger
import org.ton.lite.client.LiteClient

object TonBlockchainManager : BlockchainManager {
    @OptIn(DelicateCoroutinesApi::class)
    val liteClient = LiteClient(
        newSingleThreadContext("lite-client"), LiteServerDesc(
            id = PublicKeyEd25519(base64("n4VDnSCUuSpjnCyUk9e3QOOd6o0ItSWYbTnW3Wnn8wk=")),
            ip = 84478511,
            port = 19949
        )
    )

    fun getAddress(privateKey: PrivateKeyEd25519): String {
        return getAddrStd(privateKey).toString(userFriendly = true, bounceable = false)
    }

    override fun getAddress(privateKey: ByteArray): String =
        getAddress(PrivateKeyEd25519(privateKey))

    override suspend fun getBalance(address: String): Coins {
        val addrStd = AddrStd(address)
        val contract = getContract(addrStd)
        return Coins(CryptoCurrency.TON, contract.balance.coins.amount.value)
    }

    override suspend fun transfer(privateKey: ByteArray, address: String, value: Coins) {
        val pk = PrivateKeyEd25519(privateKey)
        val contract = getContract(getAddrStd(pk))
        contract.transfer(liteClient, pk) {
            destination = AddrStd(address)
            this.coins = org.ton.block.Coins(VarUInteger(value.amount))
        }
    }

    private fun getAddrStd(privateKey: PrivateKeyEd25519): AddrStd = WalletV3Contract.getAddress(privateKey)

    private suspend fun getContract(addrStd: AddrStd): WalletV3Contract {
        val accountState = liteClient.getAccountState(addrStd)
        return WalletV3Contract(accountState)
    }
}
