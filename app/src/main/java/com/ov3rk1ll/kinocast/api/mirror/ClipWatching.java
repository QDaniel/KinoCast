package com.ov3rk1ll.kinocast.api.mirror;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.ui.DetailActivity;
import com.ov3rk1ll.kinocast.ui.MainActivity;
import com.ov3rk1ll.kinocast.utils.Utils;

import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class ClipWatching extends Host {
    private static final String TAG = ClipWatching.class.getSimpleName();
    public static final int HOST_ID = 87;

    private static final Pattern regexMp4 = Pattern.compile("http[s]*:\\/\\/([0-9a-z.]*)\\/([0-9a-z.]*?)\\/v\\.mp4");

    @Override
    public int getId() {
        return HOST_ID;
    }

    @Override
    public String getName() {
        return "ClipWatching.com";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getVideoPath(DetailActivity.QueryPlayTask queryTask) {
        if (TextUtils.isEmpty(url)) return null;

        queryTask.updateProgress(queryTask.getContext().getString(R.string.host_progress_getdatafrom, url));

        String link = getLink(url);

        Log.d(TAG, "Request. Got " + link);
        queryTask.updateProgress(queryTask.getContext().getString(R.string.host_progress_1sttry));

        return link;
    }

    private String getLink(final String url) {

        try {
            Document doc = Utils.buildJsoup(url).get();

            Matcher m = regexMp4.matcher(doc.html());
            if (m.find()) {
                return m.group(0);
            }

            ArrayList<String> list = unPackAll(doc.html());

            for (String pack : list) {
                m = regexMp4.matcher(pack);
                if (m.find()) {
                    return m.group(0);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Boolean canHandleUri(Uri uri) {
        return ("clipwatching.com".equalsIgnoreCase(uri.getHost())
                || "www.clipwatching.com".equalsIgnoreCase(uri.getHost()));
    }

    @Override
    public void handleUri(Uri uri) {
        setUrl(uri.toString());
    }

    ArrayList<String> unPackAll(String html) {
        ArrayList<String> ret = new ArrayList<String>();
        int si = 0;
        int start = 0;

        html = html.replace("\n", "").replace("\r", "");
        Log.d("HTML", html);
        do {
            si = start;
            start = html.indexOf(">eval(function(p,a,c,k,e,d)", si);
            Log.d("Start", "" + start);
            int end = html.indexOf(")</script>", start);
            Log.d("end", "" + end);
            if (start > si && end > start) {
                String javascript = html.substring(start + 6, end);
                Log.d("SCRIPT FOUND", javascript);
                ret.add(unPack(javascript));
            }

        } while (start > si);
        return ret;
    }

    private String unPack(String javascript) {
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("rhino");
        Object eval = null;
        try {
            eval = engine.eval("JSON.stringify(" + javascript + ")");
        } catch (ScriptException e) {
            // TODO Auto-generated catch block
            Log.e(TAG, "Exception evaluating javascript " + javascript, e);
        }
        String ret = eval.toString().replace("\\\"", "\"");
        ret = ret.substring(1, ret.length() - 2);
        Log.d(TAG, ret);
        return ret;
    }
}
