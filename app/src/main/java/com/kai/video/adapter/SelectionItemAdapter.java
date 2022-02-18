package com.kai.video.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kai.video.R;
import com.kai.video.bean.obj.Selection;

import java.util.ArrayList;
import java.util.List;


public class SelectionItemAdapter extends RecyclerView.Adapter<SelectionItemAdapter.ViewHolder> {
    //局部数据
    private List<Selection> array = new ArrayList<>();
    //全局数据
    private List<Selection> arrayAll = new ArrayList<>();
    //局部定位
    private int header = 0;
    private onListener onListener = null;
    //相当于绝对位置
    private int current = 0;
    //相当于相对位置
    private int offset = 0;
    //是否selected过
    private boolean selected = false;
    static class ViewHolder extends RecyclerView.ViewHolder{
        Button title;
        TextView vip;
        Context context;
        RelativeLayout relativeLayout;
        public ViewHolder(View view){
            super(view);
            vip = view.findViewById(R.id.vip);
            title = view.findViewById(R.id.title);
            relativeLayout = view.findViewById(R.id.main);
        }
        public void addContext(Context context){
            this.context = context;
        }

        public Context getContext() {
            return context;
        }
    }

    public void setOnListener(SelectionItemAdapter.onListener onListener) {
        this.onListener = onListener;
    }
    @SuppressLint("NotifyDataSetChanged")
    public void showList(int header, int tailer){
        this.header = header;
        array.clear();
        array = new ArrayList<>();
        for(int i = header; i <= tailer; i++){
            array.add(arrayAll.get(i));
        }
        notifyDataSetChanged();
    }

    /**
     * 设置全局的集数和第一组集数
     * @param arrayAll
     * @param firstItem
     */
    @SuppressLint("NotifyDataSetChanged")
    public void setArray(List<Selection> arrayAll, GroupAdapter.GroupItem firstItem) {
        this.arrayAll = arrayAll;
        if (firstItem == null)
            showList(0, arrayAll.size()-1);
        else
            showList(0, firstItem.tailer);


    }
    public int getType(){
        if (arrayAll.size() == 0)
            return 0;
        return arrayAll.get(current).getType();
    }
    public SelectionItemAdapter(List<Selection> array){
        this.array = array;
    }
    public List<Selection> getArray() {
        return array;
    }

    public List<Selection> getArrayAll() {
        return arrayAll;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current){
        this.current = current;
        notifyChecked(current);
    }

    public int getOffset() {
        return offset;
    }

    public void notifyChecked(int index){
        selected = true;
        boolean inPage = index - header <= 50 && index >= header;
        if (inPage){
            offset = index - header;
            notifyItemChanged(offset);
        }
    }
    //用绝对定位去改变值
    public void changeAbsolutely(int position){
        try {
            if (arrayAll.size() == 0)
                return;
            int old = current;
            current = position;
            notifyChecked(old);
            notifyChecked(current);
        }catch (Exception e){
            e.printStackTrace();
        }

    }
    //正常情况，使用相对定位offset去改变值
    //这种情况下，必然是位于当前页的
    public void change(int offset){
        int position = header + offset;
        changeAbsolutely(position);
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.selection_item, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus)
                holder.title.requestFocus();
        });
        holder.addContext(parent.getContext());
        holder.title.setOnClickListener(v -> {
            try {
                int offset =holder.getAdapterPosition();
                change(offset);
                onListener.onClick(header + offset, offset);
            }catch (Exception e){
                e.printStackTrace();
            }


        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            Selection object = array.get(position);
            holder.title.setBackgroundResource(R.drawable.button_item_selector);
            if (selected) {
                holder.title.setSelected(position + header == current);
            }
            holder.title.setText(object.getTitle().replaceAll("-", ""));
            holder.vip.setText("");
            holder.vip.setBackground(null);
            switch (object.getType()){
                case 0:break;
                case 1:holder.vip.setText("预告");
                    holder.vip.setBackgroundResource(R.drawable.yugao);
                    break;
                case 2:holder.vip.setText("会员");
                    holder.vip.setBackgroundResource(R.drawable.vip);
                    break;
                case 3:holder.vip.setText("点播");
                    holder.vip.setBackgroundResource(R.drawable.dianbo);
                    break;

            }
        }catch (Exception e){
            e.printStackTrace();
        }


    }
    public int getAllCount(){
        return arrayAll.size();
    }
    @Override
    public int getItemCount() {
        return array.size();
    }


    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }
    public interface onListener{
        void onEnsure(int currentPosition);
        void onClick(int position, int offset);
    }

}
