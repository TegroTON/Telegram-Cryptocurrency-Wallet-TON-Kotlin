package money.tegro.bot.objects

enum class LogType(
    val displayName: String
) {
    DEPOSIT("Deposit"),
    WITHDRAW("Withdraw"),
    WITHDRAW_ADMIN("Withdraw"),
    DEPOSIT_CREATE("Deposit created"),
    DEPOSIT_PAID("Deposit paid"),
    RECEIPT_CREATE("Receipt created"),
    RECEIPT_DELETE("Receipt deleted"),
    RECEIPT_ACTIVATION("Receipt activated"),
}