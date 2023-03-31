package money.tegro.bot.eth

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.utils.Numeric
import java.math.BigInteger

class EthClient(
    val endpoint: String,
) {
    val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                encodeDefaults = true
                isLenient = true
            })
        }
    }

    suspend fun gasPrice(): BigInteger {
        val response = httpClient.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(EthRequest(method = "eth_gasPrice", params = emptyList<String>()))
        }.body<EthResponse<String>>()
        val result = response.result ?: throw RuntimeException(response.error?.message)
        return Numeric.decodeQuantity(result)
    }

    fun getAddress(privateKey: ByteArray): String {
        val credentials = Credentials.create(Numeric.toHexString(privateKey))
        return credentials.address
    }

    suspend fun estimateGas(
        from: String,
        to: String? = null,
        gasPrice: BigInteger? = null,
        value: BigInteger? = null,
        block: DefaultBlockParameterName = DefaultBlockParameterName.LATEST,
    ): BigInteger {
        val transaction = EthTransaction(
            from = from,
            to = to,
            gasPrice = gasPrice?.let { Numeric.encodeQuantity(it) },
            value = value?.let { Numeric.encodeQuantity(it) }
        )
        val response = httpClient.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(
                EthRequest(
                    method = "eth_estimateGas", params = listOf(
                        Json.encodeToJsonElement(transaction),
                        JsonPrimitive(block.value)
                    )
                )
            )
        }.body<EthResponse<String>>()
        val result = response.result ?: throw RuntimeException(response.error?.message)
        return Numeric.decodeQuantity(result)
    }

    suspend fun getBalance(
        address: String,
        blockParameter: DefaultBlockParameterName = DefaultBlockParameterName.LATEST
    ): BigInteger {
        val response = httpClient.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(
                EthRequest(
                    method = "eth_getBalance",
                    params = listOf(
                        address,
                        blockParameter.value
                    )
                )
            )
        }.body<EthResponse<String>>()
        val result = response.result ?: throw RuntimeException(response.error?.message)
        return Numeric.decodeQuantity(result)
    }

    suspend fun getTokenBalance(
        tokenAddress: String,
        ownerAddress: String,
        block: DefaultBlockParameterName = DefaultBlockParameterName.LATEST
    ): BigInteger {
        val address = Address(ownerAddress)
        val function = Function(
            "balanceOf",
            listOf(address),
            listOf<TypeReference<*>>(object : TypeReference<Uint256>() {})
        )
        val encodedFunction = FunctionEncoder.encode(function)

        val transaction = EthTransaction(
            from = ownerAddress,
            to = tokenAddress,
            data = encodedFunction
        )

        val response = httpClient.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(
                EthRequest(
                    method = "eth_call",
                    params = listOf(
                        Json.encodeToJsonElement(transaction),
                        JsonPrimitive(block.value)
                    )
                )
            )
        }.body<EthResponse<String>>()
        val result = response.result ?: throw RuntimeException(response.error?.message)
        return Numeric.decodeQuantity(result)
    }

    suspend fun transfer(
        privateKey: ByteArray,
        tokenAddress: String,
        toAddress: String,
        amount: BigInteger,
        block: DefaultBlockParameterName = DefaultBlockParameterName.LATEST
    ): String {
        val function = Function(
            "transfer",
            listOf(Address(toAddress), Uint256(amount)),
            emptyList()
        )
        val encodedFunction = FunctionEncoder.encode(function)

        val transaction = EthTransaction(
            from = getAddress(privateKey),
            to = tokenAddress,
            data = encodedFunction
        )

        val response = httpClient.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(
                EthRequest(
                    method = "eth_call",
                    params = listOf(
                        Json.encodeToJsonElement(transaction),
                        JsonPrimitive(block.value)
                    )
                )
            )
        }.body<EthResponse<String>>()
        return response.result ?: throw RuntimeException(response.error?.message)
    }

    suspend fun getTransactionCount(
        address: String,
        blockParameter: DefaultBlockParameterName = DefaultBlockParameterName.PENDING,
    ): BigInteger {
        val response = httpClient.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(
                EthRequest(
                    method = "eth_getTransactionCount",
                    params = listOf(
                        address,
                        blockParameter.value
                    )
                )
            )
        }.body<EthResponse<String>>()
        val result = response.result ?: throw RuntimeException(response.error?.message)
        return Numeric.decodeQuantity(result)
    }

    suspend fun sendTransaction(
        privateKey: ByteArray,
        gasPrice: BigInteger,
        gasLimit: BigInteger = BigInteger.valueOf(21_000),
        destination: String,
        value: BigInteger
    ) {
        val nonce = getTransactionCount(getAddress(privateKey))
        sendTransaction(privateKey, nonce, gasPrice, gasLimit, destination, value)
    }

    suspend fun sendTransaction(
        privateKey: ByteArray,
        nonce: BigInteger,
        gasPrice: BigInteger,
        gasLimit: BigInteger,
        destination: String,
        value: BigInteger
    ) {
        val credentials = Credentials.create(Numeric.toHexString(privateKey))
        val rawTransaction = RawTransaction.createEtherTransaction(
            nonce, gasPrice, gasLimit, destination, value
        )
        val signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials)
        sendRawTransaction(signedMessage)
    }

    suspend fun sendRawTransaction(
        signedMessage: ByteArray
    ): ByteArray {
        val hexValue = Numeric.toHexString(signedMessage)
        val response = httpClient.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(
                EthRequest(
                    method = "eth_sendRawTransaction",
                    params = listOf(hexValue)
                )
            )
        }.body<EthResponse<String>>()
        val result = response.result ?: throw RuntimeException(response.error?.message)
        return Numeric.hexStringToByteArray(result)
    }
}

//fun main(): Unit = runBlocking {
//    val client = EthClient(endpoint = "https://data-seed-prebsc-1-s1.binance.org:8545")
//    val gasPrice = (client.gasPrice().toBigDecimal() * 1.1.toBigDecimal()).toBigInteger()
//    val key1 = Random(123123).nextBytes(32)
//    val key2 = Random(321321).nextBytes(32)
//    val address = client.getAddress(key1)
//    val address2 = client.getAddress(key2)
//
//    println("gas=${gasPrice * 2001212.toBigInteger()}")
//    println("bal=" + client.getBalance(address))
//
//    println(gasPrice)
//    println(address)
//    println(address2)
//
//    val balance = client.getTokenBalance(
//        tokenAddress = "0x337610d27c682E347C9cD60BD4b3b107C9d34dDd",
//        ownerAddress = address
//    )
//    println(balance)
//
////    val estimateGas = client.estimateGas(
////        from = address,
////        to = address2,
////        gasPrice = gasPrice,
////        value = BigInteger.valueOf(1)
////    )
////    val r = estimateGas * gasPrice + BigInteger.valueOf(1)
////    println("estimate gas = $estimateGas")
////    println("res=$r")
//    client.sendTransaction(
//        privateKey = key1,
//        gasPrice = gasPrice,
//        gasLimit = BigInteger.valueOf(50000),
//        destination = address2,
//        value = BigInteger.valueOf(1)
//    )
//    val result = client.transfer(
//        privateKey = key2,
//        toAddress = address,
//        amount = Uint256(BigInteger.valueOf(1))
//    )
//    println(result)
//}