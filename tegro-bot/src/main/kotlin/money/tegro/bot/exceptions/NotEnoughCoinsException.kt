package money.tegro.bot.exceptions

import money.tegro.bot.objects.User
import money.tegro.bot.wallet.Coins

class NotEnoughCoinsException(
    val user: User,
    val coins: Coins
) : RuntimeException()