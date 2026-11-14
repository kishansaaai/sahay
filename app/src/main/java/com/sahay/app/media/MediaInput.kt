package com.sahay.app.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.io.File

object MediaInput {
    fun cameraUri(context: Context): Uri {
        val dir = File(context.cacheDir, "sahay").apply { mkdirs() }
        return FileProvider.getUriForFile(context, "${context.packageName}.files", File(dir, "capture.jpg"))
    }
    // Bound decoding memory even when the user selects a very large photo.
    fun bitmap(context: Context, uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "cannot read image" }
        val options = BitmapFactory.Options().apply { inSampleSize = 1 }
        while (maxOf(bounds.outWidth, bounds.outHeight) / options.inSampleSize > 2048) options.inSampleSize *= 2
        return context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) } ?: error("cannot decode image")
    }
    fun qr(bitmap: Bitmap): String {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmap.width, bitmap.height, pixels))),
            mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE), DecodeHintType.TRY_HARDER to true)).text
    }
    fun qrImage(text: String): Bitmap {
        val matrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, 900, 900)
        val pixels = IntArray(900 * 900) { i -> if (matrix[i % 900, i / 900]) android.graphics.Color.BLACK else android.graphics.Color.WHITE }
        return Bitmap.createBitmap(pixels, 900, 900, Bitmap.Config.ARGB_8888)
    }
}
