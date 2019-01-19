package com.ov3rk1ll.kinocast.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;
import com.ov3rk1ll.kinocast.CastApp;
import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.api.Parser;
import com.ov3rk1ll.kinocast.data.ViewModel;
import com.ov3rk1ll.kinocast.ui.helper.layout.GridLayoutManager;
import com.ov3rk1ll.kinocast.ui.helper.layout.ResultRecyclerAdapter;
import com.ov3rk1ll.kinocast.utils.BookmarkManager;
import com.ov3rk1ll.kinocast.utils.ExceptionAsyncTask;
import com.ov3rk1ll.kinocast.utils.TheMovieDb;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ListFragment extends Fragment {
    private static final String ARG_URL = "param_url";
    private static final String ARG_SPECIAL = "param_special";

    public static final int SPECIAL_BOOKMARKS = 0;

    ResultRecyclerAdapter adapter;
    private String mUrl;
    private int mSpecialId = -1;

    public static ListFragment newInstance(String url) {
        ListFragment fragment = new ListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    public static ListFragment newInstance(int specialId) {
        ListFragment fragment = new ListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SPECIAL, specialId);
        fragment.setArguments(args);
        return fragment;
    }

    public ListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            if(getArguments().containsKey(ARG_URL))
                mUrl = getArguments().getString(ARG_URL);
            if(getArguments().containsKey(ARG_SPECIAL))
                mSpecialId = getArguments().getInt(ARG_SPECIAL);
        }
        adapter = new ResultRecyclerAdapter(getActivity(), R.layout.frament_list_item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity());

        DisplayMetrics displayMetrics = getActivity().getResources().getDisplayMetrics();
        int columns = displayMetrics.widthPixels / getActivity().getResources().getDimensionPixelSize(R.dimen.list_item_width);
        if(columns < 2){
            columns = 2;
        }
        gridLayoutManager.setColumns(columns);

        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        adapter.setOnItemClickListener(new ResultRecyclerAdapter.OnRecyclerViewItemClickListener<ViewModel>() {
            @Override public void onItemClick(View view, ViewModel viewModel) {
                Intent intent = new Intent(getActivity(), DetailActivity.class);
                intent.putExtra(DetailActivity.ARG_ITEM, viewModel);
                intent.putExtra(DetailActivity.ARG_ITEM, viewModel);
                getActivity().startActivity(intent);
            }
        });

        view.findViewById(R.id.button_retry).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadData();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mSpecialId != -1){
            List<ViewModel> itemToRemove = new ArrayList<>();
            AtomicReference<BookmarkManager> bookmarks = new AtomicReference<>(new BookmarkManager(getActivity()));
            for(ViewModel item: adapter.getItems()){
                if(bookmarks.get().findItem(item) == null){
                    itemToRemove.add(item);
                }
            }
            for(ViewModel item: itemToRemove){
                adapter.remove(item);
            }
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        loadData();
    }

    public void loadData(){
        if(mUrl != null)
            (new QueryPageTask()).execute(mUrl);
        else if(mSpecialId != -1)
            (new QueryBookmarkTask()).execute(getActivity());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        GridLayoutManager gridLayoutManager = (GridLayoutManager) ((RecyclerView) getActivity().findViewById(R.id.list)).getLayoutManager();
        DisplayMetrics displayMetrics = getActivity().getResources().getDisplayMetrics();
        int columns = displayMetrics.widthPixels / getActivity().getResources().getDimensionPixelSize(R.dimen.list_item_width);
        if(columns < 2){
            columns = 2;
        }
        gridLayoutManager.setColumns(columns);
    }

    @SuppressWarnings("deprecation")
    private class QueryPageTask extends ExceptionAsyncTask<String, Void, ViewModel[]>{

        @Override
        protected void onPreExecute() {
            ((AppCompatActivity)getActivity()).setSupportProgressBarIndeterminateVisibility(true);
            getActivity().findViewById(R.id.list).setVisibility(View.VISIBLE);
            getActivity().findViewById(R.id.layout_error).setVisibility(View.GONE);
            super.onPreExecute();
        }

        @Override
        protected ViewModel[] doInBackground() throws Exception {
            List<ViewModel> list = null;
            list = Parser.getInstance().parseList(getParams()[0]);
            return list.toArray(new ViewModel[list.size()]);
        }

        @Override
        protected void onPostExecute(ViewModel[] viewModels) {
            TheMovieDb tmdbCache = new TheMovieDb(CastApp.GetCheckedContext(getActivity()));

            if(viewModels != null){
                for(ViewModel m: viewModels){
                    m.getParser(CastApp.GetCheckedContext(getActivity())).updateFromCache(tmdbCache, m);
                    adapter.add(m, adapter.getItemCount());
                }
            }else{
                getException().printStackTrace();
                FlurryAgent.onError(getString(R.string.connection_error_title), getString(R.string.connection_error_message, Parser.getInstance().getUrl()) , getException());
                getActivity().findViewById(R.id.list).setVisibility(View.GONE);
                ((TextView)getActivity().findViewById(R.id.text_error)).setText(
                        getString(R.string.connection_error_title) + "\n" +
                        getString(R.string.connection_error_message, Parser.getInstance().getUrl()) +
                        "\n" + getException().getMessage()
                );
                getActivity().findViewById(R.id.layout_error).setVisibility(View.VISIBLE);
            }
            if(getActivity() != null)
                ((AppCompatActivity)getActivity()).setSupportProgressBarIndeterminateVisibility(false);
            super.onPostExecute(viewModels);
        }
    }

    @SuppressWarnings("deprecation")
    private class QueryBookmarkTask extends ExceptionAsyncTask<Context, Void, ViewModel[]>{

        @Override
        protected void onPreExecute() {
            ((AppCompatActivity)getActivity()).setSupportProgressBarIndeterminateVisibility(true);
            super.onPreExecute();
        }

        @Override
        protected ViewModel[] doInBackground() {
            AtomicReference<BookmarkManager> bookmarks = new AtomicReference<>(new BookmarkManager(getParams()[0]));
            List<ViewModel> list = new ArrayList<>();
            for(int i = 0; i < bookmarks.get().size(); i++){
                BookmarkManager.Bookmark b = bookmarks.get().get(i);

                if(!b.isInternal()) {
                    Parser p = Parser.getParser(CastApp.GetCheckedContext(getParams()[0]), b.getParserId());
                    ViewModel model = p.loadDetail(b.getUrl());
                    if(model != null) list.add(model);
                }
            }
            return list.toArray(new ViewModel[list.size()]);
        }

        @Override
        protected void onPostExecute(ViewModel[] viewModels) {
            if(viewModels != null){
                for(ViewModel m: viewModels){
                    adapter.add(m, adapter.getItemCount());
                }
            }else{
                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.connection_error_title))
                        .setMessage(getString(R.string.connection_error_message))
                        .setPositiveButton(getString(R.string.connection_error_button), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                (new QueryPageTask()).execute(mUrl);
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                getActivity().finish();
                            }
                        })
                        .show();
            }
            if(getActivity() != null)
                ((AppCompatActivity)getActivity()).setSupportProgressBarIndeterminateVisibility(false);
            super.onPostExecute(viewModels);
        }
    }
}
