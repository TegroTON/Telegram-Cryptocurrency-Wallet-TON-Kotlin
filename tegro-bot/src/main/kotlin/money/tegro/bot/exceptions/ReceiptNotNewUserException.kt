package money.tegro.bot.exceptions

import money.tegro.bot.receipts.Receipt

class ReceiptNotNewUserException(
    val receipt: Receipt
) : RuntimeException()