package com.sahay.app.media

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MediaInputTest {
    @Test fun qrBitmapRoundTripsWithOfflineDecoder() {
        val payload = "sahayqr/1|" + "a".repeat(64) + "|0|1|0011AABB"
        val bitmap = MediaInput.qrImage(payload)
        try { assertEquals(payload, MediaInput.qr(bitmap)) } finally { bitmap.recycle() }
    }
    @Test fun imageDecodingStartsAtValidSampleSize() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val file = File(context.cacheDir, "sample.png")
        val source = Bitmap.createBitmap(32, 16, Bitmap.Config.ARGB_8888)
        file.outputStream().use { source.compress(Bitmap.CompressFormat.PNG, 100, it) }
        source.recycle()
        val decoded = MediaInput.bitmap(context, Uri.fromFile(file))
        try { assertEquals(32, decoded.width); assertEquals(16, decoded.height) } finally { decoded.recycle(); file.delete() }
    }
}
