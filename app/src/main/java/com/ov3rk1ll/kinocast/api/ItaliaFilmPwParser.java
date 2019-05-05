package com.ov3rk1ll.kinocast.api;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.api.mirror.Host;
import com.ov3rk1ll.kinocast.api.mirror.NowVideo;
import com.ov3rk1ll.kinocast.api.mirror.Openload;
import com.ov3rk1ll.kinocast.api.mirror.RapidVideo;
import com.ov3rk1ll.kinocast.api.mirror.StreamCherry;
import com.ov3rk1ll.kinocast.api.mirror.Streamango;
import com.ov3rk1ll.kinocast.api.mirror.VeryStream;
import com.ov3rk1ll.kinocast.api.mirror.Vidoza;
import com.ov3rk1ll.kinocast.data.ViewModel;
import com.ov3rk1ll.kinocast.ui.DetailActivity;
import com.ov3rk1ll.kinocast.utils.Utils;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItaliaFilmPwParser extends Parser {
    public static final int PARSER_ID = 17;
    public static final String URL_DEFAULT = "https://www.italia-film.pw/";
    public static final String TAG = "ItaliaFilmPwParser";

    private static final SparseIntArray languageResMap = new SparseIntArray();
    private static final SparseArray<String> languageKeyMap = new SparseArray<>();

    static {
        languageResMap.put(1, R.drawable.lang_en);
        languageKeyMap.put(1, "en");
        languageResMap.put(2, R.drawable.lang_de);
        languageKeyMap.put(2, "de");
        languageResMap.put(4, R.drawable.lang_zh);
        languageKeyMap.put(4, "zh");
        languageResMap.put(5, R.drawable.lang_es);
        languageKeyMap.put(5, "es");
        languageResMap.put(6, R.drawable.lang_fr);
        languageKeyMap.put(6, "fr");
        languageResMap.put(7, R.drawable.lang_tr);
        languageKeyMap.put(7, "tr");
        languageResMap.put(8, R.drawable.lang_jp);
        languageKeyMap.put(8, "jp");
        languageResMap.put(9, R.drawable.lang_ar);
        languageKeyMap.put(9, "ar");
        languageResMap.put(11, R.drawable.lang_it);
        languageKeyMap.put(11, "it");
        languageResMap.put(12, R.drawable.lang_hr);
        languageKeyMap.put(12, "hr");
        languageResMap.put(13, R.drawable.lang_sr);
        languageKeyMap.put(13, "sr");
        languageResMap.put(14, R.drawable.lang_bs);
        languageKeyMap.put(14, "bs");
        languageResMap.put(15, R.drawable.lang_de_en);
        languageKeyMap.put(15, "de");
        languageResMap.put(16, R.drawable.lang_nl);
        languageKeyMap.put(16, "nl");
        languageResMap.put(17, R.drawable.lang_ko);
        languageKeyMap.put(17, "ko");
        languageResMap.put(24, R.drawable.lang_el);
        languageKeyMap.put(24, "el");
        languageResMap.put(25, R.drawable.lang_ru);
        languageKeyMap.put(25, "ru");
        languageResMap.put(26, R.drawable.lang_hi);
        languageKeyMap.put(26, "hi");
    }

    @Override
    public String getDefaultUrl() {
        return URL_DEFAULT;
    }

    @Override
    public String getParserName() {
        return "italia-film.pw";
    }

    @Override
    public int getParserId() {
        return PARSER_ID;
    }

    private List<ViewModel> parseList(Document doc){
        List<ViewModel> list = new ArrayList<>();
        Elements files = doc.select("div#content article.post");

        for(Element element : files){
            try {
                ViewModel model = new ViewModel();
                model.setMinAge(18);
                model.setParserId(PARSER_ID);
                Elements el = element.select(".entry-header h3.entry-title > a");
                String url = el.attr("href");
                model.setType(ViewModel.Type.MOVIE);
                model.setSlug(url.substring(url.lastIndexOf(".pw/") + 4));
                model.setTitle(el.text());
                int lnId = 11;
                model.setLanguageResId(languageResMap.get(lnId));
                list.add(model);
            }catch (Exception e){
                Log.e(TAG, "Error parsing " + element.html(), e);
            }
        }
        return list;
    }

    @Override
    public List<ViewModel> parseList(String url) throws Exception {
        Log.i(TAG, "parseList: " + url);
        Map<String, String> cookies = new HashMap<>();
        Document doc = getDocument(url, cookies);
        return parseList(doc);
    }

    private ViewModel parseDetail(Document doc, ViewModel model){

        List<ViewModel> list = new ArrayList<>();
        Element element = doc.select("#primary article.post").first();

        try {
            model.setParserId(PARSER_ID);
            model.setTitle(element.select("h2.entry-title").text().trim());
            int lnId = 11;
            model.setLanguageResId(languageResMap.get(lnId));

            String imdb = element.select(".imdbRatingPlugin").attr("data-title");
            if(imdb.startsWith("tt"))
                model.setImdbId(imdb);
            else
                model.setImage(buildUrl(element.select("img.single-thumb").attr("src")));

            List<Host> hostlist = new ArrayList<>();
            Elements links = element.select(".entry-content p a[rel=noopener]");
            int i = 0;
            for(Element host: links){
                String href = host.attr("href");
                Host h = Host.selectByUri(Uri.parse(href));
                if(h == null) {
                    if (!href.startsWith(URL_DEFAULT) || href.length() != URL_DEFAULT.length() + 4)
                        continue;
                    if (!host.attr("target").equalsIgnoreCase("_blank")) continue;
                    h = getHostByText(host.text());
                }
                if(h == null || !h.isEnabled()) continue;
                i++;
                h.setMirror(i);
                h.setSlug(href);
                hostlist.add(h);
            }
            model.setMirrors(hostlist.toArray(new Host[hostlist.size()]));
            list.add(model);
        }catch (Exception e){
            Log.e(TAG, "Error parsing " + element.html(), e);
        }
        return model;
    }

    private Host getHostByText(String text){
        text = text.toLowerCase();
        if(text.contains("streamango")) return new Streamango();
        if(text.contains("openload")) return new Openload();
        if(text.contains("nowvideo")) return new NowVideo();
        if(text.contains("rapidvideo")) return new RapidVideo();
        if(text.contains("streamcherry")) return new StreamCherry();
        if(text.contains("verystream")) return new VeryStream();
        if(text.contains("vidoza")) return new Vidoza();
        return null;
    }

    @Override
    public ViewModel loadDetail(ViewModel item, boolean showui){
        try {
            Document doc = super.getDocument(URL_BASE + item.getSlug());

            return parseDetail(doc, item);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return item;
    }

    @Override
    public ViewModel loadDetail(String url) {
        ViewModel model = new ViewModel();
        model.setParserId(PARSER_ID);
        try {
            Document doc = getDocument(url);
            model.setSlug(url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf(".")));
            model = parseDetail(doc, model);
            return model;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Host> getHosterList(ViewModel item, int season, String episode){
        String url = "aGET/MirrorByEpisode/?Addr=" + item.getSlug() + "&SeriesID=" + item.getSeriesID() + "&Season=" + season + "&Episode=" + episode;
        try {
            Document doc = getDocument(URL_BASE + url);

            List<Host> hostlist = new ArrayList<>();
            Elements hosts = doc.select("li");
            for(Element host: hosts){
                int hosterId = Integer.valueOf(host.id().replace("Hoster_", ""));
                String count = host.select("div.Data").text();
                count = count.substring(count.indexOf("/") + 1, count.indexOf(" ", count.indexOf("/")));
                int c = Integer.valueOf(count);
                for(int i = 0; i < c; i++){
                    Host h = Host.selectById(hosterId);
                    if(h == null) continue;
                    h.setMirror(i + 1);
                    h.setSlug(Integer.toString(i + 1));
                    if(h.isEnabled()){
                        hostlist.add(h);
                    }
                }
            }

            return hostlist;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private String getMirrorLink(DetailActivity.QueryPlayTask queryTask, Host host, String url){

        String href;
        try {
            queryTask.updateProgress("Get host from " + url);
            href = Utils.getRedirectTarget(url);
            return href;
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return null;
    }


    @Override
    public String getMirrorLink(DetailActivity.QueryPlayTask queryTask, ViewModel item, Host host){
        return getMirrorLink(queryTask, host, host.getSlug());
    }

    @Override
    public String getMirrorLink(DetailActivity.QueryPlayTask queryTask, ViewModel item, Host host, int season, String episode){
        return getMirrorLink(queryTask, host, host.getSlug());
    }

    @SuppressWarnings("deprecation")
    @Override
    public String[] getSearchSuggestions(String query){
        return KinoxParser.searchSuggestions(query, this);
    }

    @Override
    public String getPageLink(ViewModel item){
        return URL_BASE + item.getSlug();
    }

    @SuppressWarnings("deprecation")
    @Override
    public String getSearchPage(String query){
        return URL_BASE + "?s=" + URLEncoder.encode(query);
    }

    @Override
    public String getCineMovies(){
        return URL_BASE + "category/film-streaming-2019/";
    }

    @Override
    public List<Bundle> getMenuItems(){
        List<Bundle> list = new ArrayList<>();
        Bundle b;
        b = buildBundle(getCineMovies(), 0, "2019");
        if(b != null) list.add(b);
        b = buildBundle(URL_BASE + "category/film-streaming-2018/", 0, "2018");
        if(b != null) list.add(b);
        b = buildBundle(URL_BASE + "category/film-hd-1/", 0, "Film in HD");
        if(b != null) list.add(b);
        return list;
    }

    @Override
    public String PreSaveParserUrl(String newUrl){

        return newUrl.endsWith("/") ? newUrl : newUrl + "/";
    }
}
