package money.tegro.bot.wallet

import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.FlowCollector
import money.tegro.bot.blockchain.BlockchainManager
import money.tegro.bot.utils.UserPrivateKey
import java.math.BigInteger
import java.util.*

class TokenDepositFlow(
    val blockchainManager: BlockchainManager,
    val currency: CryptoCurrency,
    private val userPk: ByteArray
) : AbstractFlow<TokenDepositFlow.Event>() {
    val userAddress = blockchainManager.getAddress(userPk)
    private val masterPk = UserPrivateKey(UUID(0, 0)).key.toByteArray()
    val masterAddress = blockchainManager.getAddress(masterPk)

    override suspend fun collectSafely(collector: FlowCollector<Event>) {
        collector.emit(Event.Start)

        collector.emit(Event.DepositGetTokenBalance)
        val userTokenBalance = blockchainManager.getTokenBalance(currency, userAddress)
        collector.emit(Event.DepositGotTokenBalance(userTokenBalance))

        if (userTokenBalance.amount <= BigInteger.ZERO) {
            collector.emit(Event.NotEnoughDeposit)
            return
        }

        collector.emit(Event.GetNativeCoinsForGas)
        val userBalance = blockchainManager.getBalance(userAddress)
        collector.emit(Event.GotNativeCoinsForFee(userBalance))

        if (userBalance.amount < userBalance.currency.networkFeeReserve) {
            val gasAmount = Coins(userBalance.currency, userBalance.currency.networkFeeReserve - userBalance.amount)
            collector.emit(Event.RequestForGas(gasAmount))


            val masterBalance = blockchainManager.getBalance(masterAddress)

            if (masterBalance < gasAmount) {
                collector.emit(Event.NotEnoughCoinsOnMasterContract(masterBalance, gasAmount))
                return
            }

            collector.emit(Event.StartTransferFromMasterToUserRequestedGas(gasAmount))
            blockchainManager.transfer(masterPk, userAddress, gasAmount)
            collector.emit(Event.CompleteTransferFromMasterToUserRequestedGas(gasAmount))
        }

        collector.emit(Event.StartTransferTokenFromUserToMaster(userTokenBalance))
        blockchainManager.transferToken(userPk, currency, masterAddress, userTokenBalance)
        collector.emit(Event.CompleteTransferTokenFromUserToMaster(userTokenBalance))

        collector.emit(Event.Complete(userTokenBalance))
    }

    sealed class Event {
        object Start : Event()
        object DepositGetTokenBalance : Event()
        class DepositGotTokenBalance(val result: Coins) : Event()
        object NotEnoughDeposit : Event()
        object GetNativeCoinsForGas : Event()
        class GotNativeCoinsForFee(val result: Coins) : Event()
        class RequestForGas(val gasAmount: Coins) : Event()
        class NotEnoughCoinsOnMasterContract(
            val currentBalance: Coins,
            val requested: Coins
        ) : Event()

        class StartTransferFromMasterToUserRequestedGas(
            val amount: Coins
        ) : Event()

        class CompleteTransferFromMasterToUserRequestedGas(
            val amount: Coins
        ) : Event()

        class StartTransferTokenFromUserToMaster(
            val amount: Coins
        ) : Event()

        class CompleteTransferTokenFromUserToMaster(
            val amount: Coins
        ) : Event()

        class Complete(
            val amount: Coins
        ) : Event()
    }
}
