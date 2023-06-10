package money.tegro.bot.objects

enum class CooldownType(
    val displayName: String
) {
    NFT_UPDATE("NFT update"),
    DEPOSIT_CHECK("Check for new deposits"),
}