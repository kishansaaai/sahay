package com.sahay.engine

/**
 * Pure logic behind the Scan screen (issues B12, I4, I8). No Android dependency so it runs in CI.
 * OCR itself happens in the app; everything here starts from the recognised text.
 */

/** Hygiene for text that came from a camera: it is data for the model, never instructions (PRD 11). */
object OcrText {
    const val MAX_CHARS = 6000
    private val CONTROL = Regex("[\\p{Cc}&&[^\\n\\t]]")
    private val TAGS = Regex("</?\\s*data\\s*>", RegexOption.IGNORE_CASE)

    fun clean(raw: String): String {
        var t = CONTROL.replace(raw.replace("\r\n", "\n").replace('\r', '\n'), "")
        t = TAGS.replace(t, "")
        t = t.lines().joinToString("\n") { it.replace(Regex("[ \\t]+"), " ").trim() }
        t = t.replace(Regex("\n{3,}"), "\n\n").trim()
        if (t.length <= MAX_CHARS) return t
        val cut = t.lastIndexOf('\n', MAX_CHARS).let { if (it < MAX_CHARS / 2) MAX_CHARS else it }
        return t.substring(0, cut).trim()
    }
}

data class DraftSkill(val id: String, val name: String, val hours: Double, val included: Boolean = true)
data class DraftPrereq(val skill: String, val requires: String)

/**
 * An editable skill graph draft from a scanned syllabus (I4). Every edit returns a new draft; [errors] runs the same
 * validator the model output must pass, so a draft that reaches the learner's plan is always a valid graph.
 */
class SyllabusDraft(val skills: List<DraftSkill>, val prereqs: List<DraftPrereq>) {

    fun included(): List<DraftSkill> = skills.filter { it.included }
    fun totalHours(): Double = included().sumOf { it.hours }

    /** Not trimmed: this is called on every keystroke, and trimming would swallow the space between two words. A blank name is caught by [errors]. */
    fun rename(id: String, name: String): SyllabusDraft =
        SyllabusDraft(skills.map { if (it.id == id) it.copy(name = name.take(80)) else it }, prereqs)

    fun withHours(id: String, hours: Double): SyllabusDraft =
        SyllabusDraft(skills.map { if (it.id == id) it.copy(hours = hours.coerceIn(0.5, 20.0)) else it }, prereqs)

    fun toggle(id: String): SyllabusDraft = SyllabusDraft(skills.map { if (it.id == id) it.copy(included = !it.included) else it }, prereqs)

    fun remove(id: String): SyllabusDraft =
        SyllabusDraft(skills.filter { it.id != id }, prereqs.filter { it.skill != id && it.requires != id })

    /** The graph of the topics the learner kept, in the shape the validator and `syllabus_compile` schema use. */
    fun toGraphMap(): JMap {
        val keep = included().map { it.id }.toSet()
        return mapOf(
            "skills" to included().map { mapOf("id" to it.id, "name" to it.name.trim(), "hours" to it.hours) },
            "prerequisites" to prereqs.filter { it.skill in keep && it.requires in keep }.map { mapOf("skill" to it.skill, "requires" to it.requires) },
        )
    }

    fun errors(): List<String> = when {
        included().isEmpty() -> listOf("select at least one topic")
        included().any { it.name.isBlank() } -> listOf("every topic needs a name")
        else -> Validators.checkGraph(toGraphMap())
    }

    companion object {
        /** Reads validated `syllabus_compile` output. Returns null when the shape is wrong, so the UI can say so. */
        @Suppress("UNCHECKED_CAST")
        fun fromOutput(out: JMap?): SyllabusDraft? {
            val raw = out?.get("skills") as? List<JMap> ?: return null
            val skills = raw.mapNotNull { s ->
                val id = s["id"] as? String ?: return@mapNotNull null
                val name = s["name"] as? String ?: return@mapNotNull null
                DraftSkill(id, name, ((s["hours"] as? Number)?.toDouble() ?: 3.0).coerceIn(0.5, 20.0))
            }
            if (skills.isEmpty()) return null
            val prereqs = (out["prerequisites"] as? List<JMap> ?: emptyList()).mapNotNull { p ->
                val a = p["skill"] as? String ?: return@mapNotNull null
                val b = p["requires"] as? String ?: return@mapNotNull null
                DraftPrereq(a, b)
            }
            return SyllabusDraft(skills, prereqs)
        }
    }
}

