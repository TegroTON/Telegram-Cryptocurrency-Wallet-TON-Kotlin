package money.tegro.bot.utils

import fr.minuskube.pastee.JPastee
import fr.minuskube.pastee.data.Paste
import fr.minuskube.pastee.data.Section
import fr.minuskube.pastee.response.SubmitResponse
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import money.tegro.bot.objects.Log
import money.tegro.bot.objects.LogType
import money.tegro.bot.objects.User
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.minutes

class LogsUtil {

    companion object {
        var logs: MutableList<Log> = emptyList<Log>().toMutableList()

        suspend fun start() {
            while (true) {
                if (logs.isNotEmpty()) {
                    PostgresLogsPersistent.pushLogs(logs)
                    logs = emptyList<Log>().toMutableList()
                }
                delay(1.minutes)
            }
        }

        fun log(user: User, info: String, logType: LogType) {
            val log = Log(
                UUID.randomUUID(),
                user.id,
                Clock.System.now(),
                logType,
                info
            )
            logs.add(log)
        }

        fun getLogsLink(logs: List<Log>, name: String): String {
            val pastee = JPastee("uvTVeu2XRFssXJJCqqZxc7qOwup2Q037oB3CVWX23")
            val text = buildString {
                for (log: Log in logs) {
                    val date = Date.from(log.time.toJavaInstant())
                    val time =
                        SimpleDateFormat("dd.MM.yyyy HH:mm").format(date)
                    appendLine("[$time] ${log.userId} >> ${log.logType.displayName}: ${log.info}")
                }
            }
            println("Creating request to paste $name")
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