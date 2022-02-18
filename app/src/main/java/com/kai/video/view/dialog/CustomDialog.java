package com.kai.video.view.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kai.video.R;
import com.kai.video.manager.DeviceManager;
import com.kai.video.adapter.DialogItemAdapter;

import java.util.List;

public class CustomDialog extends AlertDialog {
    private Context mContext;
    private String mTitle = "";
    private String mMessage = "";
    private DialogItemAdapter adapter = null;
    private LinearLayoutManager manager = null;
    private int gravity = Gravity.CENTER;
    private int contentView = R.layout.layout_dialog;
    @SuppressLint("RtlHardcoded")
    public CustomDialog(Context context) {
        super(context, DeviceManager.getDialogTheme());
        int theme = DeviceManager.getDialogTheme();
        if (theme == R.style.CustomDialog)
            setGravity(Gravity.CENTER);
        else
            setGravity(Gravity.RIGHT);
        mContext = context;
    }
    public CustomDialog(Context context, int theme){
        super(context, theme);
        mContext = context;
    }

    @SuppressLint("RtlHardcoded")
    public void setGravity(int gravity) {
        this.gravity = gravity;
        if (gravity == Gravity.RIGHT){
            contentView = R.layout.layout_banner;
        }
    }



    @SuppressLint("RtlHardcoded")
    @Override
    public void show() {
        Window window = getWindow();
        super.show();
        //设置banner类型
        if (gravity == Gravity.RIGHT){
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            window.setGravity(gravity);
            window.setAttributes(layoutParams);
        }

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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(contentView);
        setCanceledOnTouchOutside(true);
        //初始化界面控件
        TextView title = findViewById(R.id.title);
        TextView message = findViewById(R.id.message);
        RecyclerView recyclerView = findViewById(R.id.list);
        title.setText(mTitle);
        message.setText(mMessage);
        if (mMessage.isEmpty()){
            message.setVisibility(View.GONE);
        }
        recyclerView.setLayoutManager(manager);
        adapter.setOnLoadingListener(holder -> {
            Message message1 = new Message();
            manager.scrollToPositionWithOffset(adapter.getCurrent(), 0);
            mHandler.sendMessageDelayed(message1, 100);
        });
        recyclerView.setAdapter(adapter);
    }
    Handler handler = new Handler();
    Runnable currentRunnable = () -> manager.findViewByPosition(adapter.getCurrent()).findViewById(R.id.item_layout).requestFocus();
    public void resume(){
        setCurrent();
        show();
    }

    @Override
    public void dismiss() {
        OnDismissListener mDismissListener = null;
        handler.removeCallbacksAndMessages(null);
        mHandler.removeCallbacksAndMessages(null);
        mContext = null;
        super.dismiss();
    }

    public void setCurrent(int i){
        adapter.setCurrent(i);
        setCurrent();
    }
    public void setCurrent(){
        manager.scrollToPositionWithOffset(adapter.getCurrent(), 0);
        new Thread(() -> handler.postDelayed(currentRunnable, 200)).start();

    }
    public void addItem(String name, Object o){
        adapter.addItem(name, o);
    }
    public void removeItem(String name){
        adapter.removeItem(name);
    }
    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public void setMessage(String mMessage) {
        this.mMessage = mMessage;
    }

    public void setOnItemClickListener(DialogItemAdapter.OnItemClickListener itemClickListener){
        adapter.setOnItemClickListener(itemClickListener);
    }
    public void setList(List<String> names, List<Object> objects, int select){
        manager = new LinearLayoutManager(mContext);
        adapter = new DialogItemAdapter(names, objects, select, CustomDialog.this);

    }

    @Override
    public void setOnCancelListener(@Nullable OnCancelListener listener) {
        adapter.setOnDismissListener(listener);
        super.setOnCancelListener(listener);

    }



    public static class Builder{
        private final CustomDialog dialog;
        @SuppressLint("RtlHardcoded")
        public Builder(Context context, int theme){
            dialog = new CustomDialog(context, theme);
            if (theme == R.style.CustomDialog)
                dialog.setGravity(Gravity.CENTER);
            else
                dialog.setGravity(Gravity.RIGHT);
        }
        public Builder(Context context){
            dialog = new CustomDialog(context);
        }
        public Builder setTitle(String title){
            dialog.setTitle(title);
            return this;
        }
        public Builder setMessage(String message){
            dialog.setMessage(message);
            return this;
        }
        public Builder setList(List<String> names, List<Object> objects, int select){
            dialog.setList(names, objects, select);
            return this;
        }
        public Builder setOnItemClickListener(DialogItemAdapter.OnItemClickListener onItemClickListener){
            dialog.setOnItemClickListener(onItemClickListener);
            return this;
        }
        public Builder setOnCancelListner(OnCancelListener onCancelListener){
            dialog.setOnCancelListener(onCancelListener);
            return this;
        }
        public Builder setGravity(int gravity){
            dialog.setGravity(gravity);
            return this;
        }

        public CustomDialog create(){
            dialog.create();
            return dialog;
        }
        public void show(){
            dialog.show();
        }
    }
    public interface OnDismissListener{
        void onDismiss(DialogInterface dialog);
    }
}
