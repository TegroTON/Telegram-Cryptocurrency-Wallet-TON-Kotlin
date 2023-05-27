package money.tegro.bot.utils

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*
import money.tegro.bot.objects.Nft
import money.tegro.bot.objects.NftCollection
import money.tegro.bot.objects.PostgresUserPersistent
import money.tegro.bot.objects.User
import money.tegro.bot.wallet.CryptoCurrency
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction
import org.ton.block.AddrStd
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import kotlin.time.Duration.Companion.days

interface NftsPersistent {

    suspend fun getNftsByUserAddress(user: User, address: String): List<Nft>

    suspend fun getNftsByUser(user: User): List<Nft>

    companion object {
        fun getUserProfitStacking(user: User): BigDecimal {
            val perNftPercent = 0.56
            val nftCount = user.settings.nfts.size
            if (nftCount > 10) return (10 * perNftPercent).toBigDecimal()
            return (nftCount * perNftPercent).toBigDecimal()
        }

        fun countStackingPercent(user: User, basicPercent: BigDecimal): BigDecimal {
            if (user.settings.nfts.isEmpty()) return basicPercent
            return basicPercent + getUserProfitStacking(user)
        }

        fun getUserProfitBotFee(user: User): BigInteger {
            val perNftPercent = 5
            val nftCount = user.settings.nfts.size
            if (nftCount > 10) return (10 * perNftPercent).toBigInteger()
            return (nftCount * perNftPercent).toBigInteger()
        }

        fun countBotFee(user: User, currency: CryptoCurrency): BigInteger {
            if (user.settings.nfts.isEmpty()) return currency.botFee
            return currency.botFee - ((currency.botFee / 100.toBigInteger()) * getUserProfitBotFee(user))
        }
    }

}

object PostgresNftsPersistent : NftsPersistent {

    val endpoint = "https://tonapi.io/v2"

    val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                encodeDefaults = true
                isLenient = true
            })
        }
    }

    object UsersNfts : UUIDTable("users_nfts") {
        val name = text("name")
        val address = text("address")
        val ownerId = uuid("owner_id").references(PostgresUserPersistent.Users.id)
        val ownerAddress = text("owner_address")
        val imageLink = text("image_link")
        val collection = enumeration<NftCollection>("collection")
        val updateTime = timestamp("update_time").default(Clock.System.now())

        init {
            transaction { SchemaUtils.create(this@UsersNfts) }
        }
    }

    private suspend fun makeRequestNft(address: String): String {
        return httpClient.get("$endpoint/accounts/$address/nfts?limit=1000&offset=0&indirect_ownership=false") {
            contentType(ContentType.Application.Json)
        }.body<String>()
    }

    override suspend fun getNftsByUserAddress(user: User, address: String): List<Nft> {
        return getNftsByUserId(user.id, address, false)
    }

    private suspend fun getNftsByUserId(userId: UUID, address: String, force: Boolean): List<Nft> {
        if (!force) {
            val updateTime = suspendedTransactionAsync {
                val result = UsersNfts.select {
                    UsersNfts.ownerId.eq(userId)
                }.firstOrNull() ?: return@suspendedTransactionAsync null
                result[UsersNfts.updateTime]
            }.await()
            if (updateTime != null && updateTime.plus(7.days) > Clock.System.now()) {
                val nfts = suspendedTransactionAsync {
                    UsersNfts.select {
                        UsersNfts.ownerId.eq(userId)
                    }.mapNotNull {
                        Nft(
                            it[UsersNfts.id].value,
                            it[UsersNfts.name],
                            it[UsersNfts.address],
                            it[UsersNfts.ownerId],
                            it[UsersNfts.ownerAddress],
                            it[UsersNfts.imageLink],
                            it[UsersNfts.collection]
                        )
                    }
                }
                return nfts.await()
            }
        }
        val response = makeRequestNft(address)

        val nfts = parseJson(response, userId, address)
        clearNfts(userId)
        for (nft: Nft in nfts) {
            transaction {
                UsersNfts.insert {
                    it[UsersNfts.id] = nft.id
                    it[name] = nft.name
                    it[UsersNfts.address] = nft.address
                    it[ownerId] = nft.ownerId
                    it[ownerAddress] = nft.ownerAddress
                    it[imageLink] = nft.imageLink
                    it[collection] = nft.collection
                }
            }
        }
        return nfts
    }

    override suspend fun getNftsByUser(user: User): List<Nft> {
        return getNftsByUser(user, false)
    }

    suspend fun getNftsByUser(user: User, force: Boolean): List<Nft> {
        if (!force && user.settings.nfts.isNotEmpty()) return user.settings.nfts
        if (user.settings.address == "") return emptyList()
        return getNftsByUserId(user.id, user.settings.address, force)
    }

    private fun clearNfts(userId: UUID) {
        transaction {
            UsersNfts.deleteWhere {
                ownerId.eq(userId)
            }
        }
    }

    suspend fun checkTransactions(address: String, verifyCode: String): Boolean {
        val masterAddress = "EQA1Mg34Zy5nLWfXHocsuuZo911Wi5faf-iGoM-_A8X-9z0e"
        val response =
            httpClient.get("$endpoint/blockchain/accounts/$address/transactions?before_lt=0&limit=5") {
                contentType(ContentType.Application.Json)
            }.body<String>()
        val json = Json.parseToJsonElement(response)
        val array = json.jsonObject["transactions"]?.jsonArray ?: return false

        for (element: JsonElement in array) {
            val entry = element.jsonObject
            val isSuccess = entry["success"]?.jsonPrimitive?.boolean ?: return false
            val outMsgs = entry["out_msgs"]?.jsonArray ?: return false
            if (outMsgs.isEmpty()) continue
            val outMsg = outMsgs[0].jsonObject
            val value = outMsg["value"]?.jsonPrimitive?.long?.toBigInteger() ?: return false
            val toAddressRaw = outMsg["destination"]?.jsonObject?.get("address")?.jsonPrimitive?.content ?: return false
            val toAddress = AddrStd(toAddressRaw).toString(userFriendly = true)
            val message = outMsg["decoded_body"]?.jsonObject?.get("text")?.jsonPrimitive?.content
            val expectedMessage = "Connect to TegroWalletBot @$verifyCode"
            if (toAddress == masterAddress && value == CryptoCurrency.TON.botFee && message == expectedMessage) {
                return isSuccess
            }
            continue
        }
        return false
    }

    private fun parseJson(rawJson: String, userId: UUID, address: String): List<Nft> {
        val json = Json.parseToJsonElement(rawJson)

        val array = json.jsonObject["nft_items"]?.jsonArray ?: return emptyList()
        val nfts = emptyList<Nft>().toMutableList()
        for (element: JsonElement in array) {
            val item = element.jsonObject
            val metadata = item["metadata"]?.jsonObject ?: continue

            val name = metadata["name"]?.jsonPrimitive?.content ?: continue
            val itemAddress = item["address"]?.jsonPrimitive?.content ?: continue
            val imageLink = metadata["image"]?.jsonPrimitive?.content ?: continue
            val collectionAddress =
                item["collection"]?.jsonObject?.get("address")?.jsonPrimitive?.content ?: continue
            val collection = NftCollection.values().firstOrNull { it.address == collectionAddress } ?: continue

            val nft = Nft(
                UUID.randomUUID(),
                name,
                itemAddress,
                userId,
                address,
                imageLink,
                collection
            )
            nfts.add(nft)
        }
        return nfts
    }
}