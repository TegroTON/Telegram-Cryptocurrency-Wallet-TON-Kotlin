package money.tegro.bot.ton

import money.tegro.bot.utils.base64
import money.tegro.bot.utils.toByteArray
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import org.ton.api.liteserver.LiteServerDesc
import org.ton.api.pk.PrivateKeyEd25519
import org.ton.api.pub.PublicKeyEd25519
import org.ton.block.AddrStd
import org.ton.boc.BagOfCells
import org.ton.contract.wallet.WalletV4R2Contract
import org.ton.lite.api.liteserver.LiteServerAccountId
import org.ton.lite.api.liteserver.LiteServerTransactionList
import org.ton.lite.api.liteserver.functions.LiteServerGetAccountState
import org.ton.lite.client.LiteClient
import java.util.*
import kotlin.experimental.xor

suspend fun main() {
    val tr = TonUtils.getTransactions(AddrStd("EQAKtVj024T9MfYaJzU1xnDAkf_GGbHNu-V2mgvyjTuP6rvC"))
}

object TonUtils {
    @OptIn(DelicateCoroutinesApi::class)
    val liteClient = LiteClient(
        newSingleThreadContext("lite-client"), LiteServerDesc(
            id = PublicKeyEd25519(base64("n4VDnSCUuSpjnCyUk9e3QOOd6o0ItSWYbTnW3Wnn8wk=")),
            ip = 84478511,
            port = 19949
        )
    )

    fun getUserKey(uuid: UUID, masterKey: ByteArray): PrivateKeyEd25519 {
        val uuidBytes = uuid.toByteArray()
        val userKey = ByteArray(32)
        uuidBytes.copyInto(userKey, destinationOffset = 0)
        uuidBytes.copyInto(userKey, destinationOffset = 16)
        repeat(32) { byteIndex ->
            userKey[byteIndex] = userKey[byteIndex] xor masterKey[byteIndex]
        }
        return PrivateKeyEd25519(userKey)
    }

    fun getUserAddress(uuid: UUID, masterKey: ByteArray): AddrStd {
        val userKey = getUserKey(uuid, masterKey)
        return WalletV4R2Contract(0, userKey.publicKey()).address as AddrStd
    }

    suspend fun getUserTransactions(uuid: UUID, masterKey: ByteArray): LiteServerTransactionList? {
        val address = getUserAddress(uuid, masterKey)
        return getTransactions(address)
    }

    suspend fun getTransactions(address: AddrStd): LiteServerTransactionList? {
        val accountId = LiteServerAccountId(address.workchainId, address.address)


        val lastBlockId = liteClient.getLastBlockId()
        val account = liteClient.liteApi(LiteServerGetAccountState(lastBlockId, accountId))
        val shardProofCell = BagOfCells(account.shardProof).first()
        val accountProofCell = BagOfCells(account.proof).first()

        return null
    }
}