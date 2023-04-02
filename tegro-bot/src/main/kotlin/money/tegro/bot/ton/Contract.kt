package money.tegro.bot.ton

import org.ton.block.AccountInfo
import org.ton.block.AddrStd
import org.ton.block.CurrencyCollection
import org.ton.block.MsgAddressInt
import org.ton.lite.client.LiteClient

interface Contract {
    val liteClient: LiteClient
    val address: MsgAddressInt

    suspend fun balance(): CurrencyCollection =
        (liteClient.getAccountState(address as AddrStd).account.value as? AccountInfo)?.storage?.balance
            ?: CurrencyCollection()
}
