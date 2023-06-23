package money.tegro.bot.receipts

import kotlinx.datetime.Clock
import money.tegro.bot.exceptions.*
import money.tegro.bot.objects.LogType
import money.tegro.bot.objects.PostgresUserPersistent
import money.tegro.bot.objects.User
import money.tegro.bot.receipts.PostgresReceiptPersistent.UsersReceipts.activations
import money.tegro.bot.receipts.PostgresReceiptPersistent.UsersReceipts.amount
import money.tegro.bot.receipts.PostgresReceiptPersistent.UsersReceipts.captcha
import money.tegro.bot.receipts.PostgresReceiptPersistent.UsersReceipts.currency
import money.tegro.bot.receipts.PostgresReceiptPersistent.UsersReceipts.description
import money.tegro.bot.receipts.PostgresReceiptPersistent.UsersReceipts.isActive
import money.tegro.bot.receipts.PostgresReceiptPersistent.UsersReceipts.issueTime
import money.tegro.bot.receipts.PostgresReceiptPersistent.UsersReceipts.issuerId
import money.tegro.bot.receipts.PostgresReceiptPersistent.UsersReceipts.onlyNew
import money.tegro.bot.receipts.PostgresReceiptPersistent.UsersReceipts.onlyPremium
import money.tegro.bot.receipts.PostgresReceiptPersistent.UsersReceipts.recipientId
import money.tegro.bot.receipts.PostgresReceiptPersistent.UsersReceiptsChats.receiptId
import money.tegro.bot.utils.SecurityPersistent
import money.tegro.bot.wallet.Coins
import money.tegro.bot.wallet.CryptoCurrency
import money.tegro.bot.walletPersistent
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

interface ReceiptPersistent {
    suspend fun createReceipt(
        issuer: User,
        coins: Coins,
        activations: Int,
        recipient: User? = null
    ): Receipt

    suspend fun activateReceipt(
        receipt: Receipt,
        recipient: User
    )

    suspend fun loadReceipts(user: User): ReceiptCollection

    suspend fun loadActivations(receipt: Receipt): List<UUID>

    suspend fun inactivateReceipt(receipt: Receipt)

    suspend fun addChatToReceipt(receipt: Receipt, chatId: Long)

    suspend fun deleteChatFromReceipt(receipt: Receipt, chatId: Long)

    suspend fun getChatsByReceipt(receipt: Receipt): List<Long>
}

object PostgresReceiptPersistent : ReceiptPersistent {

    object UsersReceipts : UUIDTable("users_receipts") {
        val issueTime = timestamp("issue_time")
        val issuerId = uuid("issuer_id").references(PostgresUserPersistent.Users.id)
        val currency = enumeration<CryptoCurrency>("currency")
        val amount = long("amount")
        val activations = integer("activations")
        val description = text("description").default("")
        val recipientId = uuid("recipient_id").references(PostgresUserPersistent.Users.id).nullable()
        val captcha = bool("captcha").default(true)
        val onlyNew = bool("only_new").default(false)
        val onlyPremium = bool("only_premium").default(false)
        val isActive = bool("is_active")

        init {
            transaction {
                SchemaUtils.create(this@UsersReceipts)
            }
        }
    }

    object UsersReceiptsChats : Table("users_receipts_chats") {
        val receiptId = uuid("receipt_id").references(UsersReceipts.id)
        val chatId = long("chat_id")

        init {
            transaction { SchemaUtils.create(this@UsersReceiptsChats) }
        }
    }

    object UsersReceiptsActivations : Table("users_receipts_activations") {
        val receiptId = uuid("receipt_id").references(UsersReceipts.id)
        val userId = uuid("user_id").references(PostgresUserPersistent.Users.id)

        init {
            transaction { SchemaUtils.create(this@UsersReceiptsActivations) }
        }
    }

