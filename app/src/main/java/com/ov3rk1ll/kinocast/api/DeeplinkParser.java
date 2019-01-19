package com.ov3rk1ll.kinocast.api;

import android.net.Uri;
import android.os.Parcel;
import android.os.SystemClock;
import android.support.annotation.Nullable;
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

public class DeeplinkParser extends Parser {
    public static final int PARSER_ID = 9;
    public static final String TAG = "DeeplinkParser";
    public static final String URL_DEFAULT = "";

    private final List<ViewModel> lastModels = new ArrayList<>();

    @Override
    public String getDefaultUrl() {
        return URL_DEFAULT;
    }

    @Override
    public String getParserName() {
        return "DeeplinkParser (Internal)";
    }

    @Override
    public int getParserId() {
        return PARSER_ID;
    }


    @Override
    public List<ViewModel> parseList(String url) {
        return lastModels;
    }

    private void AddModels(List<ViewModel> models){
        List<String> slugs = new ArrayList<>();
        for ( ViewModel m: lastModels) {
            slugs.add(m.getSlug());
        }
        for ( ViewModel m: models) {
            if(!slugs.contains(m.getSlug())) lastModels.add(m);
        }
    }
    private void AddModel(ViewModel model){
        List<String> slugs = new ArrayList<>();
        for ( ViewModel m: lastModels) {
            slugs.add(m.getSlug());
        }
        if(!slugs.contains(model.getSlug())) lastModels.add(model);
    }

    @Override
    public ViewModel loadDetail(ViewModel item) {
        if(lastModels == null) return item;
        for ( ViewModel m: lastModels) {
            if(item.getSlug().equalsIgnoreCase(m.getSlug())) return m;
        }
        return item;
    }

    @Override
    public ViewModel loadDetail(String url) {
        if(url == null || lastModels == null) return null;
        url = url.replace("#lamguage=null","");
        url = parseSlug(url);

        for ( ViewModel m: lastModels) {
            if(url.equalsIgnoreCase(m.getSlug())) return m;
        }
        Uri uri = Uri.parse(url);
        Host host = Host.selectByUri(uri);
        if(host != null) {

            ViewModel viewModel = new ViewModel();
            viewModel.setParserId(PARSER_ID);
            viewModel.setSlug(url);
            viewModel.setType(ViewModel.Type.MOVIE);
            viewModel.setTitle(url);
            viewModel.setMirrors(new Host[]{host});
            AddModel(viewModel);
            return viewModel;
        }
        return null;
    }

    private String parseSlug(String url) {
          return url;
    }

    @Override
    public List<Host> getHosterList(ViewModel item, int season, String episode) {
        item = loadDetail(item);
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
        return item.getSlug();
    }

    @SuppressWarnings("deprecation")
    @Override
    public String getSearchPage(String query) {
        return URL_BASE;
    }

    @Override
    public String getCineMovies() {
        return URL_BASE;
    }

    @Override
    public String getPopularMovies() {
        return URL_BASE;
    }

    @Override
    public String getLatestMovies() {
        return URL_BASE;
    }

    @Override
    public String getPopularSeries() {
        return URL_BASE;
    }

    @Override
    public String getLatestSeries() {
        return URL_BASE;
    }
}
