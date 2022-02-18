package com.danikula.videocache.file;

import android.text.TextUtils;

import com.danikula.videocache.IPTool;
import com.danikula.videocache.ProxyCacheUtils;

/**
 * Implementation of {@link FileNameGenerator} that uses MD5 of url as file name
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class Md5FileNameGenerator implements FileNameGenerator {

    private static final int MAX_EXTENSION_LENGTH = 4;

    @Override
    public String generate(String url) {
        String extension = getExtension(url);
        if (url.contains("bilivideo"))
            url = url.replaceAll(".*upgcxcode", "");
        url = url.replaceAll("\\?.*", "");

        String name = ProxyCacheUtils.computeMD5(url);
        return TextUtils.isEmpty(extension) ? name : name + "." + extension;
    }

    private String getExtension(String url) {
        if (url.startsWith(IPTool.getLocal() + "/analysis?url="))
            return "ffcat";
        int dotIndex = url.lastIndexOf('.');
        int slashIndex = url.lastIndexOf('/');
        int argsIndex = url.indexOf('?');
        if (argsIndex > 0 & argsIndex > dotIndex)
            url = url.substring(0, argsIndex);

        return dotIndex != -1 && dotIndex > slashIndex && dotIndex + 2 + 4 > url.length() ?
                url.substring(dotIndex + 1, url.length()) : "";
    }
}
