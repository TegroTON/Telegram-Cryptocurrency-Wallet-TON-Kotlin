package money.tegro.bot.ton

import org.ton.block.Coins
import org.ton.block.Either
import org.ton.block.Maybe
import org.ton.block.MsgAddressInt
import org.ton.cell.Cell
import org.ton.cell.CellBuilder
import org.ton.cell.CellSlice
import org.ton.tlb.*
import org.ton.tlb.constructor.AnyTlbConstructor
import org.ton.tlb.constructor.tlbCodec

data class JettonTransfer(
    val queryId: Long,
    val amount: Coins,
    val destination: MsgAddressInt,
    val responseDestination: MsgAddressInt,
    val customPayload: Maybe<Cell>,
    val forwardTonAmount: Coins,
    val forwardPayload: Either<Cell, Cell>
) {
    companion object : TlbCodec<JettonTransfer> by JettonTransferTlConstructor.asTlbCombinator()
}

private object JettonTransferTlConstructor : TlbConstructor<JettonTransfer>(
    schema = "transfer#0f8a7ea5 query_id:uint64 amount:(VarUInteger 16) destination:MsgAddress\n" +
            "                 response_destination:MsgAddress custom_payload:(Maybe ^Cell)\n" +
            "                 forward_ton_amount:(VarUInteger 16) forward_payload:(Either Cell ^Cell)\n" +
            "                 = InternalMsgBody;"
) {
    override fun loadTlb(cellSlice: CellSlice): JettonTransfer {
        val queryId = cellSlice.loadUInt(64).toLong()
        val amount = cellSlice.loadTlb(Coins)
        val destination = cellSlice.loadTlb(MsgAddressInt)
        val responseDestination = cellSlice.loadTlb(MsgAddressInt)
        val customPayload = cellSlice.loadTlb(Maybe.tlbCodec(Cell.tlbCodec()))
        val forwardTonAmount = cellSlice.loadTlb(Coins)
        val forwardPayload = cellSlice.loadTlb(Either.tlbCodec(AnyTlbConstructor, Cell.tlbCodec()))
        return JettonTransfer(
            queryId,
            amount,
            destination,
            responseDestination,
            customPayload,
            forwardTonAmount,
            forwardPayload
        )
    }

    override fun storeTlb(cellBuilder: CellBuilder, value: JettonTransfer) {
        cellBuilder.apply {
            storeUInt(value.queryId, 64)
            storeTlb(Coins, value.amount)
            storeTlb(MsgAddressInt, value.destination)
            storeTlb(MsgAddressInt, value.responseDestination)
            storeTlb(Maybe.tlbCodec(Cell.tlbCodec()), value.customPayload)
            storeTlb(Coins, value.forwardTonAmount)
            storeTlb(
                codec = Either.tlbCodec(AnyTlbConstructor, Cell.tlbCodec()),
                value = value.forwardPayload
            )
        }
    }
}
