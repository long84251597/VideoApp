package com.kai.video.view.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kai.video.R;
import com.kai.video.bean.obj.Selection;
import com.kai.video.adapter.DialogSelectionAdapter;

import java.util.List;

public class SelectionDialog extends AlertDialog {
    private final Context mContext;
    private final List<Selection> selections;
    private DialogSelectionAdapter adapter;
    private RecyclerView recyclerView;
    private DialogSelectionAdapter.OnItemClickListener onItemClickListener;
    private LinearLayoutManager manager;

    public void setOnItemClickListener(DialogSelectionAdapter.OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @SuppressLint("HandlerLeak")
    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (adapter.getCurrent() < 0 || adapter.getCurrent() >= adapter.getItems().size())
                return;
            manager.findViewByPosition(adapter.getCurrent()).findViewById(R.id.item_layout).requestFocus();
        }
    };

    public SelectionDialog(Context context, List<Selection> selections) {
        super(context, R.style.BannerDialog);
        mContext = context;
        this.selections = selections;
        adapter = new DialogSelectionAdapter(selections, -1, this);
        manager = new LinearLayoutManager(context);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_selection_dialog);
        recyclerView = findViewById(R.id.list);
        initList();
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public void show() {
        Window window = getWindow();
        super.show();
        //设置banner类型
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        window.setGravity(Gravity.RIGHT);
        window.setAttributes(layoutParams);
    }

    private void initList(){
        recyclerView.setLayoutManager(manager);
        adapter.setOnItemClickListener((item, position, dialog) -> {
            if (onItemClickListener != null) {
                onItemClickListener.onClick(item, position, dialog);
            }
        });
        adapter.setOnLoadingListener(holder -> {
            Message message1 = new Message();
            manager.scrollToPositionWithOffset(adapter.getCurrent(), 0);
            mHandler.sendMessageDelayed(message1, 100);
        });
        recyclerView.setAdapter(adapter);
    }

    public void resume(int current){
        show();
        manager.scrollToPositionWithOffset(current, 0);
    }



    @Override
    public void dismiss() {
        CustomDialog.OnDismissListener mDismissListener = null;
        mHandler.removeCallbacksAndMessages(null);
        super.dismiss();
    }
}
