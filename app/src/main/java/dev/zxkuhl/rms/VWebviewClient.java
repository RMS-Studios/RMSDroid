package dev.zxkuhl.rms;

import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.webkit.*;
import androidx.annotation.Nullable;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class VWebviewClient extends WebViewClient {
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        var url = request.getUrl();
        if ("discord.com".equals(url.getAuthority()) || "about:blank".equals(url.toString())) {
            return false;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, url);
        view.getContext().startActivity(intent);
        return true;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        view.evaluateJavascript(HttpClient.rmsRuntime, null);
        view.evaluateJavascript(HttpClient.rmsMobileRuntime, null);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        // Inject browser.css directly since DOMContentLoaded fires before onPageStarted injection
        view.evaluateJavascript(
            "(function() {" +
                "if (!document.getElementById('rms-browser-css')) {" +
                    "var link = document.createElement('link');" +
                    "link.id = 'rms-browser-css';" +
                    "link.rel = 'stylesheet';" +
                    "link.type = 'text/css';" +
                    "link.href = 'https://github.com/zxkuhl/RMS/releases/download/browser/browser.css';" +
                    "document.documentElement.appendChild(link);" +
                "}" +
            "})()", null);
        // Inject SVG size fix
        view.evaluateJavascript(
            "(function() {" +
                "if (!document.getElementById('rms-svg-fix')) {" +
                    "var style = document.createElement('style');" +
                    "style.id = 'rms-svg-fix';" +
                    "style.textContent = 'svg { max-width: 100%; max-height: 100%; } [class*=\"icon\"] svg, [class*=\"button\"] svg, [class*=\"control\"] svg { width: 16px !important; height: 16px !important; }';" +
                    "document.head.appendChild(style);" +
                "}" +
            "})()", null);
        // Patch openEditor to show a simple inline textarea instead of a popup window,
        // which doesn't work on Android WebView
        view.evaluateJavascript(
            "(function() {" +
                "if (!window.RMSNative) return;" +
                "window.RMSNative.quickCss.openEditor = function() {" +
                    "return window.RMSNative.quickCss.get().then(function(css) {" +
                        "var overlay = document.createElement('div');" +
                        "overlay.style.cssText = 'position:fixed;top:0;left:0;right:0;bottom:0;background:#1e1e1e;z-index:2147483647;display:flex;flex-direction:column;';" +
                        "var bar = document.createElement('div');" +
                        "bar.style.cssText = 'background:#252526;padding:8px 12px;display:flex;align-items:center;gap:8px;border-bottom:1px solid #333;';" +
                        "var title = document.createElement('span');" +
                        "title.textContent = 'QuickCSS Editor';" +
                        "title.style.cssText = 'color:#ccc;font-family:sans-serif;font-size:14px;flex:1;';" +
                        "var saveBtn = document.createElement('button');" +
                        "saveBtn.textContent = 'Save';" +
                        "saveBtn.style.cssText = 'background:#0078d4;color:#fff;border:none;padding:6px 14px;border-radius:3px;cursor:pointer;font-size:13px;margin-right:6px;';" +
                        "var closeBtn = document.createElement('button');" +
                        "closeBtn.textContent = 'Close';" +
                        "closeBtn.style.cssText = 'background:#555;color:#fff;border:none;padding:6px 14px;border-radius:3px;cursor:pointer;font-size:13px;';" +
                        "bar.appendChild(title);" +
                        "bar.appendChild(saveBtn);" +
                        "bar.appendChild(closeBtn);" +
                        "var ta = document.createElement('textarea');" +
                        "ta.value = css || '';" +
                        "ta.placeholder = '/* Enter your custom CSS here */';" +
                        "ta.style.cssText = 'flex:1;background:#1e1e1e;color:#d4d4d4;border:none;padding:12px;font-size:13px;resize:none;outline:none;font-family:monospace;line-height:1.5;';" +
                        "var remove = function() { if (overlay.parentNode) overlay.parentNode.removeChild(overlay); };" +
                        "saveBtn.onclick = function() { window.RMSNative.quickCss.set(ta.value).then(remove); };" +
                        "closeBtn.onclick = remove;" +
                        "overlay.appendChild(bar);" +
                        "overlay.appendChild(ta);" +
                        "document.body.appendChild(overlay);" +
                        "ta.focus();" +
                    "});" +
                "};" +
            "})()", null);
        view.setVisibility(View.VISIBLE);
        super.onPageFinished(view, url);
    }

    @Nullable
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
        var uri = req.getUrl();
        if (req.isForMainFrame() || req.getUrl().getPath().endsWith(".css")) {
            try {
                return doFetch(req);
            } catch (IOException ex) {
                Logger.e("Error during shouldInterceptRequest", ex);
            }
        }
        return null;
    }

    private WebResourceResponse doFetch(WebResourceRequest req) throws IOException {
        var url = req.getUrl().toString();
        var conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(req.getMethod());
        for (var h : req.getRequestHeaders().entrySet()) {
            conn.setRequestProperty(h.getKey(), h.getValue());
        }
        var code = conn.getResponseCode();
        var msg = conn.getResponseMessage();
        var headers = conn.getHeaderFields();
        var modifiedHeaders = new HashMap<String, String>(headers.size());
        for (var header : headers.entrySet()) {
            if (!"Content-Security-Policy".equalsIgnoreCase(header.getKey())) {
                modifiedHeaders.put(header.getKey(), header.getValue().get(0));
            }
        }
        if (url.endsWith(".css")) modifiedHeaders.put("Content-Type", "text/css");
        return new WebResourceResponse(modifiedHeaders.getOrDefault("Content-Type", "application/octet-stream"), "utf-8", code, msg, modifiedHeaders, conn.getInputStream());
    }
}
