package com.sahay.engine

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** Multipart QR transport: the complete snapshot is compressed, checksummed and split without truncating the plan. */
object PlanSharing {
    private const val PREFIX = "sahayqr/1"
    const val MAX_BYTES = 2_000_000
    private const val MAX_PARTS = 512
    private const val CHUNK = 700
    private fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    fun encode(snapshot: Snapshot): List<String> {
        val bytes = snapshot.toJson().toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_BYTES) { "snapshot is too large for QR; use JSON export" }
        val packed = ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(bytes) } }.toByteArray()
        val text = packed.joinToString("") { "%02X".format(it) }
        val parts = text.chunked(CHUNK)
        require(parts.size <= MAX_PARTS) { "snapshot needs too many QR pages; use JSON export" }
        val hash = digest(bytes)
        return parts.mapIndexed { index, part -> "$PREFIX|$hash|$index|${parts.size}|$part" }
    }

    class Collector {
        private var hash: String? = null
        private var count = 0
        private val parts = HashMap<Int, String>()
        val received: Int get() = parts.size
        val total: Int get() = count
        fun add(text: String): Snapshot? {
            require(text.length <= CHUNK + 120) { "QR page is too large" }
            val fields = text.split('|')
            require(fields.size == 5 && fields[0] == PREFIX && fields[1].matches(Regex("[0-9a-f]{64}"))) { "not a Sahay QR page" }
            val index = fields[2].toIntOrNull() ?: error("invalid QR index")
            val n = fields[3].toIntOrNull() ?: error("invalid QR count")
            require(n in 1..MAX_PARTS && index in 0 until n && fields[4].length <= CHUNK) { "invalid QR page" }
            require(hash == null || hash == fields[1] && count == n) { "this page belongs to another transfer; reset first" }
            require(parts[index] == null || parts[index] == fields[4]) { "conflicting QR page" }
            hash = fields[1]; count = n; parts[index] = fields[4]
            if (parts.size != n) return null
            val compressed = (0 until n).joinToString("") { parts.getValue(it) }.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            val bytes = GZIPInputStream(ByteArrayInputStream(compressed)).use { input ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    require(out.size() + read <= MAX_BYTES) { "expanded snapshot is too large" }
                    out.write(buffer, 0, read)
                }
                out.toByteArray()
            }
            require(digest(bytes) == hash) { "snapshot checksum mismatch" }
            return Snapshot.fromJson(bytes.toString(Charsets.UTF_8))
        }
    }
}

data class ClassMastery(val learnerId: String, val domain: String, val values: Map<String, Double>)

/** Teachers explicitly choose a pseudonym; rescanning replaces that learner instead of counting them twice. */
class ClassHeatmap {
    private val learners = linkedMapOf<String, ClassMastery>()
    fun merge(learnerId: String, snapshot: Snapshot, content: Content) {
        require(learnerId.isNotBlank() && learnerId.length <= 80) { "enter a learner pseudonym" }
        require(snapshot.profile != null) { "snapshot has no learner" }
        require(snapshot.validationErrors(content).isEmpty()) { "invalid learner snapshot" }
        val state = replay(content, snapshot.targets, snapshot.attempts)
        val day = snapshot.attempts.maxOfOrNull { it.day } ?: 0.0
        learners[learnerId] = ClassMastery(learnerId, content.domain,
            content.graph.closure(snapshot.targets).sorted().associateWith { state.model.pEff(it, day) })
    }
    fun rows(): List<ClassMastery> = learners.values.toList()
    fun clear() = learners.clear()
}
