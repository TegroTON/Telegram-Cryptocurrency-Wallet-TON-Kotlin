package money.tegro.bot.exceptions

import money.tegro.bot.objects.Account

class UnknownAccountException(
    val account: Account
) : RuntimeException()