package com.sahay.engine

/**
 * Kotlin port of llm/sahay_llm (prompts, schema check, validators, fallbacks, orchestrator). Same behaviour, same prompt files
 * (content/prompts/), so the app and the Python evaluation harness agree. PRD 10.2 to 10.4, issues A07 and A08.
 */
typealias JMap = Map<String, Any?>

object JsonOut {
    fun write(v: Any?): String = StringBuilder().also { put(it, v) }.toString()
    private fun put(sb: StringBuilder, v: Any?) {
        when (v) {
            null -> sb.append("null")
            is Boolean -> sb.append(v)
            is Number -> { val d = v.toDouble(); if (d == Math.floor(d) && Math.abs(d) < 1e15) sb.append(d.toLong()) else sb.append(d) }
            is String -> str(sb, v)
            is Map<*, *> -> { sb.append('{'); var first = true; for ((k, x) in v) { if (!first) sb.append(','); first = false; str(sb, k.toString()); sb.append(':'); put(sb, x) }; sb.append('}') }
            is Iterable<*> -> { sb.append('['); var first = true; for (x in v) { if (!first) sb.append(','); first = false; put(sb, x) }; sb.append(']') }
            else -> str(sb, v.toString())
        }
    }
    private fun str(sb: StringBuilder, s: String) {
        sb.append('"')
        for (c in s) when {
            c == '"' -> sb.append("\\\""); c == '\\' -> sb.append("\\\\"); c == '\n' -> sb.append("\\n"); c == '\r' -> sb.append("\\r"); c == '\t' -> sb.append("\\t")
            c < ' ' -> sb.append("\\u%04x".format(c.code)); else -> sb.append(c)
        }
        sb.append('"')
    }
}

/** JSON-schema subset check; messages match the Python version because they are fed back to the model on retry. */
object SchemaCheck {
    @Suppress("UNCHECKED_CAST")
    fun validate(value: Any?, schema: JMap, path: String = "$"): List<String> {
        val errs = ArrayList<String>()
        val t = schema["type"]
        if (t != null) {
            val ts = if (t is List<*>) t.map { it.toString() } else listOf(t.toString())
            if (ts.none { typeOk(value, it) }) return listOf("$path: expected ${ts.joinToString("/")}, got ${kind(value)}")
        }
        (schema["enum"] as? List<Any?>)?.let { if (value !in it) errs.add("$path: must be one of $it") }
        if (value is String) {
            (schema["minLength"] as? Number)?.let { if (value.length < it.toInt()) errs.add("$path: too short (min ${it.toInt()} characters)") }
            (schema["maxLength"] as? Number)?.let { if (value.length > it.toInt()) errs.add("$path: too long (max ${it.toInt()} characters)") }
        }
        if (value is Number) {
            (schema["minimum"] as? Number)?.let { if (value.toDouble() < it.toDouble()) errs.add("$path: must be >= ${fmt(it)}") }
            (schema["maximum"] as? Number)?.let { if (value.toDouble() > it.toDouble()) errs.add("$path: must be <= ${fmt(it)}") }
        }
        if (value is Map<*, *>) {
            val props = (schema["properties"] as? JMap) ?: emptyMap()
            (schema["required"] as? List<*>)?.forEach { if (!value.containsKey(it)) errs.add("$path: missing required field '$it'") }
            for ((k, v) in value) {
                val sub = props[k as String]
                if (sub != null) errs += validate(v, sub as JMap, "$path.$k")
                else if (schema["additionalProperties"] == false) errs.add("$path: unexpected field '$k'")
            }
        }
        if (value is List<*>) {
            (schema["minItems"] as? Number)?.let { if (value.size < it.toInt()) errs.add("$path: needs at least ${it.toInt()} items") }
            (schema["maxItems"] as? Number)?.let { if (value.size > it.toInt()) errs.add("$path: needs at most ${it.toInt()} items") }
            (schema["items"] as? JMap)?.let { s -> value.forEachIndexed { i, v -> errs += validate(v, s, "$path[$i]") } }
        }
        return errs
    }
    private fun fmt(n: Number): String { val d = n.toDouble(); return if (d == Math.floor(d)) d.toLong().toString() else d.toString() }
    private fun kind(v: Any?) = when (v) { null -> "NoneType"; is String -> "str"; is Boolean -> "bool"; is Number -> "number"; is Map<*, *> -> "dict"; else -> "list" }
    private fun typeOk(v: Any?, t: String) = when (t) {
        "string" -> v is String; "boolean" -> v is Boolean; "object" -> v is Map<*, *>; "array" -> v is List<*>; "null" -> v == null
        "integer" -> v is Number && v.toDouble() == Math.floor(v.toDouble()); "number" -> v is Number; else -> false
    }
}