    override suspend fun createReceipt(issuer: User, coins: Coins, activations: Int, recipient: User?): Receipt {
        val toFreeze = Coins(coins.currency, coins.amount * activations.toBigInteger())
        walletPersistent.freeze(issuer, toFreeze)
        try {
            val receipt = Receipt(
                id = UUID.randomUUID(),
                issueTime = Clock.System.now(),
                issuer = issuer,
                coins = coins,
                activations = activations,
                recipient = recipient,
            )
            saveReceipt(receipt)
            SecurityPersistent.log(
                issuer,
                toFreeze,
                "${receipt.id} | $coins x $activations, recipient=$recipient",
                LogType.RECEIPT_CREATE
            )
            return receipt
        } catch (e: Throwable) {
            walletPersistent.unfreeze(issuer, coins)
            throw e
        }
    }

    fun saveReceipt(receipt: Receipt) {
        transaction {
            //addLogger(StdOutSqlLogger)

            exec(
                """
                    INSERT INTO users_receipts (id, issue_time, issuer_id, currency, amount, activations, description, recipient_id, captcha, only_new, only_premium, is_active) 
                    values (?,?,?,?,?,?,?,?,?,?,?,?)
                    ON CONFLICT (id) DO UPDATE SET issue_time=?, issuer_id=?, currency=?, amount=?, activations=?, description=?,
                    recipient_id=?, captcha=?, only_new=?, only_premium=?, is_active=?
                    """, args = listOf(
                    UsersReceipts.id.columnType to receipt.id,
                    issueTime.columnType to receipt.issueTime,
                    issuerId.columnType to receipt.issuer.id,
                    currency.columnType to receipt.coins.currency,
                    amount.columnType to receipt.coins.amount,
                    activations.columnType to receipt.activations,
                    description.columnType to receipt.description,
                    recipientId.columnType to receipt.recipient?.id,
                    captcha.columnType to receipt.captcha,
                    onlyNew.columnType to receipt.onlyNew,
                    onlyPremium.columnType to receipt.onlyPremium,
                    isActive.columnType to receipt.isActive,

                    issueTime.columnType to receipt.issueTime,
                    issuerId.columnType to receipt.issuer.id,
                    currency.columnType to receipt.coins.currency,
                    amount.columnType to receipt.coins.amount,
                    activations.columnType to receipt.activations,
                    description.columnType to receipt.description,
                    recipientId.columnType to receipt.recipient?.id,
                    captcha.columnType to receipt.captcha,
                    onlyNew.columnType to receipt.onlyNew,
                    onlyPremium.columnType to receipt.onlyPremium,
                    isActive.columnType to receipt.isActive,
                )
            )
        }
    }

    suspend fun deleteReceipt(receipt: Receipt) {
        inactivateReceipt(receipt)
        val toUnfreeze = Coins(receipt.coins.currency, receipt.coins.amount * receipt.activations.toBigInteger())
        SecurityPersistent.log(
            receipt.issuer,
            toUnfreeze,
            "${receipt.id} | left ${receipt.coins} x $activations",
            LogType.RECEIPT_INACTIVATE
        )
        walletPersistent.unfreeze(receipt.issuer, toUnfreeze)
    }

    override suspend fun inactivateReceipt(receipt: Receipt) {
        transaction {
            UsersReceipts.update({ UsersReceipts.id eq receipt.id }) {
                it[isActive] = false
            }
        }
    }

    override suspend fun addChatToReceipt(receipt: Receipt, chatId: Long) {
        transaction {
            UsersReceiptsChats.insert {
                it[receiptId] = receipt.id
                it[UsersReceiptsChats.chatId] = chatId
            }
        }
    }

    override suspend fun deleteChatFromReceipt(receipt: Receipt, chatId: Long) {
        transaction {
            UsersReceiptsChats.deleteWhere { receiptId.eq(receipt.id) and UsersReceiptsChats.chatId.eq(chatId) }
        }
    }

    override suspend fun getChatsByReceipt(receipt: Receipt): List<Long> {
        val chats = suspendedTransactionAsync {
            UsersReceiptsChats.select {
                receiptId.eq(receipt.id)
            }.mapNotNull {
                it[UsersReceiptsChats.chatId]
            }
        }
        return chats.await()
    }

