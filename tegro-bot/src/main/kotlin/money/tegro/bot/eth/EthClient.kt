package money.tegro.bot.eth

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
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
import kotlin.random.Random

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

fun main(): Unit = runBlocking {
    val client = EthClient(endpoint = "https://data-seed-prebsc-1-s1.binance.org:8545")
    val gasFactor = 1.0.toBigDecimal()

    val gasPrice = (client.gasPrice().toBigDecimal() * gasFactor).toBigInteger()
    val key1 = Random(123123).nextBytes(32)
    val key2 = Random(321321).nextBytes(32)
    val address = client.getAddress(key1)
    val address2 = client.getAddress(key2)
    val balance1 = client.getBalance(address)
    val balance2 = client.getBalance(address2)

    val gas = 21000.toBigInteger()
    val fee = gas * gasPrice

    suspend fun send(key: ByteArray, dest: String, balance: BigInteger) {
//        val value = balance - fee - 1_000_000_000.toBigInteger()
        val value = balance - fee - (1_000_000_000_000_000.toBigInteger() - 0.toBigInteger())
        //206933999999999980
        //206723999999999979
        // 206933999999999980-206723999999999979
        println("transfer: ${client.getAddress(key)} -> $dest")
        println("balance:        $balance")
        println("price:          $gasPrice")
        println("gas:            $gas")
        println("fee:            $fee")
        println("value:          $value")
        val b = gas * gasPrice + value
        println("gas*price+value=$b")
        check(balance - b >= 0.toBigInteger())
        // check(balance - (toSend + gas) == 0.toBigInteger())
//        client.sendTransaction(
//            privateKey = key,
//            gasPrice = gasPrice,
//            gasLimit = BigInteger.valueOf(50000),
//            destination = dest,
//            value = value
//        )
    }

    if (balance1 > balance2) {
        send(key1, address2, balance1)
    } else {
        send(key2, address, balance2)
    }
}
