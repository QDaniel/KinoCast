package com.ov3rk1ll.kinocast.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.flurry.android.FlurryAgent;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.common.images.WebImage;
import com.ov3rk1ll.kinocast.CastApp;
import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.api.Parser;
import com.ov3rk1ll.kinocast.api.mirror.Direct;
import com.ov3rk1ll.kinocast.api.mirror.Host;
import com.ov3rk1ll.kinocast.data.Season;
import com.ov3rk1ll.kinocast.data.ViewModel;
import com.ov3rk1ll.kinocast.ui.helper.PaletteManager;
import com.ov3rk1ll.kinocast.ui.util.glide.OkHttpViewModelStreamFetcher;
import com.ov3rk1ll.kinocast.ui.util.glide.ViewModelGlideRequest;
import com.ov3rk1ll.kinocast.utils.BookmarkManager;
import com.ov3rk1ll.kinocast.utils.ExceptionAsyncTask;
import com.ov3rk1ll.kinocast.utils.TheMovieDb;
import com.ov3rk1ll.kinocast.utils.Utils;
import com.ov3rk1ll.kinocast.utils.WeightedHostComparator;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("ConstantConditions")
public class DetailActivity extends AppCompatActivity implements ActionMenuView.OnMenuItemClickListener {
    public static final String ARG_ITEM = "param_item";
    private ViewModel item;
    private Parser parser;
    private RelativeLayout  mAdView;

    private CastContext mCastContext;

    @SuppressWarnings("FieldCanBeLocal")
    private boolean SHOW_ADS = true;

    private BookmarkManager bookmarkManager;
    private CollapsingToolbarLayout collapsingToolbarLayout;

