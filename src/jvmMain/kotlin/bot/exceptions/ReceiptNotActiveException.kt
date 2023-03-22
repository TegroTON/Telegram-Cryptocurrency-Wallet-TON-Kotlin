package bot.exceptions

import bot.receipts.Receipt

class ReceiptNotActiveException(
    val receipt: Receipt
) : RuntimeException()