/** Word-overlap matching between scanned topics and named skills; used for the 80% coverage target and the plan focus. */
object TopicMatch {
    private val STOP = setOf("and", "the", "of", "to", "in", "for", "with", "a", "an", "on", "introduction", "basics", "unit", "chapter")

    /** Crude plural stripping so "Stacks" matches "Stack" and "Queues" matches "Queue"; Latin script only. */
    private fun stem(w: String): String = when {
        w.length > 4 && w.endsWith("ies") -> w.dropLast(3) + "y"
        w.length > 4 && (w.endsWith("ses") || w.endsWith("xes")) -> w.dropLast(2)
        w.length > 3 && w.endsWith("s") && !w.endsWith("ss") -> w.dropLast(1)
        else -> w
    }

    fun tokens(s: String): Set<String> =
        Regex("[\\p{L}\\p{N}]+").findAll(s.lowercase()).map { it.value }.filter { it.length > 1 && it !in STOP }.map(::stem).toSet()

    fun score(a: String, b: String): Double {
        val x = tokens(a); val y = tokens(b)
        if (x.isEmpty() || y.isEmpty()) return 0.0
        val common = (x intersect y).size.toDouble()
        return common / minOf(x.size, y.size)
    }

    /** For each topic, the best skill id whose name overlaps at least [threshold], or null. */
    fun map(topics: List<String>, skills: Map<String, String>, threshold: Double = 0.67): Map<String, String?> =
        topics.associateWith { t ->
            skills.maxByOrNull { (_, name) -> score(t, name) }?.takeIf { score(t, it.value) >= threshold }?.key
        }

    /** Share of [expected] topics found among the draft's included topic names (target: 0.8, PRD I4). */
    fun coverage(expected: List<String>, draft: SyllabusDraft): Double {
        if (expected.isEmpty()) return 0.0
        val names = draft.included().map { it.name }
        return expected.count { e -> names.any { score(e, it) >= 0.67 } }.toDouble() / expected.size
    }
}

/** Page scan to quiz (I8): the text is split into windows and each window asks for one question. */
object ScanQuiz {
    const val QUESTIONS = 5
    private const val WINDOW = 700

    fun windows(text: String, n: Int = QUESTIONS): List<String> {
        val t = OcrText.clean(text)
        if (t.isEmpty()) return emptyList()
        if (t.length <= WINDOW) return listOf(t)
        val size = maxOf(WINDOW, t.length / n)
        val out = ArrayList<String>()
        var i = 0
        while (i < t.length && out.size < n) {
            var end = minOf(t.length, i + size)
            if (end < t.length) { val sp = t.lastIndexOf(' ', end); if (sp > i + size / 2) end = sp }
            out.add(t.substring(i, end).trim())
            i = end
        }
        return out.filter { it.isNotBlank() }
    }

    /** Input for the `draft_question` task for one window. */
    fun questionInput(window: String, index: Int): JMap = mapOf("skill_id" to "scan.page${index + 1}", "notes" to window)

    /** Input for the `explain` task used as the page summary. */
    fun summaryInput(text: String, language: String): JMap = mapOf(
        "skill_id" to "scan.page", "skill_name" to "Scanned page", "notes" to OcrText.clean(text).take(WINDOW * 2),
        "question" to "", "interests" to emptyList<String>(), "analogy" to false, "language" to language,
    )
}

/** One drafted question, already validated (exactly one correct option, unique options). */
data class ScanQuestion(val stem: String, val options: List<String>, val answer: Int, val misconceptions: List<String?>) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromOutput(out: JMap?): ScanQuestion? {
            val stem = out?.get("stem") as? String ?: return null
            val opts = out["options"] as? List<JMap> ?: return null
            val answer = (out["answer"] as? Number)?.toInt() ?: return null
            if (answer !in opts.indices) return null
            return ScanQuestion(stem, opts.map { it["text"] as? String ?: return null }, answer, opts.map { it["misconception"] as? String })
        }
    }
}
