package io.github.willyrafaelfs.speedtest.presentation.detail

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Menyimpan gambar kartu hasil ke cache aplikasi lalu membuka Share sheet.
 *
 * File dibagikan lewat [FileProvider] (content:// dengan izin baca sementara), bukan
 * path file mentah, sehingga aplikasi penerima tidak butuh izin storage apa pun.
 */
internal object ResultImageSharer {

    suspend fun share(context: Context, bitmap: Bitmap, chooserTitle: String) {
        val file = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
            // Nama tetap: gambar lama ditimpa, cache tidak menumpuk.
            File(dir, FILE_NAME).also { file ->
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, chooserTitle))
    }

    private const val SHARE_DIR = "shared"
    private const val FILE_NAME = "netra-result.png"
    private const val PNG_QUALITY = 100
}
