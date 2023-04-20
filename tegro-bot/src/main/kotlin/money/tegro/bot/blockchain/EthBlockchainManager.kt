package money.tegro.bot.blockchain

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
import money.tegro.bot.eth.EthRequest
import money.tegro.bot.eth.EthResponse
import money.tegro.bot.eth.EthTransaction
import money.tegro.bot.wallet.BlockchainType
import money.tegro.bot.wallet.Coins
import money.tegro.bot.wallet.CryptoCurrency
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

object EthBlockchainManager : BlockchainManager {
    val endpoint = "https://data-seed-prebsc-1-s1.binance.org:8545"

    val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                encodeDefaults = true
                isLenient = true
            })
        }
    }

    override val type: BlockchainType
        get() = BlockchainType.BSC

    override fun getAddress(privateKey: ByteArray): String {
        val credentials = Credentials.create(Numeric.toHexString(privateKey))
        return credentials.address
    }

    override suspend fun getBalance(address: String): Coins {
        val response = httpClient.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(
                EthRequest(
                    method = "eth_getBalance",
                    params = listOf(
                        address,
                        DefaultBlockParameterName.LATEST.value
                    )
                )
            )
        }.body<EthResponse<String>>()
        val result = response.result ?: throw RuntimeException(response.error?.message)
        println("coins found manager: ${Numeric.decodeQuantity(result)}")
        return Coins(CryptoCurrency.BNB, Numeric.decodeQuantity(result))
    }

    override suspend fun getTokenBalance(cryptoCurrency: CryptoCurrency, ownerAddress: String): Coins {
        if (cryptoCurrency.nativeBlockchainType == type) return getBalance(ownerAddress)
        val tokenAddress = requireNotNull(cryptoCurrency.getTokenContractAddress(type)) {
            "$cryptoCurrency not support $type"
        }
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
                        JsonPrimitive(DefaultBlockParameterName.LATEST.value)
                    )
                )
            )
        }.body<EthResponse<String>>()
        val result = response.result ?: throw RuntimeException(response.error?.message)
        println("coins found manager: ${Numeric.decodeQuantity(result)}")
        return Coins(cryptoCurrency, Numeric.decodeQuantity(result))
    }

    suspend fun gasPrice(): BigInteger {
        val response = httpClient.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(EthRequest(method = "eth_gasPrice", params = emptyList<String>()))
        }.body<EthResponse<String>>()
        val result = response.result ?: throw RuntimeException(response.error?.message)
        return Numeric.decodeQuantity(result)
    }

    override suspend fun transfer(privateKey: ByteArray, destinationAddress: String, value: Coins) {
        val gasFactor = 1.0.toBigDecimal()
        val gasPrice = (gasPrice().toBigDecimal() * gasFactor).toBigInteger()
        val gas = 21000.toBigInteger()
        val fee = gas * gasPrice
        val toSend = value.amount - fee - (1_000_000_000_000_000.toBigInteger() - 0.toBigInteger())
        sendTransaction(
            privateKey = privateKey,
            gasPrice = gasPrice,
            gasLimit = BigInteger.valueOf(50000),
            destination = destinationAddress,
            value = toSend
        )
    }

    override suspend fun transferToken(
        privateKey: ByteArray,
        cryptoCurrency: CryptoCurrency,
        destinationAddress: String,
        value: Coins
    ) {
        val tokenAddress = requireNotNull(cryptoCurrency.getTokenContractAddress(type)) {
            "$cryptoCurrency not support $type"
        }
        val function = Function(
            "transfer",
            listOf(Address(destinationAddress), Uint256(value.amount)),
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
                        JsonPrimitive(DefaultBlockParameterName.LATEST.value)
                    )
                )
            )
        }.body<EthResponse<String>>()
        if (response.result == null) throw RuntimeException(response.error?.message)
    }

    override fun isValidAddress(address: String?): Boolean {
        return true
        //TODO("Not yet implemented")
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