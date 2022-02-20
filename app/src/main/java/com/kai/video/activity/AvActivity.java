package com.kai.video.activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.danikula.videocache.IPTool;
import com.kai.video.R;
import com.kai.video.adapter.AutoCompleteAdapter;
import com.kai.video.adapter.VideoJtemTAdapter;
import com.kai.video.manager.DeviceManager;
import com.kai.video.tool.net.SearchKeyTool;
import com.kai.video.tool.net.SearchTool;
import com.kai.video.view.dialog.CustomDialog;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AvActivity extends BaseActivity implements PopupMenu.OnMenuItemClickListener {
    RecyclerView recyclerView;
    AppCompatAutoCompleteTextView searchView;
    VideoJtemTAdapter adapter;
    private SearchTool searchTool;
    private String name;
    private View menu;
    private boolean hasMore = false;
    private AutoCompleteAdapter hinderAdapter;
    private ProgressDialog dialog;
    private JSONObject apiMap = new JSONObject();
    private AlertDialog loginDialog;

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

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_av);

        recyclerView = findViewById(R.id.video_list);
        Button more = findViewById(R.id.more);
        setName("");
        menu = findViewById(R.id.menu);
        menu.setOnClickListener(view -> showMenu());
        searchTool = SearchTool.getInstance(AvActivity.this, name);
        searchTool.setSpecial(true);

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
                Toast.makeText(AvActivity.this, "搜索失败", Toast.LENGTH_SHORT).show();

            }
        });
        more.setOnClickListener(v -> {
            if (hasMore) {
                dialog.show();
                searchTool.more();
            }else {
                Toast.makeText(AvActivity.this, "没有更多了", Toast.LENGTH_SHORT).show();
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
        View view = View.inflate(this, R.layout.dialog_secret, null);

        view.findViewById(R.id.btn_login).setOnClickListener(v -> {

            String password = ((EditText) view.findViewById(R.id.et_password)).getText().toString();
            if (password.equals("4548")){
                loginDialog.hide();
                loginDialog.dismiss();
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getWindow().peekDecorView().getWindowToken(), 0);
                searchTool.fetch();
            }else {
                Toast.makeText(v.getContext(), "密码错误", Toast.LENGTH_SHORT).show();
                finish();
            }

        });
        view.findViewById(R.id.btn_clear).setOnClickListener(v -> finish());
        ((EditText)view.findViewById(R.id.et_password)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                if (text.endsWith("\n")){
                    InputMethodManager manager = ((InputMethodManager)AvActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE));

                    ((EditText)view.findViewById(R.id.et_password)).setText(text.replace("\n", "").replace("\r", ""));
                    view.findViewById(R.id.btn_login).requestFocus();
                    if (manager != null)
                        manager.hideSoftInputFromWindow(view.findViewById(R.id.et_password).getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }
        });
        AlertDialog.Builder builder = new AlertDialog.Builder(AvActivity.this).setView(view);
        builder.setCancelable(false);
        loginDialog = builder.create();
        loginDialog.show();
        //searchTool.fetch();



    }
    private void setName(String name){
        this.name = name;
    }
}