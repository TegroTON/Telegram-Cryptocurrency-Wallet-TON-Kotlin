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
    RECEIPT_INACTIVATE("Receipt inactivated"),
    RECEIPT_PAID("Receipt activated"),
    RECEIPT_GOT("Receipt activated"),
    RECEIPT_REFERRAL("Receipt activated"),
    ACCOUNT_CREATE("Account created"),
    ACCOUNT_INACTIVATE("Account inactivated"),
    ACCOUNT_PAID("Account payment"),
    ACCOUNT_GOT("Account payment"),
    DEPOSIT_ADMIN("Deposit"),
    COINS_FROZEN("Coins frozen"),
    COINS_UNFROZEN("Coins unfrozen"),
}