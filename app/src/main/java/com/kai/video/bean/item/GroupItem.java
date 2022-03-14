package com.kai.video.bean.item;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baozi.treerecyclerview.base.ViewHolder;
import com.baozi.treerecyclerview.factory.ItemHelperFactory;
import com.baozi.treerecyclerview.item.TreeItem;
import com.baozi.treerecyclerview.item.TreeItemGroup;
import com.kai.video.R;
import com.kai.video.bean.GlideApp;
import com.kai.video.bean.GroupBean;

import java.util.List;

public class GroupItem extends TreeItemGroup<GroupBean> {

    @Nullable
    @Override
    protected List<TreeItem> initChild(GroupBean data) {
        List<TreeItem> items = ItemHelperFactory.createItems(data.getVideoBeans(), this);
        return items;
    }

    @Override
    public int getLayoutId() {
        return R.layout.layout_item_download_group;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder) {

        viewHolder.setOnClickListener(R.id.list_container, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(view.getContext());
                Intent intent = new Intent("com.kai.video.LOCAL_BROADCAST1");
                intent.putExtra("videoAction", "expand");
                intent.putExtra("groupName", data.getGroupName());
                intent.putExtra("expand", !data.isExpand());
                localBroadcastManager.sendBroadcast(intent);
            }
        });
        if (data.isExpand())
            viewHolder.setImageResource(R.id.icon, R.drawable.up);
        else
            viewHolder.setImageResource(R.id.icon, R.drawable.down);
        viewHolder.setVisible(R.id.video_type, true);
        if (data.getPoster() != null && !data.getVideoType().equals("外部下载")){
            GlideApp.with(viewHolder.itemView)
                    .asDrawable()
                    .load(data.getPoster())
                    .placeholder(R.drawable.loading)
                    .fitCenter()
                    .dontAnimate()
                    .into((ImageView) viewHolder.itemView.findViewById(R.id.poster));
        }
        switch (data.getVideoType()){
            case "腾讯视频":viewHolder.setImageResource(R.id.video_type, R.drawable.tencent);break;
            case "爱奇艺":viewHolder.setImageResource(R.id.video_type, R.drawable.iqiyi);break;
            case "芒果视频":viewHolder.setImageResource(R.id.video_type, R.drawable.mgtv);break;
            case "哔哩哔哩": viewHolder.setImageResource(R.id.video_type, R.drawable.bilibili);break;
            case "外部下载": viewHolder.setImageResource(R.id.video_type, R.drawable.broswer);
            viewHolder.setImageResource(R.id.poster, R.drawable.broswer_bac);
            break;
            default:viewHolder.setVisible(R.id.video_type, false);
        }


        viewHolder.setText(R.id.title, data.getVideoName());
        viewHolder.setText(R.id.count, data.getOutPut());
        if (data.isAlive())
            viewHolder.getView(R.id.count).getBackground().setColorFilter(viewHolder.itemView.getContext().getResources().getColor(R.color.color_4k), PorterDuff.Mode.SRC);
        else
            viewHolder.getView(R.id.count).getBackground().setColorFilter(viewHolder.itemView.getContext().getResources().getColor(R.color.color_low), PorterDuff.Mode.SRC);
    }

    @Override
    public boolean isExpand() {
        return data.isExpand();
    }

    @Override
    public boolean isCanExpand() {
        return true;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, RecyclerView.LayoutParams layoutParams, int position) {
        super.getItemOffsets(outRect, layoutParams, position);
        outRect.bottom = 1;
    }
}
