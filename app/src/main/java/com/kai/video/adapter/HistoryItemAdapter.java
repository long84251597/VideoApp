package com.kai.video.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kai.video.R;
import com.kai.video.activity.HistoryActivity;
import com.kai.video.activity.InfoActivity;
import com.kai.video.bean.GlideApp;
import com.kai.video.bean.item.CommendItem;
import com.kai.video.bean.item.NaviItem;
import com.kai.video.manager.DeviceManager;
import com.kai.video.view.other.ScrollTextView;
import com.youth.banner.Banner;
import com.youth.banner.BannerConfig;
import com.youth.banner.loader.ImageLoader;

import java.util.ArrayList;
import java.util.List;


public class HistoryItemAdapter extends RecyclerView.Adapter<HistoryItemAdapter.ViewHolder> {
    private List<HistoryActivity.HistoryItem> items = new ArrayList<>();

    public void setItems(List<HistoryActivity.HistoryItem> items) {
        this.items = items;
    }

    public List<HistoryActivity.HistoryItem> getItems() {
        return items;
    }
    static class ViewHolder extends RecyclerView.ViewHolder{
        ImageView header;
        ImageView poster;
        TextView videoName;
        TextView videoTitle;
        TextView videoTime;
        RelativeLayout relativeLayout;
        public ViewHolder(View view){
            super(view);
            header = view.findViewById(R.id.header);
            poster = view.findViewById(R.id.pic);
            videoName = view.findViewById(R.id.name);
            videoTitle = view.findViewById(R.id.videoTitle);
            videoTime = view.findViewById(R.id.time);
            relativeLayout = view.findViewById(R.id.list_container);
        }


    }
    public HistoryItemAdapter(List<HistoryActivity.HistoryItem> items){
        this.items = items;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_history, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus){
                holder.relativeLayout.requestFocus();
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        HistoryActivity.HistoryItem element = items.get(position);
        String url = element.getUrl();
        holder.videoName.setText(element.getName());
        holder.videoTitle.setText(element.getVideoTitle());
        holder.relativeLayout.setOnFocusChangeListener(null);
        holder.videoTime.setText("观看到 " + element.getTime());
        int srcId = 0;
        holder.header.setVisibility(View.VISIBLE);
        switch (element.getVideoType()){
            case "tencent": srcId = R.drawable.tencent;break;
            case "iqiyi": srcId = R.drawable.iqiyi;break;
            case "mgtv": srcId = R.drawable.mgtv;break;
            case "bilibili": srcId = R.drawable.bilibili;break;
            default:holder.header.setVisibility(View.INVISIBLE); break;
        }
        GlideApp.with(holder.itemView).load(srcId).dontAnimate().into(holder.header);
        GlideApp.with(holder.itemView.getContext())
                .asDrawable()
                .load(element.getCoverPic())
                .placeholder(R.drawable.loading)
                .fitCenter()
                .dontAnimate()
                .into(holder.poster);
        holder.relativeLayout.setOnClickListener(v -> {
            Intent intent = new Intent(holder.header.getContext(), InfoActivity.class);
            intent.putExtra("name", element.getName());
            intent.putExtra("url", url);
            holder.header.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        if (items.get(position) == null)
            return position;
        return items.get(position).hashCode();
    }

    public interface OnFinishListener{
        void onFinish();
    }
}
