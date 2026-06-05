package dev.zxkuhl.rms;

import android.app.Activity;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

public class RMSNative {
    private final WebView wv;
    private final Activity activity;

    public RMSNative(Activity activity, WebView wv) {
        this.activity = activity;
        this.wv = wv;
    }

    @JavascriptInterface
    public void goBack() {
        activity.runOnUiThread(() -> {
            if (wv.canGoBack())
                wv.goBack();
        });
    }
}
