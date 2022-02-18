package com.kai.video.activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.leanback.widget.SearchEditText;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.kai.video.R;
import com.kai.video.adapter.AlphabetItemAdapter;
import com.kai.video.adapter.SuggestItemAdapter;
import com.kai.video.adapter.VideoJtemTAdapter;
import com.kai.video.manager.SimpleGridLayoutManager;
import com.kai.video.tool.log.LogUtil;
import com.kai.video.tool.net.SearchKeyTool;
import com.kai.video.tool.net.SearchTool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchTVActivity extends BaseActivity implements View.OnFocusChangeListener {
    RecyclerView recyclerView;
    VideoJtemTAdapter adapter;
    SuggestItemAdapter itemAdapter;
    private SearchTool searchTool;
    private RecyclerView alphabetList;
    private RecyclerView suggestList;
    private SearchEditText searchEditText;
    private String name;
    private TextView header;
    private Button more;
    private boolean hasMore = false;
    private final Handler handler = new Handler();
    private ProgressDialog dialog;
    private final Runnable runnable = new Runnable() {
        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void run() {
            dialog.hide();
            itemAdapter.notifyDataSetChanged();
        }
    };
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        LogUtil.d("TF", v.getId() + "");
    }

    @Override
    protected void onDestroy() {
        dialog.dismiss();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        alphabetList = findViewById(R.id.aphabetList);
        suggestList = findViewById(R.id.search_suggest);
        searchEditText = findViewById(R.id.search_editer);
        recyclerView = findViewById(R.id.video_list);
        header = findViewById(R.id.title);
        more = findViewById(R.id.more);
        setName(getIntent().getStringExtra("wd"), "");
        searchTool = SearchTool.getInstance(SearchTVActivity.this, name);
        initTV();
        searchTool.setSpecial(getIntent().getBooleanExtra("special", false));
        SimpleGridLayoutManager layoutManager = new SimpleGridLayoutManager(this, 4);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new VideoJtemTAdapter(new ArrayList<>());
        adapter.setOnLoading(new VideoJtemTAdapter.OnLoading() {
            @Override
            public void onStart() {
                dialog.show();
            }

            @Override
            public void onEnd() {
                dialog.hide();
            }
        });
        adapter.setOnFinishListener(position -> {
            layoutManager.scrollToPositionWithOffset(position, 0);
            View view = layoutManager.findViewByPosition(position);
            if (view != null)
                view.requestFocus();

        });
        recyclerView.setAdapter(adapter);
        searchTool.setOnFetchListner(new SearchTool.OnFetchListner() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onFetched(int page, List<SearchTool.SearchItem> items, boolean more) {
                if (page == 1) {
                    setName(searchTool.getWd(), searchTool.getSummary());
                    adapter.setItems(items);
                    adapter.notifyDataSetChanged();
                }else {
                    adapter.addItem(items);

                }
                hasMore = more;
                dialog.hide();

            }

            @Override
            public void onFetchFail() {
                dialog.hide();
                Toast.makeText(SearchTVActivity.this, "搜索失败，进入浏览器搜索", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(SearchTVActivity.this, SniffActivity.class);
                intent.putExtra("wd", searchTool.getWd());
                startActivity(intent);
            }
        });
        more.setOnClickListener(v -> {
            if (hasMore) {
                dialog.show();
                searchTool.more();
            }else {
                AlertDialog.Builder builder = new AlertDialog.Builder(SearchTVActivity.this);
                builder.setTitle("没有更多搜索结果");
                builder.setMessage("是否前往浏览器继续搜索？");
                builder.setCancelable(true);
                builder.setPositiveButton("确定", (dialog, which) -> {
                    dialog.dismiss();
                    Intent intent = new Intent(SearchTVActivity.this, SniffActivity.class);
                    intent.putExtra("wd", searchTool.getWd());
                    startActivity(intent);
                });
                //设置反面按钮
                builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

                AlertDialog dialog = builder.create();     //创建AlertDialog对象
                dialog.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).requestFocus();
                //Toast.makeText(SearchActivity.this, "进入浏览器搜索", Toast.LENGTH_SHORT).show();

            }

        });
        dialog = ProgressDialog.show(this, "正在搜索", "请等待");
        searchTool.fetch();
    }
    @SuppressLint("SetTextI18n")
    private void initTV(){
        Button backspace = findViewById(R.id.backspace);
        Button clear = findViewById(R.id.clear);
        Button type = findViewById(R.id.type);
        type.setOnClickListener(v -> {
            Intent intent = new Intent(type.getContext(), TypeActivity.class);
            type.getContext().startActivity(intent);
        });
        AlphabetItemAdapter adapter = new AlphabetItemAdapter(Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K",
                "L", "M","N","O", "P", "Q", "R", "S", "T","U", "V", "W", "X", "Y","Z","0", "1", "2", "3", "4","5", "6", "7", "8", "9"));
        StaggeredGridLayoutManager manager = new StaggeredGridLayoutManager(6, StaggeredGridLayoutManager.VERTICAL);
        adapter.setOnFinishListener(() -> {
            try {
                manager.findViewByPosition(0).requestFocus();
            }catch (Exception ignored){

            }
        });
        alphabetList.setLayoutManager(manager);
        clear.setOnClickListener(v -> {
            if (searchEditText.getText().length()>0) {
                searchEditText.setText("");
                suggestList.postDelayed(() -> manager.findViewByPosition(0).requestFocus(), 100);

            }
        });
        backspace.setOnClickListener(v -> {
            if (searchEditText.getText().length()>0) {
                searchEditText.setText(searchEditText.getText().subSequence(0, searchEditText.getText().length() - 1));

            }
        });
        adapter.setOnItemClickListener(key -> {
            searchEditText.setText(searchEditText.getText().toString() + key);
            searchEditText.setSelection(searchEditText.getText().length());
        });
        alphabetList.setAdapter(adapter);
        itemAdapter = new SuggestItemAdapter(SearchKeyTool.defaultList);
        itemAdapter.setOnItemClickListener(item -> {
            dialog.show();
            recyclerView.scrollToPosition(0);
            searchTool.setWd(item);
            setName(item, "");
            searchTool.fetch();
        });
        LinearLayoutManager manager1 = new LinearLayoutManager(SearchTVActivity.this);
        suggestList.setLayoutManager(manager1);
        suggestList.setAdapter(itemAdapter);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    itemAdapter.setItems(SearchKeyTool.defaultList);
                    handler.post(runnable);
                    adapter.notifyDataSetChanged();
                    return;
                }
                manager1.scrollToPositionWithOffset(0, 0);
                handler.removeCallbacks(runnable);
                new Thread(() -> {
                    try {

                        itemAdapter.setItems(SearchKeyTool.search(s.toString()));
                        handler.post(runnable);


                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }).start();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }
    @SuppressLint("SetTextI18n")
    private void setName(String name, String result){
        this.name = name;
        if (name.isEmpty()){
            header.setText("搜索技术由CMS采集提供");
        }else if (result.isEmpty()){
            header.setText("影视凯搜索：" + name);
        }else {
            header.setText("关键字：" + name + "，共搜索到" + result);
        }
    }
    public static boolean isScreenOriatationPortrait(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }
}
