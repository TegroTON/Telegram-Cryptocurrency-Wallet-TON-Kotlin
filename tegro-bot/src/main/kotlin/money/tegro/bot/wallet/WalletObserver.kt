package money.tegro.bot.wallet

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import money.tegro.bot.MASTER_KEY
import money.tegro.bot.blockchain.BlockchainManager
import money.tegro.bot.objects.PostgresUserPersistent
import money.tegro.bot.objects.User
import money.tegro.bot.ton.TonBlockchainManager
import money.tegro.bot.utils.UserPrivateKey
import money.tegro.bot.walletPersistent
import java.math.BigInteger
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object WalletObserver {
    @OptIn(DelicateCoroutinesApi::class)
    private val nativeCache = Caffeine.newBuilder()
        .expireAfterWrite(5000, TimeUnit.MILLISECONDS)
        .buildAsync<UUID, List<Coins>> { userId, e ->
            GlobalScope.async(e.asCoroutineDispatcher()) {
                println("Start checking: $userId")
                val user = PostgresUserPersistent.load(userId) ?: return@async emptyList()
                listOf(
                    async { checkForNewDeposits(user, TonBlockchainManager, CryptoCurrency.TON) },
                ).awaitAll().also {
                    println("User deposits $user:\n ${it.joinToString("\n ")}")
                }
            }.asCompletableFuture()
        }

    private val tokenDepositFlowCache = ConcurrentHashMap<Pair<UUID, CryptoCurrency>, Job>()

    suspend fun checkDeposit(user: User): List<Coins> {
        val coins = nativeCache.get(user.id).await().filter { it.amount > BigInteger.ZERO }
        return if (nativeCache.asMap().remove(user.id) != null) {
            coins
        } else {
            emptyList()
        }
    }

    suspend fun checkDeposit(user: User, blockchainManager: BlockchainManager, currency: CryptoCurrency): Coins {
        val key = user.id to currency
        val coins = suspendCoroutine { continuation ->
            tokenDepositFlowCache.getOrPut(key) {
                GlobalScope.launch {
                    val flow = TokenDepositFlow(
                        blockchainManager = blockchainManager,
                        currency = currency,
                        userPk = UserPrivateKey(user.id).key.toByteArray()
                    )
                    flow.collect {
                        when (it) {
                            is TokenDepositFlow.Event.Complete -> {
                                continuation.resume(it.amount)
                            }

                            is TokenDepositFlow.Event.NotEnoughDeposit -> {
                                continuation.resume(currency.ZERO)
                            }

                            is TokenDepositFlow.Event.NotEnoughCoinsOnMasterContract -> {
                                continuation.resumeWithException(RuntimeException("Not enough $currency on master contract"))
                            }

                            else -> {

                            }
                        }
                    }
                }
            }
        }
        tokenDepositFlowCache[key]?.join()?.also {
            tokenDepositFlowCache.remove(key)
        }
        return coins
    }

    private suspend fun checkForNewDeposits(
        user: User,
        blockchainManager: BlockchainManager,
        cryptoCurrency: CryptoCurrency
    ): Coins {
        val userWalletPk = UserPrivateKey(user.id, MASTER_KEY).key.toByteArray()
        val userWalletAddress = TonBlockchainManager.getAddress(userWalletPk)
        val balance = blockchainManager.getTokenBalance(cryptoCurrency, userWalletAddress)
        val reserve = cryptoCurrency.networkFeeReserve
        if (balance.amount > reserve) {
            println("Нашли у $user ($userWalletAddress) денег на контракте: $balance")
            val depositCoins = balance - reserve
            val masterWalletPk = UserPrivateKey(UUID(0, 0), MASTER_KEY).key.toByteArray()
            val masterWalletAddress = blockchainManager.getAddress(masterWalletPk)
            if (cryptoCurrency.isNative) {
                println("transfer $depositCoins | $userWalletAddress -> $masterWalletAddress")
                blockchainManager.transfer(
                    privateKey = userWalletPk,
                    destinationAddress = masterWalletAddress,
                    value = depositCoins
                )
                walletPersistent.updateActive(user, depositCoins.currency) { oldCoins ->
                    (oldCoins + depositCoins).also { newCoins ->
                        println(
                            "New deposit: $user\n" +
                                    "     old coins: $oldCoins\n" +
                                    " deposit coins: $depositCoins\n" +
                                    "     new coins: $newCoins"
                        )
                    }
                }
            } else {
                TODO()
//                val nativeBalance = blockchainManager.getBalance(
//                    blockchainManager.getAddress(userWalletPk)
//                )
//                if (nativeBalance.amount < nativeBalance.currency.networkFeeReserve) {
//                    val additionalDeposit = nativeBalance.currency.networkFeeReserve - nativeBalance.amount
//                    blockchainManager.transfer(
//                        masterWalletPk,
//                        userWalletAddress,
//                        Coins(nativeBalance.currency, additionalDeposit)
//                    )
//                }
//                blockchainManager.transferToken(
//                    privateKey = userWalletPk,
//                    cryptoCurrency = cryptoCurrency,
//                    destinationAddress = masterWalletAddress,
//                    value = depositCoins
//                )
            }
            return depositCoins
        } else {
            return Coins(cryptoCurrency, 0.toBigInteger())
        }
    }
}
