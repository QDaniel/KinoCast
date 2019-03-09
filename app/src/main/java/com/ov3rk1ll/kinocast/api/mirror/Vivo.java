package com.ov3rk1ll.kinocast.api.mirror;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.ov3rk1ll.kinocast.ui.DetailActivity;
import com.ov3rk1ll.kinocast.utils.Utils;

import org.jsoup.nodes.Document;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Vivo extends Host {
    private static final String TAG = Vivo.class.getSimpleName();
    public static final int HOST_ID = 998;

    @Override
    public int getId() {
        return HOST_ID;
    }

    @Override
    public String getName() {
        return "Vivo.sx";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getVideoPath(DetailActivity.QueryPlayTask queryTask) {
        Log.d(TAG, "GET " + url);

        if(TextUtils.isEmpty(url)) return null;
        try {
            queryTask.updateProgress(url);
            Document doc = Utils.buildJsoup(url)
                    .get();

            String base64 = doc.select("#player").attr("data-stream");

            byte[] data = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
            return new String(data, StandardCharsets.UTF_8);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Boolean canHandleUri(Uri uri) {
        return "vivo.sx".equalsIgnoreCase(uri.getHost());
    }
    @Override
    public void handleUri(Uri uri) {
        setUrl(uri.toString());
    }
}
