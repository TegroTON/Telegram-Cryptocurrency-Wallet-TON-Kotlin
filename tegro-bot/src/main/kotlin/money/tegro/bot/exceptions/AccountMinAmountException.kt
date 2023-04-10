package money.tegro.bot.exceptions

import money.tegro.bot.objects.Account

class AccountMinAmountException(
    val account: Account
) : RuntimeException()