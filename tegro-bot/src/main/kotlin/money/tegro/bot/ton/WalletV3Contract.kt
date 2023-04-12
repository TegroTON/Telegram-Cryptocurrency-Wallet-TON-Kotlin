package money.tegro.bot.ton

import money.tegro.bot.wallet.AccountNotInitializedException
import org.ton.api.pk.PrivateKeyEd25519
import org.ton.api.pub.PublicKeyEd25519
import org.ton.bitstring.BitString
import org.ton.block.*
import org.ton.cell.Cell
import org.ton.cell.CellBuilder
import org.ton.cell.buildCell
import org.ton.contract.wallet.WalletContract
import org.ton.contract.wallet.WalletTransfer
import org.ton.contract.wallet.WalletTransferBuilder
import org.ton.lite.client.LiteClient
import org.ton.tl.asByteString
import org.ton.tlb.CellRef
import org.ton.tlb.constructor.AnyTlbConstructor
import org.ton.tlb.storeRef

class WalletV3Contract(
    override val liteClient: LiteClient,
    override val address: AddrStd
) : Contract {
    @Throws(AccountNotInitializedException::class)
    suspend fun getWalletData(): WalletV3Data {
        val data =
            ((liteClient.getAccountState(address).account.value as? AccountInfo)?.storage?.state as? AccountActive)?.value?.data?.value?.value?.beginParse()
        require(data != null) { throw AccountNotInitializedException(address) }
        return WalletV3Data(
            seqno = data.loadUInt(32).toInt(),
            subWalletId = data.loadUInt(32).toInt(),
            publicKey = PublicKeyEd25519(data.loadBits(256).toByteArray().asByteString())
        )
    }

    suspend fun getWalletDataOrNull(): WalletV3Data? = try {
        getWalletData()
    } catch (e: AccountNotInitializedException) {
        null
    }

    suspend fun transfer(
        privateKey: PrivateKeyEd25519,
        transfer: WalletTransferBuilder.() -> Unit
    ) {
        val walletData = getWalletDataOrNull()
        val seqno = walletData?.seqno ?: 0
        val walletId = walletData?.subWalletId ?: WalletContract.DEFAULT_WALLET_ID
        val stateInit = if (walletData == null) createStateInit(seqno, walletId, privateKey.publicKey()).value else null
        val message = createTransferMessage(
            address = address,
            stateInit = stateInit,
            privateKey = privateKey,
            walletId = walletId,
            validUntil = Int.MAX_VALUE,
            seqno = seqno,
            WalletTransferBuilder().apply(transfer).build()
        )
        println("Send message: $message")
        liteClient.sendMessage(message)
        while (true) {
            if ((getWalletDataOrNull()?.seqno ?: 0) != (walletData?.seqno ?: 0)) {
                println("$address changed!")
                return
            } else {
                println("Check $address")
            }
        }
    }

    data class WalletV3Data(
        val seqno: Int,
        val subWalletId: Int,
        val publicKey: PublicKeyEd25519
    )

    companion object {
        const val DEFAULT_WALLET_ID: Int = 698983191
        val CODE =
            Cell("FF0020DD2082014C97BA218201339CBAB19F71B0ED44D0D31FD31F31D70BFFE304E0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED54")

        fun getAddress(privateKey: PrivateKeyEd25519): AddrStd {
            val stateInitRef = createStateInit(0, DEFAULT_WALLET_ID, privateKey.publicKey())
            val hash = stateInitRef.hash()
            return AddrStd(0, hash)
        }

        fun createStateInit(
            seqno: Int,
            subWalletId: Int,
            publicKey: PublicKeyEd25519
        ): CellRef<StateInit> {
            val data = buildCell {
                storeUInt(seqno, 32)
                storeUInt(subWalletId, 32)
                storeBytes(publicKey.key.toByteArray())
            }
            return CellRef(
                StateInit(
                    CODE, data
                ),
                StateInit
            )
        }

        fun createTransferMessage(
            address: MsgAddressInt,
            stateInit: StateInit?,
            privateKey: PrivateKeyEd25519,
            walletId: Int,
            validUntil: Int,
            seqno: Int,
            vararg transfers: WalletTransfer
        ): Message<Cell> {
            val info = ExtInMsgInfo(
                src = AddrNone,
                dest = address,
                importFee = Coins()
            )
            val maybeStateInit =
                Maybe.of(stateInit?.let { Either.of<StateInit, CellRef<StateInit>>(null, CellRef(it)) })
            val transferBody = createTransferMessageBody(
                privateKey,
                walletId,
                validUntil,
                seqno,
                *transfers
            )
            val body = Either.of<Cell, CellRef<Cell>>(null, CellRef(transferBody))
            return Message(
                info = info,
                init = maybeStateInit,
                body = body
            )
        }

        private fun createTransferMessageBody(
            privateKey: PrivateKeyEd25519,
            walletId: Int,
            validUntil: Int,
            seqno: Int,
            vararg gifts: WalletTransfer
        ): Cell {
            val unsignedBody = CellBuilder.createCell {
                storeUInt(walletId, 32)
                storeUInt(validUntil, 32)
                storeUInt(seqno, 32)
                for (gift in gifts) {
                    var sendMode = 3
                    if (gift.sendMode > -1) {
                        sendMode = gift.sendMode
                    }
                    val intMsg = CellRef(createIntMsg(gift))

                    storeUInt(sendMode, 8)
                    storeRef(MessageRelaxed.tlbCodec(AnyTlbConstructor), intMsg)
                }
            }
            val signature = BitString(privateKey.sign(unsignedBody.hash().toByteArray()))

            return CellBuilder.createCell {
                storeBits(signature)
                storeBits(unsignedBody.bits)
                storeRefs(unsignedBody.refs)
            }
        }

        private fun createIntMsg(gift: WalletTransfer): MessageRelaxed<Cell> {
            val info = CommonMsgInfoRelaxed.IntMsgInfoRelaxed(
                ihrDisabled = true,
                bounce = gift.bounceable,
                bounced = false,
                src = AddrNone,
                dest = gift.destination,
                value = gift.coins,
                ihrFee = Coins(),
                fwdFee = Coins(),
                createdLt = 0u,
                createdAt = 0u
            )
            val init = Maybe.of(gift.stateInit?.let {
                Either.of<StateInit, CellRef<StateInit>>(null, CellRef(it))
            })
            val giftBody = gift.body
            val body = if (giftBody == null) {
                Either.of<Cell, CellRef<Cell>>(Cell.empty(), null)
            } else {
                Either.of<Cell, CellRef<Cell>>(null, CellRef(giftBody))
            }

            return MessageRelaxed(
                info = info,
                init = init,
                body = body,
            )
        }
    }
}
