package com.ov3rk1ll.kinocast.api.mirror;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.ui.DetailActivity;
import com.ov3rk1ll.kinocast.ui.MainActivity;
import com.ov3rk1ll.kinocast.utils.Utils;

public class MixDrop extends Host {
    private static final String TAG = MixDrop.class.getSimpleName();
    public static final int HOST_ID = 89;

    @Override
    public int getId() {
        return HOST_ID;
    }

    @Override
    public String getName() {
        return MixDrop.class.getSimpleName();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getVideoPath(DetailActivity.QueryPlayTask queryTask) {
        if(TextUtils.isEmpty(url)) return null;

        queryTask.updateProgress(queryTask.getContext().getString(R.string.host_progress_getdatafrom, url));

        Log.d(TAG, "resolve " + url);
        if(url.contains("/e/")){
            String id = url.substring(url.indexOf("/e/") + 3);
            if(id.contains("/")) id = id.substring(0, id.indexOf("/"));
            queryTask.updateProgress(queryTask.getContext().getString(R.string.host_progress_getvideoforid,id));
            url = "https://mixdrop.co/e/" + id;
        }
        else if(url.contains("/embed/")){
            String id = url.substring(url.indexOf("/embed/") + 7);
            if(id.contains("/")) id = id.substring(0, id.indexOf("/"));
            queryTask.updateProgress(queryTask.getContext().getString(R.string.host_progress_getvideoforid,id));
            url = "https://mixdrop.to/e/" + id;
        }

        String link = getLink(url);

        Log.d(TAG, "Request. Got " + link);
        queryTask.updateProgress(queryTask.getContext().getString(R.string.host_progress_1sttry));

        return link;
    }

    private String getLink(final String url){

        final boolean[] requestDone = {false};
        final String[] solvedUrl = {null};

        MainActivity.activity.runOnUiThread(new Runnable() {
            @SuppressLint("SetJavaScriptEnabled")
            @Override
            public void run() {
                // Virtual WebView
                final WebView webView = MainActivity.webView;

                webView.setVisibility(View.GONE);
                webView.getSettings().setUserAgentString(Utils.USER_AGENT);
                webView.getSettings().setJavaScriptEnabled(true);
                webView.setWebViewClient(new WebViewClient() {

                    public void onPageFinished(WebView view, String url) {

                        webView.evaluateJavascript(
                                "(function() { return MDCore.vsrc; })();",
                                new ValueCallback<String>() {
                                    @Override
                                    public void onReceiveValue(String html) {
                                        // code here
                                        html = html.replaceAll("^\"|\"$", "");
                                        Log.d("MDCore.vsrc", html);
                                        solvedUrl[0] = html;
                                        requestDone[0] = true;
                                    }
                                });

                    }

                });
                webView.loadUrl(url);
            }
        });

        int timeout = 50;
        // Wait for the webView to load the correct url
        while (!requestDone[0]){
            SystemClock.sleep(200);
            timeout--;
            if(timeout <= 0)
                break;
        }

        if(Utils.isStringEmpty(solvedUrl[0])) return null;
        return Utils.getUrl(solvedUrl[0]);
    }

    @Override
    public Boolean canHandleUri(Uri uri) {
        return ("mixdrop.co".equalsIgnoreCase(uri.getHost())
                ||  "www.mixdrop.co".equalsIgnoreCase(uri.getHost()));
    }
    @Override
    public void handleUri(Uri uri) {
        setUrl(uri.toString());
    }
}
