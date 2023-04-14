package money.tegro.bot.exceptions

import money.tegro.bot.objects.Account

class AccountOverdraftException(
    val account: Account
) : RuntimeException()