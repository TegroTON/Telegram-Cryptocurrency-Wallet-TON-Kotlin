package money.tegro.bot.ton

import money.tegro.bot.ton.TonBlockchainManager.liteClient
import org.ton.block.AddrStd
import org.ton.block.Coins
import org.ton.block.MsgAddressInt
import org.ton.block.StateInit
import org.ton.cell.Cell
import org.ton.cell.buildCell
import org.ton.lite.api.liteserver.LiteServerAccountId
import org.ton.lite.client.LiteClient
import org.ton.tlb.CellRef
import org.ton.tlb.loadTlb
import org.ton.tlb.storeTlb

class JettonMasterContract(
    override val liteClient: LiteClient,
    override val address: MsgAddressInt
) : Contract {
    @Volatile
    private var jettonData: JettonData? = null

    suspend fun getJettonData(): JettonData {
        val jettonData = jettonData
        return if (jettonData != null) {
            return jettonData
        } else {
            val result =
                liteClient.runSmcMethod(LiteServerAccountId(address.workchainId, address.address), "get_jetton_data")
                    .toMutableVmStack()
            JettonData(
                totalSupply = Coins(result.popInt()),
                isMintable = result.popBool(),
                adminAddress = result.popSlice().loadTlb(MsgAddressInt),
                jettonContent = result.popCell(),
                jettonWalletCode = result.popCell()
            ).also {
                this.jettonData = it
            }
        }
    }

    suspend fun getWalletAddress(ownerAddress: MsgAddressInt): MsgAddressInt {
        return jettonWalletAddress(
            ownerAddress,
            address,
            getJettonData().jettonWalletCode
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JettonMasterContract) return false

        return address == other.address
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }

    data class JettonData(
        val totalSupply: Coins,
        val isMintable: Boolean,
        val adminAddress: MsgAddressInt,
        val jettonContent: Cell,
        val jettonWalletCode: Cell
    )

    companion object {
        fun jettonWalletData(
            balance: Coins,
            ownerAddress: MsgAddressInt,
            jettonMasterAddress: MsgAddressInt,
            jettonWalletCode: Cell
        ) = buildCell {
            storeTlb(Coins, balance)
            storeTlb(MsgAddressInt, ownerAddress)
            storeTlb(MsgAddressInt, jettonMasterAddress)
            storeRef(jettonWalletCode)
        }

        fun jettonWalletStateInit(
            ownerAddress: MsgAddressInt,
            jettonMasterAddress: MsgAddressInt,
            jettonWalletCode: Cell
        ): CellRef<StateInit> = CellRef(
            value = StateInit(
                data = jettonWalletData(Coins(0), ownerAddress, jettonMasterAddress, jettonWalletCode),
                code = jettonWalletCode
            ),
            codec = StateInit
        )

        fun jettonWalletAddress(
            stateInit: StateInit,
            workchain: Int = 0
        ): AddrStd = jettonWalletAddress(CellRef(stateInit, StateInit), workchain)

        fun jettonWalletAddress(
            stateInit: CellRef<StateInit>,
            workchain: Int = 0
        ): AddrStd = AddrStd(workchain, stateInit.hash(StateInit))

        fun jettonWalletAddress(
            ownerAddress: MsgAddressInt,
            jettonMasterAddress: MsgAddressInt,
            jettonWalletCode: Cell
        ): AddrStd = jettonWalletAddress(jettonWalletStateInit(ownerAddress, jettonMasterAddress, jettonWalletCode))
    }
}

suspend fun main() {

    val jettonMasterContract =
        JettonMasterContract(liteClient, AddrStd("EQC_1YoM8RBixN95lz7odcF3Vrkc_N8Ne7gQi7Abtlet_Efi"))
    val walletContractAddress =
        jettonMasterContract.getWalletAddress(AddrStd("EQAKtVj024T9MfYaJzU1xnDAkf_GGbHNu-V2mgvyjTuP6rvC"))

//    println(walletContractAddress)
    val jettonWalletContract = JettonWalletContract(liteClient, walletContractAddress)
    println(jettonWalletContract.balance().coins)

    jettonWalletContract.balance()

//
//    val walletContract = JettonWalletContract(TonBlockchainManager.liteClient.getAccountState(walletContractAddress))
//    println(walletContract.jettonBalance)


}
