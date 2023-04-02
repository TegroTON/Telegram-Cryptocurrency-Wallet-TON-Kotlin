package money.tegro.bot.exceptions

class InvalidAddressException(
    val address: String
) : IllegalArgumentException("Invalid address: '$address'")