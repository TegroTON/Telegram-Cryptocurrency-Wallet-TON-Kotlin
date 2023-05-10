package money.tegro.bot.exceptions

import money.tegro.bot.receipts.Receipt

class ReceiptNotPremiumUserException(
    val receipt: Receipt
) : RuntimeException()