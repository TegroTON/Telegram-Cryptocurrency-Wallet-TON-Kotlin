package bot.exceptions

import bot.receipts.Receipt

class ReceiptIssuerActivationException(
    val receipt: Receipt
) : RuntimeException()