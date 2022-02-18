package com.kai.video.activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.jeffmony.downloader.model.VideoTaskState;
import com.kai.video.R;
import com.kai.video.adapter.AutoCompleteAdapter;
import com.kai.video.adapter.VideoJtemTAdapter;
import com.kai.video.manager.ActivityCollector;
import com.kai.video.tool.log.LogUtil;
import com.kai.video.tool.net.SearchKeyTool;
import com.kai.video.tool.net.SearchTool;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends BaseActivity implements View.OnFocusChangeListener {
    RecyclerView recyclerView;
    AppCompatAutoCompleteTextView searchView;
    VideoJtemTAdapter adapter;
    private SearchTool searchTool;
    private String name;
    private Button more;
    private boolean hasMore = false;
    private AutoCompleteAdapter hinderAdapter;
    private ProgressDialog dialog;
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        LogUtil.d("TF", v.getId() + "");
    }

    @Override
    protected void onDestroy() {
        dialog.dismiss();
        super.onDestroy();
    }


    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_none_phone);
        recyclerView = findViewById(R.id.video_list);
        more = findViewById(R.id.more);
        setName(getIntent().getStringExtra("wd"));
        findViewById(R.id.avtv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                Intent intent = new Intent(SearchActivity.this, SniffActivity.class);
                intent.putExtra("wd", searchTool.getWd());
                startActivity(intent);
            }
        });
        searchTool = SearchTool.getInstance(SearchActivity.this, name);
        searchTool.setSpecial(getIntent().getBooleanExtra("special", false));
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(isScreenOriatationPortrait(SearchActivity.this) ? 4 : 7, StaggeredGridLayoutManager.VERTICAL);
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
        adapter.setOnFinishListener(position -> layoutManager.scrollToPositionWithOffset(position, 0));
        recyclerView.setAdapter(adapter);
        searchTool.setOnFetchListner(new SearchTool.OnFetchListner() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onFetched(int page, List<SearchTool.SearchItem> items, boolean more) {
                if (page == 1) {
                    setName(searchTool.getWd());
                    adapter.setItems(items);
                    adapter.notifyDataSetChanged();
                }else {
                    adapter.addItem(items);

                }
                dialog.hide();
                hasMore = more;
                //SearchActivity.this.more.setVisibility(more?View.VISIBLE:View.GONE);
            }

            @Override
            public void onFetchFail() {
                dialog.hide();
                Toast.makeText(SearchActivity.this, "搜索失败，进入浏览器搜索", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(SearchActivity.this, SniffActivity.class);
                intent.putExtra("wd", searchTool.getWd());
                startActivity(intent);

            }
        });
        more.setOnClickListener(v -> {
            if (hasMore) {
                dialog.show();
                searchTool.more();
            }else {
                AlertDialog.Builder builder = new AlertDialog.Builder(SearchActivity.this);
                builder.setTitle("没有更多搜索结果");
                builder.setMessage("是否前往浏览器继续搜索？");
                builder.setCancelable(true);
                builder.setPositiveButton("确定", (dialog, which) -> {
                    dialog.dismiss();
                    Intent intent = new Intent(SearchActivity.this, SniffActivity.class);
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
        searchView = findViewById(R.id.search_bar);
        searchView.setText(getIntent().getStringExtra("wd"));
        @SuppressLint("UseCompatLoadingForDrawables") Drawable drawable=getResources().getDrawable(R.drawable.search_phone); //获取图片
        drawable.setBounds(0, 0, 50, 50);
        searchView.setCompoundDrawables(drawable, null, null, null);
        searchView.setOnEditorActionListener((v, actionId, event) -> {
            if ((actionId == EditorInfo.IME_ACTION_SEARCH)) {//如果是搜索按钮
                String s = v.getText().toString();
                adapter.setItems(new ArrayList<>());
                adapter.notifyDataSetChanged();
                dialog.show();
                searchTool.setWd(s);
                setName(s);
                searchTool.fetch();
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                View view = getWindow().peekDecorView();
                if (null != v) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
            return false;
        });
        hinderAdapter = new AutoCompleteAdapter(this);
        hinderAdapter.setOnItemClickListener(s -> {
            adapter.setItems(new ArrayList<>());
            adapter.notifyDataSetChanged();
            dialog.show();
            searchView.setText(s, false);
            searchView.setSelection(s.length());
            searchTool.setWd(s);
            setName(s);
            searchTool.fetch();
            searchView.dismissDropDown();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            View v = getWindow().peekDecorView();
            if (null != v) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        });
        searchView.setAdapter(hinderAdapter);
        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                new Thread(() -> {
                    try {
                        final List<String> results = SearchKeyTool.search(s.toString());
                        searchView.post(() -> {
                            hinderAdapter.setStringList(results);
                            hinderAdapter.notifyDataSetChanged();
                        });
                    }catch (Exception e){

                    }

                }).start();

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {



            }
        });

        searchTool.fetch();



    }
    private void setName(String name){
        this.name = name;
    }
    public static boolean isScreenOriatationPortrait(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }
}
