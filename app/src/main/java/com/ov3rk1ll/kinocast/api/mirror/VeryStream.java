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

import org.jsoup.nodes.Document;

public class VeryStream extends Host {
    private static final String TAG = VeryStream.class.getSimpleName();
    public static final int HOST_ID = 997;

    @Override
    public int getId() { return HOST_ID; }

    @Override
    public String getName() {
        return "VeryStream";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getVideoPath(DetailActivity.QueryPlayTask queryTask) {
        Log.d(TAG, "GET " + url);

        if (TextUtils.isEmpty(url)) return null;
        try {
            queryTask.updateProgress(url);
            Document doc = Utils.buildJsoup(url)
                    .get();

            String lnk = doc.select("p#videolink").text();
            if (!TextUtils.isEmpty(lnk))
                return "https://verystream.com/gettoken/" + lnk + "?mime=true";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Boolean canHandleUri(Uri uri) {

        if("verystream.com".equalsIgnoreCase(uri.getHost())) {
            if(uri.getPath().contains("stream/")) return true;
        }
        return false;
    }
    @Override
    public void handleUri(Uri uri) {
        setUrl(uri.toString());
    }

}
