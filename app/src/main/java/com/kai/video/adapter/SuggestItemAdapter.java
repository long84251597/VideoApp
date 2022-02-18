package com.kai.video.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.kai.video.R;
import com.kai.video.view.other.ScrollTextView;

import java.util.List;


public class SuggestItemAdapter extends RecyclerView.Adapter<SuggestItemAdapter.ViewHolder> {
    private List<String> items;
    private OnItemClickListener onItemClickListener = null;
    public static class ViewHolder extends RecyclerView.ViewHolder{
        LinearLayout itemLayout;
        ScrollTextView item;
        boolean selected = false;
        public ViewHolder(View view){
            super(view);
            item = view.findViewById(R.id.item);
            itemLayout = view.findViewById(R.id.item_layout);
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public boolean isSelected() {
            return selected;
        }
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    private AlertDialog dialog;
    public SuggestItemAdapter(List<String> items){
        this.items = items;
    }

    public List<String> getItems() {
        return items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_suggest, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus)
                holder.itemLayout.requestFocus();
        });
        holder.itemLayout.setOnClickListener(v -> onItemClickListener.onClick(items.get(holder.getAdapterPosition())));
        return holder;
    }
    boolean scrollToPosition = false;
    private OnLoadListener onLoadingListener;

    public void setOnLoadingListener(OnLoadListener onLoadingListener) {
        this.onLoadingListener = onLoadingListener;
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (onLoadingListener != null && holder.isSelected() && !scrollToPosition){
            scrollToPosition = true;
            onLoadingListener.onFinish(holder);
        }
    }

    public ViewHolder getCurrentHolder() {
        return currentHolder;
    }

    private final ViewHolder currentHolder = null;
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String name = items.get(position);
        holder.item.setText(name);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }
    public interface OnLoadListener{
        void onFinish(ViewHolder holder);
    }
    public interface OnItemClickListener{
        void onClick(String item);
    }

}