data class ChatMessage(val role: String, val content: String)

class Example(val lang: String, val input: JMap, val output: JMap)

@Suppress("UNCHECKED_CAST")
class TaskSpec(json: JMap) {
    val name = json["task"] as String
    val temperature = (json["temperature"] as Number).toDouble()
    val maxTokens = (json["max_tokens"] as Number).toInt()
    val instruction = json["instruction"] as Map<String, String>
    val schema = json["schema"] as JMap
    val examples = (json["examples"] as List<JMap>).map { Example(it["lang"] as String, it["input"] as JMap, it["output"] as JMap) }
}

object Tasks {
    val ALL = listOf("goal_parse", "plan_narrate", "explain", "hint", "draft_question", "teachback_grade", "syllabus_compile")
}

/** Loads content/prompts/ through [readText] (file name to text), so the app can read from assets and tests from disk. */
@Suppress("UNCHECKED_CAST")
class PromptLibrary(private val readText: (String) -> String) {
    val system: String by lazy { readText("system.txt").trim() }
    private val specs = HashMap<String, TaskSpec>()
    fun task(name: String): TaskSpec = specs.getOrPut(name) { TaskSpec(Json.parse(readText("$name.json")) as JMap) }

    private fun data(inp: JMap) = "<data>" + JsonOut.write(inp.filterKeys { !it.startsWith("_") }).replace("</data>", "<\\/data>") + "</data>"

    fun messages(task: String, inp: JMap, language: String = "en", error: String? = null, previous: String? = null): List<ChatMessage> {
        require(language == "en" || language == "hi") { "language must be en or hi" }
        val t = task(task)
        val shown = t.examples.filter { it.lang == language }.joinToString("\n\n") { "Input: ${data(it.input)}\nOutput: ${JsonOut.write(it.output)}" }
        val user = "${t.instruction.getValue(language)}\nLanguage: ${if (language == "hi") "Hindi" else "English"}\nSchema: ${JsonOut.write(t.schema)}\n\n$shown\n\nInput: ${data(inp)}\nOutput:"
        val msgs = arrayListOf(ChatMessage("system", system), ChatMessage("user", user))
        if (error != null) {
            msgs.add(ChatMessage("assistant", previous ?: ""))
            msgs.add(ChatMessage("user", "That reply was invalid: $error\nReply again with only the corrected JSON object."))
        }
        return msgs
    }

    fun toLlama3(ms: List<ChatMessage>): String =
        "<|begin_of_text|>" + ms.joinToString("") { "<|start_header_id|>${it.role}<|end_header_id|>\n\n${it.content}<|eot_id|>" } + "<|start_header_id|>assistant<|end_header_id|>\n\n"
}

@Suppress("UNCHECKED_CAST")
object Validators {
    /** First balanced JSON object in [text] (models add code fences or chatter). */
    fun extractJson(text: String): JMap {
        val start = text.indexOf('{')
        require(start >= 0) { "no JSON object found in the reply" }
        var depth = 0; var inStr = false; var esc = false
        for (i in start until text.length) {
            val c = text[i]
            if (inStr) { esc = c == '\\' && !esc; if (c == '"' && !esc) inStr = false; continue }
            if (c == '"') inStr = true
            else if (c == '{') depth++
            else if (c == '}') {
                depth--
                if (depth == 0) {
                    val v = try { Json.parse(text.substring(start, i + 1)) } catch (e: Exception) { throw IllegalArgumentException("invalid JSON: ${e.message}") }
                    return v as? JMap ?: throw IllegalArgumentException("reply is not a JSON object")
                }
            }
        }
        throw IllegalArgumentException("JSON object is not closed (reply was cut off)")
    }

    fun norm(s: String) = s.trim().lowercase().replace(Regex("\\s+"), " ")

    /** True if [needle] appears in [haystack] as whole tokens (so "1" does not match "10"). */
    fun containsText(haystack: String, needle: String): Boolean {
        val n = norm(needle)
        if (n.isEmpty()) return false
        return Regex("(?<![\\w.])" + Regex.escape(n) + "(?!\\w)").containsMatchIn(norm(haystack))
    }

