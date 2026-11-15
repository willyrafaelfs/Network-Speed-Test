package io.github.willyrafaelfs.speedtest.data.remote

import java.io.IOException
import okhttp3.Response

/** Server menjawab dengan status HTTP gagal. */
class HttpStatusException(val code: Int) : IOException("HTTP $code")

/** Melempar [HttpStatusException] bila response bukan 2xx. */
internal fun Response.requireSuccess() {
    if (!isSuccessful) throw HttpStatusException(code)
}
