package com.kai.video.adapter;


import android.content.DialogInterface;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kai.video.R;
import com.kai.video.bean.obj.Selection;

import java.util.ArrayList;
import java.util.List;


public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.ViewHolder> {
    private final List<GroupItem> items = new ArrayList<>();
    public static int LENGTH_DEFAULT = 50;
    private OnItemClickListener onItemClickListener = null;
    private int current = 0;
    public static class ViewHolder extends RecyclerView.ViewHolder{
        LinearLayout itemLayout;
        TextView item;
        public ViewHolder(View view){
            super(view);
            item = view.findViewById(R.id.item);
            itemLayout = view.findViewById(R.id.item_layout);
        }
    }
    public void changeGroup(int itemCount, Searcher searcher){
        Handler handler = new Handler();
        new Thread(() -> {
            int length = getItemCount();
            for(int i = 0; i < length; i++){
                GroupItem groupItem = items.get(i);
                if (groupItem.header <= itemCount && groupItem.tailer >= itemCount){
                    final int group = i;
                    handler.post(() -> searcher.onSearch(group, groupItem.header, groupItem.tailer));
                    break;
                }
            }
        }).start();
    }
    public interface Searcher{
        void onSearch(int group, int header, int tailer);
    }
    public void addGroup(int itemCount, int realCount){
        int groupCount = (realCount -1 ) / LENGTH_DEFAULT;
        GroupItem item;
        if (items.size() > groupCount){
            Log.e("tag", itemCount + "_" + realCount);
            item = items.get(groupCount);
            item.increase(itemCount);
            items.set(groupCount, item);
        }else {
            item = new GroupItem(itemCount, realCount);
            items.add(item);
        }
    }
    public GroupItem getLastItem(){
        if (items.size() == 0)
            return null;
        return items.get(items.size() - 1);
    }
    public GroupItem getFirstItem(){
        if (items.size() == 0)
            return null;
        return items.get(0);
    }
    public void initItems(List<Selection> array) {
        int length = array.size();
        for (int i = 0; i < length; i++){
            Selection object = array.get(i);
            try {
                if (object.getType() != 1){
                    Log.e("tag", i + " " + object.getTitle());
                    int count = Integer.parseInt(object.getTitle());
                    addGroup(i, count);
                }
            }catch (Exception ignored){

            }
        }
    }

    public void setOnDismissListener(DialogInterface.OnCancelListener cancelListener) {
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;

    }

    public GroupAdapter(){

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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_group, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus)
                holder.itemLayout.requestFocus();
        });
        holder.itemLayout.setOnClickListener(v -> {
            GroupItem item = items.get(holder.getAdapterPosition());
            onItemClickListener.onClick(holder.getAdapterPosition(), item.header, item.tailer);
        });
        return holder;
    }
    boolean scrollToPosition = false;

    public void setOnLoadingListener(OnLoadListener onLoadingListener) {
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GroupItem item = items.get(position);
        //设置选中效果
        //设置未选中效果
        holder.itemLayout.setSelected(position == current);
        holder.item.setText(item.getName());
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
        void onClick(int index, int header, int tailer);
    }

    public static class GroupItem{
        String name;
        int header;//在所有剧集中的次序
        int leader;//最开始的集数
        int length;//集数的长度
        int tailer;//在所有剧集中的结束次序
        int LENGTH_DEFAULT = 50;//默认长度为50
        public GroupItem(int header, int leader){
            this.header = header;
            this.leader = leader;
            this.tailer = header;
        }

        public void increase(int tailer){
            length++;
            this.tailer = tailer;
        }

        public String getName(){
            if (name == null)
                this.name = leader + "-" + (leader + length);
            return name;
        }

        public int getHeader() {
            return header;
        }

        public int getTailer() {
            return tailer;
        }
    }

}
