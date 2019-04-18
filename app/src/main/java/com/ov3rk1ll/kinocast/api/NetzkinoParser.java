package com.ov3rk1ll.kinocast.api;

import android.os.SystemClock;
import android.util.Log;

import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.api.mirror.Direct;
import com.ov3rk1ll.kinocast.api.mirror.Host;
import com.ov3rk1ll.kinocast.data.ViewModel;
import com.ov3rk1ll.kinocast.ui.DetailActivity;
import com.ov3rk1ll.kinocast.utils.Utils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class NetzkinoParser extends CachedParser {
    public static final int PARSER_ID = 8;
    public static final String TAG = "NetzkinoParser";
    public static final String URL_DEFAULT = "http://api.netzkino.de.simplecache.net/";

    @Override
    public String getDefaultUrl() {
        return URL_DEFAULT;
    }

    @Override
    public String getParserName() {
        return "Netzkino.de";
    }

    @Override
    public int getParserId() {
        return PARSER_ID;
    }

    @Override
    public List<ViewModel> parseList(String url) {
        Log.i("Parser", "parseList: " + url);

        JSONObject doc = Utils.readJson(url);
        return parseList(doc);
    }

    private List<ViewModel> parseList(JSONObject doc) {
        List<ViewModel> list = new ArrayList<>();
        try {
            JSONArray entries = doc.getJSONArray("posts");
            for (int i = 0; i < entries.length(); i++) {
                JSONObject item = entries.getJSONObject(i);

                try {
                    JSONObject cf = item.getJSONObject("custom_fields");
                    ViewModel model = new ViewModel();
                    model.setParserId(PARSER_ID);
                    model.setSummary(Jsoup.parse(item.getString("content")).text());
                    model.setSlug(item.getString("slug"));
                    model.setTitle(item.getString("title"));

                    if(cf.has("IMDb-Link")){
                        String imdburl = cf.getJSONArray("IMDb-Link").getString(0);
                        String imdb = imdburl.substring(imdburl.indexOf("/tt") + 1);
                        imdb = imdb.replaceAll("/", "");
                        model.setImdbId(imdb);
                    } else if(cf.has("TV_Movie_Cover")){
                        model.setImage(cf.getJSONArray("TV_Movie_Cover").getString(0));
                    }

                    model.setType(ViewModel.Type.MOVIE);

                    model.setLanguageResId(R.drawable.lang_de);

                    List<Host> hostlist = new ArrayList<>();
                    Host h = new Direct();
                    h.setMirror(1);
                    String url = "http://pmd.netzkino-seite.netzkino.de/" + cf.getJSONArray("Streaming").getString(0) + ".mp4";
                    h.setSlug(url);
                    h.setUrl(url);
                    if (h.isEnabled()) {
                        hostlist.add(h);
                    }
                    model.setMirrors(hostlist.toArray(new Host[hostlist.size()]));
                    list.add(model);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing " + doc.toString(), e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing " + doc.toString(), e);
        }
        UpdateModels(list);
        return list;
    }


    @Override
    public ViewModel loadDetail(String url) {
        url = parseSlug(url);
        if(url == null) return null;

        ViewModel m = FindModel(url);
        if(m != null) return m;

        try {
            List<ViewModel> list = parseList(getSearchPage(url.replace("-", " ").replace("_", " ")));
            UpdateModels(list);

            return FindModel(url);
        } catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    private String parseSlug(String url) {
        if(url == null) return null;
        url = url.substring(url.indexOf("filme/") + 6);
        int i = url.indexOf("/");
        if(i < 1) i = url.indexOf("#");
        if(i > 0) url = url.substring(0, i);
        return url;
    }

    @Override
    public List<Host> getHosterList(ViewModel item, int season, String episode) {
        item = loadDetail(item, false);
        List<Host> hostlist = new ArrayList<>();
        for (Host host : item.getMirrors()) {
            hostlist.add(host);
        }
        return hostlist;
    }

    @Override
    public String getMirrorLink(DetailActivity.QueryPlayTask queryTask, ViewModel item, Host host) {
        return host.getUrl();
    }

    @Override
    public String getMirrorLink(DetailActivity.QueryPlayTask queryTask, ViewModel item, Host host, int season, String episode) {
        return host.getUrl();
    }

    @SuppressWarnings("deprecation")
    @Override
    public String[] getSearchSuggestions(String query) {
        String url = KinoxParser.URL_DEFAULT + "aGET/Suggestions/?q=" + URLEncoder.encode(query) + "&limit=10&timestamp=" + SystemClock.elapsedRealtime();
        String data = getBody(url);

        String suggestions[] = data != null ? data.split("\n") : new String[0];
        if (suggestions[0].trim().equals("")) return null;
        // Remove duplicates
        return new HashSet<>(Arrays.asList(suggestions)).toArray(new String[new HashSet<>(Arrays.asList(suggestions)).size()]);
    }

    @Override
    public String getPageLink(ViewModel item) {
        return "http://www.netzkino.de/filme/" + item.getSlug();
    }

    private final String uriparam = "?d=www&l=de-DE&v=1.2.1";

    @SuppressWarnings("deprecation")
    @Override
    public String getSearchPage(String query) {
        return URL_BASE + "capi-2.0a/search" + uriparam + "&q=" + URLEncoder.encode(query);
    }

    @Override
    public String getCineMovies() {
        return URL_BASE + "capi-2.0a/categories/1.json" + uriparam;
    }

    @Override
    public String getPopularMovies() { return null; }
    @Override
    public String getLatestMovies() { return null; }

    @Override
    public String getPopularSeries() { return null; }
    @Override
    public String getLatestSeries() { return null; }

    @Override
    public String PreSaveParserUrl(String newUrl) {
        return newUrl.endsWith("/") ? newUrl : newUrl + "/";
    }
}
