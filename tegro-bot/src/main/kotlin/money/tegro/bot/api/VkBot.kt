package money.tegro.bot.api

import com.petersamokhin.vksdk.core.api.botslongpoll.VkBotsLongPollApi
import com.petersamokhin.vksdk.core.client.VkApiClient
import com.petersamokhin.vksdk.core.http.paramsOf
import com.petersamokhin.vksdk.core.io.FileOnDisk
import com.petersamokhin.vksdk.core.model.VkSettings
import com.petersamokhin.vksdk.core.model.event.MessagePartial
import com.petersamokhin.vksdk.http.VkOkHttpClient
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Contextual
import money.tegro.bot.inlines.MainMenu
import money.tegro.bot.menuPersistent
import money.tegro.bot.objects.*
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.receipts.PostgresReceiptPersistent
import money.tegro.bot.utils.PostgresDepositsPersistent
import money.tegro.bot.utils.SecurityPersistent
import money.tegro.bot.wallet.WalletObserver
import java.io.File
import java.io.InputStream
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.hours

class VkBot : Bot, CoroutineScope {

    private val job = SupervisorJob()

    override val coroutineContext: CoroutineContext = Dispatchers.Default + job

    @Contextual
    lateinit var client: VkApiClient

    @Suppress("OPT_IN_USAGE")
    fun start(clientId: String, accessToken: String) {
        val httpClient = VkOkHttpClient()

        val settings = VkSettings(
            httpClient,
            defaultParams = paramsOf("lang" to "en")
        )

        client = VkApiClient(clientId.toInt(), accessToken, VkApiClient.Type.Community, settings)

        client.onMessage { messageNew ->
            launch {
                val message = messageNew.message
                val ref = getRefId(message.ref)
                val randomUUID = UUID.nameUUIDFromBytes("vk_${message.peerId}".toByteArray())
                val user =
                    PostgresUserPersistent.loadByVk(message.peerId.toLong()) ?: PostgresUserPersistent.save(
                        User(
                            randomUUID, null, message.peerId.toLong(), UserSettings(
                                randomUUID,
                                Language.RU,
                                LocalCurrency.RUB,
                                ref
                            )
                        )
                    )
                val menu = menuPersistent.loadMenu(user)
                val fwdMessages = emptyList<BotMessage>().toMutableList()
                for (fwdMessage: MessagePartial in message.fwdMessages) {
                    val fwdBotMessage = BotMessage(
                        fwdMessage.id.toLong(),
                        fwdMessage.fromId.toLong(),
                        fwdMessage.peerId.toLong(),
                        false,
                        fwdMessage.text,
                        null,
                        null,
                        emptyList(),
                        emptyMap()
                    )
                    fwdMessages.add(fwdBotMessage)
                }
                val body = if (message.ref != null) "/start " + message.ref else message.text
                val botMessage = BotMessage(
                    message.id.toLong(),
                    message.fromId.toLong(),
                    message.peerId.toLong(),
                    message.isFromChat(),
                    body,
                    message.payload,
                    null,
                    fwdMessages,
                    emptyMap()
                )
                GlobalScope.launch {
                    repeat(6) {
                        WalletObserver.checkDeposit(user).forEach { coins ->
                            sendMessage(botMessage.peerId, Messages[user].walletMenuDepositMessage.format(coins))
                            SecurityPersistent.log(user, coins, "$coins", LogType.DEPOSIT)
                        }
                        delay(15_000)
                    }
                }
                if (message.text.startsWith("/")) {
                    println("Got command from ${user.id}: ${message.text}")
                    Commands.execute(user, botMessage, this@VkBot, menu, null)
                    return@launch
                }
                try {
                    println("Open menu $menu for user ${user.id}")
                    if (menu?.handleMessage(this@VkBot, botMessage) == true) {
                        return@launch
                    }
                } catch (e: Throwable) {
                    user.setMenu(this@VkBot, MainMenu(user), botMessage)
                    throw RuntimeException("Failed handle message for user $user in menu: $menu", e)
                }
                println("Open main menu for user ${user.id}")
                user.setMenu(this@VkBot, MainMenu(user), botMessage)
            }
        }

        launch {
            while (true) {
                val result = PostgresDepositsPersistent.check()
                if (result.isNotEmpty()) {
                    for (deposit: Deposit in result) {
                        if (deposit.issuer.vkId == null) continue
                        if (deposit.finishDate <= Clock.System.now()) {
                            PostgresDepositsPersistent.payDeposit(deposit, this@VkBot)
                        }
                    }
                }
                delay(1.hours)
            }
        }

        runBlocking { client.startLongPolling(settings = VkBotsLongPollApi.Settings(maxFails = -1)) }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun sendMessage(to: Long, message: String) {
        GlobalScope.launch {
            client.sendMessage {
                peerId = to.toInt()
                this.message = message
            }.execute()
        }
    }

    override suspend fun sendMessageKeyboard(to: Long, message: String, keyboard: BotKeyboard) {
        val vkKeyboard = keyboard.toVk()
        client.sendMessage {
            peerId = to.toInt()
            this.message = message
            this.keyboard = vkKeyboard
        }.execute()
    }

    override suspend fun updateKeyboard(to: Long, lastMenuMessageId: Long?, message: String, keyboard: BotKeyboard) {
        val deleteRequest =
            client.call("messages.delete", paramsOf("message_ids" to lastMenuMessageId, "delete_for_all" to 1))
        deleteRequest.execute()
        sendMessageKeyboard(to, message, keyboard)
    }

    override suspend fun sendPhoto(to: Long, message: String, file: File, keyboard: BotKeyboard?) {
        val imageAttachmentString = client.uploader().uploadPhotoForMessage(
            to.toInt(),
            FileOnDisk(file.path)
        )
        client.sendMessage {
            peerId = to.toInt()
            this.message = message
            this.keyboard = keyboard?.toVk()
            attachment = imageAttachmentString
        }.execute()
    }

    override suspend fun sendPhoto(
        to: Long,
        message: String,
        inputStream: InputStream,
        filename: String,
        keyboard: BotKeyboard?
    ) {
        val imageAttachmentString = client.uploader().uploadPhotoForMessage(
            to.toInt(),
            withContext(Dispatchers.IO) {
                inputStream.readAllBytes()
            }
        )
        client.sendMessage {
            peerId = to.toInt()
            this.message = message
            this.keyboard = keyboard?.toVk()
            attachment = imageAttachmentString
        }.execute()
    }

    override suspend fun deleteMessage(peerId: Long, messageId: Long) {
        val deleteRequest =
            client.call(
                "messages.delete",
                paramsOf("peer_id" to peerId, "message_ids" to messageId, "delete_for_all" to 1)
            )
        deleteRequest.execute()
    }

    override suspend fun sendPopup(botMessage: BotMessage, message: String): Boolean {
        client.sendMessage {
            peerId = botMessage.peerId.toInt()
            this.message = message
        }.execute()
        return false
    }

    override suspend fun getChat(chatId: Long): Chat? {
        TODO("Not yet implemented")
    }

    override suspend fun isUserInChat(chatId: Long, userId: Long): Boolean {
        TODO("Not yet implemented")
    }

    private suspend fun getRefId(ref: String?): UUID? {
        if (ref == null) return null
        val split = ref.split("-")
        if (split.size != 6) {
            return null
        }
        if (split[0] == "RF") return UUID.fromString(ref.drop(3))
        if (split[0] == "RC") {
            val receipt = PostgresReceiptPersistent.loadReceipt(UUID.fromString(ref.drop(3)))
            return receipt?.issuer?.id
        }
        return null
    }
}