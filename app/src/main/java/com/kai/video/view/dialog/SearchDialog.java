package com.kai.video.view.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AlertDialog;
import androidx.leanback.widget.SearchEditText;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kai.video.R;
import com.kai.video.activity.SearchActivity;
import com.kai.video.adapter.DialogItemAdapter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.List;

public class SearchDialog extends AlertDialog {
    private final Context mContext;
    private DialogItemAdapter adapter;
    private SearchEditText searchEditText;

    public SearchDialog(Context context) {
        super(context, R.style.CustomDialog);
        mContext = context;
    }


    private final Handler handler = new Handler();
    private final Runnable runnable = new Runnable() {
        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void run() {
            adapter.notifyDataSetChanged();
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_search);
        setCanceledOnTouchOutside(true);
        searchEditText = findViewById(R.id.editer);
        RecyclerView recyclerView = findViewById(R.id.list);
        LinearLayoutManager manager = new LinearLayoutManager(mContext);
        adapter = new DialogItemAdapter(new ArrayList<>(), null, -1, SearchDialog.this);
        adapter.setOnItemClickListener((item, o, position, dialog) -> {
            searchEditText.setText(item);
            Intent intent = new Intent(mContext, SearchActivity.class);
            intent.putExtra("wd",item);
            mContext.startActivity(intent);
        });
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        searchEditText.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager)mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
            searchEditText.setSelection(searchEditText.getText().length());
        });
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    adapter.setItems(new ArrayList<>());
                    adapter.notifyDataSetChanged();
                    return;
                }
                new Thread(() -> {
                    try {
                        handler.removeCallbacks(runnable);
                        Connection.Response response = Jsoup.connect("https://search.video.iqiyi.com/m")
                                .data("if", "related_query")
                                .data("key", s.toString())
                                .ignoreContentType(true)
                                .execute();
                        JSONArray array = new JSONObject(response.body()).getJSONArray("query_list");
                        List<String> result = new ArrayList<>();
                        int length = Math.min(array.length(), 6);
                        for(int i = 0; i < length; i++){
                            result.add(array.get(i).toString());
                        }
                        adapter.setItems(result);
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
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if ((actionId == 0 || actionId == 3) && event != null) {
                //点击搜索要做的操作
                Intent intent = new Intent(mContext, SearchActivity.class);
                intent.putExtra("wd",searchEditText.getText().toString());
                mContext.startActivity(intent);
            }
            return false;
        });
        //初始化界面控件

    }

}
