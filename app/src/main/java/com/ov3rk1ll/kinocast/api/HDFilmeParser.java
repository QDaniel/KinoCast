package com.ov3rk1ll.kinocast.api;

import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.api.mirror.HDFilme;
import com.ov3rk1ll.kinocast.api.mirror.Host;
import com.ov3rk1ll.kinocast.data.ViewModel;
import com.ov3rk1ll.kinocast.ui.DetailActivity;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class HDFilmeParser extends Parser {
    public static final int PARSER_ID = 7;
    public static final String TAG = "HDFilmeParser";
    public static final String URL_DEFAULT = "https://hdfilme.net/";

    private static final SparseIntArray languageResMap = new SparseIntArray();
    private static final SparseArray<String> languageKeyMap = new SparseArray<>();

    static {
        languageResMap.put(1, R.drawable.lang_de);
        languageKeyMap.put(1, "de");
        languageResMap.put(2, R.drawable.lang_en);
        languageKeyMap.put(2, "en");
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
        return "HDFilme.net";
    }

    @Override
    public int getParserId() {
        return PARSER_ID;
    }

    private List<ViewModel> parseList(Document doc) {
        List<ViewModel> list = new ArrayList<>();
        Elements files = doc.select("li > div.box-product");

        for (Element element : files) {
            element = element.parent();
            try {
                ViewModel model = new ViewModel();
                model.setParserId(PARSER_ID);
                model.setType(ViewModel.Type.MOVIE);
                Element urltitle =  element.select(".decaption > .title-product > a").first();
                String url = urltitle.attr("href");
               model.setSlug(url.substring(url.lastIndexOf("/") + 1));
                model.setTitle(urltitle.textNodes().get(0).text());
                model.setLanguageResId(R.drawable.lang_de);
                model.setImage(element.select("img.img").attr("src"));

                list.add(model);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing " + element.html(), e);
            }
        }
        return list;
    }

    @Override
    public List<ViewModel> parseList(String url) throws Exception {
        Log.i("Parser", "parseList: " + url);
        Document doc = getDocument(url);
        return parseList(doc);
    }

    private ViewModel parseDetail(Document doc, ViewModel model) {
        Element element = doc.select("#play-area-wrapper").first();
        try {

            model.setTitle(element.select(".title-film").first().text().trim());
            model.setImage(element.select("div.img-thumb img").first().attr("src"));
            model.setSummary(element.select(".caption > .caption-scroll").text().trim());
            model.setType(ViewModel.Type.MOVIE);
            model.setLanguageResId(R.drawable.lang_de);
            //model.setGenre(doc.select("li[Title=Genre]").text());

            List<Host> hostlist = new ArrayList<>();
            Elements files = doc.select("ul.list-film li a.new");
            int i=0;
            for (Element file : files) {
                i++;
                Host h = new HDFilme();
                String episodeId=file.attr("data-episode-id");
                String itemId = doc.select("#send_movie_watch_later").attr("movie-id");
                h.setUrl("https://hdfilme.net/movie/load-stream/" + itemId + "/" + episodeId + "?server=1");
                h.setMirror(i);
                hostlist.add(h);
            }

            model.setMirrors(hostlist.toArray(new Host[hostlist.size()]));

        } catch (Exception e) {
            Log.e(TAG, "Error parsing " + element.html(), e);
        }
        return model;
    }

    @Override
    public ViewModel loadDetail(ViewModel item, boolean showui) {
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
            Document doc = getDocument(url, null);
            model.setSlug(url.substring(url.lastIndexOf("/") + 1));

            model = parseDetail(doc, model);
            return model;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Host> getHosterList(ViewModel item, int season, String episode) {
        String url = "aGET/MirrorByEpisode/?Addr=" + item.getSlug() + "&SeriesID=" + item.getSeriesID() + "&Season=" + season + "&Episode=" + episode;
        try {
            Document doc = getDocument(URL_BASE + url);

            List<Host> hostlist = new ArrayList<>();
            Elements hosts = doc.select("li");
            for (Element host : hosts) {
                int hosterId = Integer.valueOf(host.id().replace("Hoster_", ""));
                String count = host.select("div.Data").text();
                count = count.substring(count.indexOf("/") + 1, count.indexOf(" ", count.indexOf("/")));
                int c = Integer.valueOf(count);
                for (int i = 0; i < c; i++) {
                    Host h = Host.selectById(hosterId);
                    if (h == null) continue;
                    h.setMirror(i + 1);
                    if (h.isEnabled()) {
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
        /*try {
            byte ptext[] = data.getBytes("ISO-8859-1");
            data = new String(ptext, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }*/
        String suggestions[] = data != null ? data.split("\n") : new String[0];
        if (suggestions[0].trim().equals("")) return null;
        // Remove duplicates
        return new HashSet<>(Arrays.asList(suggestions)).toArray(new String[new HashSet<>(Arrays.asList(suggestions)).size()]);
    }

    @Override
    public String getPageLink(ViewModel item) {
        return URL_BASE + item.getSlug() + ".html";
    }

    @SuppressWarnings("deprecation")
    @Override
    public String getSearchPage(String query) {
        return URL_BASE + "movie-search?key=" + URLEncoder.encode(query);
    }

    @Override
    public String getCineMovies() {
        return URL_BASE + "";
    }

    @Override
    public String getPopularMovies() {
        return URL_BASE + "movie-movies";
    }

    @Override
    public String getPopularSeries() {
        return URL_BASE + "movie-series";
    }


    @Override
    public String PreSaveParserUrl(String newUrl){

        return newUrl.endsWith("/") ? newUrl : newUrl + "/";
    }
}
