package bot.exceptions

import bot.objects.User
import bot.receipts.Receipt

class IllegalRecipientException(
    val receipt: Receipt,
    val recipient: User
) : RuntimeException("Invalid recipient, expected: ${receipt.recipient}, actual: $recipient")