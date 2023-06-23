package money.tegro.bot.utils

import fr.minuskube.pastee.JPastee
import fr.minuskube.pastee.data.Paste
import fr.minuskube.pastee.data.Section
import fr.minuskube.pastee.response.SubmitResponse
import kotlinx.coroutines.delay
import kotlinx.datetime.toJavaInstant
import money.tegro.bot.objects.Log
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.walletPersistent
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.minutes

class LogsUtil {

    companion object {
        private var logs: MutableList<Log> = emptyList<Log>().toMutableList()

        suspend fun start() {
            while (true) {
                if (logs.isNotEmpty()) {
                    PostgresLogsPersistent.pushLogs(logs)
                    logs = emptyList<Log>().toMutableList()
                }
                delay(1.minutes)
            }
        }

        fun log(log: Log) {
            logs.add(log)
        }

        suspend fun logsByUser(user: User): String {
            val walletState = walletPersistent.loadWalletState(user)
            val userInfo = buildString {
                appendLine("User TG id: ${user.tgId}")
                appendLine("User VK id: ${user.vkId}")
                appendLine("User address: ${user.settings.address}")
                appendLine("User referral id: ${user.settings.referralId}")
                appendLine()
                walletState.active.forEach {
                    appendLine("· ${it.currency.displayName}: ${it.toStringWithRate(user.settings.localCurrency)}")
                }
                val frozen = walletState.frozen.filter { it.amount > BigInteger.ZERO }
                if (frozen.isNotEmpty()) {
                    appendLine()
                    appendLine(Messages[user].menuWalletFrozenTitle)
                    appendLine()
                    frozen.forEach {
                        appendLine("· ${it.currency.displayName}: $it")
                    }
                }
            }
            return getLogsLink(
                PostgresLogsPersistent.getLogsByUser(user),
                userInfo,
                "Logs by ${user.id}"
            )
        }

        fun getLogsLink(logs: List<Log>, userInfo: String, name: String): String {
            val pastee = JPastee("uVXw8WfhrRzWlb0zii4lJufhUgN2uUjBchdyjhjcw")
            val text = buildString {
                for (log: Log in logs) {
                    val date = Date.from(log.time.toJavaInstant())
                    val time =
                        SimpleDateFormat("dd.MM.yyyy HH:mm").format(date)
                    appendLine("[$time] ${log.userId} >> ${log.logType.displayName}: ${log.info}")
                }
            }
            val paste = Paste.builder()
                .description(name)
                .addSection(
                    Section.builder()
                        .name("User info")
                        .contents(userInfo)
                        .syntax(pastee.getSyntaxFromName("text").get())
                        .build()
                )
                .addSection(
                    Section.builder()
                        .name(name)
                        .contents(text)
                        .syntax(pastee.getSyntaxFromName("text").get())
                        .build()
                )
                .build()

            val resp: SubmitResponse = pastee.submit(paste)

            if (!resp.isSuccess) {
                println("Pasting err: \n\nname=$name\ntext=\n$text\n\nERROR: \n${resp.errorString}")
                return "Pasting error, contact administrator"
            }
            return resp.link
        }

        fun getLogsLink(logs: List<Log>, name: String): String {
            val pastee = JPastee("uVXw8WfhrRzWlb0zii4lJufhUgN2uUjBchdyjhjcw")
            val text = buildString {
                for (log: Log in logs) {
                    val date = Date.from(log.time.toJavaInstant())
                    val time =
                        SimpleDateFormat("dd.MM.yyyy HH:mm").format(date)
                    appendLine("[$time] ${log.userId} >> ${log.logType.displayName}: ${log.info}")
                }
            }
            val paste = Paste.builder()
                .description(name)
                .addSection(
                    Section.builder()
                        .name(name)
                        .contents(text)
                        .syntax(pastee.getSyntaxFromName("text").get())
                        .build()
                )
                .build()

            val resp: SubmitResponse = pastee.submit(paste)

            if (!resp.isSuccess) {
                println("Pasting err: \n\nname=$name\ntext=\n$text\n\nERROR: \n${resp.errorString}")
                return "Pasting error, contact administrator"
            }
            return resp.link
        }
    }
}