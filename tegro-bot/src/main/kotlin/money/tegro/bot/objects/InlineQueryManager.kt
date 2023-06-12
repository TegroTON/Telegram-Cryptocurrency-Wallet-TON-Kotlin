package money.tegro.bot.objects

import kotlinx.coroutines.future.await
import kotlinx.serialization.Serializable
import money.tegro.bot.api.TgBot
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.receipts.PostgresReceiptPersistent
import money.tegro.bot.utils.PostgresAccountsPersistent
import money.tegro.bot.utils.linkButton
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle
import java.util.*


class InlineQueryManager {

    companion object {
        suspend fun answer(bot: TgBot, user: User, inlineQuery: InlineQuery) {
            val results: MutableList<InlineQueryResult> = ArrayList()
            val query = inlineQuery.query

            if (query.startsWith("RC") || query.startsWith("AC")) {
                val split = query.split("-")
                if (split.size == 6) {
                    val type = split[0]
                    val code = query.drop(3)
                    when (type) {
                        "RC" -> {
                            val receipt = PostgresReceiptPersistent.loadReceipt(UUID.fromString(code))
                            if (receipt != null) {
                                val receiptArticle = getArticle(
                                    results.size.toString(),
                                    Messages[user].inlineReceiptText.format(receipt.coins.toStringWithRate(user)),
                                    Messages[user].inlineReceiptButtonText.format(receipt.coins.toStringWithRate(user)),
                                    String.format("t.me/%s?start=$query", System.getenv("TG_USER_NAME")),
                                    Messages[user].inlineReceiptTitle,
                                    Messages[user].inlineReceiptDescription
                                )
                                results.add(receiptArticle)
                            } else {
                                results.add(getInviteArticle(user, results.size.toString()))
                            }
                        }

                        "AC" -> {
                            val account = PostgresAccountsPersistent.loadAccount(UUID.fromString(code))
                            if (account != null) {
                                val receiptArticle = getArticle(
                                    results.size.toString(),
                                    Messages[user].inlineAccountText.format(account.maxCoins.toStringWithRate(user)),
                                    Messages[user].inlineAccountButtonText.format(account.maxCoins.toStringWithRate(user)),
                                    String.format("t.me/%s?start=$query", System.getenv("TG_USER_NAME")),
                                    Messages[user].inlineAccountTitle,
                                    Messages[user].inlineAccountDescription
                                )
                                results.add(receiptArticle)
                            } else {
                                results.add(getInviteArticle(user, results.size.toString()))
                            }
                        }

                        else -> results.add(getInviteArticle(user, results.size.toString()))
                    }
                } else {
                    results.add(getInviteArticle(user, results.size.toString()))
                }
            } else {
                results.add(getInviteArticle(user, results.size.toString()))
            }

            val answerInlineQuery = AnswerInlineQuery()
            answerInlineQuery.inlineQueryId = inlineQuery.id
            answerInlineQuery.cacheTime = 15
            answerInlineQuery.results = results

            bot.executeAsync(answerInlineQuery).await()
        }

        private fun getInviteArticle(user: User, articleId: String): InlineQueryResultArticle {
            return getArticle(
                articleId,
                Messages[user].inlineInviteText,
                Messages[user].inlineInviteButtonText,
                String.format("t.me/%s?start=RF-%s", System.getenv("TG_USER_NAME"), user.id.toString()),
                Messages[user].inlineInviteTitle,
                Messages[user].inlineInviteDescription
            )
        }

        private fun getArticle(
            articleId: String,
            messageText: String,
            buttonText: String,
            link: String,
            title: String,
            description: String
        ): InlineQueryResultArticle {
            val messageContent = InputTextMessageContent()
            messageContent.disableWebPagePreview
            messageContent.messageText = messageText

            val keyboard = BotKeyboard {
                row {
                    linkButton(
                        buttonText,
                        link,
                        ButtonPayload.serializer(),
                        ButtonPayload.LINK
                    )
                }
            }
            val article = InlineQueryResultArticle()
            article.inputMessageContent = messageContent
            article.id = articleId
            article.title = title
            article.description = description
            article.thumbUrl = "https://spyme.su/files/images/tegro.png"
            article.replyMarkup = keyboard.toTg()
            return article
        }
    }

    @Serializable
    private enum class ButtonPayload {
        LINK
    }
}