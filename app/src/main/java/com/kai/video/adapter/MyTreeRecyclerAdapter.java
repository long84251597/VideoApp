package com.kai.video.adapter;

import com.baozi.treerecyclerview.adpater.TreeRecyclerAdapter;
import com.baozi.treerecyclerview.adpater.TreeRecyclerType;

public class MyTreeRecyclerAdapter extends TreeRecyclerAdapter {
    public MyTreeRecyclerAdapter(TreeRecyclerType showExpand) {
        super(showExpand);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
}
