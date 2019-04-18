package com.ov3rk1ll.kinocast.api;

import android.net.Uri;

import com.ov3rk1ll.kinocast.api.mirror.Host;
import com.ov3rk1ll.kinocast.data.ViewModel;
import com.ov3rk1ll.kinocast.ui.DetailActivity;

import java.io.IOException;
import java.util.List;

public class DeeplinkParser extends CachedParser {
    public static final int PARSER_ID = 9;
    public static final String TAG = "DeeplinkParser";
    public static final String URL_DEFAULT = "";

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
    public List<ViewModel> parseList(String url) throws IOException {
        List<ViewModel> list = super.parseList(url);
        if(list != null) return list;
        return lastModels;
    }

    @Override
    public ViewModel loadDetail(String url) {
        if(url == null || lastModels == null) return null;
        url = url.replace("#language=null","");
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
            return UpdateModel(viewModel);
        }
        return null;
    }

    private String parseSlug(String url) {
          return url;
    }

    @Override
    public String getMirrorLink(DetailActivity.QueryPlayTask queryTask, ViewModel item, Host host) {
        return host.getUrl();
    }

    @Override
    public String getMirrorLink(DetailActivity.QueryPlayTask queryTask, ViewModel item, Host host, int season, String episode) {
        return host.getUrl();
    }


    @Override
    public String getPageLink(ViewModel item) {
        return item.getSlug();
    }

    @Override
    public String getCineMovies() {
        return URL_BASE;
    }

    @Override
    public String getPopularMovies() {
        return null;
    }

    @Override
    public String getLatestMovies() {
        return null;
    }

    @Override
    public String getPopularSeries() {
        return null;
    }

    @Override
    public String getLatestSeries() {
        return null;
    }
}
