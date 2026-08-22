package com.watchvault.data.urlimport

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Real network fetcher backing [UrlImportPipeline]. Cleartext traffic is disabled app-wide
 *  (see AndroidManifest), so this only ever succeeds against https:// URLs. */
class OkHttpUrlFetcher(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) : UrlFetcher {

    override suspend fun fetch(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android) WatchVault/1.0 (+personal offline-first app)")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} fetching $url")
            }
            response.body?.string() ?: throw IOException("Empty response body from $url")
        }
    }
}
