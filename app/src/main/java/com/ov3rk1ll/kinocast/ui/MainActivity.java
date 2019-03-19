package com.ov3rk1ll.kinocast.ui;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.ov3rk1ll.kinocast.CastApp;
import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.api.DeeplinkParser;
import com.ov3rk1ll.kinocast.api.KinoxParser;
import com.ov3rk1ll.kinocast.api.NothingParser;
import com.ov3rk1ll.kinocast.api.Parser;
import com.ov3rk1ll.kinocast.ui.helper.layout.SearchSuggestionAdapter;
import com.ov3rk1ll.kinocast.utils.Utils;
import com.winsontan520.wversionmanager.library.WVersionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{
    // private final int RESOLVE_CONNECTION_REQUEST_CODE = 5001;

    private static final String STATE_TITLE = "state_title";
    private static final String STATE_IS_SEARCHVIEW = "state_is_searchview";
    private static final String NAV_ITEM_ID = "navItemId";

    private CharSequence mTitle;
    private boolean mIsSearchView = false;
    private ProgressBar mProgressBar;
    private CastContext mCastContext;
    private Map<Integer, Bundle> drawdata = new HashMap<>();

    private SearchSuggestionAdapter searchSuggestionAdapter;
    private DrawerLayout mDrawerLayout;
    private int mNavItemId;
    private final Handler mDrawerActionHandler = new Handler();
    private MenuItem searchMenuItem;
    private int mNavItemLast = -1;

    // TODO: Implement this in a better way
    @SuppressLint("StaticFieldLeak")
    public static MainActivity activity;
    @SuppressLint("StaticFieldLeak")
    public static WebView webView;

    @SuppressWarnings("deprecation")
    @Override
    public void setSupportProgressBarIndeterminateVisibility(boolean visible) {
        //super.setSupportProgressBarIndeterminateVisibility(visible);
        mProgressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public static List<Parser> getParserList(Context context) {
        List<Parser> list = new ArrayList<>();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int key = Integer.parseInt(preferences.getString("parser", "0"));
        list.add(Parser.getParser(context, key));
        if(list.size() < 1) list.add(Parser.getParser(context, KinoxParser.PARSER_ID));
        return list;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Parser.getInstance().getParserId() == DeeplinkParser.PARSER_ID){
            CastApp.getApplication().LoadParser();
        }
        //if(BuildConfig.GMS_CHECK) BaseCastManager.checkGooglePlayServices(this);
        setContentView(R.layout.activity_main);

        activity = this;
        webView = (WebView)findViewById(R.id.webview);
        Log.i("CloudflareDdos", "onCreate: static fields are ready");
        //supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        WVersionManager versionManager = new WVersionManager(this);
        versionManager.setVersionContentUrl(getString(R.string.update_check));
        versionManager.checkVersion();

        /*BackupManager bm = new BackupManager(this);
        bm.requestRestore(new RestoreObserver() {
            @Override
            public void restoreFinished(int error) {
                Log.i("BackupManager", "RestoreObserver.restoreFinished with " + error);
                super.restoreFinished(error);
            }
        });*/


        mTitle = getTitle();
        mNavItemId = R.string.title_section1;
        if (savedInstanceState != null) {
            mTitle = savedInstanceState.getString(STATE_TITLE);
            mIsSearchView = savedInstanceState.getBoolean(STATE_IS_SEARCHVIEW);
            mNavItemId = savedInstanceState.getInt(NAV_ITEM_ID);
        }

        mProgressBar = new ProgressBar(this);
        mProgressBar.setVisibility(View.GONE);
        mProgressBar.setIndeterminate(true);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mProgressBar.setIndeterminateTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accent)));
        }

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar_actionbar);
        Toolbar.LayoutParams layoutParams = new Toolbar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.END;
        toolbar.addView(mProgressBar, layoutParams);
        setSupportActionBar(toolbar);

        mCastContext = Utils.getCastContext(this);

        // listen for navigation events
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation);
        navigationView.setNavigationItemSelectedListener(this);
        Menu menuNav = navigationView.getMenu();

