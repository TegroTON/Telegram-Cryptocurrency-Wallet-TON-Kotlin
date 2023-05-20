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
import kotlin.time.Duration.Companion.days

interface NftsPersistent {

    suspend fun getNftsByUserAddress(user: User, address: String): List<Nft>

    suspend fun getNftsByUser(user: User): List<Nft>

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

    override suspend fun getNftsByUserAddress(user: User, address: String): List<Nft> {
        val updateTime = suspendedTransactionAsync {
            val result = UsersNfts.select {
                UsersNfts.ownerId.eq(user.id)
            }.firstOrNull() ?: return@suspendedTransactionAsync null
            result[UsersNfts.updateTime]
        }.await()
        if (updateTime != null && updateTime.plus(7.days) > Clock.System.now()) {
            val nfts = suspendedTransactionAsync {
                UsersNfts.select {
                    UsersNfts.ownerId.eq(user.id)
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
        val response =
            httpClient.get("$endpoint/accounts/$address/nfts?limit=1000&offset=0&indirect_ownership=false") {
                contentType(ContentType.Application.Json)
            }.body<String>()

        val nfts = parseJson(response, user, address)
        clearNfts(user)
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
        if (user.settings.address == "") return emptyList()
        return getNftsByUserAddress(user, user.settings.address)
    }

    private fun clearNfts(user: User) {
        transaction {
            UsersNfts.deleteWhere {
                ownerId.eq(user.id)
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
            println(
                "isSuccess: $isSuccess\n" +
                        "value: $value\n" +
                        "toAddress: $toAddress\n" +
                        "message: $message"
            )
            val expectedMessage = "Connect to TegroWalletBot @$verifyCode"
            if (toAddress == masterAddress && value == CryptoCurrency.TON.botFee && message == expectedMessage) {
                return isSuccess
            }
            continue
        }
        return false
    }

    private fun parseJson(rawJson: String, user: User, address: String): List<Nft> {
        val json = Json.parseToJsonElement(rawJson)

        val array = json.jsonObject["nft_items"]?.jsonArray ?: return emptyList()
        val nfts = emptyList<Nft>().toMutableList()
        for (element: JsonElement in array) {
            val item = element.jsonObject
            val metadata = item["metadata"]?.jsonObject ?: return emptyList()

            val name = metadata["name"]?.jsonPrimitive?.content ?: return emptyList()
            val itemAddress = item["address"]?.jsonPrimitive?.content ?: return emptyList()
            val imageLink = metadata["image"]?.jsonPrimitive?.content ?: return emptyList()
            val collectionAddress =
                item["collection"]?.jsonObject?.get("address")?.jsonPrimitive?.content ?: return emptyList()
            val collection = NftCollection.values().firstOrNull { it.address == collectionAddress } ?: continue

            val nft = Nft(
                java.util.UUID.randomUUID(),
                name,
                itemAddress,
                user.id,
                address,
                imageLink,
                collection
            )
            nfts.add(nft)
        }
        return nfts
    }
}