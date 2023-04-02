package money.tegro.bot.wallet

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import money.tegro.bot.blockchain.BlockchainManager
import money.tegro.bot.masterKey
import money.tegro.bot.objects.PostgresUserPersistent
import money.tegro.bot.objects.User
import money.tegro.bot.ton.TonBlockchainManager
import money.tegro.bot.utils.UserPrivateKey
import money.tegro.bot.walletPersistent
import java.math.BigInteger
import java.util.*
import java.util.concurrent.TimeUnit

object WalletObserver {
    @OptIn(DelicateCoroutinesApi::class)
    private val cache = Caffeine.newBuilder()
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

    suspend fun checkDeposit(user: User): List<Coins> {
        val coins = cache.get(user.id).await().filter { it.amount > BigInteger.ZERO }
        return if (cache.asMap().remove(user.id) != null) {
            coins
        } else {
            emptyList()
        }
    }

    private suspend fun checkForNewDeposits(
        user: User,
        blockchainManager: BlockchainManager,
        cryptoCurrency: CryptoCurrency
    ): Coins {
        val userWalletPk = UserPrivateKey(user.id, masterKey).key.toByteArray()
        val userWalletAddress = TonBlockchainManager.getAddress(userWalletPk)
        val balance = blockchainManager.getTokenBalance(cryptoCurrency, userWalletAddress)
        val reserve = cryptoCurrency.networkFeeReserve
        if (balance.amount > reserve) {
            println("Нашли у $user ($userWalletAddress) денег на контракте: $balance")
            val depositCoins = balance - reserve
            val masterWalletPk = UserPrivateKey(UUID(0, 0), masterKey).key.toByteArray()
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