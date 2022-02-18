package com.kai.video.adapter;


import android.annotation.SuppressLint;
import android.content.DialogInterface;
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


public class DialogItemAdapter extends RecyclerView.Adapter<DialogItemAdapter.ViewHolder> {
    private List<String> items;
    private final List<Object> objects;
    private OnItemClickListener onItemClickListener = null;
    int current;
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

    public void setOnDismissListener(DialogInterface.OnCancelListener cancelListener) {
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;

    }
    public void addItem(String name, Object o){
        items.add(name);
        if (objects != null)
            objects.add(o);
        notifyItemInserted(getItemCount() - 1);
    }
    public void removeItem(String name){
        for (int i = 0; i < items.size(); i++) {
            String item = items.get(i);
            if (item.equals(name)){
                items.remove(i);
                objects.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }
    private final AlertDialog dialog;
    public DialogItemAdapter(List<String> items, List<Object> objects, int select, AlertDialog dialog){
        this.items = items;
        this.objects = objects;
        current = select;
        this.dialog = dialog;
    }

    public List<String> getItems() {
        return items;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        if (current == this.current)
            return;
        notifyItemChanged(this.current);
        this.current = current;
        notifyItemChanged(this.current);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_dialog, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus)
                holder.itemLayout.requestFocus();
        });
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

    private ViewHolder currentHolder = null;
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        String name = items.get(position);
        holder.item.setText(name);
        //如果发现容器位置符合
        if (position == 2)
            holder.setSelected(true);
        if (position == current){
            currentHolder = holder;
            holder.itemLayout.setBackgroundResource(R.drawable.dialog_item_background_selected);
        }else {
            holder.itemLayout.setBackgroundResource(R.drawable.dialog_item_background);
        }
        holder.itemLayout.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus){
                holder.item.startScroll();
            }else
                holder.item.stopScroll();
        });
        holder.itemLayout.setOnClickListener(v -> {
            if (current != -1){
                currentHolder = holder;
                int currentOld = current;
                current = position;
                notifyItemChanged(currentOld);
                notifyItemChanged(current);
                currentHolder.itemView.requestFocus();
            }

            if (objects != null && objects.size() > position)
                onItemClickListener.onClick(items.get(position), objects.get(position), position, dialog);
            else
                onItemClickListener.onClick(items.get(position), null, position, dialog);
        });

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
        void onClick(String item, Object o, int position, AlertDialog dialog);
    }



}
