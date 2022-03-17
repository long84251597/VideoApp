package com.kai.video.activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.danikula.videocache.IPTool;
import com.kai.video.R;
import com.kai.video.adapter.AutoCompleteAdapter;
import com.kai.video.adapter.PageItemAdapter;
import com.kai.video.adapter.VideoJtemTAdapter;
import com.kai.video.manager.DeviceManager;
import com.kai.video.manager.MyOrientoinListener;
import com.kai.video.tool.net.SearchKeyTool;
import com.kai.video.tool.net.SearchTool;
import com.kai.video.view.dialog.CustomDialog;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AvActivity extends BaseActivity implements PopupMenu.OnMenuItemClickListener {
    private RecyclerView recyclerView;
    private RecyclerView offsetRecyclerView;
    private AppCompatAutoCompleteTextView searchView;
    private VideoJtemTAdapter adapter;
    private PageItemAdapter pageItemAdapter;
    private SearchTool searchTool;
    private View menu;
    private boolean hasMore = false;
    private AutoCompleteAdapter hinderAdapter;
    private ProgressDialog dialog;
    private JSONObject apiMap = new JSONObject();

    @Override
    protected void onDestroy() {
        dialog.dismiss();
        super.onDestroy();
    }

    private void setApis(){
        new Thread(() -> {
            try {
                Connection.Response response = Jsoup.connect(IPTool.getLocal() + "/quicksearch")
                        .data("action", "api")
                        .method(Connection.Method.GET)
                        .execute();
                apiMap = JSONObject.parseObject(response.body(), Feature.OrderedField);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU){
            showMenu();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showTypes(){
        new Thread(() -> {
            try {
                Connection.Response response = Jsoup.connect(IPTool.getLocal() + "/quicksearch")
                        .data("action", "type")
                        .data("api", searchTool.getApi())
                        .execute();
                JSONArray array = JSONArray.parseArray(response.body());
                List<String> names = new ArrayList<>();
                names.add("不限");
                List<Object> types = new ArrayList<>();
                types.add("");
                for(Object o: array){
                    JSONObject object = (JSONObject) o;
                    if (object.containsKey("type_name"))
                        names.add(object.getString("type_name"));
                    else if (object.containsKey("title"))
                        names.add(object.getString("title"));
                    if (object.containsKey("type_id"))
                        types.add(object.getString("type_id"));
                    else if (object.containsKey("cid"))
                        types.add(object.getString("cid"));
                }
                runOnUiThread(() -> new CustomDialog.Builder(AvActivity.this)
                        .setTitle("切换资源站")
                        .setList(names, types,  types.indexOf(searchTool.getType()))
                        .setOnItemClickListener((item, o, position, d) -> {
                            recyclerView.scrollToPosition(0);
                            d.dismiss();
                            dialog.show();
                            searchTool.setType(o.toString());
                            getIntent().putExtra("type", o.toString());
                            searchTool.fetch();
                        })
                        .create()
                        .show());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()){
            case R.id.api:
                List<String> names = new ArrayList<>();
                List<Object> apis = new ArrayList<>();
                for (String key: apiMap.keySet()) {
                    names.add(key);
                    apis.add(apiMap.getString(key));
                }
                new CustomDialog.Builder(AvActivity.this)
                        .setTitle("切换资源站")
                        .setList(names, apis, Integer.parseInt(searchTool.getApi())-1)
                        .setOnItemClickListener((item, o, position, d) -> {
                            d.dismiss();
                            recyclerView.scrollToPosition(0);
                            dialog.show();
                            searchTool.setApi(o.toString());
                            getIntent().putExtra("api", o.toString());
                            searchTool.setType("");
                            searchTool.fetch();
                        })
                        .create()
                        .show();
                break;
            case R.id.sift:
                showTypes();
                break;
            default:
                break;

        }
        return false;
    }

    private void showMenu(){
        PopupMenu popup = new PopupMenu(this, menu);//第二个参数是绑定的那个view
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.av_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    private int getSpanCount(){
        if (DeviceManager.isTv()){
            return 4;
        }else{
            Configuration configuration = getResources().getConfiguration();
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                return 3;
            }else
                return 2;
        }
    }

    @Override
    protected int getTvLayout() {
        return R.layout.activity_av;
    }

    @Override
    protected int getPhoneLandLayout() {
        return R.layout.activity_av;
    }

    @Override
    protected int getPhonePortLayout() {
        return R.layout.activity_av;
    }

    @Override
    protected int getPadPortLayout() {
        return R.layout.activity_av;
    }

    @Override
    protected int getPadLandLayout() {
        return R.layout.activity_av;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyOrientoinListener orientoinListener = MyOrientoinListener.getInstance(this);
        orientoinListener.setForceLand(false);
        orientoinListener.enable();
        recyclerView = findViewById(R.id.video_list);
        setName(getIntent().getStringExtra("name"));
        menu = findViewById(R.id.menu);
        menu.setOnClickListener(view -> showMenu());
        searchTool = SearchTool.getInstance(AvActivity.this, getIntent().getStringExtra("name"));
        searchTool.setSpecial(true);
        searchTool.setApi(getIntent().getStringExtra("api"));
        searchTool.setType(getIntent().getStringExtra("type"));
        searchTool.setWd(getIntent().getStringExtra("wd"));
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(getSpanCount(), StaggeredGridLayoutManager.VERTICAL);
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
        searchTool.setUseOffset(true);
        searchTool.setOnFetchListner(new SearchTool.OnFetchListner() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onFetched(int page, List<SearchTool.SearchItem> items, boolean more) {
                setName(searchTool.getWd());
                adapter.setItems(items);
                adapter.notifyDataSetChanged();
                dialog.hide();
                hasMore = more;
                if (page > 0) {
                    pageItemAdapter.init(page);
                    getIntent().putExtra("page", page);
                }
                //SearchActivity.this.more.setVisibility(more?View.VISIBLE:View.GONE);
            }

            @Override
            public void onFetchFail() {
                dialog.hide();
                Toast.makeText(AvActivity.this, "搜索失败", Toast.LENGTH_SHORT).show();

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
                getIntent().putExtra("wd", s);
                searchTool.fetch();
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                View view = getWindow().peekDecorView();
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
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
                    }catch (Exception ignored){

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
        setApis();
        pageItemAdapter = new PageItemAdapter(Arrays.asList("<", "1", "2", "3", "4", "5", "6", ">"));
        pageItemAdapter.setOnItemClickListener(new PageItemAdapter.OnItemClickListener() {
            @Override
            public void onClick(int position, String offset, int sum) {

                if (offset.equals("<")){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            int current = Integer.parseInt(pageItemAdapter.getFirst()) + 1;
                            int groupCurrent = current / 6;
                            if ((current & 6) != 0)
                                groupCurrent--;
                            if (groupCurrent < 0)
                                return;
                            int start = groupCurrent * 6 + 1;
                            int end = start + 5;
                            if (end > sum)
                                end = sum;
                            List<String > array = new ArrayList<>();
                            array.add("<");
                            for(int i = start; i <= end; i++){
                                array.add(String.valueOf(i));
                            }
                            array.add(">");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pageItemAdapter.setArray(array);
                                    pageItemAdapter.notifyDataSetChanged();
                                }
                            });
                        }
                    }).start();

                }else if (offset.equals(">")){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            int current = Integer.parseInt(pageItemAdapter.getFirst()) + 1;
                            int groupCurrent = current / 6 + 1;
                            int start = groupCurrent * 6 + 1;
                            if (start > sum)
                                return;
                            int end = start + 5;
                            if (end > sum)
                                end = sum;
                            List<String > array = new ArrayList<>();
                            array.add("<");
                            for(int i = start; i <= end; i++){
                                array.add(String.valueOf(i));
                            }
                            array.add(">");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pageItemAdapter.setArray(array);
                                    pageItemAdapter.notifyDataSetChanged();
                                }
                            });

                        }
                    }).start();
                }else {
                    layoutManager.scrollToPosition(0);
                    dialog.show();
                    getIntent().putExtra("offset", offset);
                    searchTool.more(offset);
                }
            }
        });
        LinearLayoutManager manager = new GridLayoutManager(this, 8);
        offsetRecyclerView = findViewById(R.id.more);
        offsetRecyclerView.setAdapter(pageItemAdapter);
        offsetRecyclerView.setLayoutManager(manager);
        String offset = getIntent().getStringExtra("offset");
        if (offset != null){
            searchTool.setUseOffset(true);
            pageItemAdapter.init(getIntent().getIntExtra("page", 0), offset);
            searchTool.more(offset);
        }else
            searchTool.fetch();



    }
    private void setName(String name){
        if (name == null)
            name = "";
        getIntent().putExtra("name", name);
    }
}