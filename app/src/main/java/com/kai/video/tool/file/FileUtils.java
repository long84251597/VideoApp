package com.kai.video.tool.file;

import android.content.Context;

import com.just.x5.util.FilePath;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FileUtils {
    public static String readableFileSize(long size) {
        if (size <= 0)
            return "0";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
    private static long getTotalSizeOfFilesInDir(final ExecutorService service, final File file) throws InterruptedException, ExecutionException, TimeoutException {
        if (file.isFile())
            return file.length();

        long total = 0;
        final File[] children = file.listFiles();

        if (children != null) {
            final List<Future<Long>> partialTotalFutures = new ArrayList<>();
            for (final File child : children) {
                partialTotalFutures.add(service.submit(() -> getTotalSizeOfFilesInDir(service, child)));
            }

            for (final Future<Long> partialTotalFuture : partialTotalFutures)
                total += partialTotalFuture.get(100, TimeUnit.SECONDS);

        }

        return total;

    }

    public static long getTotalSizeOfFile(final File file) {
        final ExecutorService service = Executors.newFixedThreadPool(100);
        try {
            try {
                return getTotalSizeOfFilesInDir(service, file);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        } finally {
            service.shutdown();
        }
        return 0;
    }
    public static void remove(File file){
        File[] files = file.listFiles();//将file子目录及子文件放进文件数组
        if (files!=null){//如果包含文件进行删除操作
            for (File value : files) {
                if (value.isFile()) {//删除子文件
                    value.delete();
                } else if (value.isDirectory()) {//通过递归方法删除子目录的文件
                    remove(value);
                }
                value.delete();//删除子目录
            }
        }

    }
    public static void clearVideoCache(Context context){
        remove(FilePath.getCachePath(context, ""));
    }
    public static String getVideoCacheSize(Context context){
        return readableFileSize(getTotalSizeOfFile(FilePath.getCachePath(context, "")));
    }

    public static String getFileSize(File file){
        return readableFileSize(getTotalSizeOfFile(file));
    }
}
