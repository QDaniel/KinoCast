package com.ov3rk1ll.kinocast.api.mirror;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.ui.DetailActivity;
import com.ov3rk1ll.kinocast.utils.Utils;

import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

            ArrayList<String> list = Utils.unPackAll(doc.html());

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

}
