package bot.exceptions

import bot.objects.User
import bot.wallet.Coins

class NotEnoughCoinsException(
    val user: User,
    val coins: Coins
) : RuntimeException()