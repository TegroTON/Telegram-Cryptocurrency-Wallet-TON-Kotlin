package money.tegro.bot.ton

import org.ton.bitstring.BitString
import org.ton.block.Coins
import org.ton.block.Either
import org.ton.block.Maybe
import org.ton.block.MsgAddressInt
import org.ton.cell.Cell
import org.ton.cell.buildCell
import org.ton.lite.api.liteserver.LiteServerAccountId
import org.ton.lite.client.LiteClient
import org.ton.tlb.constructor.AnyTlbConstructor
import org.ton.tlb.constructor.tlbCodec
import org.ton.tlb.loadTlb
import org.ton.tlb.storeTlb

class JettonWalletContract(
    override val liteClient: LiteClient,
    override val address: MsgAddressInt
) : Contract {

    suspend fun getWalletData(): JettonWalletData {
        val result =
            liteClient.runSmcMethod(LiteServerAccountId(address.workchainId, address.address), "get_wallet_data")
                .toMutableVmStack()
        return JettonWalletData(
            balance = Coins(result.popNumber().toBigInt()),
            ownerAddress = result.popSlice().loadTlb(MsgAddressInt),
            jettonMasterAddress = result.popSlice().loadTlb(MsgAddressInt),
            jettonWalletCode = result.popCell()
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JettonWalletContract) return false
        return address == other.address
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }

    data class JettonWalletData(
        val balance: Coins,
        val ownerAddress: MsgAddressInt,
        val jettonMasterAddress: MsgAddressInt,
        val jettonWalletCode: Cell
    )

    companion object {
        fun transferMessageBody(
            queryId: Long,
            amount: Coins,
            destination: MsgAddressInt,
            responseDestination: MsgAddressInt,
            customPayload: Cell?,
            forwardTonAmount: Coins,
            forwardPayload: Cell
        ) = buildCell {
            storeBits(BitString("0f8a7ea5"))
            storeUInt(queryId, 64)
            storeTlb(Coins, amount)
            storeTlb(MsgAddressInt, destination)
            storeTlb(MsgAddressInt, responseDestination)
            storeTlb(Maybe.tlbCodec(Cell.tlbCodec()), Maybe.of(customPayload))
            storeTlb(Coins, forwardTonAmount)
            storeTlb(
                codec = Either.tlbCodec(AnyTlbConstructor, Cell.tlbCodec()),
                value = if (remainingBits >= forwardPayload.bits.size) {
                    Either.of(forwardPayload, null)
                } else {
                    Either.of(null, forwardPayload)
                }
            )
        }
    }
}
