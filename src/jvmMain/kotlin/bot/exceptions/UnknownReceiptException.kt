package bot.exceptions

import bot.receipts.Receipt

class UnknownReceiptException(
    val receipt: Receipt
) : RuntimeException()