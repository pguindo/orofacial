package com.pguindo.orofacial

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

/**
 * WebView a pantalla completa que carga la PWA de GitHub Pages.
 * Expone window.Android.guardarDato(json) al JS de index.html.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        const val WEB_URL = "https://pguindo.github.io/orofacial/"
        private const val TAG = "MainActivity"
    }

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.builtInZoomControls = false
            settings.displayZoomControls = false

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    // Mantener la navegación dentro del WebView (misma web).
                    return false
                }
            }
            webChromeClient = WebChromeClient()

            // Puente JS -> nativo. El JS llama: Android.guardarDato(JSON.stringify({...}))
            addJavascriptInterface(JsBridge(this@MainActivity), "Android")
        }

        setContentView(webView)

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl(WEB_URL)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (::webView.isInitialized && webView.canGoBack()) webView.goBack()
                else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::webView.isInitialized) webView.saveState(outState)
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) webView.onResume()
    }

    override fun onPause() {
        if (::webView.isInitialized) webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) webView.destroy()
        super.onDestroy()
    }

    /** Puente llamado desde el JS de la web. Se ejecuta en hilo no-UI: no tocar vistas aquí. */
    class JsBridge(private val context: Context) {

        @JavascriptInterface
        fun guardarDato(json: String) {
            Log.d(TAG, "guardarDato: $json")
            val ok = WidgetData.saveFromJson(context.applicationContext, json)
            Log.d(TAG, "guardarDato guardado=$ok")
            // Avisar al widget aunque el JSON viniera vacío: recalcula con la hora actual.
            OrofacialWidgetProvider.requestUpdate(context.applicationContext)
        }

        companion object {
            private const val TAG = "JsBridge"
        }
    }
}
