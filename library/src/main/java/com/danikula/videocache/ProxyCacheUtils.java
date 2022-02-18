package com.danikula.videocache;

import static com.danikula.videocache.Preconditions.checkArgument;
import static com.danikula.videocache.Preconditions.checkNotNull;

import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Just simple utils.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class ProxyCacheUtils {
    //普通流媒体标记，如MP4、FLV
    public static String TAG_STREAM = "request=stream";
    //HLS流清单标记，如M3U、M3U8
    public static String TAG_M3U8 = "request=m3u8";
    //HLS流媒体标记，主要是m4a和ts
    public static String TAG_TS = "request=ts";
    //CONCAT流媒体标记，任意媒体都有可能
    public static String TAG_CONCAT = "request=concat_seg";
    private static final Logger LOG = LoggerFactory.getLogger("ProxyCacheUtils");
    //调大缓存区
    static final int DEFAULT_BUFFER_SIZE = 8 * 1024;
    static final int MAX_ARRAY_PREVIEW = 16;

    static String getSupposablyMime(String url) {
        MimeTypeMap mimes = MimeTypeMap.getSingleton();
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        return TextUtils.isEmpty(extension) ? null : mimes.getMimeTypeFromExtension(extension);
    }

    static boolean isM3U8(String url) {
        return url.contains(TAG_M3U8);
    }
    public static boolean isConcatSegment(String url){
        return url.contains(TAG_CONCAT) ;
    }
    public static boolean isConcatList(String url) {
        return url.startsWith( IPTool.getLocal() + "/analysis");
    }
    static boolean isTS(String url) {
        //return "ts".equals(MimeTypeMap.getFileExtensionFromUrl(url)) || url.endsWith(".key")||url.contains(".mp4")||url.contains();
        return url.contains(TAG_TS);
    }

    static void assertBuffer(byte[] buffer, long offset, int length) {
        checkNotNull(buffer, "Buffer must be not null!");
        checkArgument(offset >= 0, "Data offset must be positive!");
        checkArgument(length >= 0 && length <= buffer.length, "Length must be in range [0..buffer.length]");
    }

    static String preview(byte[] data, int length) {
        int previewLength = Math.min(MAX_ARRAY_PREVIEW, Math.max(length, 0));
        byte[] dataRange = Arrays.copyOfRange(data, 0, previewLength);
        String preview = Arrays.toString(dataRange);
        if (previewLength < length) {
            preview = preview.substring(0, preview.length() - 1) + ", ...]";
        }
        return preview;
    }

    static String encode(String url) {
        try {
            return URLEncoder.encode(url, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Error encoding url", e);
        }
    }

    static String decode(String url) {
        try {
            return URLDecoder.decode(url, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Error decoding url", e);
        }
    }

    static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                LOG.error("Error closing resource", e);
            }
        }
    }

    public static String computeMD5(String string) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] digestBytes = messageDigest.digest(string.getBytes());
            return bytesToHexString(digestBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
