package money.tegro.bot.exceptions

import money.tegro.bot.receipts.Receipt

class RecipientNotSubscriberException(
    val receipt: Receipt,
    val chatName: String
) : RuntimeException("Recipient must be subscribed to $chatName")