package com.ov3rk1ll.kinocast.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.ov3rk1ll.kinocast.api.DeeplinkParser;
import com.ov3rk1ll.kinocast.api.Parser;
import com.ov3rk1ll.kinocast.api.mirror.Host;
import com.ov3rk1ll.kinocast.data.ViewModel;
import com.ov3rk1ll.kinocast.utils.Utils;

import java.util.Iterator;
import java.util.Set;


public class DeeplinkActivity extends AppCompatActivity {

    private static final String LOG_TAG = "Deeplink";
    private String url = "";
    private String subject = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        dumpIntent(getIntent());
        Uri uri = getIntent().getData();
        if(uri != null) url = uri.toString();
        if(Utils.isStringEmpty(url)) url = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        subject = url;
        String subExt = getIntent().getStringExtra(Intent.EXTRA_SUBJECT);
        if(!Utils.isStringEmpty(subExt)) subject = subExt;

        if(!Utils.isStringEmpty(url)){
            DeeplinkParser dp = new DeeplinkParser();
            ViewModel viewModel =  dp.loadDetail(url);

            if(viewModel != null){
                viewModel.setTitle(subExt);
                Parser.setInstance(dp);
                Intent intent = new Intent(this, DetailActivity.class);
                intent.putExtra(DetailActivity.ARG_ITEM, viewModel);
                intent.putExtra(DetailActivity.ARG_ITEM, viewModel);
                startActivity(intent);
                finish();
                url = null;
                return;
            }
        }

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();

    }
    public static void dumpIntent(Intent i){

        Log.d(LOG_TAG,"Dumping Intent start");
        Log.d(LOG_TAG," - Type = " + i.getType());
        Log.d(LOG_TAG," - Action = " + i.getAction());
        Log.d(LOG_TAG," - Package = " + i.getPackage());
        Log.d(LOG_TAG," - Scheme = " + i.getScheme());
        Bundle bundle = i.getExtras();
        if (bundle != null) {

            Set<String> keys = bundle.keySet();
            Iterator<String> it = keys.iterator();
            while (it.hasNext()) {
                String key = it.next();
                Log.d(LOG_TAG,"[" + key + "=" + bundle.get(key)+"]");
            }
        }
        Log.d(LOG_TAG,"Dumping Intent end");
    }
}
