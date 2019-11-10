package com.ov3rk1ll.kinocast.api.mirror;

import android.text.TextUtils;
import android.util.Log;

import com.ov3rk1ll.kinocast.ui.DetailActivity;
import com.ov3rk1ll.kinocast.utils.Utils;

import org.json.JSONObject;
import org.jsoup.nodes.Document;

public class VidCloud extends Host {
    private static final String TAG = VidCloud.class.getSimpleName();
    public static final int HOST_ID = 81;

    @Override
    public int getId() {
        return HOST_ID;
    }

    @Override
    public String getName() {
        return "VidCloud";
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

            String player = doc.select("div.vidcloud-player-embed script").html();
            player = player.substring(player.indexOf("url: '/player") + 6);
            player = player.substring(0, player.indexOf("',"));

            JSONObject json = Utils.readJson("https://vidcloud.co" + player);
            String html = json.getString("html");
            html = html.substring(html.indexOf("[{\"file\":\"") + 10);
            html = html.substring(0, html.indexOf("\""));
            html = html.replace("\\/","/");
            return Utils.getUrl(html);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
