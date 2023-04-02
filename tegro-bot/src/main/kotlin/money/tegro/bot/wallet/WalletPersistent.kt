package money.tegro.bot.wallet

import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import money.tegro.bot.exceptions.NotEnoughCoinsException
import money.tegro.bot.objects.PostgresUserPersistent
import money.tegro.bot.objects.User
import money.tegro.bot.utils.JSON
import net.dzikoysk.exposed.upsert.upsert
import net.dzikoysk.exposed.upsert.withUnique
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.*

interface WalletPersistent {
    suspend fun loadWalletState(user: User): WalletState
    suspend fun saveWalletState(user: User, state: WalletState): WalletState
    suspend fun transfer(sender: User, receiver: User, coins: Coins)
    suspend fun freeze(user: User, coins: Coins)
    suspend fun unfreeze(user: User, coins: Coins)

    suspend fun updateActive(user: User, cryptoCurrency: CryptoCurrency, onUpdate: (Coins) -> Coins): Coins {
        val currentWalletState = loadWalletState(user)
        val currentCoins = currentWalletState.active[cryptoCurrency]
        val newCoins = onUpdate(currentCoins)
        val newWalletState = currentWalletState.copy(
            active = currentWalletState.active.withCoins(newCoins)
        )
        saveWalletState(user, newWalletState)
        return newCoins
    }

    suspend fun updateFreeze(user: User, cryptoCurrency: CryptoCurrency, onUpdate: (Coins) -> Coins): Coins {
        val currentWalletState = loadWalletState(user)
        val currentCoins = currentWalletState.frozen[cryptoCurrency]
        val newCoins = onUpdate(currentCoins)
        val newWalletState = currentWalletState.copy(
            frozen = currentWalletState.frozen.withCoins(newCoins)
        )
        saveWalletState(user, newWalletState)
        return newCoins
    }
}

object PostgresWalletPersistent : WalletPersistent {

    object UsersActiveAssets : Table("users_active_assets") {
        val userId = uuid("user_id").references(PostgresUserPersistent.Users.id)
        val cryptoCurrency = enumeration<CryptoCurrency>("crypto_currency")
        val amount = long("amount")

        val uniqueTypeValue = withUnique("CyanP3tux", userId, cryptoCurrency)

        init {
            transaction { SchemaUtils.create(this@UsersActiveAssets) }
        }
    }

    object UsersFrozenAssets : Table("users_frozen_assets") {
        val userId = uuid("user_id").references(PostgresUserPersistent.Users.id)
        val cryptoCurrency = enumeration<CryptoCurrency>("crypto_currency")
        val amount = long("amount")

        val uniqueTypeValue = withUnique("Espymizan", userId, cryptoCurrency)

        init {
            transaction { SchemaUtils.create(this@UsersFrozenAssets) }
        }
    }

    override suspend fun loadWalletState(user: User): WalletState = coroutineScope {
        val activeCoins =
            suspendedTransactionAsync {
                UsersActiveAssets.select {
                    UsersActiveAssets.userId.eq(user.id)
                }.map {
                    val amount = it[UsersActiveAssets.amount]
                    val cryptoCurrency = it[UsersActiveAssets.cryptoCurrency]
                    Coins(
                        currency = cryptoCurrency,
                        amount = amount.toBigInteger()
                    )
                }
            }


        val frozenCoins =
            suspendedTransactionAsync {
                UsersFrozenAssets.select {
                    UsersFrozenAssets.userId.eq(user.id)
                }.map {
                    val amount = it[UsersFrozenAssets.amount]
                    val cryptoCurrency = it[UsersFrozenAssets.cryptoCurrency]
                    Coins(
                        currency = cryptoCurrency,
                        amount = amount.toBigInteger()
                    )
                }
            }

        WalletState(
            user,
            CoinsCollection(activeCoins.await()),
            CoinsCollection(frozenCoins.await())
        )
    }

    override suspend fun saveWalletState(user: User, state: WalletState): WalletState = coroutineScope {
        val active =
            suspendedTransactionAsync {
                state.active.forEach { coins ->
                    UsersActiveAssets.upsert(conflictIndex = UsersActiveAssets.uniqueTypeValue,
                        insertBody = {
                            it[userId] = user.id
                            it[cryptoCurrency] = coins.currency
                            it[amount] = coins.amount.toLong()
                        },
                        updateBody = {
                            it[amount] = coins.amount.toLong()
                        }
                    )
                }
            }

        val frozen =
            suspendedTransactionAsync {
                state.frozen.forEach { coins ->
                    UsersFrozenAssets.upsert(conflictIndex = UsersFrozenAssets.uniqueTypeValue,
                        insertBody = {
                            it[userId] = user.id
                            it[cryptoCurrency] = coins.currency
                            it[amount] = coins.amount.toLong()
                        },
                        updateBody = {
                            it[amount] = coins.amount.toLong()
                        }
                    )
                }
            }

        active.join()
        frozen.join()
        state
    }

