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

import org.json.JSONException;
import org.json.JSONObject;

public class JetLoad extends Host {
    private static final String TAG = JetLoad.class.getSimpleName();
    public static final int HOST_ID = 86;


    @Override
    public int getId() {
        return HOST_ID;
    }

    @Override
    public String getName() {
        return "JetLoad";
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

        Uri uri = Uri.parse(url);
        String id = uri.getFragment();
        Integer lpos = id.lastIndexOf("/v/");
        if(lpos <= 0) return null;
        String key = id.substring(lpos + 3);
        Integer npos = key.indexOf("/");
        if(npos > 0) key = key.substring(0, npos);
        Log.d(TAG, "founded key: " + key);

        queryTask.updateProgress(queryTask.getContext().getString(R.string.host_progress_getvideoforid,key));
        String link = getLink(key);

        Log.d(TAG, "Request. Got " + link);
        queryTask.updateProgress(queryTask.getContext().getString(R.string.host_progress_1sttry));

        return link;
    }

    private String getLink(final String key) {
        JSONObject jobj = Utils.readJson("https://jetload.net/api/get_direct_video/" + key);
        try {
            //String type = jobj.getString("type");
            String fname = jobj.getJSONObject("file").getString("file_name");
            String hname = jobj.getJSONObject("server").getString("hostname");
            return hname + "/v2/schema/" + fname + "/med.m3u8";
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
    @Override
    public Boolean canHandleUri(Uri uri) {
        return ("jetload.net".equalsIgnoreCase(uri.getHost())
                ||  "jetload.net".equalsIgnoreCase(uri.getHost()));
    }
    @Override
    public void handleUri(Uri uri) {
        setUrl(uri.toString());
    }
}
