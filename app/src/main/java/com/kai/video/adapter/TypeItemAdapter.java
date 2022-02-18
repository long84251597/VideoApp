package com.kai.video.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kai.video.R;

import java.util.List;


public class TypeItemAdapter extends RecyclerView.Adapter<TypeItemAdapter.ViewHolder> {
    private List<String> items;
    private int current = 0;
    private onItemClickListener onItemClickListener = null;
    private OnFinishListener onFinishListener = null;
    public void setOnItemClickListener(TypeItemAdapter.onItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public void setCurrent(int current) {
        int origin = this.current;
        this.current = current;
        notifyItemChanged(origin);
        notifyItemChanged(current);
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
    public TypeItemAdapter(List<String> items){

        this.items = items;
    }






    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.type_item, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus)
                holder.title.requestFocus();
        });
        holder.title.setOnClickListener(v -> {
            try {
                int position =holder.getAdapterPosition();
                setCurrent(position);
                if (onItemClickListener!= null)
                    onItemClickListener.onClick(position);
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
            if (position == current){
                holder.title.setBackgroundResource(R.drawable.selection_item_selector_active);
            }else {
                holder.title.setBackgroundResource(R.drawable.button_item_selector);
            }
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
        void onClick(int position);
    }
    public interface OnFinishListener{
        void onFinish();
    }

}
