package com.kai.video.activity;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kai.video.R;
import com.kai.video.manager.DeviceManager;
import com.kai.video.tool.net.SearchTool;
import com.kai.video.manager.SimpleGridLayoutManager;
import com.kai.video.adapter.TypeItemAdapter;
import com.kai.video.tool.net.TypeTool;
import com.kai.video.adapter.VideoJtemAdapter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TypeActivity extends AppCompatActivity {
    private TypeTool typeTool;
    private VideoJtemAdapter adapter;
    private RecyclerView recyclerView;
    View progressBar;
    private Button more;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_type);
        typeTool = TypeTool.getInstance(this);
        initTv();
        recyclerView = (RecyclerView)findViewById(R.id.video_list);
        more = (Button)findViewById(R.id.more);
        SimpleGridLayoutManager layoutManager = new SimpleGridLayoutManager(this, DeviceManager.isPhone()?4:7);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new VideoJtemAdapter(new ArrayList<>());
        adapter.setOnLoading(new VideoJtemAdapter.OnLoading() {
            @Override
            public void onStart() {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onEnd() {
                progressBar.setVisibility(View.INVISIBLE);
            }
        });
        adapter.setOnFinishListener(new VideoJtemAdapter.OnFinishListener() {
            @Override
            public void onFinish(int position) {
                layoutManager.scrollToPositionWithOffset(position, 0);
                View view = layoutManager.findViewByPosition(position);
                if (view != null)
                    view.requestFocus();
            }
        });
        recyclerView.setAdapter(adapter);
        typeTool.setOnFetchListner(new TypeTool.OnFetchListner() {
            @Override
            public void onFetched(int page, List<SearchTool.SearchJtem> items, boolean more, boolean reload) {
                if (page == 1){
                    recyclerView.scrollToPosition(0);
                    if (reload)
                        reloadTv();
                    adapter.setItems(items);
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                }else {
                    adapter.addItem(items);
                    progressBar.setVisibility(View.GONE);

                }
                if (more){
                    TypeActivity.this.more.setVisibility(View.VISIBLE);
                }else
                    TypeActivity.this.more.setVisibility(View.GONE);


            }

            @Override
            public void onFetchFail(int page) {
                Toast.makeText(TypeActivity.this, "搜索失败", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.INVISIBLE);

            }
        });
        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                typeTool.fetch(false);
            }
        });
        progressBar = (View) findViewById(R.id.progress);
        typeTool.fetch(false);
    }
    private LinearLayoutManager getLayoutManager(){
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(RecyclerView.HORIZONTAL);
        return manager;
    }
    private List<TypeItemAdapter> adapters = new ArrayList<>();
    private void reloadTv(){

        for (int i = 1; i < adapters.size(); i++){
            TypeItemAdapter adapter = adapters.get(i);
            adapter.setItems(Arrays.asList(typeTool.getTypeByIndex(i)));
            adapter.setCurrent(0);
            adapter.notifyDataSetChanged();

        }

    }
    private LinearLayoutManager linearLayoutManager;
    private void initTv(){
        RecyclerView list1 = (RecyclerView)findViewById(R.id.list1);
        RecyclerView list2 = (RecyclerView)findViewById(R.id.list2);
        RecyclerView list3 = (RecyclerView)findViewById(R.id.list3);
        RecyclerView list4 = (RecyclerView)findViewById(R.id.list4);
        RecyclerView list5 = (RecyclerView)findViewById(R.id.list5);
        List<RecyclerView> recyclerViews = new ArrayList<>();
        recyclerViews.add(list1);
        recyclerViews.add(list2);
        recyclerViews.add(list3);
        recyclerViews.add(list4);
        recyclerViews.add(list5);
        for(int i = 0; i < recyclerViews.size(); i++){
            final int index = i;
            RecyclerView recyclerView = recyclerViews.get(i);
            LinearLayoutManager manager = getLayoutManager();
            TypeItemAdapter itemAdapter = new TypeItemAdapter(Arrays.asList(typeTool.getTypeByIndex(i)));
            adapters.add(itemAdapter);
            if (i == 0){
                linearLayoutManager = manager;
                itemAdapter.setOnFinishListener(new TypeItemAdapter.OnFinishListener() {
                    @Override
                    public void onFinish() {
                        try {
                            manager.findViewByPosition(0).requestFocus();
                        }catch (Exception e){

                        }
                    }
                });
            }
            itemAdapter.setOnItemClickListener(new TypeItemAdapter.onItemClickListener() {
                @Override
                public void onClick(int position) {
                    typeTool.resetPageOffset();
                    recyclerView.scrollToPosition(0);
                    typeTool.setTypeByIndex(index, position);
                    progressBar.setVisibility(View.VISIBLE);

                }
            });
            recyclerView.setLayoutManager(manager);
            recyclerView.setAdapter(itemAdapter);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && DeviceManager.isTv()){
            if (linearLayoutManager != null && !linearLayoutManager.findViewByPosition(0).hasFocus()) {
                linearLayoutManager.findViewByPosition(0).requestFocus();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
