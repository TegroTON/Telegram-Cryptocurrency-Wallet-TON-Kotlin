package money.tegro.bot.exceptions

import money.tegro.bot.receipts.Receipt

class ReceiptNotActiveException(
    val receipt: Receipt
) : RuntimeException()