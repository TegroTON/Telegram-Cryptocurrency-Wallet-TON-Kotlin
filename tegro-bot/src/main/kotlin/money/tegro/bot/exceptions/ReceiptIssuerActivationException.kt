package money.tegro.bot.exceptions

import money.tegro.bot.receipts.Receipt

class ReceiptIssuerActivationException(
    val receipt: Receipt
) : RuntimeException()