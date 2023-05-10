package money.tegro.bot.exceptions

import money.tegro.bot.receipts.Receipt

class ReceiptOnlyTgException(
    val receipt: Receipt
) : RuntimeException()