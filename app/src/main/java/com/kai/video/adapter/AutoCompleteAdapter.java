package com.kai.video.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class AutoCompleteAdapter extends BaseAdapter implements Filterable {


    private List<String> stringList;
    private Context context;

    private OnItemClickListener onItemClickListener;

    public AutoCompleteAdapter(Context context) {
        this.context = context;
    }
    public void clearAll(){
        stringList.clear();
    }
    public void addAll(List<String> adds){
        stringList.addAll(adds);
    }
    //更新数据
    public void setStringList(List<String> stringList) {
        this.stringList = stringList;
        notifyDataSetChanged();
    }
    //设置item条目点击监听
    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public int getCount() {
        return stringList != null ? stringList.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return stringList != null ? stringList.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.textView = convertView.findViewById(android.R.id.text1);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.textView.setText(stringList.get(position));
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickListener != null)
                    onItemClickListener.OnItemClick(stringList.get(position));
            }
        });

        return convertView;
    }

    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                ArrayList<String> newData = new ArrayList<>();
                if (stringList != null && stringList.size() != 0) {
                    newData.addAll(stringList);
                }
                results.values = newData;
                results.count = newData.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                stringList = (ArrayList) results.values;
                notifyDataSetChanged();
            }
        };
        return filter;
    }

    class ViewHolder {
        private TextView textView;
    }

    public interface OnItemClickListener {
        void OnItemClick(String aaa);
    }


}