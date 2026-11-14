package com.sahay.engine

import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * Lossless snapshots packed into a share code (issue B17/X3).
 *
 * The whole snapshot is written with the same JSON codec the on-device store uses, compressed with raw deflate and
 * encoded as unpadded base64url, so a roadmap can go phones without a network, a server or a second file format. The
 * bytes are small enough to fit in a QR code for a typical plan; [qrFits] lets the UI refuse rather than draw an
 * unreadable code. This is the same payload the teacher heatmap decodes, which keeps one format for both features.
 */
object ShareCode {
    /** Versioned prefix so an old build can reject a code it does not understand instead of guessing. */
    const val PREFIX = "SAHAY1."

    /**
     * Practical ceiling for one QR symbol at error correction L, version 40 is 2953 bytes of byte-mode data. Staying
     * below 2900 leaves room for the ASCII prefix and keeps the symbol scannable from a phone screen.
     */
    const val QR_MAX_BYTES = 2900

    class InvalidShareCode(message: String) : IllegalArgumentException(message)

    fun encode(snapshot: Snapshot): String {
        val json = snapshot.toJson().toByteArray(Charsets.UTF_8)
        return PREFIX + Base64Url.encode(deflate(json))
    }

    /** Decode a share code into a snapshot. Throws [InvalidShareCode] with a readable reason on any damage. */
    fun decode(text: String): Snapshot {
        if (text.length > 3_000_000) throw InvalidShareCode("share code is too large")
        val trimmed = text.trim()
        if (!trimmed.startsWith(PREFIX)) throw InvalidShareCode("not a Sahay share code")
        val packed = try {
            Base64Url.decode(trimmed.substring(PREFIX.length))
        } catch (e: IllegalArgumentException) {
            throw InvalidShareCode("share code is not valid base64url text")
        }
        val json = try {
            inflate(packed).toString(Charsets.UTF_8)
        } catch (e: Exception) {
            throw InvalidShareCode("share code could not be unpacked")
        }
        return try {
            Snapshot.fromJson(json)
        } catch (e: Exception) {
            throw InvalidShareCode(e.message ?: "share code does not contain a snapshot")
        }
    }

    /** True when one QR symbol can carry [text] without exceeding version 40. */
    fun qrFits(text: String): Boolean = text.toByteArray(Charsets.UTF_8).size <= QR_MAX_BYTES

    private fun deflate(data: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        try {
            deflater.setInput(data)
            deflater.finish()
            val out = java.io.ByteArrayOutputStream()
            val buf = ByteArray(4096)
            while (!deflater.finished()) {
                val n = deflater.deflate(buf)
                out.write(buf, 0, n)
            }
            return out.toByteArray()
        } finally {
            deflater.end()
        }
    }

    private fun inflate(data: ByteArray): ByteArray {
        val inflater = Inflater(true)
        try {
            inflater.setInput(data)
            val out = java.io.ByteArrayOutputStream()
            val buf = ByteArray(4096)
            while (!inflater.finished()) {
                val n = inflater.inflate(buf)
                if (out.size() + n > 2_000_000) throw InvalidShareCode("expanded snapshot is too large")
                if (n == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) throw IllegalStateException("truncated")
                } else {
                    out.write(buf, 0, n)
                }
            }
            return out.toByteArray()
        } finally {
            inflater.end()
        }
    }
}

/** Per-skill line in the teacher heatmap (issue B17). [students] is how many of the merged snapshots include the skill. */
data class SkillHeat(val skillId: String, val name: String, val students: Int, val meanMastery: Double)

/**
 * Merge several learner snapshots into per-skill mean mastery. Each student's mastery is rebuilt by replaying that
 * student's own attempt log against the same content pack the teacher phone has, so no extra numbers travel in the code.
 * Skills outside a student's target closure are not counted as zero; they are simply not that student's skill yet.
 */
fun heatmap(content: Content, snapshots: List<Snapshot>): List<SkillHeat> {
    val perSkill = HashMap<String, MutableList<Double>>()
    for (snap in snapshots) {
        val targets = snap.targets.filter { it in content.graph.skills }
        if (targets.isEmpty()) continue
        val ids = content.graph.closure(targets)
        val state = replay(content, targets, snap.attempts)
        val now = snap.attempts.maxOfOrNull { it.day } ?: (snap.profile?.createdAt ?: 0.0)
        for (id in ids) perSkill.getOrPut(id) { ArrayList() }.add(state.model.pEff(id, now))
    }
    return perSkill.entries
        .map { (id, values) ->
            SkillHeat(
                skillId = id,
                name = content.graph.skills[id]?.name ?: id,
                students = values.size,
                meanMastery = values.average(),
            )
        }
        .sortedWith(compareByDescending<SkillHeat> { it.meanMastery }.thenBy { it.name })
}

/**
 * Unpadded base64url (RFC 4648 section 5), written here because `java.util.Base64` needs Android API 26 and the app
 * supports API 24. No padding keeps the share code compact and copy-friendly. Public only so the plain-kotlinc test
 * runner, which compiles as a separate module, can check the RFC vector.
 */
object Base64Url {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    private val VALUES = IntArray(128) { -1 }.also { table ->
        ALPHABET.forEachIndexed { i, c -> table[c.code] = i }
    }

    fun encode(data: ByteArray): String {
        val sb = StringBuilder((data.size * 4 + 2) / 3)
        var i = 0
        while (i < data.size) {
            val b0 = data[i].toInt() and 0xFF
            val b1 = if (i + 1 < data.size) data[i + 1].toInt() and 0xFF else -1
            val b2 = if (i + 2 < data.size) data[i + 2].toInt() and 0xFF else -1
            sb.append(ALPHABET[b0 shr 2])
            sb.append(ALPHABET[((b0 and 0x03) shl 4) or (if (b1 >= 0) b1 shr 4 else 0)])
            if (b1 >= 0) sb.append(ALPHABET[((b1 and 0x0F) shl 2) or (if (b2 >= 0) b2 shr 6 else 0)])
            if (b2 >= 0) sb.append(ALPHABET[b2 and 0x3F])
            i += 3
        }
        return sb.toString()
    }

    fun decode(text: String): ByteArray {
        if (text.length % 4 == 1) throw IllegalArgumentException("bad base64url length")
        val out = java.io.ByteArrayOutputStream(text.length * 3 / 4)
        var buffer = 0
        var bits = 0
        for (c in text) {
            val v = if (c.code < 128) VALUES[c.code] else -1
            if (v < 0) throw IllegalArgumentException("bad base64url character")
            buffer = (buffer shl 6) or v
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out.write((buffer shr bits) and 0xFF)
            }
        }
        return out.toByteArray()
    }
}
