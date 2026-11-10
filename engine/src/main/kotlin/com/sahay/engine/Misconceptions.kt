package com.sahay.engine

/**
 * Misconception engine (PRD 9.3). Confident wrong answers weigh about twice a guess; two confident hits create a repair
 * item; resolved after three correct answers spanning at least two days. Port of misconceptions.py.
 */
const val CONFIDENT = 70.0
const val GUESS = 30.0

fun evidenceWeight(confidence: Double): Double = when {
    confidence >= CONFIDENT -> 1.0
    confidence <= GUESS -> 0.5
    else -> 0.5 + 0.5 * (confidence - GUESS) / (CONFIDENT - GUESS)
}

class MState {
    var evidence = 0.0
    var confidentHits = 0
    var lastTriggered: Double? = null
    var resolved = false
    var correctDays = ArrayList<Double>()
}

class MisconceptionEngine {
    val state = HashMap<String, MState>()

    private fun s(id: String) = state.getOrPut(id) { MState() }

    /** Record one answer to a bank question. */
    fun observe(question: Question, chosenIndex: Int, confidence: Double, now: Double) {
        val chosen = question.options[chosenIndex].misconception
        if (chosen == null) {
            // credit every misconception that appeared as a distractor on this question
            for (mid in question.options.mapNotNull { it.misconception }.toSortedSet()) {
                val st = s(mid)
                if (st.confidentHits >= REPAIR_HITS || st.evidence > 0) {
                    st.correctDays.add(now)
                    maybeResolve(st)
                }
            }
            return
        }
        val st = s(chosen)
        st.evidence += evidenceWeight(confidence)
        if (confidence >= CONFIDENT) st.confidentHits += 1
        st.lastTriggered = now
        st.correctDays = ArrayList()      // a relapse restarts the resolution count
        st.resolved = false
    }

    private fun maybeResolve(st: MState) {
        val d = st.correctDays
        if (d.size >= RESOLVE_CORRECT && (d.last() - d[d.size - RESOLVE_CORRECT]) >= RESOLVE_SPAN_DAYS) st.resolved = true
    }

    /** Misconception ids that currently need a repair item. */
    fun repairs(): List<String> = state.filter { it.value.confidentHits >= REPAIR_HITS && !it.value.resolved }.keys.sorted()

    fun active(): List<String> = state.filter { it.value.evidence > 0 && !it.value.resolved }.keys.sorted()

    fun isResolved(id: String): Boolean = state[id]?.resolved == true

    companion object {
        const val REPAIR_HITS = 2
        const val RESOLVE_CORRECT = 3
        const val RESOLVE_SPAN_DAYS = 2.0
    }
}
