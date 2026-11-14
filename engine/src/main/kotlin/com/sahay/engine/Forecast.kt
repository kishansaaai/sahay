package com.sahay.engine

import kotlin.math.ceil
import kotlin.math.floor

/** Mean mastery over the goal closure at the end of one day that has at least one attempt. */
data class DayMastery(val day: Int, val mastery: Double, val answers: Int)

/** Why a forecast has or has not produced a number of days. The UI maps this to a localised sentence. */
enum class ForecastBasis { TARGET_REACHED, NOT_ENOUGH_DAYS, NO_TREND, TOO_FAR, ESTIMATE }

/**
 * A projection to the target mastery from the learner's own attempt history (PRD 9.7 dashboard, feature C7).
 *
 * This is a least-squares slope over the mean mastery observed on each study day, not a promise. It is deliberately blunt:
 * when there is less than two study days, no upward trend or a projection beyond a year, [daysToTarget] is null and
 * [basis] says why, so the UI can avoid showing a made-up number.
 */
data class GoalForecast(
    val trajectory: List<DayMastery>,
    val currentMastery: Double,
    val targetMastery: Double,
    val gainPerDay: Double,
    val daysToTarget: Int?,
    val basis: ForecastBasis,
)

/** Mean mastery after each study day, replaying the attempt log once. */
fun masteryTrajectory(
    content: Content, targets: Collection<String>, attempts: List<AttemptRow>, params: Params = Params(),
): List<DayMastery> {
    val ids = content.graph.closure(targets).sorted()
    if (ids.isEmpty()) return emptyList()
    val model = LearnerModel(params)
    val mis = MisconceptionEngine()
    val out = ArrayList<DayMastery>()
    val rows = attempts.sortedBy { it.seq }.groupBy { floor(it.day).toInt() }.toSortedMap()
    for ((day, group) in rows) {
        for (a in group.sortedBy { it.seq }) {
            val q = content.questions[a.questionId] ?: continue
            if (a.chosenIndex !in q.options.indices) continue
            applyAnswer(content, model, mis, ids, q, a.chosenIndex, a.confidence, a.responseMs, a.hintLevel, a.day)
        }
        val mean = ids.sumOf { model.pEff(it, day.toDouble()) } / ids.size
        out.add(DayMastery(day, mean, group.size))
    }
    return out
}

fun forecastToTarget(
    content: Content, targets: Collection<String>, attempts: List<AttemptRow>, now: Double,
    targetMastery: Double = 0.8, params: Params = Params(),
): GoalForecast {
    val ids = content.graph.closure(targets).sorted()
    val current = if (ids.isEmpty()) 0.0 else {
        val m = replay(content, targets, attempts, params).model
        ids.sumOf { m.pEff(it, now) } / ids.size
    }
    val trajectory = masteryTrajectory(content, targets, attempts, params)
    if (current >= targetMastery) {
        return GoalForecast(trajectory, current, targetMastery, 0.0, 0, ForecastBasis.TARGET_REACHED)
    }
    if (trajectory.size < 2) {
        return GoalForecast(trajectory, current, targetMastery, 0.0, null, ForecastBasis.NOT_ENOUGH_DAYS)
    }
    val x0 = trajectory.first().day
    val xs = trajectory.map { (it.day - x0).toDouble() }
    val ys = trajectory.map { it.mastery }
    val meanX = xs.average()
    val meanY = ys.average()
    val denom = xs.sumOf { (it - meanX) * (it - meanX) }
    val slope = if (denom <= 1e-12) 0.0 else xs.indices.sumOf { (xs[it] - meanX) * (ys[it] - meanY) } / denom
    if (slope <= 1e-9) {
        return GoalForecast(trajectory, current, targetMastery, slope, null, ForecastBasis.NO_TREND)
    }
    val days = ceil((targetMastery - current) / slope).toInt().coerceAtLeast(1)
    return if (days > 366) {
        GoalForecast(trajectory, current, targetMastery, slope, null, ForecastBasis.TOO_FAR)
    } else {
        GoalForecast(trajectory, current, targetMastery, slope, days, ForecastBasis.ESTIMATE)
    }
}