//        menuNav.findItem(mNavItemId).setChecked(true);

        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        //noinspection deprecation
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        navigate(mNavItemId);
    }

    @Override
    protected void onDestroy() {
        /*BackupManager bm = new BackupManager(this);
        bm.dataChanged();*/
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // TODO mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(Parser.getInstance().getParserId() == DeeplinkParser.PARSER_ID){
            CastApp.getApplication().LoadParser();
        }
        NavigationView navigationView = findViewById(R.id.navigation);
        invalidateOptionsMenu();
        Parser p = Parser.getInstance();
        Menu menu = navigationView.getMenu();

        AddParserMenu();

        // remove active state from settings
        if(mNavItemLast != -1) {
            navigationView.setCheckedItem(mNavItemLast);
            navigate(mNavItemLast);
            mNavItemLast = -1;
        }

    }

    private void AddParserMenu(){
        NavigationView navigationView = findViewById(R.id.navigation);
        navigationView.getMenu().clear();
        navigationView.inflateMenu(R.menu.drawer);
        Menu menu = navigationView.getMenu();
        drawdata.clear();
        int m_id = 1234;
        List<Parser> plist = getParserList(getApplicationContext());
        for (Parser p: plist) {
            Menu submenu = menu.addSubMenu(p.getParserName());
            List<Bundle> items = p.getMenuItems();
            for (Bundle b: items) {
                m_id++;
                String url = b.getString("url", "");
                if(Utils.isStringEmpty(url)) continue;
                b.putInt("parser", p.getParserId());
                int tid = b.getInt("title_id", 0);
                String title = (tid>0) ? getString(tid) :b.getString("title","List");
                b.putString("menu_title", title);
                int mid = submenu.add(0, m_id,0 ,title).getItemId();
                drawdata.put(mid, b);
            }
        }
        navigationView.invalidate();
    }


    private void ConfigNavItem(Parser p, Menu menuNav, int id, String url){
        String title = p.getTitleForString(id);
        if(Utils.isStringEmpty(title)) title = getString(id);
        menuNav.findItem(id).setTitle(title);
        menuNav.findItem(id).setVisible(!Utils.isStringEmpty(url));
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_TITLE, mTitle.toString());
        outState.putBoolean(STATE_IS_SEARCHVIEW, mIsSearchView);
        outState.putInt(NAV_ITEM_ID, mNavItemId);
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            //actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(mTitle);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        if (!mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            getMenuInflater().inflate(R.menu.main, menu);
            CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu, R.id.media_route_menu_item);

            searchMenuItem = menu.findItem(R.id.action_search);

            final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
            searchView.setQueryHint(getResources().getString(R.string.searchable_hint));

            //noinspection ConstantConditions
            searchSuggestionAdapter = new SearchSuggestionAdapter(
                    getSupportActionBar().getThemedContext(),
                    android.R.layout.simple_list_item_1,
                    null,
                    new String[]{"item"},
                    new int[]{android.R.id.text1},
                    0);

            searchView.setSuggestionsAdapter(searchSuggestionAdapter);
            searchView.setOnSuggestionListener(new android.support.v7.widget.SearchView.OnSuggestionListener() {
                @Override
                public boolean onSuggestionSelect(int position) {
                    return false;
                }

                @Override
                public boolean onSuggestionClick(int position) {
                    Cursor cursor = (Cursor) searchSuggestionAdapter.getItem(position);
                    doSearch(cursor.getString(1));
                    MenuItemCompat.collapseActionView(searchMenuItem);
                    return true;
                }
            });

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextChange(String newText) {
                    if(newText.length() >= 2)
                        searchSuggestionAdapter.getFilter().filter(newText);
                    return true;
                }

                @Override
                public boolean onQueryTextSubmit(String query) {
                    MenuItemCompat.collapseActionView(searchMenuItem);
                    doSearch(query);
                    return true;
                }
            });
            
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            //restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            doSearch(query);
            MenuItemCompat.collapseActionView(searchMenuItem);
        }
    }

    private void doSearch(String query){
        FragmentManager fragmentManager = getSupportFragmentManager();
        mIsSearchView = true;
        mTitle = "\"" + query + "\"";
        restoreActionBar();
        // TODO Utils.trackPath(MainActivity.this, "Search.html?q=" + query);
        fragmentManager.beginTransaction()
                .replace(R.id.container, ListFragment.newInstance(Parser.getInstance().getSearchPage(query)))
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawers();
        }else if(mIsSearchView) {
            mIsSearchView = false;
            navigate(mNavItemId);
        }else{
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem menuItem) {
        menuItem.setChecked(true);
        if(menuItem.getItemId() != R.string.title_section7) {
            mNavItemId = menuItem.getItemId();
        } else {
            mNavItemLast = mNavItemId;
        }

        // allow some time after closing the drawer before performing real navigation
        // so the user can see what is happening
        mDrawerLayout.closeDrawer(GravityCompat.START);
        mDrawerActionHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                navigate(menuItem.getItemId());
            }
        }, 250);
        return true;
    }

    private void navigate(int menuItemId){
        String query = null;
        boolean isSpecial = false;
        switch (menuItemId) {
            case R.string.title_section1:
                query = Parser.getInstance().getCineMovies();
                mTitle = getString(R.string.title_section1);
                break;
            case R.string.title_section2:
                query = Parser.getInstance().getPopularMovies();
                mTitle = getString(R.string.title_section2);
                break;
            case R.string.title_section3:
                query = Parser.getInstance().getLatestMovies();
                mTitle = getString(R.string.title_section3);
                break;
            case R.string.title_section4:
                query = Parser.getInstance().getPopularSeries();
                mTitle = getString(R.string.title_section4);
                break;
            case R.string.title_section5:
                query = Parser.getInstance().getLatestSeries();
                mTitle = getString(R.string.title_section5);
                break;
            case R.string.title_section6:
                query = String.valueOf(ListFragment.SPECIAL_BOOKMARKS);
                isSpecial = true;
                mTitle = getString(R.string.title_section6);
                break;
            case R.string.title_section7:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            default:
              if(drawdata.containsKey(menuItemId)){
                  Bundle b = drawdata.get(menuItemId);
                  Parser.setInstance(Parser.getParser(this, b.getInt("parser", 0)));
                  query = b.getString("url");
                  mTitle = b.getString("menu_title");
              }
              break;
        }
        restoreActionBar();

        if(query != null) {
            ListFragment fragment;
            if(isSpecial){
                int special = Integer.valueOf(query);
                fragment = ListFragment.newInstance(special);
                query = "Special/";
                switch (special){
                    case ListFragment.SPECIAL_BOOKMARKS: query += "Bookmarks.html";
                }
            } else {
                fragment = ListFragment.newInstance(query);
            }
            // TODO Utils.trackPath(MainActivity.this, query);
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        }
    }
}
