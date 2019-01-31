package com.ov3rk1ll.kinocast.api;

import android.os.Bundle;
import android.util.Log;

import com.ov3rk1ll.kinocast.api.mirror.Direct;
import com.ov3rk1ll.kinocast.api.mirror.Host;
import com.ov3rk1ll.kinocast.data.ViewModel;
import com.ov3rk1ll.kinocast.ui.DetailActivity;
import com.ov3rk1ll.kinocast.utils.Utils;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TVDEParser extends CachedParser {
    public static final int PARSER_ID = 13;
    public static final String URL_DEFAULT = "http://www.pro7livestream.com/";
    public static final String URL_EXTRA = "http://www.tv-de.com/";
    public static final String TAG = "TVDEParser";


    @Override
    public String getDefaultUrl() {
        return URL_DEFAULT;
    }

    @Override
    public String getParserName() {
        return "tv-de.com";
    }

    @Override
    public int getParserId() {
        return PARSER_ID;
    }

    private List<ViewModel> parseList(Document doc) {

        List<ViewModel> list = new ArrayList<>();

        Elements files = doc.select("div.moviefilm");

        for (Element element : files) {
            try {
                Element link = element.select("a").first();
                Element img = element.select("img").first();
                if(Utils.isStringEmpty(img.attr("alt"))) continue;

                ViewModel model = new ViewModel();
                model.setParserId(PARSER_ID);
                model.setTitle(img.attr("alt"));
                String url = link.attr("href");
                model.setSlug(url);
                model.setImage(img.attr("src"));
                list.add(model);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing " + element.html(), e);
            }
        }
        return UpdateModels(list);
    }

    @Override
    public List<ViewModel> parseList(String url) throws IOException {
        Log.i(TAG, "parseList: " + url);
        List<ViewModel> list = super.parseList(url);
        if(list != null) return list;

        Document doc = getDocument(url);
        return parseList(doc);
    }

    @Override
    public ViewModel loadDetail(ViewModel item, boolean showui) {
        item = UpdateModel(item);
        if(item.getMirrors() == null) {

            try {
                Document doc = getDocument(item.getSlug());

                List<Host> hostlist = new ArrayList<>();

                Element elm = doc.selectFirst("div.filmicerik");
                Elements iframe = elm.select("iframe");
                if(!Utils.isStringEmpty(iframe.attr("src")))
                {
                    Document doci = getDocument(iframe.attr("src"));
                    String html = doci.html();
                    int i = html.indexOf(" file: \"http");
                    int j = html.indexOf("\"", i + 8);
                    if(i > 0 && j > 0){
                        Host h = new Direct();
                        h.setUrl(html.substring(i + 8, j));
                        h.setMirror(1);
                        hostlist.add(h);
                    }
                } else {
                    String html = elm.html();
                    int i = html.indexOf("source: 'http");
                    int j = html.indexOf("'", i + 9);
                    if(i > 0 && j > 0){
                        Host h = new Direct();
                        h.setUrl(html.substring(i + 9, j));
                        h.setMirror(1);
                        hostlist.add(h);
                    }

                }

                item.setMirrors(hostlist.toArray(new Host[hostlist.size()]));

            } catch (IOException e) {
                e.printStackTrace();
            }


            item = UpdateModel(item);
        }
        return item;
    }

    @Override
    public ViewModel loadDetail(String url) {
        if(url == null) return null;
        ViewModel item = FindModel(url);
        if(item == null){
            item = new ViewModel();
            item.setSlug(url);
        }
        return loadDetail(item, false);
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
    public String getPopularMovies() { return URL_EXTRA; }

    @Override
    public List<Bundle> getMenuItems(){
        List<Bundle> list = new ArrayList<>();
        Bundle b;
        b = buildBundle(getCineMovies(), 0, "pro7livestream");
        if(b != null) list.add(b);
        b = buildBundle(getPopularMovies(), 0, "tv-de");
        if(b != null) list.add(b);
        return list;
    }
}
