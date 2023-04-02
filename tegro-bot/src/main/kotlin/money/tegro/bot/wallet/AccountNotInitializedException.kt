package money.tegro.bot.wallet

import org.ton.block.MsgAddressInt

class AccountNotInitializedException(
    val address: MsgAddressInt
) : IllegalStateException("Account $address not initialized")
