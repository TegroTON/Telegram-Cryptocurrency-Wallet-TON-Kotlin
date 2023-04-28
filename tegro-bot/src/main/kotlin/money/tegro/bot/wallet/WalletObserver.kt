package money.tegro.bot.wallet

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import money.tegro.bot.MASTER_KEY
import money.tegro.bot.blockchain.BlockchainManager
import money.tegro.bot.blockchain.EthBlockchainManager
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
    val hashMap = emptyMap<User, TokenDepositFlow>().toMutableMap()

    @OptIn(DelicateCoroutinesApi::class)
    private val tonNativeCache = Caffeine.newBuilder().expireAfterWrite(15000, TimeUnit.MILLISECONDS)
        .buildAsync<UUID, List<Coins>> { userId, e ->
            GlobalScope.async(e.asCoroutineDispatcher()) {
//                println("Start checking: $userId")
                val user = PostgresUserPersistent.load(userId) ?: return@async emptyList()
                listOf(
                    async { checkForNewDeposits(user, TonBlockchainManager, CryptoCurrency.TON) },
                ).awaitAll().also {
//                    println("User deposits $user:\n ${it.joinToString("\n ")}")
                }
            }.asCompletableFuture()
        }

    @OptIn(DelicateCoroutinesApi::class)
    private val bnbNativeCache = Caffeine.newBuilder().expireAfterWrite(30000, TimeUnit.MILLISECONDS)
        .buildAsync<UUID, List<Coins>> { userId, e ->
            GlobalScope.async(e.asCoroutineDispatcher()) {
//                println("Start checking: $userId")
                val user = PostgresUserPersistent.load(userId) ?: return@async emptyList()
                listOf(
                    async { checkForNewDeposits(user, EthBlockchainManager, CryptoCurrency.BNB) },
                ).awaitAll().also {
//                    println("User deposits $user:\n ${it.joinToString("\n ")}")
                }
            }.asCompletableFuture()
        }

    private val tokenDepositFlowCache = ConcurrentHashMap<Pair<UUID, CryptoCurrency>, Job>()

    suspend fun checkDeposit(user: User): List<Coins> {
        val coins = tonNativeCache.get(user.id).await().filter { it.amount > BigInteger.ZERO }
        return if (tonNativeCache.asMap().remove(user.id) != null) {
            coins
        } else {
            emptyList()
        }
    }

    suspend fun checkDepositBnb(user: User): List<Coins> {
        val coins = bnbNativeCache.get(user.id).await().filter { it.amount > BigInteger.ZERO }
        return if (bnbNativeCache.asMap().remove(user.id) != null) {
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
                                continuation.resume(Coins(currency, 0.toBigInteger()))
                            }

                            is TokenDepositFlow.Event.NotEnoughCoinsOnMasterContract -> {
                                continuation.resumeWithException(RuntimeException("Not enough $currency on master contract, current ${it.currentBalance}, requested ${it.requested}"))
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

    suspend fun checkForNewDeposits(
        user: User,
        blockchainManager: BlockchainManager,
        cryptoCurrency: CryptoCurrency
    ): Coins {
        val userWalletPk = UserPrivateKey(user.id, MASTER_KEY).key.toByteArray()
        val userWalletAddress = blockchainManager.getAddress(userWalletPk)
        val balance = blockchainManager.getTokenBalance(cryptoCurrency, userWalletAddress)
        //println("balance $balance : ${balance.amount}")
        val reserve = cryptoCurrency.networkFeeReserve
        //println("reserve ${Coins(cryptoCurrency, reserve)} : $reserve")
        if (balance.amount > reserve) {
            println("Нашли у $user ($userWalletAddress) денег на контракте: $balance")
            val depositCoins = balance - reserve
            //println("balance - reserve $depositCoins : ${depositCoins.amount}")
            val masterWalletPk = UserPrivateKey(UUID(0, 0), MASTER_KEY).key.toByteArray()
            val masterWalletAddress = blockchainManager.getAddress(masterWalletPk)
            if (cryptoCurrency.isNative) {
                println("transfer native $depositCoins | $userWalletAddress -> $masterWalletAddress")
                blockchainManager.transfer(
                    privateKey = userWalletPk, destinationAddress = masterWalletAddress, value = depositCoins
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
                return depositCoins
            } else {
                val a = TokenDepositFlow(blockchainManager, cryptoCurrency, userWalletPk)
                if (!hashMap.contains(user)) {
                    hashMap[user] = a
                    a.collect {
                        when (it) {
                            is TokenDepositFlow.Event.Complete -> {
                                hashMap.remove(user)
                                println("Deposit flow ${it.amount} : ${it.amount.amount}")
                                it.amount
                            }

                            is TokenDepositFlow.Event.NotEnoughCoinsOnMasterContract -> {
                                println("Not enough coins on master contract, current ${it.currentBalance}, requested ${it.requested}")
                            }

                            else -> {

                            }
                        }
                    }
                    val nativeBalance = blockchainManager.getBalance(
                        blockchainManager.getAddress(userWalletPk)
                    )
                    println("native bal $nativeBalance : ${nativeBalance.amount}")
                    println("reserve ${Coins(cryptoCurrency, reserve)} : $reserve")
                    if (nativeBalance.amount < nativeBalance.currency.networkFeeReserve) {
                        val additionalDeposit = Coins(
                            nativeBalance.currency,
                            nativeBalance.currency.networkFeeReserve - nativeBalance.amount
                        )
                        println("additional deposit $additionalDeposit : ${additionalDeposit.amount}")
                        blockchainManager.transfer(
                            masterWalletPk, userWalletAddress, additionalDeposit
                        )
                    }
                    println("transfer $depositCoins | $userWalletAddress -> $masterWalletAddress")
                    blockchainManager.transferToken(
                        privateKey = userWalletPk,
                        cryptoCurrency = cryptoCurrency,
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
                    return depositCoins
                }
            }
            return Coins(cryptoCurrency, 0.toBigInteger())
        } else {
            return Coins(cryptoCurrency, 0.toBigInteger())
        }
    }
}
