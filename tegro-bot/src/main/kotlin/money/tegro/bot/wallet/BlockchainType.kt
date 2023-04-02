package money.tegro.bot.wallet

enum class BlockchainType(
    val displayName: String,
    val tokenStandard: String
) {
    TON(
        displayName = "The Open Network",
        tokenStandard = "TEP-74"
    ),
    BSC(
        displayName = "Binance Smart Chain",
        tokenStandard = "BEP-20"
    ),
    ETH(
        displayName = "Ethereum",
        tokenStandard = "ERC-20"
    )
}
