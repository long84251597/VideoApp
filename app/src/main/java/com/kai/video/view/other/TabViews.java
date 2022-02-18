package com.kai.video.view.other;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;
import com.kai.video.R;

import java.util.ArrayList;
import java.util.List;

public class TabViews {
    private boolean focused = false;
    private final TabLayout tabs;
    private final List<View> tabViews = new ArrayList<>();

    @SuppressLint("UseCompatLoadingForDrawables")
    public TabViews(TabLayout tabs, OnTabFocusItemListener onTabFocusItemListener){
        this.tabs = tabs;
        for(int i = 0; i < 4; i++){
            final int index = i;
            View tabView = getTabView(i);
            tabView.setFocusableInTouchMode(false);
            tabView.setBackground(tabs.getContext().getDrawable(R.drawable.tab_item_none_selector));
            tabViews.add(tabView);
            tabView.setOnFocusChangeListener((v, hasFocus) -> {
                //所有TabView都失去焦点，说明焦点下移给
                if (hasFocus){
                    if (!focused){
                        focusCurrent();
                        focused = true;
                    }else {
                        tabs.selectTab(tabs.getTabAt(index), true);
                    }
                }
                if (!isTabsFocused() && !hasFocus){
                    focused = false;
                    onTabFocusItemListener.onUnfocused();
                }
            });
        }
    }
    public void clearFocus(){
        for (View tabview : tabViews) {
            if (tabview.hasFocus())
                tabview.clearFocus();
        }
        focused = false;
    }
    public boolean isFocused() {
        return focused;
    }

    public boolean isTabsFocused(){
        boolean focused = false;
        for (View tabview : tabViews) {
            focused|= tabview.hasFocus();
        }
        return focused;
    }
    public View getTabView(int index){
        return ((ViewGroup) tabs.getChildAt(0)).getChildAt(index);
    }
    public interface OnTabFocusItemListener{
        void onUnfocused();
    }
    public void focusCurrent(){
        tabViews.get(tabs.getSelectedTabPosition()).requestFocus();
    }

}
