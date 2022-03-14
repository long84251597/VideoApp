package com.kai.video.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kai.video.R;
import com.kai.video.bean.obj.Selection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class PageItemAdapter extends RecyclerView.Adapter<PageItemAdapter.ViewHolder> {
    //局部数据
    private List<String> array = new ArrayList<>();
    private OnItemClickListener onItemClickListener = null;
    private String current = "...";
    private Handler handler = new Handler();
    private int sum = 0;

    public void setCurrent(String current) {
        this.current = current;
    }

    public void init(int sum, String current){
        this.sum = sum;
        this.current = current;
        new Thread(new Runnable() {
            @Override
            public void run() {
                int c = Integer.parseInt(current);
                int start = c/6 + 1;
                int end = start + 5;
                if (end > sum)
                    end = sum;
                array.clear();
                array.add("<");
                for (int i = start; i <= end; i++){
                    array.add(String.valueOf(i));
                }
                array.add(">");
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
            }
        }).start();

    }

    public void init(int sum){
        current = "1";
        this.sum = sum;
        new Thread(new Runnable() {
            @Override
            public void run() {

                if (sum <= 7){
                    array.clear();
                    for (int i = 1; i <= sum; i++){
                        array.add(String.valueOf(i));
                    }
                }else {
                    array = new ArrayList<>(Arrays.asList("<", "1", "2", "3", "4", "5", "6", ">"));
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
            }
        }).start();

    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        Button title;
        LinearLayout relativeLayout;
        public ViewHolder(View view){
            super(view);
            title = view.findViewById(R.id.title);
            relativeLayout = view.findViewById(R.id.main);
        }
    }





    public PageItemAdapter(List<String> array){
        this.array = new ArrayList<>(array);
    }
    public List<String> getArray() {
        return array;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_page, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus)
                holder.title.requestFocus();
        });

        return holder;
    }

    public String getCurrent() {
        return current;
    }

    public void setArray(List<String> array) {
        this.array = array;
    }



    public String getFirst(){
        if (array.isEmpty())
            return "0";
        return array.get(1);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            String index = array.get(position);
            holder.title.setBackgroundResource(R.drawable.button_item_selector);
            holder.title.setText(index);
            if (current.equals(index))
                holder.title.setSelected(true);
            else
                holder.title.setSelected(false);
            holder.title.setOnClickListener(v -> {
                try {
                    if (onItemClickListener != null)
                        onItemClickListener.onClick(position, index, sum);
                    if (!index.equals("<") && !index.equals(">")){
                        setCurrent(index);
                        notifyDataSetChanged();
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }


            });
        }catch (Exception e){
            e.printStackTrace();
        }


    }
    @Override
    public int getItemCount() {
        return array.size();
    }


    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }
    public interface OnItemClickListener{
        void onClick(int position, String offset, int sum);
    }


}
