package com.sahay.engine

/** Skill graph and question bank (PRD section 8). Reads the same JSON files the app bundles. */
data class Skill(val id: String, val name: String, val prereqs: List<String>, val difficulty: Int, val hours: Double, val notes: String = "")
data class Option(val text: String, val misconception: String?)
data class Question(val id: String, val skill: String, val difficulty: Int, val stem: String, val options: List<Option>, val answer: Int)
data class Misconception(val id: String, val skill: String, val description: String)

class Graph(skillList: List<Skill>) {
    val skills: Map<String, Skill> = skillList.associateBy { it.id }
    private val dependentsMap: Map<String, MutableList<String>> = skills.keys.associateWith { mutableListOf<String>() }
    private val closureCache = HashMap<String, Set<String>>()

    init {
        for (s in skillList) for (p in s.prereqs) dependentsMap.getValue(p).add(s.id)
    }

    fun prereqs(id: String): List<String> = skills.getValue(id).prereqs
    fun dependents(id: String): List<String> = dependentsMap.getValue(id)

    /** All transitive prerequisites of [id], excluding [id]. */
    fun prereqClosure(id: String): Set<String> = closureCache.getOrPut(id) {
        val out = HashSet<String>()
        for (p in prereqs(id)) { out.add(p); out.addAll(prereqClosure(p)) }
        out
    }

    /** Targets plus every prerequisite of each target. */
    fun closure(targets: Collection<String>): Set<String> {
        val out = HashSet<String>()
        for (t in targets) { out.add(t); out.addAll(prereqClosure(t)) }
        return out
    }

    fun topoOrder(ids: Collection<String>? = null): List<String> {
        val set = (ids ?: skills.keys).toSet()
        val indeg = set.associateWith { i -> prereqs(i).count { it in set } }.toMutableMap()
        val ready = indeg.filter { it.value == 0 }.keys.sorted().toMutableList()
        val out = ArrayList<String>()
        while (ready.isNotEmpty()) {
            val n = ready.removeAt(0)
            out.add(n)
            for (d in dependents(n).sorted()) {
                if (d in set) {
                    indeg[d] = indeg.getValue(d) - 1
                    if (indeg.getValue(d) == 0) ready.add(d)
                }
            }
            ready.sort()
        }
        if (out.size != set.size) throw IllegalArgumentException("skill graph has a cycle")
        return out
    }

    fun estHours(id: String): Double = skills.getValue(id).hours
}

class Content(val domain: String, val graph: Graph, val misconceptions: Map<String, Misconception>, val questions: Map<String, Question>) {
    val bySkill: Map<String, List<Question>> = questions.values.groupBy { it.skill }

    fun misconceptionsOf(skill: String): List<Misconception> = misconceptions.values.filter { it.skill == skill }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromJson(graphJson: String, bankJson: String): Content {
            val g = Json.parse(graphJson) as Map<String, Any?>
            val b = Json.parse(bankJson) as Map<String, Any?>
            val skills = (g["skills"] as List<Map<String, Any?>>).map {
                Skill(it["id"] as String, it["name"] as String, (it["prereqs"] as List<String>),
                    (it["difficulty"] as Double).toInt(), it["hours"] as Double, (it["notes"] as? String) ?: "")
            }
            val ms = (b["misconceptions"] as List<Map<String, Any?>>).map {
                Misconception(it["id"] as String, it["skill"] as String, (it["description"] as? String) ?: "")
            }
            val qs = (b["questions"] as List<Map<String, Any?>>).map { q ->
                Question(q["id"] as String, q["skill"] as String, (q["difficulty"] as Double).toInt(), (q["stem"] as? String) ?: "",
                    (q["options"] as List<Map<String, Any?>>).map { Option(it["text"] as String, it["misconception"] as String?) },
                    (q["answer"] as Double).toInt())
            }
            return Content(g["domain"] as String, Graph(skills), ms.associateBy { it.id }, qs.associateBy { it.id })
        }
    }
}