    private fun words(s: String) = s.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    private val ID = Regex("^[a-z][a-z0-9_]*$")

    fun checkGraph(c: JMap, maxSkills: Int = 60): List<String> {
        val e = ArrayList<String>()
        val skills = c["skills"] as List<JMap>
        val ids = skills.map { it["id"] as String }
        if (ids.size > maxSkills) e.add("at most $maxSkills skills allowed, got ${ids.size}")
        if (ids.toSet().size != ids.size) e.add("skill ids must be unique")
        ids.filter { !ID.matches(it) }.forEach { e.add("id '$it' must be lowercase snake_case") }
        val deps = ids.associateWith { HashSet<String>() }
        for (p in c["prerequisites"] as List<JMap>) {
            val a = p["skill"] as String; val b = p["requires"] as String
            when { a !in deps || b !in deps -> e.add("prerequisite $a -> $b uses an unknown skill id"); a == b -> e.add("skill '$a' cannot require itself"); else -> deps.getValue(a).add(b) }
        }
        if (e.isEmpty()) {
            val state = HashMap<String, Int>()
            fun dfs(n: String): Boolean {
                state[n] = 1
                for (m in deps.getValue(n)) { if (state[m] == 1) return true; if (m !in state && dfs(m)) return true }
                state[n] = 2; return false
            }
            if (deps.keys.any { it !in state && dfs(it) }) e.add("prerequisites contain a cycle; the graph must be acyclic")
        }
        return e
    }

    fun semantic(task: String, inp: JMap, out: JMap): List<String> {
        val e = ArrayList<String>()
        when (task) {
            "goal_parse" -> if (out["domain_id"] !in (inp["domains"] as? List<*> ?: emptyList<Any>())) e.add("domain_id '${out["domain_id"]}' is not one of ${inp["domains"]}")
            "plan_narrate" -> {
                val want = (inp["items"] as List<JMap>).map { it["id"] }; val got = (out["items"] as List<JMap>).map { it["id"] }
                if (got != want) e.add("items must be exactly $want in this order, got $got")
            }
            "explain" -> {
                if (out["skill_id"] != inp["skill_id"]) e.add("skill_id must be '${inp["skill_id"]}'")
                val name = inp["skill_name"] as? String ?: ""
                val tail = (inp["skill_id"] as String).substringAfterLast('.').replace('_', ' ')
                val ex = out["explanation"] as String
                if (!(containsText(ex, name) || containsText(ex, tail))) e.add("explanation must mention the skill '$name'")
                if (words(ex) > 130) e.add("explanation is too long (max 120 words)")
                val analogy = (out["analogy"] as String).isNotBlank()
                if (inp["analogy"] == true && !analogy) e.add("analogy is required because analogy is true")
                if (inp["analogy"] != true && analogy) e.add("analogy must be an empty string because analogy is false")
            }
            "hint" -> {
                val level = (inp["level"] as Number).toInt(); val correct = inp["_correct_text"] as? String; val h = out["hint"] as String
                if (level !in 0..3) return listOf("level must be 0 to 3")
                if (correct != null) {
                    val leaked = containsText(h, correct)
                    if (level <= 1 && leaked) e.add("hint must not contain the correct option text at level $level")
                    if (level == 3 && !leaked) e.add("level 3 must state the correct answer")
                }
                if (level <= 1 && Regex("\\b(the )?(correct )?answer is\\b|सही उत्तर", RegexOption.IGNORE_CASE).containsMatchIn(h)) e.add("hint must not announce the answer at level $level")
            }
            "draft_question" -> {
                val opts = out["options"] as List<JMap>
                val texts = opts.map { norm(it["text"] as String) }
                if (texts.toSet().size != texts.size) e.add("options must be unique")
                val nulls = opts.indices.filter { opts[it]["misconception"] == null }
                if (nulls != listOf((out["answer"] as Number).toInt())) e.add("exactly one option (the one at index answer) must have misconception null; every wrong option needs a misconception")
            }
            "teachback_grade" -> {
                if (out["skill_id"] != inp["skill_id"]) e.add("skill_id must be '${inp["skill_id"]}'")
                if ((out["feedback"] as String).split(Regex("[.!?।]+")).count { it.isNotBlank() } > 3) e.add("feedback must be two sentences")
            }
            "syllabus_compile" -> e += checkGraph(out)
        }
        return e
    }

