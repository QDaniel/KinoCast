package com.ov3rk1ll.kinocast.api;

import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.api.mirror.Direct;
import com.ov3rk1ll.kinocast.api.mirror.DivxStage;
import com.ov3rk1ll.kinocast.api.mirror.Host;
import com.ov3rk1ll.kinocast.api.mirror.SharedSx;
import com.ov3rk1ll.kinocast.api.mirror.Sockshare;
import com.ov3rk1ll.kinocast.api.mirror.StreamCloud;
import com.ov3rk1ll.kinocast.data.ViewModel;
import com.ov3rk1ll.kinocast.ui.DetailActivity;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class MyKinoParser extends Parser {
    public static final int PARSER_ID = 4;
    public static final String TAG = "MyKinoParser";
    public static final String URL_DEFAULT = "http://mykino.to/";

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
        return "myKino";
    }

    @Override
    public int getParserId() {
        return PARSER_ID;
    }

    private List<ViewModel> parseList(Document doc) {
        List<ViewModel> list = new ArrayList<>();
        Elements files = doc.select("div.w25 > div.item");

        for (Element element : files) {
            element = element.parent();
            try {
                ViewModel model = new ViewModel();
                model.setParserId(PARSER_ID);
                model.setType(ViewModel.Type.MOVIE);
                String url = element.select("div.poster a.play").attr("href");
                model.setSlug(url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf(".")));
                model.setTitle(element.select("div.name").first().textNodes().get(0).text());
                model.setSummary(element.select("div.desk").text());
                model.setLanguageResId(R.drawable.lang_de);

                String genre = element.select("div.ganre").text();
                if (genre.contains("Serien,")) {
                    model.setType(ViewModel.Type.SERIES);
                    genre = genre.replace("Serien,", "").trim();
                }
                if (genre.contains(",")) genre = genre.substring(0, genre.indexOf(","));
                model.setGenre(genre);

                list.add(model);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing " + element.html(), e);
            }
        }
        return list;
    }

    @Override
    public List<ViewModel> parseList(String url) throws IOException {
        Log.i("Parser", "parseList: " + url);
        Map<String, String> cookies = new HashMap<>();
        Document doc = getDocument(url, cookies);
        return parseList(doc);
    }

    private ViewModel parseDetail(Document doc, ViewModel model) {

        Element element = doc.select("div.single-product").first();
        model.setTitle(element.select("h1.post-title").first().textNodes().get(0).text().trim());
        model.setSummary(element.select("div.deskription").text().trim());
        model.setType(ViewModel.Type.MOVIE);
        model.setLanguageResId(R.drawable.lang_de);
        //model.setGenre(doc.select("li[Title=Genre]").text());

        model.setImdbId(doc.select("span.imdbRatingPlugin").attr("data-title"));

        List<Host> hostlist = new ArrayList<>();

        String downl = element.select("div.down_links script").html();
        if (downl.contains("file:\"//")) {
            downl = downl.substring(downl.indexOf("file:\"") + 6);
            downl = downl.substring(0, downl.indexOf(" "));
            Host h = Host.selectById(Direct.HOST_ID);
            h.setUrl("http:" + downl);
            h.setMirror(1);
            if (h.isEnabled()) {
                hostlist.add(h);
            }
        }

        Elements mirrors = element.select("ul.mirrors-selector a");
        for (Element mirror : mirrors) {
            String data = mirror.attr("data-href");
            String[] urls = data.split(",");
            int hoster = getHosterId(data);
            if (hoster == 0) continue;
            for (int i = 0; i < urls.length; i++) {
                Host h = Host.selectById(hoster);
                h.setMirror(i + 1);
                h.setSlug(urls[i]);
                h.setUrl(urls[i].replaceAll("#$", ""));
                if (h.isEnabled()) {
                    hostlist.add(h);
                }
            }
        }

        model.setMirrors(hostlist.toArray(new Host[hostlist.size()]));
        return model;
    }

    private static int getHosterId(String urls) {
        if (urls.contains("streamcloud")) return StreamCloud.HOST_ID;
        if (urls.contains("sockshare")) return Sockshare.HOST_ID;
        if (urls.contains("divxstage")) return DivxStage.HOST_ID;
        if (urls.contains("shared.sx")) return SharedSx.HOST_ID;
        return 0;
    }

    @Override
    public ViewModel loadDetail(ViewModel item, boolean showui) {
        try {
            Document doc = super.getDocument(URL_BASE + item.getSlug() + ".html");

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
            Document doc = getDocument(url, null);
            model.setSlug(url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf(".")));

            model = parseDetail(doc, model);
            return model;
        } catch (IOException e) {
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

        } catch (IOException e) {
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
        return URL_BASE + "index.php?story=" + URLEncoder.encode(query) + "&do=search&subaction=search";
    }

    @Override
    public String getCineMovies() {
        return URL_BASE + "xfsearch/2018";
    }

    @Override
    public String getPopularMovies() {
        return URL_BASE + "filme/";
    }

    @Override
    public String getPopularSeries() {
        return URL_BASE + "serien/";
    }


    @Override
    public String PreSaveParserUrl(String newUrl){

        return newUrl.endsWith("/") ? newUrl : newUrl + "/";
    }
}