    private int mRestoreSeasonIndex = -1;
    private int mRestoreEpisodeIndex = -1;

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 2;
    public static DetailActivity activity = null;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        supportFinishAfterTransition();
    }


    private void checkPerm(){

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this.getBaseContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Toast.makeText(getApplicationContext(),"Asking for permission to Save/Load Bookmarks to SDCard", Toast.LENGTH_LONG).show();
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions( this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
        }

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this.getBaseContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Toast.makeText(getApplicationContext(),"Asking for permission to Save/Load Bookmarks to SDCard", Toast.LENGTH_SHORT).show();
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions( this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
        }


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;

        //if (BuildConfig.GMS_CHECK) BaseCastManager.checkGooglePlayServices(this);
        mCastContext = Utils.getCastContext(this);

        setContentView(R.layout.activity_detail);

        // actionBar.setDisplayHomeAsUpEnabled(true);

        //((ActionMenuView) findViewById(R.id.bar_split)).setOnMenuItemClickListener(this);

        checkPerm();
        bookmarkManager = new BookmarkManager(getApplication());
        bookmarkManager.restore();

        item = (ViewModel) getIntent().getSerializableExtra(ARG_ITEM);

        if (item == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        parser = item.getParser(this);

        initToolbar();
        initInstances();
        attemptColor(null);

        findViewById(R.id.button_donate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.paypal_donate)));
                startActivity(intent);
            }
        });

        FlurryAgent.onStartSession(this);

        mAdView = (RelativeLayout)findViewById(R.id.adView);
        if (SHOW_ADS) {
            mAdView.setVisibility(View.GONE);
            findViewById(R.id.donateView).setVisibility(View.VISIBLE);
        } else {
            mAdView.setVisibility(View.GONE);
            findViewById(R.id.donateView).setVisibility(View.GONE);
            findViewById(R.id.hr2).setVisibility(View.GONE);
        }

        int screenWidthPx = getResources().getDisplayMetrics().widthPixels;

        ((TextView) findViewById(R.id.detail)).setText(item.getSummary());

        final ImageView headerImage = (ImageView) findViewById(R.id.image_header);
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        findViewById(R.id.progressBar).invalidate();
        Glide.with(this)
                .load(new ViewModelGlideRequest(item, screenWidthPx, "backdrop"))
                .placeholder(R.drawable.ic_loading_placeholder)
                .listener(new RequestListener<ViewModelGlideRequest, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, ViewModelGlideRequest model, Target<GlideDrawable> target, boolean isFirstResource) {
                        if(e != null) e.printStackTrace();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, ViewModelGlideRequest model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        findViewById(R.id.progressBar).clearAnimation();
                        findViewById(R.id.progressBar).setVisibility(View.GONE);
                        findViewById(R.id.progressBar).invalidate();
                        findViewById(R.id.top_content).invalidate();
                        attemptColor(((GlideBitmapDrawable)resource.getCurrent()).getBitmap());
                        return false;
                    }
                })
                .into(headerImage);
        headerImage.setVisibility(View.VISIBLE);

        ((ImageView) findViewById(R.id.language)).setImageResource(item.getLanguageResId());

        ((Spinner) findViewById(R.id.spinnerSeason)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                final Spinner spinnerEpisode = (Spinner) findViewById(R.id.spinnerEpisode);

                spinnerEpisode.setAdapter(
                        new ArrayAdapter<>(DetailActivity.this, android.R.layout.simple_list_item_1, item.getSeasons()[position].episodes));

                if (mRestoreEpisodeIndex != -1) {
                    spinnerEpisode.setSelection(mRestoreEpisodeIndex);
                    mRestoreEpisodeIndex = -1;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ((Spinner) findViewById(R.id.spinnerEpisode)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                (new QueryHosterTask()).execute((Void) null);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        findViewById(R.id.buttonPlay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!com.ov3rk1ll.kinocast.utils.Utils.isWifiConnected(DetailActivity.this)) {
                    new AlertDialog.Builder(DetailActivity.this)
                            .setMessage(getString(R.string.player_warn_no_wifi))
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    (new QueryPlayTask(DetailActivity.this)).execute((Void) null);
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                } else {
                    (new QueryPlayTask(DetailActivity.this)).execute((Void) null);
                }
            }
        });

        (new QueryDetailTask()).execute((Void) null);
    }

    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void initInstances() {
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsingToolbarLayout);
        collapsingToolbarLayout.setTitle(item.getTitle());
    }

    boolean hasColors = false;

    private void attemptColor(Bitmap bitmap) {
        if (hasColors) return;
        PaletteManager.getInstance().getPalette(item.getSlug(), bitmap, new PaletteManager.Callback() {
            @Override
            public void onPaletteReady(Palette palette) {
                if (palette == null) return;
                Palette.Swatch swatch = palette.getDarkVibrantSwatch();
                if (swatch != null) {
                    collapsingToolbarLayout.setContentScrimColor(swatch.getRgb());

                    findViewById(R.id.hr1).setBackgroundColor(swatch.getRgb());
                    findViewById(R.id.hr2).setBackgroundColor(swatch.getRgb());

                    //collapsingToolbarLayout.setCollapsedTitleTextColor(swatch.getTitleTextColor());
                    //collapsingToolbarLayout.setExpandedTitleColor(swatch.getTitleTextColor());

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Log.i("progressBar", "set color to " + swatch.getRgb());
                        ((ProgressBar) findViewById(R.id.progressBar)).setIndeterminateTintList(ColorStateList.valueOf(swatch.getRgb()));
                        float hsv[] = new float[3];
                        Color.colorToHSV(swatch.getRgb(), hsv);
                        hsv[2] = 0.2f;
                        getWindow().setStatusBarColor(Color.HSVToColor(hsv));
                        Log.i("progressBar", "Visibility in color = " + findViewById(R.id.progressBar).getVisibility());
                    }

                    hasColors = true;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        CastApp.setCurrentActivity(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        //Update Bookmark to keep series info
        if (item.getType() == ViewModel.Type.SERIES) {
            BookmarkManager.Bookmark b = new BookmarkManager.Bookmark(parser.getParserId(), parser.getPageLink(item));
            b.setSeason(((Spinner) findViewById(R.id.spinnerSeason)).getSelectedItemPosition());
            b.setEpisode(((Spinner) findViewById(R.id.spinnerEpisode)).getSelectedItemPosition());
            int idx = bookmarkManager.indexOf(b);
            if (idx == -1) {
                bookmarkManager.add(b);
            } else {
                b.setInternal(bookmarkManager.get(idx).isInternal());
                bookmarkManager.set(idx, b);
            }
        }
        //if(mAdView != null) mAdView.onPause();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.detail, menu);
        //menu = ((ActionMenuView) findViewById(R.id.bar_split)).getMenu();
        menu.clear();
        getMenuInflater().inflate(R.menu.detail, menu);
        CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu, R.id.media_route_menu_item);
        // Set visibility depending on detail data
        menu.findItem(R.id.action_imdb).setVisible(item.getImdbId() != null);

        BookmarkManager.Bookmark b = bookmarkManager.findItem(this.item);
        if (b != null && !b.isInternal()) {
            menu.findItem(R.id.action_bookmark_on).setVisible(true);
            menu.findItem(R.id.action_bookmark_off).setVisible(false);
        } else {
            menu.findItem(R.id.action_bookmark_on).setVisible(false);
            menu.findItem(R.id.action_bookmark_off).setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            supportFinishAfterTransition();
            //NavUtils.navigateUpFromSameTask(this);
            return true;
        } else if (id == R.id.action_share) {
            openUrl(parser.getPageLink(this.item));
            return true;
        } else if (id == R.id.action_imdb) {
            openUrl("http://www.imdb.com/title/" + this.item.getImdbId());
            return true;
        } else if (id == R.id.action_bookmark_on) {
            //Remove bookmark
            bookmarkManager.remove(new BookmarkManager.Bookmark(
                    parser.getParserId(),
                    parser.getPageLink(this.item))
            );
            //Show confirmation
            Toast.makeText(getApplication(), getString(R.string.detail_bookmark_on_confirm), Toast.LENGTH_SHORT).show();
            supportInvalidateOptionsMenu();
            return true;
        } else if (id == R.id.action_bookmark_off) {
            //Add bookmark
            bookmarkManager.addAsPublic(new BookmarkManager.Bookmark(
                    parser.getParserId(),
                    parser.getPageLink(this.item))
            );
            //Show confirmation
            Toast.makeText(getApplication(), getString(R.string.detail_bookmark_off_confirm), Toast.LENGTH_SHORT).show();
            supportInvalidateOptionsMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void setMirrorSpinner(Host mirrors[]) {
        if (mirrors != null && mirrors.length > 0) {
            Arrays.sort(mirrors, new WeightedHostComparator(Utils.getWeightedHostList(getApplicationContext())));
            ((Spinner) findViewById(R.id.spinnerMirror)).setAdapter(
                    new ArrayAdapter<>(DetailActivity.this, android.R.layout.simple_list_item_1,
                            mirrors));
            findViewById(R.id.spinnerMirror).setEnabled(true);
            findViewById(R.id.buttonPlay).setEnabled(true);
        } else {
            ((Spinner) findViewById(R.id.spinnerMirror)).setAdapter(
                    new ArrayAdapter<>(DetailActivity.this, android.R.layout.simple_list_item_1,
                            new String[]{getString(R.string.no_host_found)}));
            findViewById(R.id.spinnerMirror).setEnabled(false);
            findViewById(R.id.buttonPlay).setEnabled(false);
        }
        findViewById(R.id.layoutMirror).setVisibility(mirrors == null ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        return this.onOptionsItemSelected(menuItem);
    }


    public boolean isCastConnected() {
        if(mCastContext == null) return false;
        CastSession castSession = mCastContext
                .getSessionManager()
                .getCurrentCastSession();
        return (castSession != null && castSession.isConnected());
    }

    private class QueryDetailTask extends ExceptionAsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            // Set loader for content
            findViewById(R.id.buttonPlay).setEnabled(false);
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground() throws Exception {
            item = parser.loadDetail(item, true);
            if(item!=null) {
                Map<String, String> articleParams = new HashMap<>();
                articleParams.put("parser", parser.getParserName());
                articleParams.put("video_name", item.getTitle());
                articleParams.put("video_type", item.getType() == ViewModel.Type.MOVIE ? "Movie" : "Series");
                articleParams.put("video_imdb", item.getImdbId());
                articleParams.put("video_url", parser.getPageLink(item));
                FlurryAgent.logEvent("Movie_Detail", articleParams);
            }

            String path = Environment.getExternalStorageDirectory().toString() + File.separator + Environment.DIRECTORY_DOWNLOADS;
            File file = new File(path + File.separator + item.getImdbId() + "_" + item.getLanguageResId() + ".mp4");

            if(file.exists() && file.isFile()){
                List<Host> hosts = new ArrayList<>(Arrays.asList(item.getMirrors()));
                Direct d = new Direct();
                d.setName("Download");
                d.setUrl(file.getAbsoluteFile().getPath());
                hosts.add(0, d);
                item.setMirrors(hosts.toArray(new Host[0]));
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if(getException() != null) {
                Log.e("QueryDetailTask","can't load details", getException());
                return;
            }

            if (item.getType() == ViewModel.Type.SERIES) {
                BookmarkManager.Bookmark b = bookmarkManager.findItem(item);
                if (b != null) {
                    mRestoreSeasonIndex = b.getSeason();
                    mRestoreEpisodeIndex = b.getEpisode();
                }

                String seasons[] = new String[item.getSeasons() == null ? 0 :  item.getSeasons().length];
                for (int i = 0; i < seasons.length; i++) {
                    seasons[i] = String.valueOf(item.getSeasons()[i].id);
                }
                ((Spinner) findViewById(R.id.spinnerSeason)).setAdapter(
                        new ArrayAdapter<>(DetailActivity.this, android.R.layout.simple_list_item_1, seasons));

                if (mRestoreSeasonIndex != -1) {
                    ((Spinner) findViewById(R.id.spinnerSeason)).setSelection(mRestoreSeasonIndex);
                    mRestoreSeasonIndex = -1;
                }
                findViewById(R.id.layoutSeries).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.layoutSeries).setVisibility(View.GONE);
                if(item.getMirrors() == null)
                    (new QueryHosterTask()).execute((Void) null);

                setMirrorSpinner(item.getMirrors());
            }

            ((TextView) findViewById(R.id.detail)).setText(item.getSummary());
            ActivityCompat.invalidateOptionsMenu(DetailActivity.this);
        }
    }

    private class QueryHosterTask extends ExceptionAsyncTask<Void, Void, List<Host>> {
        Season s;
        int position;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            findViewById(R.id.layoutMirror).setVisibility(View.GONE);
            if (item.getType() == ViewModel.Type.SERIES) {
                s = item.getSeasons()[((Spinner) findViewById(R.id.spinnerSeason)).getSelectedItemPosition()];
                position = ((Spinner) findViewById(R.id.spinnerEpisode)).getSelectedItemPosition();
            }
        }

        @Override
        protected List<Host> doInBackground() throws Exception {
            List<Host> list;
            if (item.getType() == ViewModel.Type.SERIES) {
                list = parser.getHosterList(item, s.id, s.episodes[position]);
            } else {
                list = parser.getHosterList(item, 0, "");
            }
            return list;
        }

        @Override
        protected void onPostExecute(List<Host> list) {
            super.onPostExecute(list);
            if(getException() != null) {
                Log.e("QueryHosterTask","can't load hoster", getException());
                return;
            }
            setMirrorSpinner(list == null ? null : list.toArray(new Host[list.size()]));
        }
    }

    public class QueryPlayTask extends ExceptionAsyncTask<Void, String, String> {
        private ProgressDialog progressDialog;
        private Context context;
        Host host;
        int spinnerSeasonItemPosition;
        int spinnerEpisodeItemPosition;

        public QueryPlayTask(Context context) {
            this.context = context;
            progressDialog = new ProgressDialog(context);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage(getString(R.string.loading));
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    cancel(true);
                }
            });
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    cancel(true);
                }
            });
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.show();
            //noinspection unchecked
            ArrayAdapter<Host> hosts = (ArrayAdapter<Host>) ((Spinner) findViewById(R.id.spinnerMirror)).getAdapter();
            host = hosts.getItem(((Spinner) findViewById(R.id.spinnerMirror)).getSelectedItemPosition());
            spinnerSeasonItemPosition = ((Spinner) findViewById(R.id.spinnerSeason)).getSelectedItemPosition();
            spinnerEpisodeItemPosition = ((Spinner) findViewById(R.id.spinnerEpisode)).getSelectedItemPosition();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            progressDialog.dismiss();
        }

        public void updateProgress(String... values){
            publishProgress(values);
        }

        @Override
        protected String doInBackground() throws Exception {
            String video = host.getVideoUrl();
            if(video == null || video.isEmpty()) {

                String link = host.getUrl();
                if (link == null || link.isEmpty()) {
                    if (item.getType() == ViewModel.Type.SERIES) {
                        Season s = item.getSeasons()[spinnerSeasonItemPosition];
                        String e = s.episodes[spinnerEpisodeItemPosition];
                        link = parser.getMirrorLink(this, item, host, s.id, e);
                    } else {
                        link = parser.getMirrorLink(this, item, host);
                    }
                    if(link.contains("streamcrypt.net/")) link = Utils.getMultiRedirectTarget(link);
                    host.setUrl(link);
                }
                video = host.getVideoPath(this);
                host.setVideoUrl(video);
            }
            return video;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            progressDialog.setMessage(getString(R.string.loading) + "\n" + values[0]);
        }

        @SuppressWarnings("deprecation")
        @Override
        protected void onPostExecute(final String link) {
            super.onPostExecute(link);
            progressDialog.dismiss();

            if(getException() != null) {
                Log.e("QueryHosterTask","can't load player", getException());
                return;
            }

            Boolean isDl = host.getName().equals("Download");

            if (!TextUtils.isEmpty(link)) {
                Log.i("Play", "Getting player for '" + link + "'");
                final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                intent.setDataAndType(Uri.parse(link), "video/mp4");

                AlertDialog.Builder builder = new AlertDialog.Builder(DetailActivity.this);
                builder.setTitle(getString(R.string.player_select_dialog_title));

                PackageManager pm = getPackageManager();
                List<ResolveInfo> launchables = pm.queryIntentActivities(intent, 0);
                List<AppAdapter.App> apps = new ArrayList<>();
                Collections.sort(launchables, new ResolveInfo.DisplayNameComparator(pm));
                if (!isDl && isCastConnected()) {
                    apps.add(new AppAdapter.App(
                            getString(R.string.player_chromecast_list_entry),
                            getResources().getDrawable(R.drawable.ic_player_chromecast),
                            null
                    ));
                }
                if (!isDl && !link.contains(".m3u8") && Utils.isDownloadManagerAvailable(getContext())) {
                    apps.add(new AppAdapter.App(
                            getString(R.string.player_download_list_entry),
                            getResources().getDrawable(R.drawable.ic_player),
                            new ComponentName(DetailActivity.this, DetailActivity.class)
                    ));
                }

                apps.add(new AppAdapter.App(
                        getString(R.string.player_internal_list_entry),
                        getResources().getDrawable(R.drawable.ic_player),
                        new ComponentName(DetailActivity.this, ExoPlayerActivity.class)
                ));
                for (ResolveInfo resolveInfo : launchables) {
                    ActivityInfo activity = resolveInfo.activityInfo;
                    AppAdapter.App app = new AppAdapter.App(
                            resolveInfo.loadLabel(pm),
                            resolveInfo.loadIcon(pm),
                            new ComponentName(activity.applicationInfo.packageName, activity.name)
                    );
                    apps.add(app);
                }

                final AppAdapter adapter = new AppAdapter(DetailActivity.this, apps, pm);
                builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int position) {
                        AppAdapter.App app = adapter.getItem(position);

                        Map<String, String> articleParams = new HashMap<>();
                        articleParams.put("parser", parser.getParserName());
                        articleParams.put("video_name", item.getTitle());
                        articleParams.put("video_type", item.getType() == ViewModel.Type.MOVIE ? "Movie" : "Series");
                        articleParams.put("video_imdb", item.getImdbId());
                        articleParams.put("video_url", parser.getPageLink(item));
                        articleParams.put("host_name", host.getName());
                        articleParams.put("host_url", host.getUrl());
                        articleParams.put("host_videourl", host.getVideoUrl());
                        articleParams.put("Player",(app.getComponent() == null) ? "Chromecast" : app.getComponent().toString());
                        FlurryAgent.logEvent("Movie_Play", articleParams);

                        if (app.getComponent() == null) {
                            startPlaybackOnChromecast(link);
                        }
                        else if (app.getComponent().getClassName() == DetailActivity.class.getName()) {
                            startDownload(link);
                        } else {
                            intent.setComponent(app.getComponent());
                            startActivity(intent);
                        }
                        dialog.dismiss();
                    }
                });
                final AlertDialog dialog = builder.create();
                //dialog.getListView().setDivider(getResources().getDrawable(R.drawable.abc_list_divider_holo_light));
                dialog.getListView().setDividerHeight(1);
                dialog.show();

            } else { // no link found
                Toast.makeText(DetailActivity.this, getString(R.string.host_resolve_error), Toast.LENGTH_SHORT).show();
            }
        }

        public Context getContext() {
            return context;
        }
        public ProgressDialog getDialog() {
            return progressDialog;
        }
    }

    public void startPlaybackOnChromecast(String link) {
        if (mCastContext == null) return;
        MediaMetadata mediaMetadata;
        if (item.getType() == ViewModel.Type.SERIES) {
            Season s = item.getSeasons()[((Spinner) findViewById(R.id.spinnerSeason)).getSelectedItemPosition()];
            String e = s.episodes[((Spinner) findViewById(R.id.spinnerEpisode)).getSelectedItemPosition()];
            mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_TV_SHOW);
            try {
                mediaMetadata.putString(MediaMetadata.KEY_TITLE, String.format("%s - Folge S%02dE%02d", item.getTitle(), s.id, Integer.parseInt(e)));
            } catch (Exception ex)
            {
                mediaMetadata.putString(MediaMetadata.KEY_TITLE, String.format("%s - Folge S%02dE%s", item.getTitle(), s.id, e));
            }
        } else {
            mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
            mediaMetadata.putString(MediaMetadata.KEY_TITLE, item.getTitle());
        }
        mediaMetadata.putString(MediaMetadata.KEY_SUBTITLE, getString(R.string.chromecast_subtitle));



        // Use TheMovieDb to get the image
        String url = getCachedImage(96, "poster");
        Log.i("Chromecast", "use image: " + url);
        if(TextUtils.isEmpty(url)) url = "https://kinoca.st/img/kinocast_icon_512.png";
        mediaMetadata.addImage(new WebImage(Uri.parse(url)));

        // TODO Use Glide to get image
        url = getCachedImage(getResources().getDisplayMetrics().widthPixels, "poster"); // new CoverImage(item.getImageRequest(getResources().getDisplayMetrics().widthPixels, "poster")).getBitmapUrl(getApplication());
        Log.i("Chromecast", "use image: " + url);
        if(TextUtils.isEmpty(url)) url = "https://kinoca.st/img/kinocast_icon_512.png";
        mediaMetadata.addImage(new WebImage(Uri.parse(url)));
        Log.i("cast", "play " + link);
        MediaInfo mediaInfo = new MediaInfo.Builder(link)
                .setContentType("video/mp4")
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(mediaMetadata)
                .build();

        mCastContext.getSessionManager()
                .getCurrentCastSession()
                .getRemoteMediaClient()
                .load(mediaInfo, true);
    }

    public void startDownload(String link) {
        if (ContextCompat.checkSelfPermission(this.getBaseContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "No write Permissions", Toast.LENGTH_LONG).show();
            return;
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(link));
        request.setDescription(item.getSummary());
        request.setTitle(item.getTitle());
        // in order for this if to run, you must use the android 3.2 to compile your app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        }
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, item.getImdbId()+ "_" + item.getLanguageResId() + ".mp4");
        // get download service and enqueue file
        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
    }

    private String getCachedImage(int size, String type){
        TheMovieDb tmdbCache = new TheMovieDb(CastApp.GetCheckedContext(getApplication()));
        String cacheUrl = parser.getImdbLink(item);
        JSONObject json = tmdbCache.get(cacheUrl, item, false);
        if(json != null){
            try {
                String key = type + "_path";
                if (type.equals("backdrop"))
                    return TheMovieDb.IMAGE_BASE_PATH + OkHttpViewModelStreamFetcher.getBackdropSize(size) + json.getString(key);
                else
                    return TheMovieDb.IMAGE_BASE_PATH + OkHttpViewModelStreamFetcher.getPosterSize(size) + json.getString(key);
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
        return "";
    }

    static class AppAdapter extends ArrayAdapter<AppAdapter.App> {
        PackageManager pm;

        AppAdapter(Context context, List<AppAdapter.App> objects, PackageManager pm) {
            super(context, R.layout.player_list_item, android.R.id.text1, objects);
            this.pm = pm;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            App item = getItem(position);
            View view = super.getView(position, convertView, parent);
            ((TextView) view.findViewById(android.R.id.text1)).setText(item.getLabel());
            ((ImageView) view.findViewById(android.R.id.icon)).setImageDrawable(item.getIcon());
            return view;
        }

        @SuppressWarnings("unused")
        static class App {
            private CharSequence label;
            private Drawable icon;
            private ComponentName component;

            App(CharSequence label, Drawable icon, ComponentName component) {
                this.label = label;
                this.icon = icon;
                this.component = component;
            }

            CharSequence getLabel() {
                return label;
            }

            public void setLabel(CharSequence label) {
                this.label = label;
            }

            public Drawable getIcon() {
                return icon;
            }

            public void setIcon(Drawable icon) {
                this.icon = icon;
            }

            ComponentName getComponent() {
                return component;
            }

            public void setComponent(ComponentName component) {
                this.component = component;
            }
        }
    }
}
