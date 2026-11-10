package com.sahay.engine

import java.io.File

/** Prints fixed-scenario plans; engine/parity_python.py prints the same lines from the Python reference. They must match exactly. */
fun main(args: Array<String>) {
    val root = File(args.getOrElse(0) { "content" })
    val c = Content.fromJson(File(root, "skillgraphs/dsa.json").readText(), File(root, "questions/dsa.json").readText())
    val prof = Profile(hoursPerWeek = 8.0, deadlineDay = 60)
    val t = listOf("dsa.dp_2d", "dsa.topo")
    val m = LearnerModel(); val mis = MisconceptionEngine()
    val q = c.bySkill.getValue("dsa.arrays")[0]
    val wrong = q.options.indexOfFirst { it.misconception != null }
    mis.observe(q, wrong, 90.0, 0.0); mis.observe(q, wrong, 90.0, 0.5)
    for (s in listOf("dsa.complexity", "dsa.stack")) { val st = m.state(s); st.p = 0.9; st.S = 6.0; st.lastSeen = 0.0 }
    val plan = buildPlan(c, m, mis, prof, 3.0, t)
    for (i in plan.items) println("plan ${i.type} ${i.skillId} ${i.day} ${i.minutes}")
    val rec = recover(c, m, prof, 3.0, t, plan, 400, mis = mis)
    for (i in rec.items) println("rec ${i.type} ${i.skillId} ${i.day} ${i.minutes}")
    for (i in rec.dropped) println("drop ${i.skillId} ${i.minutes} ${i.reason}")
}
