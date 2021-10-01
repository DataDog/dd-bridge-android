package com.datadog.android.bridge.internal

import android.util.Log
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

internal class ProxyAuthenticator(
    internal val username: String,
    internal val password: String
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        val proxyAuthorization = response.code() == PROXY_AUTHORIZATION_REQUIRED_STATUS_CODE

        if (!proxyAuthorization) {
            Log.w(
                ProxyAuthenticator::class.java.canonicalName,
                "Unexpected response code=${response.code()}" +
                    " received during proxy authentication request."
            )
            return null
        }

        val challenges = response.challenges()
        for (challenge in challenges) {
            val scheme = challenge.scheme()
            if ("Basic".equals(scheme, ignoreCase = true) ||
                "OkHttp-Preemptive".equals(scheme, ignoreCase = true)
            ) {
                val credential = Credentials.basic(
                    username,
                    password,
                    challenge.charset()
                )
                return response.request().newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build()
            }
        }

        Log.w(
            ProxyAuthenticator::class.java.canonicalName,
            "No known challenges are satisfied during proxy authentication request."
        )

        return null
    }

    private companion object {
        const val PROXY_AUTHORIZATION_REQUIRED_STATUS_CODE = 407
    }
}
