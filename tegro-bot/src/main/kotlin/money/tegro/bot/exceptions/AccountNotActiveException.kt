package money.tegro.bot.exceptions

import money.tegro.bot.objects.Account

class AccountNotActiveException(
    val account: Account
) : RuntimeException()