    override suspend fun transfer(sender: User, receiver: User, coins: Coins) = coroutineScope {
        val senderState = async {
            loadWalletState(sender)
        }
        val receiverState = async {
            loadWalletState(receiver)
        }
        val senderCoins = senderState.await().frozen[coins.currency]
        if (senderCoins < coins) {
            throw NotEnoughCoinsException(sender, coins)
        }
        val newSenderCoins = senderCoins - coins
        val newReceiverCoins = receiverState.await().active[coins.currency] + coins

        val newSenderState = senderState.await().copy(
            active = senderState.await().frozen.withCoins(newSenderCoins)
        )
        val newReceiverState = receiverState.await().copy(
            active = receiverState.await().active.withCoins(newReceiverCoins)
        )

        val senderSaveJob = async {
            saveWalletState(sender, newSenderState)
        }
        val receiverSaveJob = async {
            saveWalletState(receiver, newReceiverState)
        }
        senderSaveJob.join()
        receiverSaveJob.join()
    }

    override suspend fun freeze(user: User, coins: Coins) {
        val walletState = loadWalletState(user)
        val currentCoins = walletState.active[coins.currency]
        val currentFrozen = walletState.frozen[coins.currency]

        val newCoins = walletState.active.withCoins(currentCoins - coins)
        val newFrozen = walletState.frozen.withCoins(currentFrozen + coins)

        val newWalletState = walletState.copy(
            active = newCoins,
            frozen = newFrozen
        )
        saveWalletState(user, newWalletState)
    }

    override suspend fun unfreeze(user: User, coins: Coins) {
        val walletState = loadWalletState(user)
        val currentFrozen = walletState.frozen[coins.currency]
        val currentCoins = walletState.active[coins.currency]

        val newFrozen = walletState.frozen.withCoins(currentFrozen - coins)
        val newCoins = walletState.active.withCoins(currentCoins + coins)

        val newWalletState = walletState.copy(
            active = newCoins,
            frozen = newFrozen
        )
        saveWalletState(user, newWalletState)
    }

}

// FOR DEBUG ONLY!!! DON'T USE IT IN PRODUCTION!
class JsonWalletPersistent(
    val file: File
) : WalletPersistent {
    init {
        if (file.parentFile != null && !file.parentFile.exists()) file.parentFile.mkdirs()
        if (!file.exists()) {
            file.createNewFile()
            file.writeText("{}")
        }
    }

    private val fileLock = reentrantLock()

    override suspend fun loadWalletState(user: User): WalletState {
        return fileLock.withLock {
            loadWalletStateUnsafe(user)
        }
    }

    override suspend fun saveWalletState(user: User, state: WalletState): WalletState {
        fileLock.withLock {
            saveWalletStateUnsafe(user, state)
        }
        return state
    }

    override suspend fun transfer(sender: User, receiver: User, coins: Coins) {
        fileLock.withLock {
            val senderState = loadWalletStateUnsafe(sender)
            val receiverState = loadWalletStateUnsafe(receiver)
            val senderCoins = senderState.frozen[coins.currency]
            if (senderCoins < coins) {
                throw NotEnoughCoinsException(sender, coins)
            }
            val newSenderCoins = senderCoins - coins
            val newReceiverCoins = receiverState.active[coins.currency] + coins

            val newSenderState = senderState.copy(
                active = senderState.frozen.withCoins(newSenderCoins)
            )
            val newReceiverState = receiverState.copy(
                active = receiverState.active.withCoins(newReceiverCoins)
            )

            saveWalletStateUnsafe(sender, newSenderState)
            saveWalletStateUnsafe(receiver, newReceiverState)
        }
    }

    override suspend fun freeze(user: User, coins: Coins) {
        fileLock.withLock {
            val walletState = loadWalletStateUnsafe(user)
            val currentCoins = walletState.active[coins.currency]
            val currentFrozen = walletState.frozen[coins.currency]

            val newCoins = walletState.active.withCoins(currentCoins - coins)
            val newFrozen = walletState.frozen.withCoins(currentFrozen + coins)

            val newWalletState = walletState.copy(
                active = newCoins,
                frozen = newFrozen
            )
            saveWalletStateUnsafe(user, newWalletState)
        }
    }

    override suspend fun unfreeze(user: User, coins: Coins) {
        fileLock.withLock {
            val walletState = loadWalletStateUnsafe(user)
            val currentFrozen = walletState.frozen[coins.currency]
            val currentCoins = walletState.active[coins.currency]

            val newFrozen = walletState.frozen.withCoins(currentFrozen - coins)
            val newCoins = walletState.active.withCoins(currentCoins + coins)

            val newWalletState = walletState.copy(
                active = newCoins,
                frozen = newFrozen
            )
            saveWalletStateUnsafe(user, newWalletState)
        }
    }

    private fun loadWalletStateUnsafe(user: User): WalletState {
        val map = loadMap()
        return map[user.id] ?: WalletState(
            user = user,
            active = CoinsCollection.ZERO,
            frozen = CoinsCollection.ZERO
        )
    }

    private fun saveWalletStateUnsafe(user: User, state: WalletState) {
        val menuMap = loadMap().toMutableMap()
        menuMap[user.id] = state
        saveMap(menuMap)
    }

    private fun loadMap() =
        JSON.decodeFromString<Map<UUID, WalletState>>(file.readText())

    private fun saveMap(map: Map<UUID, WalletState>) = file.writeText(
        JSON.encodeToString(map)
    )
}