    fun validate(lib: PromptLibrary, task: String, inp: JMap, out: JMap): List<String> {
        val errs = SchemaCheck.validate(out, lib.task(task).schema)
        return if (errs.isNotEmpty()) errs else semantic(task, inp, out)
    }
}

/** Deterministic fallbacks (PRD 10.4 step 3). null means: ask the learner a simpler quick-reply question, or skip the optional feature. */
@Suppress("UNCHECKED_CAST")
object Fallbacks {
    private val HINT = mapOf(
        "en" to listOf("Re-read the question slowly and say in your own words what it is asking.",
            "Look at the key idea in your notes for this topic and test each option against it.",
            "Work the first step on paper, then rule out the options that break that step.", "The correct answer is: %s."),
        "hi" to listOf("प्रश्न को धीरे से दोबारा पढ़ें और अपने शब्दों में बताएँ कि वह क्या पूछ रहा है।",
            "इस विषय के नोट्स का मुख्य विचार देखें और हर विकल्प को उससे जाँचें।",
            "पहला चरण कागज़ पर हल करें, फिर जो विकल्प उसे तोड़ते हैं उन्हें हटाएँ।", "सही उत्तर है: %s।"))
    private val CHECK = mapOf("en" to "In one sentence, how would you explain %s to a friend?", "hi" to "एक वाक्य में बताएँ कि आप %s अपने मित्र को कैसे समझाएँगे?")

    fun make(task: String, inp: JMap, lang: String): JMap? = when (task) {
        "goal_parse" -> {
            val hits = (inp["domains"] as List<String>).filter { (inp["goal"] as String).lowercase().contains(it.lowercase()) }
            if (hits.size != 1) null else {
                val m = Regex("(\\d+)\\s*(week|wk|हफ्ते|सप्ताह|month|महीने)").find((inp["goal"] as String).lowercase())
                val weeks = if (m == null) 8 else m.groupValues[1].toInt() * (if (m.groupValues[2] == "month" || m.groupValues[2] == "महीने") 4 else 1)
                mapOf("goal" to (inp["goal"] as String).take(200), "domain_id" to hits[0], "target_level" to "intermediate", "timeframe_weeks" to weeks.coerceIn(1, 52))
            }
        }
        "plan_narrate" -> mapOf("summary" to (if (lang == "hi") "यह रही आपकी योजना, एक-एक कदम करके।" else "Here is your plan, one step at a time."),
            "items" to (inp["items"] as List<JMap>).map { i ->
                val mins = (i["minutes"] as Number).toInt()
                mapOf("id" to i["id"], "text" to (if (lang == "hi") "$mins मिनट: ${i["skill"]} (${i["kind"]})।" else "$mins min: ${i["skill"]} (${i["kind"]}).").take(160))
            })
        "explain" -> {
            val sentences = Regex("(?<=[.।])\\s+").split(inp["notes"] as? String ?: "").map { it.trim() }.filter { it.isNotEmpty() }.take(3)
            val name = (inp["skill_name"] as? String) ?: (inp["skill_id"] as String).substringAfterLast('.')
            mapOf("skill_id" to inp["skill_id"], "explanation" to "$name: ${sentences.joinToString(" ")}".trim().take(900), "analogy" to "", "check_question" to CHECK.getValue(lang).format(name))
        }
        "hint" -> {
            val level = (inp["level"] as Number).toInt(); val correct = inp["_correct_text"] as? String
            mapOf("hint" to if (level == 3 && correct != null) HINT.getValue(lang)[3].format(correct) else if (level == 3) HINT.getValue(lang)[2] else HINT.getValue(lang)[level])
        }
        "draft_question" -> null
        "teachback_grade" -> {
            val notes = Regex("\\w{5,}").findAll((inp["notes"] as? String ?: "").lowercase()).map { it.value }.toSet()
            val text = inp["learner_text"] as String
            val got = Regex("\\w{5,}").findAll(text.lowercase()).map { it.value }.toSet()
            val cover = (notes intersect got).size.toDouble() / maxOf(1, notes.size)
            val comp = if (cover > 0.4) 3 else if (cover > 0.2) 2 else if (cover > 0.05) 1 else 0
            val n = text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
            val clar = if (n < 4) 0 else if (n < 10) 1 else if (n < 80) 2 else 3
            val ex = if (Regex("\\b(for example|for instance|such as|like)\\b|उदाहरण|जैसे", RegexOption.IGNORE_CASE).containsMatchIn(text)) 2 else 0
            mapOf("task" to "teachback_grade", "skill_id" to inp["skill_id"], "scores" to mapOf("accuracy" to comp, "completeness" to comp, "clarity" to clar, "example" to ex),
                "feedback" to if (lang == "hi") "हम इसे विस्तार से नहीं जाँच सके। नोट्स की बात और एक वास्तविक उदाहरण जोड़ें।" else "We could not review this in detail. Add what the notes say and one real example.")
        }
        "syllabus_compile" -> {
            val skills = ArrayList<JMap>(); val seen = HashSet<String>()
            for (line in (inp["ocr_text"] as String).split(Regex("[\\n.;]+"))) {
                var name = line.trim().replace(Regex("^\\s*(?i:unit|chapter|topic|इकाई)?\\s*\\d*[).:\\-]*\\s*"), "").replace(Regex("\\(.*?\\)"), "").trim()
                val sid = name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
                if (name.isEmpty() || !Regex("^[a-z]").containsMatchIn(sid) || sid in seen || skills.size >= 60) continue
                seen.add(sid); skills.add(mapOf("id" to sid.take(40), "name" to name.take(80), "hours" to 3))
            }
            if (skills.isEmpty()) null else mapOf("skills" to skills, "prerequisites" to emptyList<JMap>())
        }
        else -> throw IllegalArgumentException("unknown task $task")
    }
}

