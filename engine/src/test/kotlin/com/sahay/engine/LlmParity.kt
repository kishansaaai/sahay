package com.sahay.engine

import java.io.File
import java.security.MessageDigest

/** Prints the same lines as llm/parity_python.py so CI can check the Kotlin prompt builder matches the Python one. Arg: content dir. */
fun main(args: Array<String>) {
    val lib = PromptLibrary { File(args[0], "prompts/$it").readText() }
    fun h(s: String) = MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }.take(16)
    for (t in Tasks.ALL) for ((i, e) in lib.task(t).examples.withIndex()) {
        val a = lib.toLlama3(lib.messages(t, e.input, e.lang))
        val b = lib.toLlama3(lib.messages(t, e.input, e.lang, "some error", "bad reply"))
        println("$t $i ${h(a)} ${h(b)}")
    }
}
