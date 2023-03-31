package money.tegro.bot.objects

data class BotMessage(
    val messageId: Long,
    val userId: Long,
    val peerId: Long,
    val isFromChat: Boolean,
    val body: String?,
    val payload: String?,
    val lastMenuMessageId: Long?,
    val forwardMessages: List<BotMessage>
) {
}