# Keep JavascriptInterface methods (called from WebView JS).
-keepclassmembers class com.pguindo.orofacial.MainActivity$JsBridge {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
