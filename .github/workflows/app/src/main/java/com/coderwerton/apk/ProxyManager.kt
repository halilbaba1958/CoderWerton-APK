package com.coderwerton.apk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ProxyServer(val host: String, val port: Int, val ping: Long = 0)

object ProxyManager {

    private val proxySources = listOf(
        "https://raw.githubusercontent.com/TheSpeedX/SOCKS-List/master/http.txt",
        "https://cdn.jsdelivr.net/gh/proxifly/free-proxy-list@main/proxies/all/data.txt",
        "https://raw.githubusercontent.com/nikita29a/FreeProxyList/main/mirror/1.txt",
        "https://cdn.jsdelivr.net/gh/proxifly/free-proxy-list@main/proxies/countries/US/data.txt",
        "https://api.proxyscrape.com/v4/free-proxy-list/get?request=display_proxies&proxy_format=ipport&format=text"
    )

    private val proxyPattern = Pattern.compile(
        "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
        "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?):\\d{2,5}\\b"
    )

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun getBestProxy(): ProxyServer? = withContext(Dispatchers.IO) {
        val proxies = fetchAllProxies()
        if (proxies.isEmpty()) return@withContext null
        return@withContext testProxies(proxies)
    }

    private fun fetchAllProxies(): List<ProxyServer> {
        val all = mutableListOf<ProxyServer>()

        for (url in proxySources) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: continue
                    val found = parseProxies(body)
                    all.addAll(found)
                }
            } catch (e: Exception) {
                continue
            }
        }

        return all.distinctBy { "${it.host}:${it.port}" }
    }

    private fun parseProxies(text: String): List<ProxyServer> {
        val matcher = proxyPattern.matcher(text)
        val result = mutableListOf<ProxyServer>()

        while (matcher.find()) {
            val parts = matcher.group().split(":")
            if (parts.size == 2) {
                try {
                    result.add(ProxyServer(parts[0], parts[1].toInt()))
                } catch (e: Exception) {
                    continue
                }
            }
        }
        return result
    }

    private fun testProxies(proxies: List<ProxyServer>): ProxyServer? {
        val testUrls = listOf(
            "http://httpbin.org/ip",
            "http://ip-api.com/json"
        )

        val working = mutableListOf<ProxyServer>()

        // İlk 100 proxyyi test et
        for (proxy in proxies.take(100)) {
            try {
                val okProxy = Proxy(
                    Proxy.Type.HTTP,
                    InetSocketAddress(proxy.host, proxy.port)
                )
                val testClient = OkHttpClient.Builder()
                    .proxy(okProxy)
                    .connectTimeout(4, TimeUnit.SECONDS)
                    .readTimeout(4, TimeUnit.SECONDS)
                    .build()

                val start = System.currentTimeMillis()
                val request = Request.Builder()
                    .url(testUrls[0])
                    .build()

                val response = testClient.newCall(request).execute()
                val ping = System.currentTimeMillis() - start

                if (response.isSuccessful) {
                    working.add(ProxyServer(proxy.host, proxy.port, ping))
                    if (working.size >= 5) break // 5 tane bulduk yeter
                }
            } catch (e: Exception) {
                continue
            }
        }

        return working.minByOrNull { it.ping }
    }

    fun applyProxy(proxy: ProxyServer) {
        System.setProperty("http.proxyHost", proxy.host)
        System.setProperty("http.proxyPort", proxy.port.toString())
        System.setProperty("https.proxyHost", proxy.host)
        System.setProperty("https.proxyPort", proxy.port.toString())
    }
}
