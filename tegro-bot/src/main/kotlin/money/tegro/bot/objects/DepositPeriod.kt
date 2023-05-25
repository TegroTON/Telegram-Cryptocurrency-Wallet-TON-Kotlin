package money.tegro.bot.objects

import money.tegro.bot.utils.NftsPersistent
import java.math.BigDecimal

enum class DepositPeriod(
    val period: Int,
    val yield: BigDecimal,
) {
    SHORT(3, 8.toBigDecimal()),
    MEDIUM(6, 12.toBigDecimal()),
    LONG(12, 18.toBigDecimal()),
    INVESTOR(24, 28.toBigDecimal());

    companion object {
        fun getDisplayName(deposit: DepositPeriod, user: User): String {
            return Messages[user].menuDepositSelectPeriod.format(
                deposit.period,
                getWord(
                    deposit.period,
                    Messages[user].monthOne,
                    Messages[user].monthTwo,
                    Messages[user].monthThree
                ),
                NftsPersistent.countStackingPercent(user, deposit.yield).toString() + "%"
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
