package com.ov3rk1ll.kinocast.api.mirror;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.ui.DetailActivity;
import com.ov3rk1ll.kinocast.ui.MainActivity;
import com.ov3rk1ll.kinocast.utils.Utils;

import java.io.ByteArrayInputStream;

public class VevIo extends Host {
    private static final String TAG = VevIo.class.getSimpleName();
    public static final int HOST_ID = 996;


    @Override
    public int getId() {
        return HOST_ID;
    }

    @Override
    public String getName() {
        return "vev.io";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getVideoPath(DetailActivity.QueryPlayTask queryTask) {
        if(TextUtils.isEmpty(url)) return null;

        queryTask.updateProgress(queryTask.getContext().getString(R.string.host_progress_getdatafrom, url));
        String link = getLink(url);
        Log.d(TAG, "Request. Got " + link);

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

                    @Override
                    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                        if(url.contains("&vct=") && url.contains(".mp4?k")){
                            solvedUrl[0] = url;
                            requestDone[0] = true;
                        }
                        return Uri.parse(url).getHost().contains("vev.io") ? null: new WebResourceResponse("text/html", "", new ByteArrayInputStream(new byte[0]));
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

        Log.d("URL", solvedUrl[0]);
        if(Utils.isStringEmpty(solvedUrl[0])) return null;

        return solvedUrl[0];

    }

    @Override
    public Boolean canHandleUri(Uri uri) {

        if("vev.io".equalsIgnoreCase(uri.getHost())) return true;

        return false;
    }
    @Override
    public void handleUri(Uri uri) {
        setUrl(uri.toString());
    }

}
