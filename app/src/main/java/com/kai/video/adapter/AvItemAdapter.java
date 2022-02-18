package com.kai.video.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.kai.video.R;
import com.kai.video.activity.InfoActivity;
import com.kai.video.activity.PlayAcivity;
import com.kai.video.activity.SniffActivity;
import com.kai.video.tool.net.SearchTool;
import com.kai.video.view.dialog.CustomDialog;
import com.kai.video.view.other.ScrollTextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class AvItemAdapter extends RecyclerView.Adapter<AvItemAdapter.ViewHolder> {
    private List<SearchTool.SearchItem> items = new ArrayList<>();
    private ViewHolder firstHolder;
    static class ViewHolder extends RecyclerView.ViewHolder{
        boolean first = false;
        ImageView posterView;
        ScrollTextView videoTitle;
        Context context;
        TextView score;
        TextView year;
        TextView subTitle;
        RelativeLayout relativeLayout;
        public ViewHolder(View view){
            super(view);
            year = view.findViewById(R.id.year);
            relativeLayout = view.findViewById(R.id.main);
            posterView = view.findViewById(R.id.poster);
            videoTitle = view.findViewById(R.id.title);
            score = view.findViewById(R.id.score);
            subTitle = view.findViewById(R.id.subTitle);
        }

        public void setFirst(boolean first) {
            this.first = first;
        }

        public boolean isFirst() {
            return first;
        }

        public void addContext(Context context){
            this.context = context;
        }

        public Context getContext() {
            return context;
        }
    }

    public void setItems(List<SearchTool.SearchItem> items) {
        this.items = items;
    }

    public AvItemAdapter(List<SearchTool.SearchItem> items){
        this.items = items;
    }
    public void addItem(List<SearchTool.SearchItem> items){
        if (items.size() == 0)
            return;
        int origin = this.items.size();
        this.items.addAll(items);
        int length = items.size();
        notifyItemRangeInserted(origin, length);
        onFinishListener.onFinish(origin-1);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.videopic_search, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        holder.addContext(parent.getContext());
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus){
                holder.relativeLayout.requestFocus();
            }
        });
        holder.relativeLayout.setOnClickListener(v -> {
            try {
                if (onLoading != null)
                    onLoading.onStart();
                Log.i("tag", "start");
                int position = holder.getAdapterPosition();
                SearchTool.SearchItem item = items.get(position);
                item.location(new SearchTool.OnConnectListner() {
                    @Override
                    public void onConnected(String href, String title) {
                        v.post(new Runnable() {
                            @Override
                            public void run() {
                                if (onLoading != null)
                                    onLoading.onEnd();
                                Intent intent = new Intent(holder.getContext(), PlayAcivity.class);
                                intent.putExtra("url", href);
                                intent.putExtra("title", item.getType() + " " + title);
                                intent.putExtra("contentType", "video/mepg-url");
                                intent.putExtra("extra", new Bundle());
                                holder.context.startActivity(intent);
                            }
                        });

                    }

                    @Override
                    public void onConnected(List<String> names, List<String> sites) {
                        v.post(new Runnable() {
                            @Override
                            public void run() {
                                if (onLoading != null)
                                    onLoading.onEnd();
                                new CustomDialog.Builder(parent.getContext())
                                        .setTitle("选择播放源")
                                        .setList(names, null, -1)
                                        .setOnItemClickListener((item1, o, position1, dialog) -> {
                                            Intent intent = new Intent(parent.getContext(), InfoActivity.class);
                                            intent.putExtra("url", sites.get(position1));
                                            parent.getContext().startActivity(intent);
                                            dialog.cancel();
                                        })
                                        .create().show();
                            }
                        });

                    }

                    @Override
                    public void onDisConnected() {

                        v.post(new Runnable() {
                            @Override
                            public void run() {
                                if (onLoading != null)
                                    onLoading.onEnd();
                                new CustomDialog.Builder(parent.getContext())
                                        .setTitle("选择播放源")
                                        .setList(Collections.singletonList("全网搜索：" + item.getName()), null, -1)
                                        .setOnItemClickListener((item1, o, position1, dialog) -> {
                                            Toast.makeText(parent.getContext(), "跳转到全网浏览器搜索" , Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(parent.getContext(), SniffActivity.class);
                                            intent.putExtra("wd", item.getName());
                                            parent.getContext().startActivity(intent);
                                        })
                                        .create().show();
                            }
                        });

                    }
                });
            }catch (Exception e){
                e.printStackTrace();
            }


        });
        holder.relativeLayout.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus)
                holder.videoTitle.startScroll();
            else
                holder.videoTitle.stopScroll();
        });
        return holder;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {

            if (position == 0)
                holder.setFirst(true);
            SearchTool.SearchItem item = items.get(position);
            holder.year.setText(item.getYear());
            Glide.with(holder.getContext())
                    .asDrawable()
                    .fitCenter()
                    .load(item.getPoster()).placeholder(R.drawable.loading)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    //.centerInside()
                    .into(holder.posterView);
            holder.videoTitle.setText(item.getName());
            holder.subTitle.setText(item.getType().replaceAll("（.*）", ""));
            if (item.getScore().isEmpty()){
                holder.score.setVisibility(View.INVISIBLE);
            }else {
                holder.score.setVisibility(View.VISIBLE);
                holder.score.setText(item.getScore());
            }


        }catch (Exception e){
            e.printStackTrace();
        }


    }
    boolean first = false;
    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (holder.isFirst() && !first){
            first = true;
            if (onFinishListener != null)
                onFinishListener.onFinish(0);
        }
    }
    private OnFinishListener onFinishListener;

    public void setOnFinishListener(OnFinishListener onFinishListener) {
        this.onFinishListener = onFinishListener;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    public RecyclerView.ViewHolder getFirstHolder() {
        return firstHolder;
    }
    public interface OnFinishListener{
        void onFinish(int position);
    }
    private OnLoading onLoading;

    public void setOnLoading(OnLoading onLoading) {
        this.onLoading = onLoading;
    }

    public interface OnLoading{
        void onStart();
        void onEnd();
    }
}