data class LlmResult(val task: String, val output: JMap?, val source: String, val attempts: Int, val errors: List<String>)

/** Per-task validity log for the report (issue A08): first-try validity, validity after retry, fallback rate. */
class LlmStats {
    class Row(var calls: Int = 0, var firstTry: Int = 0, var afterRetry: Int = 0, var fallback: Int = 0)
    val rows = LinkedHashMap<String, Row>()
    fun record(r: LlmResult) {
        val s = rows.getOrPut(r.task) { Row() }
        s.calls++
        when (r.source) { "model" -> s.firstTry++; "retry" -> s.afterRetry++; else -> s.fallback++ }
    }
    fun validityAfterRetry(task: String): Double = rows[task]?.let { (it.firstTry + it.afterRetry).toDouble() / it.calls } ?: 0.0
    fun toJsonl(): String = rows.entries.joinToString("\n") { (k, s) ->
        JsonOut.write(mapOf("task" to k, "calls" to s.calls, "first_try" to s.firstTry, "after_retry" to s.afterRetry, "fallback" to s.fallback))
    }
}

/**
 * Validate the output against the task schema and validator, retry up to [maxRetries] times appending the exact validation
 * error, then use a deterministic fallback. The learner never sees a parse error: [LlmResult.output] is valid or null.
 * [generate] receives the Llama-3 formatted prompt, the temperature and the token limit; exceptions count as a failed attempt.
 */
class LlmOrchestrator(
    private val lib: PromptLibrary,
    private val generate: (prompt: String, temperature: Double, maxTokens: Int) -> String,
    val stats: LlmStats = LlmStats(),
    private val maxRetries: Int = 2,
) {
    fun run(task: String, inp: JMap, language: String = "en", maxTokensCap: Int = Int.MAX_VALUE): LlmResult {
        val spec = lib.task(task)
        val errors = ArrayList<String>()
        var error: String? = null; var previous: String? = null
        for (attempt in 1..maxRetries + 1) {
            val prompt = lib.toLlama3(lib.messages(task, inp, language, error, previous))
            val raw = try { generate(prompt, spec.temperature, minOf(spec.maxTokens, maxTokensCap)) } catch (e: Exception) {
                error = "generation failed: ${e.message}"; errors.add(error); previous = ""; continue
            }
            var out: JMap? = null
            val errs = try { out = Validators.extractJson(raw); Validators.validate(lib, task, inp, out) } catch (e: IllegalArgumentException) { listOf(e.message ?: "invalid reply") }
            if (errs.isEmpty()) {
                val r = LlmResult(task, out, if (attempt == 1) "model" else "retry", attempt, errors); stats.record(r); return r
            }
            error = errs.take(3).joinToString("; "); previous = raw; errors.add(error)
        }
        val r = LlmResult(task, Fallbacks.make(task, inp, language), "fallback", maxRetries + 1, errors); stats.record(r); return r
    }
}
