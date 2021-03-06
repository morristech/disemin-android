package gr.exm.agroxm.data.io

import com.squareup.moshi.Moshi
import gr.exm.agroxm.data.AuthToken
import gr.exm.agroxm.util.AuthHelper
import gr.exm.agroxm.util.AuthHelper.NO_AUTH_HEADER
import gr.exm.agroxm.util.AuthHelper.signedRequest
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber


/**
 * {@see okhttp3.Interceptor} that adds authorization header
 */
class AuthTokenInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // Original request
        val request: Request = chain.request()

        // Add Bearer token header in all requests, except some /auth paths
        if (request.header(NO_AUTH_HEADER).equals("true")) {
            Timber.d("Skipping auth header for path: ${request.url.encodedPath}")
            // Try to extract auth token from response before proceeding
            return chain.proceed(request).extractAuthToken()
        }

        // Add auth header using the auth token with Bearer schema
        Timber.d("Adding auth header")
        return chain.proceed(request.signedRequest())
    }
}

private fun Response.extractAuthToken(): Response {
    if (this.isSuccessful) {
        try {
            Timber.d("Trying to extract auth token")
            val body = this.peekBody(Long.MAX_VALUE).string()
            val moshi = Moshi.Builder().build()
            val adapter = moshi.adapter(AuthToken::class.java).lenient()
            val authToken = adapter.fromJson(body)
            authToken?.let {
                Timber.d("Got token from response.")
                AuthHelper.setAuthToken(it)
            }
        } catch (e: Exception) {
            Timber.d(e, "Failed to extract auth token")
        }
    }
    return this
}
