package com.danikula.videocache.file;

import java.io.File;

/**
 * {@link DiskUsage} that uses LRU (Least Recently Used) strategy and trims cache size to max files count if needed.
 *
 * @author Alexey Danilov (danikula@gmail.com).·
 */
public class TotalCountLruDiskUsage extends LruDiskUsage {

    private final int maxCount;

    public TotalCountLruDiskUsage(int maxCount) {
        if (maxCount <= 0) {
            throw new IllegalArgumentException("Max count must be positive number!");
        }
        this.maxCount = maxCount;
    }
    //超过数量界限且时间过期的文件要被删除
    @Override
    protected boolean accept(File file, long totalSize, int totalCount) {
        return totalCount <= maxCount;
    }
}
