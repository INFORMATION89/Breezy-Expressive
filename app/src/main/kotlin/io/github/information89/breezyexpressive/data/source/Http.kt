package io.github.information89.breezyexpressive.data.source

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Shared network setup: one client, one JSON config. */
object Network {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    fun client(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
}

class HttpException(val code: Int, message: String) : IOException("HTTP $code: $message")

/** GETs [url] and decodes the JSON body as [T]. Cancellable; throws [IOException] on failure. */
internal suspend inline fun <reified T> OkHttpClient.getJson(json: Json, url: HttpUrl): T {
    val request = Request.Builder().url(url).header("Accept", "application/json").build()
    val body = newCall(request).awaitBody()
    return json.decodeFromString<T>(body)
}

/** Runs the call on OkHttp's thread pool; cancelling the coroutine cancels the request. */
internal suspend fun Call.awaitBody(): String = suspendCancellableCoroutine { cont ->
    cont.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (!cont.isCancelled) cont.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                try {
                    val body = it.body.string()
                    if (it.isSuccessful) cont.resume(body) else cont.resumeWithException(HttpException(it.code, body.take(200)))
                } catch (e: IOException) {
                    cont.resumeWithException(e)
                }
            }
        }
    })
}
