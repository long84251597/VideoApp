package com.kai.sniffwebkit;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.akexorcist.roundcornerprogressbar.TextRoundCornerProgressBar;


public class LoadingView extends LinearLayout {
    private final TextRoundCornerProgressBar progressBar;
    private final TextView title;
    private final TextView subTitle;
    private final Handler handler = new Handler();
    public LoadingView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.layout, this);
        progressBar = findViewById(R.id.bar);
        title = findViewById(R.id.title);
        subTitle = findViewById(R.id.subTitle);
    }
    public void setSubTitle(String t){
        subTitle.setText(t);
    }
    public void setProgress(int progress, String text){
        if (progress < 0)
            progress = 0;
        if (progress > 100)
            progress = 100;
        if (progress > progressBar.getProgress())
            progressBar.enableAnimation();
        else
            progressBar.disableAnimation();
        progressBar.setProgress(progress);
        progressBar.setProgressText(progress + "%");
        title.setText(text);
    }

    public void  finish(String finishMessage){
        progressBar.enableAnimation();
        progressBar.setProgress(100);
        progressBar.setProgressText("100%");
        title.setText(finishMessage);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                hide();
            }
        }, 500);
    }

    public void show(){
        handler.removeCallbacksAndMessages(null);
        setVisibility(VISIBLE);
        measure(0,0);
        ViewGroup.LayoutParams params = progressBar.getLayoutParams();
        params.width = getMeasuredWidth();
        progressBar.setLayoutParams(params);
    }

    public void hide(){
        title.setText("");
        subTitle.setText("");
        progressBar.setProgressText("");
        progressBar.setProgress(0);
        setVisibility(INVISIBLE);
    }
}
