package com.coderwerton.apk

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var navBar: LinearLayout

    private val sites = mapOf(
    "Instagram" to "https://www.instagram.com",
    "TikTok" to "https://www.tiktok.com",
    "YouTube" to "https://www.youtube.com",
    "X" to "https://twitter.com",
    "Facebook" to "https://www.facebook.com",
    "Snapchat" to "https://web.snapchat.com",
    "Pinterest" to "https://www.pinterest.com"
)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)
        navBar = findViewById(R.id.navBar)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        }

        webView.webViewClient = WebViewClient()

        // Proxy bul, sonra yükle
        progressBar.visibility = View.VISIBLE
        navBar.visibility = View.GONE
        statusText.text = "Bağlantı aranıyor..."

        lifecycleScope.launch {
            val proxy = ProxyManager.getBestProxy()
            if (proxy != null) {
                ProxyManager.applyProxy(proxy)
                statusText.text = "Bağlandı: ${proxy.host} (${proxy.ping}ms)"
            } else {
                statusText.text = "Direkt bağlantı kullanılıyor"
            }

            progressBar.visibility = View.GONE
            navBar.visibility = View.VISIBLE
            webView.loadUrl(sites["Instagram"]!!)
        }

        // Nav butonları
        findViewById<Button>(R.id.btnInstagram).setOnClickListener {
            webView.loadUrl(sites["Instagram"]!!)
        }
        findViewById<Button>(R.id.btnYoutube).setOnClickListener {
            webView.loadUrl(sites["YouTube"]!!)
        }
        findViewById<Button>(R.id.btnFacebook).setOnClickListener {
            webView.loadUrl(sites["Facebook"]!!)
        }
        findViewById<Button>(R.id.btnTwitter).setOnClickListener {
            webView.loadUrl(sites["Twitter"]!!)
        }
        findViewById<Button>(R.id.btnTiktok).setOnClickListener {
            webView.loadUrl(sites["TikTok"]!!)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}
