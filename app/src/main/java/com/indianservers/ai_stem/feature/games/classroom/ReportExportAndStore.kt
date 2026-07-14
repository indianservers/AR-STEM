package com.indianservers.ai_stem.feature.games.classroom

import android.content.Context
import java.io.File

class LocalMatchReportStore(private val context: Context) {
    private val dir: File = File(context.filesDir, "ar_math_arena_reports").apply { mkdirs() }

    fun save(summary: MatchSummaryRecord): File {
        val file = File(dir, "${summary.matchId}.json")
        file.writeText(MatchReportExporter.toJson(summary, includeIndividuals = summary.individualHistoryEnabled))
        return file
    }

    fun listReports(): List<File> = dir.listFiles()?.filter { it.extension == "json" }?.sortedByDescending { it.lastModified() }.orEmpty()
    fun deleteMatch(matchId: String): Boolean = File(dir, "$matchId.json").delete()
    fun deleteAll(): Int = listReports().count { it.delete() }
    fun keepLast(count: Int) {
        listReports().drop(count.coerceAtLeast(0)).forEach { it.delete() }
    }
}

object MatchReportExporter {
    fun toCsv(summary: MatchSummaryRecord, includeIndividuals: Boolean): String = buildString {
        appendLine("section,id,name,score,accuracy,hints,missions,baseHealth")
        summary.teamSummaries.forEach {
            appendLine("team,${it.teamId},${it.teamName},${it.finalScore},${"%.2f".format(it.accuracy)},${it.hintUse},${it.missionsCompleted},${it.baseHealth}")
        }
        summary.topicPerformance.forEach {
            appendLine("topic,${it.topic},${it.topic},,${"%.2f".format(it.accuracy)},,${it.attempts},")
        }
        if (includeIndividuals) {
            summary.playerParticipation.forEach {
                appendLine("player,${it.playerId},${it.localAlias},,${"%.2f".format(it.accuracy)},${it.hintUse},${it.answerContributionCount},")
            }
        }
    }

    fun toJson(summary: MatchSummaryRecord, includeIndividuals: Boolean): String = buildString {
        append("{")
        append("\"schemaVersion\":${summary.schemaVersion},")
        append("\"matchId\":\"${summary.matchId.escapeJson()}\",")
        append("\"mode\":\"${summary.mode}\",")
        append("\"finalStage\":\"${summary.finalStage}\",")
        append("\"teams\":[${summary.teamSummaries.joinToString(",") { "{\"teamId\":\"${it.teamId.escapeJson()}\",\"teamName\":\"${it.teamName.escapeJson()}\",\"symbol\":\"${it.symbol}\",\"score\":${it.finalScore},\"accuracy\":${it.accuracy}}" }}],")
        append("\"topics\":[${summary.topicPerformance.joinToString(",") { "{\"topic\":\"${it.topic}\",\"attempts\":${it.attempts},\"accuracy\":${it.accuracy}}" }}],")
        append("\"interventions\":[${summary.interventions.joinToString(",") { "{\"type\":\"${it.type}\",\"reason\":\"${it.reason.escapeJson()}\"}" }}]")
        if (includeIndividuals) {
            append(",\"players\":[${summary.playerParticipation.joinToString(",") { "{\"playerId\":\"${it.playerId.escapeJson()}\",\"alias\":\"${it.localAlias.escapeJson()}\",\"accuracy\":${it.accuracy}}" }}]")
        }
        append("}")
    }

    fun toPrintableHtml(summary: MatchSummaryRecord, includeIndividuals: Boolean): String = buildString {
        append("<!doctype html><html><head><meta charset=\"utf-8\"><title>AR Math Arena Report</title></head><body>")
        append("<h1>AR Math Arena Match Report</h1>")
        append("<h2>Teams</h2><ul>")
        summary.teamSummaries.forEach { append("<li>${it.teamName.escapeHtml()} (${it.symbol}) - Score ${it.finalScore}, Accuracy ${"%.0f".format(it.accuracy * 100)}%</li>") }
        append("</ul><h2>Topics</h2><ul>")
        summary.topicPerformance.forEach { append("<li>${it.topic}: ${"%.0f".format(it.accuracy * 100)}% accuracy</li>") }
        append("</ul>")
        if (includeIndividuals) {
            append("<h2>Private Individual Summaries</h2><ul>")
            summary.playerParticipation.forEach { append("<li>${it.localAlias.escapeHtml()}: ${"%.0f".format(it.accuracy * 100)}% accuracy</li>") }
            append("</ul>")
        }
        append("</body></html>")
    }

    private fun String.escapeJson(): String = replace("\\", "\\\\").replace("\"", "\\\"")
    private fun String.escapeHtml(): String = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
