package money.tegro.bot.bnb

import io.ktor.client.*
import io.ktor.client.engine.cio.*

private val httpClient = HttpClient(CIO)

//@OptIn(ExperimentalTime::class)
//fun main() {
//
//    repeat(1) {
//        runBlocking {
//            measureTime {
//                GlobalScope.launch {
//                    BnbUtils.test()
//                }.join()
//            }.let {
//                println("time: $it")
//            }
//        }
//    }
//}

//object BnbUtils {
//    private val binanceManager = BinanceManager(BinanceManager.Network.TESTNET)
//
//    suspend fun test() = coroutineScope {
//        val privateKey = PrivateKeyEd25519(Random(123123).nextBytes(32))
//        val privateKey2 = PrivateKeyEd25519(Random(321321).nextBytes(32))
//        val address = getAddress(privateKey)
//        val address2 = getAddress(privateKey2)
//        println("address: $address")
//        println("address2: $address2")
//        val gasPrice = (getGasPrice().toBigDecimal() * BigDecimal("1.01")).toBigInteger()
//        println("gas price: $gasPrice")
//
//        val b1 = launch {
//            println("start 1")
//            println("balance: ${getBnbBalance(address)}")
//        }
//        val b2 = launch {
//            println("start 2")
//            println("balance2: ${getBnbBalance(address2)}")
//        }
//
//        b1.join()
//        b2.join()
//
//        sendBnb(privateKey, gasPrice, BigDecimal("0.000001"), address2)
//    }
//    // 10000000000
//    //
//
//    fun getAddress(privateKey: PrivateKeyEd25519): String {
//        val credentials = binanceManager.getCredentials(privateKey.key.toByteArray())
//        return credentials.address
//    }
//
//    suspend fun getBnbBalance(address: String): BigDecimal = suspendCoroutine { continuation ->
//        binanceManager.getBNBBalance(address).subscribe { result, error ->
//            if (result != null) {
//                continuation.resume(result)
//            } else {
//                continuation.resumeWithException(error)
//            }
//        }
//    }
//
//    suspend fun getGasPrice(): BigInteger = suspendCoroutine { continuation ->
//        continuation.resume(binanceManager.gasPrice)
//    }
//
//    suspend fun sendBnb(privateKey: PrivateKeyEd25519, gasPrice: BigInteger, coins: BigDecimal, address: String) =
//        suspendCoroutine { continuation ->
//            val credentials = binanceManager.getCredentials(privateKey.key.toByteArray())
//
//            binanceManager.sendBNB(
//                credentials,
//                gasPrice,
//                BigInteger.valueOf(25_000_000),
//                coins,
//                address
//            ).subscribe { result, error ->
//                if (result != null) {
//                    continuation.resume(result)
//                } else {
//                    continuation.resumeWithException(error)
//                }
//            }
//        }
//}
