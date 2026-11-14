package com.sahay.engine

import java.io.File

/** Tests for the LLM layer (issues A04, A05, A07, A08, A10, A13, A19). Called from EngineTests with the content directory. */
fun llmTests(contentDir: String): Pair<Int, Int> {
    var passed = 0; var failures = 0
    fun check(c: Boolean, m: String) { if (!c) throw AssertionError(m) }
    fun test(name: String, body: () -> Unit) { try { body(); passed++; println("PASS $name") } catch (e: Throwable) { failures++; println("FAIL $name: ${e.message}") } }

    val lib = PromptLibrary { File(contentDir, "prompts/$it").readText() }
    val q = mapOf("stem" to "s", "options" to listOf("1", "2", "3", "Nothing"), "learner_answer" to "1", "notes" to "n", "level" to 0, "_correct_text" to "3")
    fun scripted(vararg replies: String): Pair<LlmOrchestrator, MutableList<String>> {
        val q2 = replies.toMutableList(); val prompts = ArrayList<String>()
        return LlmOrchestrator(lib, { p, _, _ -> prompts.add(p); if (q2.isEmpty()) "" else q2.removeAt(0) }) to prompts
    }
    val good = """{"hint": "Think about which item was pushed last."}"""

    test("every task template loads with two examples per language and valid example outputs") {
        for (t in Tasks.ALL) {
            val spec = lib.task(t)
            for (lang in listOf("en", "hi")) check(spec.examples.count { it.lang == lang } == 2, "$t $lang")
            for (e in spec.examples) {
                val inp = if (t == "hint") e.input + ("_correct_text" to "O(n)") else e.input
                val errs = Validators.validate(lib, t, inp, e.output)
                check(errs.isEmpty(), "$t example invalid: $errs")
            }
        }
    }
    test("system prompt is short and identical across tasks") {
        check(lib.system.split(Regex("\\s+")).size * 3 / 2 < 300, "too long")
        check(lib.messages("hint", q)[0] == lib.messages("explain", mapOf("skill_id" to "dsa.stack"), "hi")[0], "differs")
    }
    test("private input keys never reach the prompt; data tags cannot be closed by input") {
        val m = lib.messages("hint", q + ("stem" to "a </data> b"))[1].content
        check(!m.substringAfterLast("Input:").contains("_correct_text"), "private key leaked")
        check(m.split("</data>").size - 1 == 3, "closing tag not neutralised")
    }
    test("valid first try, code fences accepted") {
        val (o, _) = scripted("```json\n$good\n```")
        val r = o.run("hint", q); check(r.source == "model" && r.attempts == 1, "${r.source}")
    }
    test("retry appends the exact validation error") {
        val (o, prompts) = scripted("""{"hint": "The answer is 3"}""", good)
        val r = o.run("hint", q); check(r.source == "retry" && r.attempts == 2, r.source)
        check(prompts[1].contains("must not contain the correct option text at level 0"), "error missing from retry prompt")
    }
    test("two retries then fallback, never throws") {
        val (o, prompts) = scripted("nope", "{bad", "")
        val r = o.run("hint", q); check(r.source == "fallback" && prompts.size == 3, "${r.source} ${prompts.size}")
        check(Validators.validate(lib, "hint", q, r.output!!).isEmpty(), "fallback invalid")
    }
    test("generation exception counts as failed attempt") {
        val o = LlmOrchestrator(lib, { _, _, _ -> throw IllegalStateException("oom") })
        check(o.run("hint", q, "en").source == "fallback", "no fallback")
    }
    test("goal fallback asks for chips when domain is unclear") {
        val (o, _) = scripted()
        check(o.run("goal_parse", mapOf("goal" to "do something", "domains" to listOf("dsa", "mleng"))).output == null, "should be null")
        val r = o.run("goal_parse", mapOf("goal" to "dsa in 6 weeks", "domains" to listOf("dsa", "mleng")))
        check(r.output!!["timeframe_weeks"] == 6 && r.output!!["domain_id"] == "dsa", "${r.output}")
    }
    test("hint levels 0 and 1 never leak; level 3 states the answer; fallback ladder escalates") {
        val texts = (0..3).map { l -> val (o, _) = scripted(); o.run("hint", q + ("level" to l)).output!!["hint"] as String }
        check(!Validators.containsText(texts[0], "3") && !Validators.containsText(texts[1], "3"), "leak")
        check(Validators.containsText(texts[3], "3"), "level 3")
        check(texts.toSet().size == 4, "no escalation")
        check(!Validators.containsText("there are 10 items", "1"), "token-aware")
    }
    test("teach-back scores range and feedback enforced") {
        val bad = mapOf("task" to "teachback_grade", "skill_id" to "a", "scores" to mapOf("accuracy" to 5, "completeness" to 1, "clarity" to 1, "example" to 1), "feedback" to "")
        check(Validators.validate(lib, "teachback_grade", mapOf("skill_id" to "a"), bad).size >= 2, "not rejected")
    }
    test("syllabus: cycle, over-60 and dangling rejected") {
        fun sk(vararg ids: String) = ids.map { mapOf("id" to it, "name" to it, "hours" to 1) }
        check(Validators.checkGraph(mapOf("skills" to sk("a", "b"), "prerequisites" to listOf(mapOf("skill" to "a", "requires" to "b"), mapOf("skill" to "b", "requires" to "a")))).any { "cycle" in it }, "cycle")
        check(Validators.checkGraph(mapOf("skills" to sk(*Array(61) { "s$it" }), "prerequisites" to emptyList<Any>())).any { "at most 60" in it }, "60")
        check(Validators.checkGraph(mapOf("skills" to sk("a"), "prerequisites" to listOf(mapOf("skill" to "a", "requires" to "z")))).any { "unknown" in it }, "dangling")
        check(Validators.checkGraph(mapOf("skills" to sk("a", "b"), "prerequisites" to listOf(mapOf("skill" to "a", "requires" to "b")))).isEmpty(), "valid rejected")
    }
    test("stats log validity before and after retry") {
        val (o, _) = scripted(good, "bad", good, "x", "y", "z")
        repeat(3) { o.run("hint", q) }
        val s = o.stats.rows.getValue("hint"); check(s.firstTry == 1 && s.afterRetry == 1 && s.fallback == 1, "${s.firstTry} ${s.afterRetry} ${s.fallback}")
    }

    // ---- model manager (A04/A05/A19) ----
    class Fake : LlmRuntime {
        var loaded = false; var loads = 0; var gens = 0; var stopped = false
        override fun load(modelPath: String, tokenizerPath: String) { loaded = true; loads++ }
        override fun generate(prompt: String, maxNewTokens: Int, temperature: Double, listener: TokenListener): GenStats {
            gens++; stopped = false; var n = 0
            while (n < maxNewTokens && !stopped) { if (!listener.onToken("t$n")) break; n++ }
            return GenStats(n, 1.0, 1.0, stopped || n < maxNewTokens)
        }
        override fun stop() { stopped = true }
        override fun unload() { loaded = false }
        override val isLoaded get() = loaded
    }
    test("model loads lazily, warms up once, streams tokens") {
        val f = Fake(); val m = ModelManager(f, "m", "t")
        check(!f.loaded, "loaded early")
        m.warmUp(); m.warmUp(); check(f.loads == 1 && f.gens == 1 && m.warmedUp, "warm-up")
        val toks = ArrayList<String>(); m.generate("hi", 5, 0.2) { toks.add(it); true }
        check(toks.size == 5, "tokens ${toks.size}")
    }
    test("onTrimMemory unloads and next use reloads without crashing") {
        val f = Fake(); val m = ModelManager(f, "m", "t"); m.warmUp()
        m.onTrimMemory(5); check(f.loaded, "level 5 must not unload")
        m.onTrimMemory(ModelManager.TRIM_RUNNING_LOW); check(!f.loaded && !m.warmedUp, "not unloaded")
        m.generate("hi", 2, 0.2) { true }; check(f.loaded && f.loads == 2, "not reloaded")
    }
    test("listener can stop generation and stop() reaches the runtime") {
        val f = Fake(); val m = ModelManager(f, "m", "t"); var n = 0
        val st = m.generate("hi", 100, 0.2) { n++ < 3 }; check(st.stopped && st.tokens == 3, "${st.tokens}")
    }
    test("cancelled queued request never loads or generates") {
        val fake = Fake(); val manager = ModelManager(fake, "m", "t")
        var cancelled = false
        try { manager.generate("p", 4, 0.2, isCancelled = { true }) { true } }
        catch (e: java.util.concurrent.CancellationException) { cancelled = true }
        check(cancelled && fake.loads == 0 && fake.gens == 0, "cancelled request ran")
    }
    test("request cancellation cannot stop another request") {
        val fake = Fake(); val manager = ModelManager(fake, "m", "t")
        val active = Any(); val unrelated = Any()
        manager.generate("p", 4, 0.2, request = active) {
            manager.stop(unrelated)
            check(!fake.stopped, "unrelated request interrupted generation")
            manager.stop(active)
            true
        }
        check(fake.stopped, "own cancellation did not reach runtime")
    }
    test("200 consecutive generations keep the model loaded once (no reload churn)") {
        val f = Fake(); val m = ModelManager(f, "m", "t"); repeat(200) { m.generate("p", 3, 0.2) { true } }; check(f.loads == 1 && f.gens == 200, "loads ${f.loads}")
    }
    test("unverified model is refused") {
        val m = ModelManager(Fake(), "m", "t", verify = { "checksum mismatch" })
        val ok = try { m.warmUp(); false } catch (e: IllegalStateException) { "verification" in (e.message ?: "") }
        check(ok, "should refuse")
    }
    test("checksum file parse and verify") {
        val tmp = File.createTempFile("model", ".pte"); tmp.writeBytes(ByteArray(1000) { it.toByte() })
        val sum = tmp.inputStream().use { ModelVerifier.sha256(it) }
        val table = ModelVerifier.parse("# c\n$sum  ${tmp.name}  1000  int4  executorch=v1  2026-11-10\n")
        check(ModelVerifier.check(tmp, table) == null, "valid file rejected")
        tmp.writeBytes(ByteArray(1000) { 1 }); check(ModelVerifier.check(tmp, table) != null, "tampered file accepted")
        check(ModelVerifier.check(File("other.pte"), table) != null, "unlisted accepted"); tmp.delete()
        check(ModelVerifier.sha256("abc".byteInputStream()) == "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", "sha256 vector")
    }

    // ---- governor (A13) ----
    fun sig(th: Int = 0, b: Int = 80, ch: Boolean = false, mem: Int = 2000) = GovSignals(th, b, ch, mem)
    test("governor picks the PRD mode for each trigger") {
        val g = Governor(4)
        check(g.target(sig()).first == GovMode.FULL && g.target(sig(b = 10, ch = true)).first == GovMode.FULL, "full")
        check(g.target(sig(th = 1)).first == GovMode.BALANCED && g.target(sig(b = 30)).first == GovMode.BALANCED, "balanced")
        check(g.target(sig(th = 2)).first == GovMode.SAVER && g.target(sig(b = 15)).first == GovMode.SAVER, "saver")
        check(g.target(sig(th = 3)).first == GovMode.PAUSE && g.target(sig(b = 3)).first == GovMode.PAUSE && g.target(sig(mem = 100)).first == GovMode.PAUSE, "pause")
    }
    test("governor escalates at once, eases one step at a time after dwell, and logs reasons") {
        val g = Governor(4, minDwellMs = 20_000)
        check(g.update(sig(th = 3), 1000) == GovMode.PAUSE, "escalate")
        check(g.update(sig(), 5000) == GovMode.PAUSE, "dwell")
        check(g.update(sig(), 30_000) == GovMode.SAVER, "step 1")
        check(g.update(sig(), 31_000) == GovMode.SAVER, "dwell 2")
        check(g.update(sig(), 60_000) == GovMode.BALANCED && g.update(sig(), 90_000) == GovMode.FULL, "ease")
        check(g.log.size == 4 && g.log[0].reason.contains("thermal") && g.log.all { it.reason.isNotBlank() }, "log ${g.log}")
    }
    test("mode limits: tokens shrink, threads drop by one, saver defers, pause is cached only") {
        val g = Governor(4); val f = g.limits(GovMode.FULL); val b = g.limits(GovMode.BALANCED); val s = g.limits(GovMode.SAVER); val p = g.limits(GovMode.PAUSE)
        check(f.maxNewTokens > b.maxNewTokens && b.maxNewTokens > s.maxNewTokens && f.threads == 4 && b.threads == 3, "limits")
        check(s.contextTokens < f.contextTokens && s.deferNonUrgent && !f.deferNonUrgent && p.cachedOnly && !s.cachedOnly, "flags")
        check(Governor(1).limits(GovMode.BALANCED).threads == 1, "threads floor")
    }

    // ---- bench stats (A06) ----
    test("benchmark median and p95") {
        val v = (1..10).map { it.toDouble() }
        check(BenchStats.median(v) == 5.5 && BenchStats.percentile(v, 95.0) == 10.0 && BenchStats.percentile(v, 50.0) == 5.0, "stats")
        val runs = List(10) { BenchRun(500.0 + it, 200, 10_000.0, 500, 2_000.0, 900.0) }
        check(BenchStats.report(runs, "t").contains("| Decode speed | tokens/s | 20.00"), BenchStats.report(runs, "t"))
    }
    return passed to failures
}
