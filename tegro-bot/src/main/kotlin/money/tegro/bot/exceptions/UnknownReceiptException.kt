package money.tegro.bot.exceptions

import money.tegro.bot.receipts.Receipt

class UnknownReceiptException(
    val receipt: Receipt
) : RuntimeException()