    override suspend fun activateReceipt(receipt: Receipt, recipient: User) {
        val receipts = loadReceipts(receipt.issuer).toMutableList()
        val currentReceipt = receipts.find { it.id == receipt.id } ?: throw UnknownReceiptException(receipt)
        val activations = loadActivations(receipt)

        if (currentReceipt.recipient != null && currentReceipt.recipient != recipient) {
            throw InvalidRecipientException(receipt, recipient)
        }
        if (currentReceipt.issuer == recipient) {
            throw ReceiptIssuerActivationException(receipt)
        }
        if (activations.contains(recipient.id)) {
            throw ReceiptNotActiveException(receipt)
        }
        if (receipt.onlyNew) {
            if (recipient.settings.referralId != null) {
                if (recipient.settings.referralId != receipt.issuer.id) {
                    throw ReceiptNotNewUserException(receipt)
                }
            } else {
                throw ReceiptNotNewUserException(receipt)
            }
        }
        if (!currentReceipt.isActive || currentReceipt.activations < 1) {
            throw ReceiptNotActiveException(receipt)
        }
        receipt.issuer.transfer(recipient, receipt.coins)
        SecurityPersistent.log(
            receipt.issuer,
            receipt.coins,
            "paid ${receipt.coins} for receipt ${receipt.id}",
            LogType.RECEIPT_PAID
        )
        SecurityPersistent.log(
            recipient,
            receipt.coins,
            "got ${receipt.coins} from receipt ${receipt.id}",
            LogType.RECEIPT_GOT
        )
        var currentActivations = receipt.activations
        currentActivations--
        if (currentActivations < 1) {
            inactivateReceipt(receipt)
        } else {
            transaction {
                UsersReceipts.update({ UsersReceipts.id eq receipt.id }) {
                    it[UsersReceipts.activations] = currentActivations
                }
                UsersReceiptsActivations.insert {
                    it[receiptId] = receipt.id
                    it[userId] = recipient.id
                }
            }
        }
    }

    suspend fun loadReceipt(receiptId: UUID): Receipt? {
        val receipt = suspendedTransactionAsync {
            val result = UsersReceipts.select {
                UsersReceipts.id.eq(receiptId)
            }.firstOrNull() ?: return@suspendedTransactionAsync null
            val issuer = PostgresUserPersistent.load(result[issuerId]) ?: return@suspendedTransactionAsync null
            val recipient = result[recipientId]?.let { uuid -> PostgresUserPersistent.load(uuid) }
            Receipt(
                id = result[UsersReceipts.id].value,
                issueTime = result[issueTime],
                issuer = issuer,
                coins = Coins(
                    currency = result[currency],
                    amount = result[amount].toBigInteger()
                ),
                activations = result[activations],
                description = result[description],
                recipient = recipient,
                captcha = result[captcha],
                onlyNew = result[onlyNew],
                onlyPremium = result[onlyPremium],
                isActive = result[isActive]
            )
        }
        return receipt.await()
    }

    override suspend fun loadReceipts(user: User): ReceiptCollection {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(UsersReceipts)
        }
        val receipts = suspendedTransactionAsync {
            UsersReceipts.select {
                issuerId.eq(user.id)
            }.mapNotNull {
                val issuer = PostgresUserPersistent.load(it[issuerId]) ?: return@mapNotNull null
                val recipient = it[recipientId]?.let { uuid -> PostgresUserPersistent.load(uuid) }
                Receipt(
                    id = it[UsersReceipts.id].value,
                    issueTime = it[issueTime],
                    issuer = issuer,
                    coins = Coins(
                        currency = it[currency],
                        amount = it[amount].toBigInteger()
                    ),
                    activations = it[activations],
                    description = it[description],
                    recipient = recipient,
                    captcha = it[captcha],
                    onlyNew = it[onlyNew],
                    onlyPremium = it[onlyPremium],
                    isActive = it[isActive]
                )
            }
        }
        return ReceiptCollection(receipts = receipts.await())
    }

    override suspend fun loadActivations(receipt: Receipt): List<UUID> {
        val activations = suspendedTransactionAsync {
            UsersReceiptsActivations.select {
                UsersReceiptsActivations.receiptId.eq(receipt.id)
            }.map {
                it[UsersReceiptsActivations.userId]
            }
        }
        return activations.await()
    }
}