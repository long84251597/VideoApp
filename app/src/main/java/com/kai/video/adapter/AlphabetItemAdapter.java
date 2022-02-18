package com.kai.video.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kai.video.R;

import java.util.ArrayList;
import java.util.List;


public class AlphabetItemAdapter extends RecyclerView.Adapter<AlphabetItemAdapter.ViewHolder> {
    private List<String> items = new ArrayList<>();
    private onItemClickListener onItemClickListener = null;
    private OnFinishListener onFinishListener = null;
    public void setOnItemClickListener(AlphabetItemAdapter.onItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void setOnFinishListener(OnFinishListener onFinishListener) {
        this.onFinishListener = onFinishListener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        Button title;
        boolean first;
        public ViewHolder(View view){
            super(view);
            title = view.findViewById(R.id.title);
        }

        public boolean isFirst() {
            return first;
        }

        public void setFirst(boolean first) {
            this.first = first;
        }
    }
    public AlphabetItemAdapter(List<String> items){

        this.items = items;
    }






    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.alphabet_item, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus)
                holder.title.requestFocus();
        });
        holder.title.setOnClickListener(v -> {
            try {
                int position =holder.getAdapterPosition();
                if (onItemClickListener!= null)
                    onItemClickListener.onClick(items.get(position));
            }catch (Exception e){
                e.printStackTrace();
            }


        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            holder.setFirst(position == 0);
            String item = items.get(position);
            holder.title.setText(item);
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
            if (onFinishListener!=null)
                onFinishListener.onFinish();
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }
    public interface onItemClickListener{
        void onClick(String key);
    }
    public interface OnFinishListener{
        void onFinish();
    }

}
