package money.tegro.bot.objects

enum class DepositPeriod(
    val period: Int,
    val yield: Int,
) {
    SHORT(3, 8),
    MEDIUM(6, 12),
    LONG(12, 18),
    INVESTOR(24, 28);

    companion object {
        fun getDisplayName(lang: Language): String {
            //TODO
            return ""
        }
    }

    fun getWord(num: Int, vararg arr: String?): String? {
        val result: String?
        val num100 = num % 100
        result = if (num100 in 5..20) arr[2] else {
            val num10 = num100 % 10
            if (num10 == 1) arr[0] else if (num10 in 2..4) arr[1] else arr[2]
        }
        return result
    }
}
