package money.tegro.bot.exceptions

import money.tegro.bot.objects.Account

class AccountIssuerActivationException(
    val account: Account
) : RuntimeException()