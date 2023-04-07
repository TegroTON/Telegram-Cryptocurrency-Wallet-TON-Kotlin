package money.tegro.bot.objects

import java.math.BigInteger

enum class DepositPeriod(
    val period: Int,
    val yield: BigInteger,
) {
    SHORT(3, 8.toBigInteger()),
    MEDIUM(6, 12.toBigInteger()),
    LONG(12, 18.toBigInteger()),
    INVESTOR(24, 28.toBigInteger());

    companion object {
        fun getDisplayName(deposit: DepositPeriod, lang: Language): String {
            return Messages[lang].menuDepositSelectPeriod.format(
                deposit.period,
                getWord(
                    deposit.period,
                    Messages[lang].monthOne,
                    Messages[lang].monthTwo,
                    Messages[lang].monthThree
                ),
                deposit.yield.toString() + "%"
            )
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
}
