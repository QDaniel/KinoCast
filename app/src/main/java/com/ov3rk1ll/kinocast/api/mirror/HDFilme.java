package com.ov3rk1ll.kinocast.api.mirror;

import android.text.TextUtils;

import com.ov3rk1ll.kinocast.api.Parser;
import com.ov3rk1ll.kinocast.ui.DetailActivity;

import org.jsoup.nodes.Document;

import java.io.IOException;

public class HDFilme extends Host {
    private static final String TAG = HDFilme.class.getSimpleName();
    public static final int HOST_ID = 98;

    @Override
    public int getId() {
        return HOST_ID;
    }

    @Override
    public String getName() {
        return "HDFilme";
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public String getVideoPath(DetailActivity.QueryPlayTask queryTask) {
        if(TextUtils.isEmpty(url)) return null;
        try {
            Document doc = Parser.getDocument(url);
            String data = doc.html();
            data = data.substring(data.lastIndexOf("'file' : \"https:")+10);
            data = data.substring(0,data.indexOf("\""));
            return data;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
