package com.ov3rk1ll.kinocast.api;

import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.api.mirror.Host;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilmpalastParser extends Parser {
    public static final int PARSER_ID = 15;
    public static final String URL_DEFAULT = "https://filmpalast.to/";
    public static final String TAG = "FilmpalastParser";

    private static final SparseIntArray languageResMap = new SparseIntArray();
    private static final SparseArray<String> languageKeyMap = new SparseArray<>();

    private static final Pattern mPattern;

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

        mPattern = Pattern.compile(" Veröffentlicht: ([1-2][0-9]{3}) ");
    }

    @Override
    public String getDefaultUrl() {
        return URL_DEFAULT;
    }

    @Override
    public String getParserName() {
        return "filmpalast.to";
    }

    @Override
    public int getParserId() {
        return PARSER_ID;
    }

    private List<ViewModel> parseList(Document doc){
        List<ViewModel> list = new ArrayList<>();
        Elements files = doc.select("div#content article.glowliste");

        for(Element element : files){
            try {
                ViewModel model = new ViewModel();
                model.setParserId(PARSER_ID);
                Elements el = element.select("cite > h2 > a");
                String url = el.attr("href");
                model.setSlug(url.substring(url.lastIndexOf(".to/") + 4));
                model.setTitle(el.text());

                model.setImage(buildUrl(element.select("a img").attr("src")));

                int lnId = 2;
                model.setLanguageResId(languageResMap.get(lnId));

                Elements ratings = element.select("div > img[src*=star_on]");
                model.setRating(ratings.size());
                list.add(model);
            }catch (Exception e){
                Log.e(TAG, "Error parsing " + element.html(), e);
            }
        }
        return list;
    }

    @Override
    public List<ViewModel> parseList(String url) throws IOException {
        Log.i(TAG, "parseList: " + url);
        Map<String, String> cookies = new HashMap<>();
        Document doc = getDocument(url, cookies);
        return parseList(doc);
    }

    private ViewModel parseDetail(Document doc, ViewModel model){
        Element element = doc.select("div#content article.detail").first();

        try {
            model.setParserId(PARSER_ID);
            model.setTitle(element.select("cite > h2").text());

            int lnId = 2;
            model.setLanguageResId(languageResMap.get(lnId));

            Elements schemMovie = element.select("li[itemtype=http://schema.org/Movie]");
            model.setImage(buildUrl(schemMovie.select("img[itemprop=image]").attr("src")));
            model.setSummary(schemMovie.select("span[itemprop=description]").text());
            Elements ratings = element.select("div#star-rate > img[src*=star_on]");
            model.setRating(ratings.size());

            String info = element.select("ul#detail-content-list > li").text();
            Matcher matcher = mPattern.matcher(info);
            if(matcher.find())
            {
                model.setYear(matcher.group(1));
            }

            List<Host> hostlist = new ArrayList<>();
            Elements hosts = doc.select("ul.currentStreamLinks a.iconPlay[target=_blank]");
            int i = 0;
            for(Element host: hosts){
                Host h = Host.selectByUri(Uri.parse(host.attr("href")));
                if(h == null || !h.isEnabled()) continue;
                i++;
                h.setMirror(i);
                h.setSlug(Integer.toString(i));
                hostlist.add(h);
            }
            model.setMirrors(hostlist.toArray(new Host[hostlist.size()]));
            Utils.LogModel(CastApp.getContext(), model);
        }catch (Exception e){
            Log.e(TAG, "Error parsing " + element.html(), e);
        }
        return model;
    }

    @Override
    public ViewModel loadDetail(ViewModel item, boolean showui){
        try {
            Document doc = getDocument(URL_BASE + item.getSlug());

            return parseDetail(doc, item);
        } catch (IOException e) {
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

        } catch (IOException e) {
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

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private String getMirrorLink(DetailActivity.QueryPlayTask queryTask, Host host, String url){

        String href = "";
        Method getLink = null;

        try {
            queryTask.updateProgress("Get host from " + URL_BASE + url);
            JSONObject json = getJson(URL_BASE + url);
            Document doc = Jsoup.parse(json != null ? json.getString("Stream") : null);
            href = getMirrorLink(doc);

            queryTask.updateProgress("Get video from " + href);
            return href;
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return null;
    }

    public String getMirrorLink(Document doc) {
        try {
            String href = null;
            Elements elem = doc.select("iframe");
            if (elem != null) {
                href = elem.attr("src");
            }
            if (Utils.isStringEmpty(href)) {
                elem = doc.select("a");
                if (elem != null) {
                    href = elem.attr("href");
                }
            }
            return Utils.getUrl(href);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return null;
    }

    @Override
    public String getMirrorLink(DetailActivity.QueryPlayTask queryTask, ViewModel item, Host host){
        return getMirrorLink(queryTask, host,"aGET/Mirror/" + item.getSlug() + "&Hoster=" + host.getId() + "&Mirror=" + host.getSlug());
    }

    @Override
    public String getMirrorLink(DetailActivity.QueryPlayTask queryTask, ViewModel item, Host host, int season, String episode){
        return getMirrorLink(queryTask, host,"aGET/Mirror/" + item.getSlug() + "&Hoster=" + host.getId() + "&Mirror=" + host.getSlug() + "&Season=" + season + "&Episode=" + episode);
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
        return URL_BASE + "search/title/" + URLEncoder.encode(query);
    }

    @Override
    public String getCineMovies(){
        return URL_BASE + "movies/new";
    }

    @Override
    public String getPopularMovies(){
        return URL_BASE + "movies/top";
    }

    @Override
    public String getLatestMovies(){
        return URL_BASE + "movies/new";
    }

    @Override
    public String PreSaveParserUrl(String newUrl){

        return newUrl.endsWith("/") ? newUrl : newUrl + "/";
    }
}
