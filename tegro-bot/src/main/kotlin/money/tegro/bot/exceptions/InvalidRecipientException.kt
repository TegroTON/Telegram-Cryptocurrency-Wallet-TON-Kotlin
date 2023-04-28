package money.tegro.bot.exceptions

import money.tegro.bot.objects.User
import money.tegro.bot.receipts.Receipt

class InvalidRecipientException(
    val receipt: Receipt,
    val recipient: User
) : RuntimeException("Invalid recipient, expected: ${receipt.recipient}, actual: $recipient")