package com.ov3rk1ll.kinocast.api;

import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.api.mirror.Direct;
import com.ov3rk1ll.kinocast.api.mirror.Host;
import com.ov3rk1ll.kinocast.data.ViewModel;
import com.ov3rk1ll.kinocast.ui.DetailActivity;
import com.ov3rk1ll.kinocast.utils.Utils;

import org.jsoup.Connection;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class TVStreamsParser extends Parser {
    public static final int PARSER_ID = 6;
    public static final String URL_DEFAULT = "https://raw.githubusercontent.com/jnk22/kodinerds-iptv/master/iptv/clean/clean_tv_main.m3u";
    public static final String TAG = "TVStreamsParser";

    private static final SparseIntArray languageResMap = new SparseIntArray();
    private static final SparseArray<String> languageKeyMap = new SparseArray<>();

    private List<ViewModel> lastModels;

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
        return "M3U Streams";
    }

    @Override
    public int getParserId() {
        return PARSER_ID;
    }

    private List<ViewModel> parseListM3U(String doc) {
        List<ViewModel> list = new ArrayList<>();
        try {
            M3UParser m3up = new M3UParser();
            M3UPlaylist mlist = m3up.parseFile(doc);
            for (M3UItem item : mlist.getPlaylistItems()) {
                ViewModel model = new ViewModel();
                model.setParserId(PARSER_ID);
                model.setSlug(item.getItemUrl());
                model.setTitle(item.getItemName());
                model.setType(ViewModel.Type.MOVIE);
                model.setImage((item.getItemIcon() == null) ?"" : item.getItemIcon());
                model.setSummary(item.getItemName());
                model.setLanguageResId(R.drawable.lang_de);

                List<Host> hostlist = new ArrayList<>();
                Host h = new Direct();
                h.setMirror(1);
                h.setSlug(item.getItemUrl());
                h.setUrl(item.getItemUrl());
                if (h.isEnabled()) {
                    hostlist.add(h);
                }
                model.setMirrors(hostlist.toArray(new Host[hostlist.size()]));
                list.add(model);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing " + doc.toString(), e);
        }
        lastModels = list;
        return list;
    }

    @Override
    public List<ViewModel> parseList(String url) throws IOException {
        Log.i("Parser", "parseList: " + url);

        Connection conn = Utils.buildJsoup(url);
        String data = conn.ignoreContentType(true)
                .maxBodySize(0)
                .timeout(600000)
                .execute()
                .body();
        return parseListM3U(data);
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
        for ( ViewModel m: lastModels) {
            if(url.equalsIgnoreCase(m.getSlug())) return m;
        }
        return null;
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
        return item.getSlug() + "/" + Integer.toString(item.getLanguageResId());
    }

    @SuppressWarnings("deprecation")
    @Override
    public String getSearchPage(String query) {
        term = query;
        rating = "1";
        lang = "0";
        return URL_BASE + "search";
    }

    private String term = "";
    private String lang = "0";
    private String rating = "1";

    @Override
    public String getCineMovies() {
        term="";
        rating = "1";
        lang = "2";
        return URL_BASE;
    }

    @Override
    public String getPopularMovies() {
        rating = "5";
        lang = "0";
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


    class M3UItem {

        private String itemDuration;

        private String itemName;

        private String itemUrl;

        private String itemIcon;

        public String getItemDuration() {
            return itemDuration;
        }

        public void setItemDuration(String itemDuration) {
            this.itemDuration = itemDuration;
        }

        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

        public String getItemUrl() {
            return itemUrl;
        }

        public void setItemUrl(String itemUrl) {
            this.itemUrl = itemUrl;
        }

        public String getItemIcon() {
            return itemIcon;
        }

        public void setItemIcon(String itemIcon) {
            this.itemIcon = itemIcon;
        }
    }

    class M3UPlaylist {

        private String playlistName;

        private String playlistParams;

        private List<M3UItem> playlistItems;

        List<M3UItem> getPlaylistItems() {
            return playlistItems;
        }

        void setPlaylistItems(List<M3UItem> playlistItems) {
            this.playlistItems = playlistItems;
        }

        public String getPlaylistName() {
            return playlistName;
        }

        public void setPlaylistName(String playlistName) {
            this.playlistName = playlistName;
        }

        public String getPlaylistParams() {
            return playlistParams;
        }

        public void setPlaylistParams(String playlistParams) {
            this.playlistParams = playlistParams;
        }

        public String getSingleParameter(String paramName) {
            String[] paramsArray = this.playlistParams.split(" ");
            for (String parameter : paramsArray) {
                if (parameter.contains(paramName)) {
                    return parameter.substring(parameter.indexOf(paramName) + paramName.length()).replace("=", "");
                }
            }
            return "";
        }
    }

    public class M3UParser {

        private static final String EXT_M3U = "#EXTM3U";
        private static final String EXT_INF = "#EXTINF:";
        private static final String EXT_PLAYLIST_NAME = "#PLAYLIST";
        private static final String EXT_LOGO = "tvg-logo";
        private static final String EXT_URL = "http";

        public M3UPlaylist parseFile(String stream) throws FileNotFoundException {
            M3UPlaylist m3UPlaylist = new M3UPlaylist();
            List<M3UItem> playlistItems = new ArrayList<>();
            String linesArray[] = stream.split(EXT_INF);
            for (int i = 0; i < linesArray.length; i++) {
                String currLine = linesArray[i];
                if (currLine.contains(EXT_M3U)) {
                    //header of file
                    if (currLine.contains(EXT_PLAYLIST_NAME)) {
                        String fileParams = currLine.substring(EXT_M3U.length(), currLine.indexOf(EXT_PLAYLIST_NAME));
                        String playListName = currLine.substring(currLine.indexOf(EXT_PLAYLIST_NAME) + EXT_PLAYLIST_NAME.length()).replace(":", "");
                        m3UPlaylist.setPlaylistName(playListName);
                        m3UPlaylist.setPlaylistParams(fileParams);
                    } else {
                        m3UPlaylist.setPlaylistName("Noname Playlist");
                        m3UPlaylist.setPlaylistParams("No Params");
                    }
                } else {
                    M3UItem playlistItem = new M3UItem();
                    String[] dataArray = currLine.split(",");
                    if (dataArray[0].contains(EXT_LOGO)) {
                        String duration = dataArray[0].substring(0, dataArray[0].indexOf(EXT_LOGO)).replace(":", "").replace("\n", "");
                        String icon = dataArray[0].substring(dataArray[0].indexOf(EXT_LOGO) + EXT_LOGO.length()).replace("=", "").replace("\"", "").replace("\n", "");
                        playlistItem.setItemDuration(duration);
                        playlistItem.setItemIcon(icon);
                    } else {
                        String duration = dataArray[0].replace(":", "").replace("\n", "");
                        playlistItem.setItemDuration(duration);
                        playlistItem.setItemIcon("");
                    }
                    try {
                        String url = dataArray[1].substring(dataArray[1].indexOf(EXT_URL)).replace("\n", "").replace("\r", "");
                        String name = dataArray[1].substring(0, dataArray[1].indexOf(EXT_URL)).replace("\n", "");
                        playlistItem.setItemName(name);
                        playlistItem.setItemUrl(url);
                    } catch (Exception fdfd) {
                        Log.e("Google", "Error: " + fdfd.fillInStackTrace());
                    }
                    playlistItems.add(playlistItem);
                }
            }
            m3UPlaylist.setPlaylistItems(playlistItems);
            return m3UPlaylist;
        }
    }
}
