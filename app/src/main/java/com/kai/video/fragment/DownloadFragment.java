package com.kai.video.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.jeffmony.downloader.model.VideoTaskItem;
import com.kai.video.R;

public abstract class DownloadFragment extends Fragment {
    public abstract void updateData(VideoTaskItem item);
    protected View contentView;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        contentView = inflater.inflate(R.layout.fragment_main_download, container, false);
        return contentView;
    }
}
