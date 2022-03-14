package com.kai.video.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kai.video.activity.MainActivity;
import com.kai.video.R;
import com.kai.video.activity.SearchTVActivity;
import com.kai.video.activity.TypeActivity;
import com.kai.video.manager.DeviceManager;
import com.kai.video.bean.item.NaviItem;
import com.kai.video.manager.FocusGridLayoutManager;
import com.kai.video.tool.net.SimpleConductor;
import com.kai.video.adapter.VideoItemAdapter;
import com.kai.video.view.other.GridSpacesItemDecoration;
import com.winton.bottomnavigationview.NavigationView;

import java.util.ArrayList;
import java.util.List;

import q.rorbin.verticaltablayout.VerticalTabLayout;
import q.rorbin.verticaltablayout.adapter.TabAdapter;
import q.rorbin.verticaltablayout.widget.ITabView;
import q.rorbin.verticaltablayout.widget.TabView;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment {
    private final String[] actions = {"tv", "film", "cartoon", "zy"};
    private RecyclerView recyclerView;
    private View progressBar;
    private NavigationView navigationView;
    private FocusGridLayoutManager layoutManager;
    private boolean isViewInitFinished = false;
    private boolean dataLoad = false;
    private VerticalTabLayout tabLayout;
    private  int index;
    private VideoItemAdapter adapter;
    private SimpleConductor simpleConductor;
    public PlaceholderFragment(){
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_main, container, false);
        Bundle bundle = getArguments();
        index = bundle.getInt("index");
        simpleConductor = new SimpleConductor(actions[index]);
        recyclerView = root.findViewById(R.id.video_list);
        progressBar = root.findViewById(R.id.progress);
        isViewInitFinished = true;

        tabLayout = root.findViewById(R.id.tablayout);
        View searchButton = root.findViewById(R.id.search_button);
        searchButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), SearchTVActivity.class);
            intent.putExtra("wd", "");
            startActivity(intent);
        });

        navigationView = root.findViewById(R.id.navigation);
        View userButton = root.findViewById(R.id.user);
        userButton.setOnClickListener(v -> ((MainActivity)getActivity()).showUserSetting());
        View typeButton = root.findViewById(R.id.hot);
        typeButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), TypeActivity.class);
            getContext().startActivity(intent);
        });
        if (DeviceManager.isPad()) {
            searchButton.setVisibility(View.INVISIBLE);
            userButton.setVisibility(View.INVISIBLE);
            typeButton.setVisibility(View.INVISIBLE);
        }
        if(DeviceManager.isPhone()){
            root.findViewById(R.id.tv_bar).setVisibility(View.GONE);
            root.findViewById(R.id.bar_line).setVisibility(View.GONE);
        }else {
            navigationView.setVisibility(View.GONE);
        }
        return root;
    }
    public static boolean isScreenOriatationPortrait(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }
    private final Handler focusHandler = new Handler();
    private final Runnable focusRunnable = new Runnable() {
        @Override
        public void run() {
            getActivity().runOnUiThread(() -> {
                View focusView = layoutManager.findViewByPosition(position);
                if (focusView != null)
                    focusView.findViewById(R.id.main).requestFocus();
            });

        }
    };

    public void scrollItemToTop(){
        try {
            if (DeviceManager.isTv()){
                layoutManager.scrollToPositionWithOffset(position, 0);
                focusHandler.postDelayed(focusRunnable, 500);
            }

        }catch (Exception ignored){

        }

    }



    private int position = 0;
    public void getData(){
        dataLoad = true;
        new Thread(new Runnable() {
            @Override
            public void run() {

                simpleConductor.get(new SimpleConductor.OnGetListener() {
                    @Override
                    public void onFinish(final List<NaviItem> list) {
                        ((Activity)getContext()).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter = new VideoItemAdapter(list, getContext(), index==0);
                                layoutManager = new FocusGridLayoutManager(getContext(), DeviceManager.getSpanCount(getContext()), adapter);
                                layoutManager.setOnReachHeader(position -> new Handler().postDelayed(() -> {
                                    ((MainActivity) getActivity()).clearTabFocus();
                                    layoutManager.findViewByPosition(position).findViewById(R.id.main).requestFocus();
                                }, 800));
                                recyclerView.setLayoutManager(layoutManager);
                                adapter.setHasStableIds(true);
                                layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                                    @Override
                                    public int getSpanSize(int position) {
                                        if (adapter.getItemViewType(position) == 2)
                                            return layoutManager.getSpanCount();
                                        else
                                            return 1;
                                    }
                                });
                                adapter.setOnFocusListner((type, position) -> {
                                    PlaceholderFragment.this.position = position;
                                    tabLayout.setTabSelected(simpleConductor.getTypes().indexOf(type), false);
                                });
                                adapter.setOnFinishListener(() -> {
                                    progressBar.setVisibility(View.GONE);
                                    try {
                                        //如果处于当前页面，会自动
                                        if (index == ((MainActivity)getActivity()).getCurrentPage() && !((MainActivity)getActivity()).isTabFocused()){
                                            scrollItemToTop();
                                        }
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }
                                });

                                recyclerView.setAdapter(adapter);
                                GridSpacesItemDecoration decorator = new GridSpacesItemDecoration(getActivity(), adapter, actions[index]);
                                recyclerView.addItemDecoration(decorator);
                                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M && DeviceManager.isPhone())
                                recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
                                    @Override
                                    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                                        super.onScrollStateChanged(recyclerView, newState);
                                        try {
                                            String top = adapter.getAction(((GridLayoutManager)recyclerView.getLayoutManager()).findFirstVisibleItemPosition());
                                            String bottom = adapter.getAction(((GridLayoutManager)recyclerView.getLayoutManager()).findLastVisibleItemPosition());
                                            if ((top != null && bottom != null) && (!top.equals(actions[index]) || !bottom.equals(actions[index]))){
                                                navigationView.setOnTabSelectedListener(null);
                                                if (top.equals(actions[index])){
                                                    navigationView.check(simpleConductor.getTypes().indexOf(bottom));
                                                }else {
                                                    navigationView.check(simpleConductor.getTypes().indexOf(top));
                                                }
                                                navigationView.setOnTabSelectedListener(onTabSelectedListener);
                                            }
                                        }catch (Exception e){
                                            e.printStackTrace();
                                        }

                                    }
                                });
                                initTabs();
                            }
                        });

                    }
                });
            }
        }).start();
    }
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && isViewInitFinished  && index != 0){
            if (!dataLoad)
                getData();
        }
    }
    private NavigationView.OnTabSelectedListener onTabSelectedListener;
    private void initTabs(){
        if (DeviceManager.isTv() || DeviceManager.isPad()){
            tabLayout.setTabAdapter(new TabAdapter() {
                @Override
                public int getCount() {
                    return simpleConductor.getTypes().size();
                }

                @Override
                public TabView.TabBadge getBadge(int position) {
                    return null;
                }

                @Override
                public TabView.TabIcon getIcon(int position) {
                    String type = simpleConductor.getTypes().get(position);
                    final float scale = getActivity().getResources().getDisplayMetrics().density;
                    switch (type){
                        case "tencent": return new ITabView.TabIcon.Builder()
                                .setIcon(R.drawable.tencent, R.drawable.tencent)
                                .setIconSize((int)(scale*80+0.5f), (int)(scale*30+0.5f))
                                .build();
                        case "iqiyi": return new ITabView.TabIcon.Builder()
                                .setIcon(R.drawable.iqiyi, R.drawable.iqiyi)
                                .setIconSize((int)(scale*80+0.5f), (int)(scale*35+0.5f))
                                .build();
                        case "mgtv" : return new ITabView.TabIcon.Builder()
                                .setIcon(R.drawable.mgtv, R.drawable.mgtv)
                                .setIconSize((int)(scale*80+0.5f), (int)(scale*35+0.5f))
                                .build();
                        case "bilibili":
                        case "bilibili1":
                            return new ITabView.TabIcon.Builder()
                                .setIcon(R.drawable.bilibili, R.drawable.bilibili)
                                .setIconSize((int)(scale*80+0.5f), (int)(scale*30+0.5f))
                                .build();
                        case "youku":
                            return new ITabView.TabIcon.Builder()
                                    .setIcon(R.drawable.youku, R.drawable.youku)
                                    .setIconSize((int)(scale*80+0.5f), (int)(scale*30+0.5f))
                                    .build();

                    }
                    return null;


                }

                @Override
                public TabView.TabTitle getTitle(int position) {
                    return null;
                }

                @Override
                public int getBackground(int position) {
                    return R.drawable.tab_item_selector;
                }
            });
            tabLayout.addOnTabSelectedListener(new VerticalTabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabView tab, int position) {
                    String type = simpleConductor.getTypes().get(position);
                    scrollType(type);
                }

                @Override
                public void onTabReselected(TabView tab, int position) {

                }
            });
        }else {
            List<NavigationView.Model> tabs = new ArrayList<>();
            for (String type : simpleConductor.getTypes()) {
                switch (type){
                    case "tencent": tabs.add(new NavigationView.Model.Builder(R.drawable.tencent,R.drawable.tencent).title("腾讯视频").build());continue;
                    case "iqiyi": tabs.add(new NavigationView.Model.Builder(R.drawable.iqiyi,R.drawable.iqiyi).title("爱奇艺").build());continue;
                    case "mgtv" : tabs.add(new NavigationView.Model.Builder(R.drawable.mgtv,R.drawable.mgtv).title("芒果TV").build());continue;
                    case "bilibili":tabs.add(new NavigationView.Model.Builder(R.drawable.bilibili,R.drawable.bilibili).title("哔哩哔哩").build());continue;
                    case "bilibili1":tabs.add(new NavigationView.Model.Builder(R.drawable.bilibili,R.drawable.bilibili).title("哔哩哔哩").build());continue;
                    case "youku":tabs.add(new NavigationView.Model.Builder(R.drawable.youku,R.drawable.youku).title("优酷视频").build());continue;
                }
            }
            navigationView.setItems(tabs);
            if (simpleConductor.getTypes().size() > 0)
                navigationView.build();
            onTabSelectedListener = new NavigationView.OnTabSelectedListener() {
                @Override
                public void selected(int index, NavigationView.Model model) {
                    String type = simpleConductor.getTypes().get(index);
                    scrollType(type);

                }

                @Override
                public void unselected(int index, NavigationView.Model model) {

                }

            };
            navigationView.setOnTabSelectedListener(onTabSelectedListener);
        }
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //initTabs();
        if(index == 0) {
            getData();
        }

    }
    public boolean inBanner(){
        View currentView;
        try {
            currentView = layoutManager.findViewByPosition(position).findViewById(R.id.main);
        }catch (Exception e){
            return false;
        }
        return adapter.getItemViewType(position)==2 && !currentView.isFocused() && currentView.hasFocus();
    }
    public void exitBanner(){
        layoutManager.findViewByPosition(position).findViewById(R.id.main).requestFocus();
    }
    public Handler scrollHandler = new Handler();
    private void scrollType(String type){
        new Thread(() -> {
            if (adapter == null || adapter.getItems() == null)
                return;
            List<NaviItem> elements = adapter.getItems();
            for (int i = 0; i < elements.size(); i++) {
                if (adapter.getItemViewType(i) == 2 && adapter.getItems().get(i).getHeader().equals(type)){
                    final int index = i;
                    scrollHandler.post(() -> layoutManager.scrollToPositionWithOffset(index, 0));

                    return;
                }
            }
        }).start();

    }

}