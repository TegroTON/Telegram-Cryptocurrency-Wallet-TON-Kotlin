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
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
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

    val web3j = Web3j.build(HttpService("https://bsc-dataseed.binance.org/"))

    fun gasPrice(): BigInteger {
        return web3j.ethGasPrice().sendAsync().get().gasPrice
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

    fun getBalance(
        address: String,
        block: DefaultBlockParameterName = DefaultBlockParameterName.LATEST
    ): BigInteger {
        return web3j.ethGetBalance(address, block).sendAsync().get().balance
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

    fun transfer(
        privateKey: ByteArray,
        tokenAddress: String,
        toAddress: String,
        amount: BigInteger,
        block: DefaultBlockParameterName = DefaultBlockParameterName.LATEST
    ): String {
        val credentials = Credentials.create(Numeric.toHexString(privateKey))
        val gasFactor = 1.0.toBigDecimal()
        val gasPrice = (gasPrice().toBigDecimal() * gasFactor).toBigInteger()
        val gas = 21000.toBigInteger()
        val fee = gas * gasPrice


        val function = Function("transfer", listOf<Type<*>>(Address(toAddress), Uint256(amount)), emptyList())

        val rawTransaction = RawTransaction.createTransaction(
            null, gasPrice, fee, tokenAddress,
            BigInteger.ZERO, FunctionEncoder.encode(function)
        )

        val hexValue = Numeric.toHexString(TransactionEncoder.signMessage(rawTransaction, credentials))

        val response = web3j.ethSendRawTransaction(hexValue).send()
        println(response.result)
        return response.result


//        val credentials = Credentials.create(Numeric.toHexString(privateKey))
//        val contract = Bep20Contract(tokenAddress, web3j, credentials)
//        val receipt = contract.transfer(Address(toAddress), Uint256(amount))
//        println(receipt.toString())
//        return receipt.status
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
    val address1 = client.getAddress(key1)
    val address2 = client.getAddress(key2)
    val balance1 = client.getBalance(address1)
    val balance2 = client.getBalance(address2)
    val tokenAddress = "0x337610d27c682E347C9cD60BD4b3b107C9d34dDd"
    val tokenBalance1 = client.getTokenBalance(tokenAddress, address1)
    val tokenBalance2 = client.getTokenBalance(tokenAddress, address2)
    println(address1)
    println(address2)
    println(balance1)
    println(balance2)
    println(tokenBalance1)
    println(tokenBalance2)

    val gas = 21000.toBigInteger()
    val fee = gas * gasPrice

    fun send(key: ByteArray, dest: String, balance: BigInteger) {
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
        val result = client.transfer(
            privateKey = key,
            tokenAddress = tokenAddress,
            toAddress = dest,
            amount = balance / 2.toBigInteger()
        )
        println(result)
    }

    if (tokenBalance1 > tokenBalance2) {
        send(key1, address2, tokenBalance1)
    } else {
        send(key2, address1, tokenBalance2)
    }

//    if (balance1 > balance2) {
//        send(key1, address2, balance1)
//    } else {
//        send(key2, address1, balance2)
//    }
}
