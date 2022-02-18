package com.danikula.videocache;

import static com.danikula.videocache.ProxyCacheUtils.DEFAULT_BUFFER_SIZE;

import android.content.Context;
import android.telephony.mbms.DownloadRequest;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.danikula.videocache.file.FileCache;
import com.danikula.videocache.file.LogContent;
import com.danikula.videocache.parser.Element;
import com.danikula.videocache.parser.Playlist;
import com.kingsoft.media.httpcache.KSYProxyService;
import com.kingsoft.media.httpcache.OnCacheStatusListener;
import com.kingsoft.media.httpcache.stats.OnLogEventListener;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloadQueueSet;
import com.liulishuo.filedownloader.FileDownloader;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class M3U8ProxyCache extends HttpProxyCache implements OnCacheStatusListener, OnLogEventListener {
    //为大分片准备，最多支持20分片并行下载，可根据分片总数量决定
    private static final int MAX_FILES = 6;
    private static final int MAX_CACHE_SIZE = 1024 * 1024 * 1024;
    private final Context context;
    private final List<String> mUrls = new ArrayList<>();
    private static int M3U8_CACHE_STEP = 20;
    private File m3u8CacheDir;
    private String baseUrl;
    private String baseCachePath;
    private List<Element> elements;
    private Map<String, M3U8Item> m3U8ItemMap = new HashMap<>();
    private KSYProxyService proxy;
    private int mCurRequestPos = 0; // 客户端正在请求的ts索引
    private int mCurCachePos = 0;   // 代理端正在缓存的，离mCurRequestPose最远的ts索引
    private int mCacheShift = 20;    // 缓存位置与播放位置偏移，对需要ts需要解码的情况，就不能有偏移了，必须全部走缓存流程
    private DecryptInfo mDecryptInfo;
    private volatile boolean mTransformed = false;
    private final Map<String, String> downloadingTask = new HashMap<>();

    public static void setM3u8CacheStep(int percentage, int tsSize){
        M3U8_CACHE_STEP = tsSize / 100 * percentage;
    }
    static class M3U8Item {
        public Element element;
        public DownloadRequest dlRequest;
        public M3U8Item(Element element, DownloadRequest request) {
            this.element = element;
            this.dlRequest = request;
        }

        public M3U8Item(Element element) {
            this(element, null);
        }
    }

    public static class DecryptInfo {
        public int step;
        public String key;

        public DecryptInfo(String key, int step) {
            this.key = key;
            this.step = step;
        }
    }

    public static class CacheItem {
        public DecryptInfo decryptInfo;
        public String filePath;
        public CacheItem(String filePath, DecryptInfo decryptInfo) {
            this.filePath = filePath;
            this.decryptInfo = decryptInfo;
        }
    }
    @Override
    public void onLogEvent(String log) {
        try {
            LogContent content = JSONObject.parseObject(log, LogContent.class);
            String type = content.getBody_type();

            switch (type){
                case "probe":
                    //logE("次序：" + mUrls.indexOf(downloadingTask.get(content.getCacheId())));
                    int index = mUrls.indexOf(downloadingTask.get(content.getCacheId()));
                    if (index > 0) {
                        mCurRequestPos = index;
                        scheduleCache();
                    }
                    break;
                case "stopCache":
                    if (mUrls.get(mCurCachePos).equals(content.getUrl()))
                        scheduleCache();
                    break;
                case "startCache":
                    downloadingTask.put(content.getCacheId(), content.getUrl());
                    break;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void OnCacheStatus(String url, long sourceLength, int percentsAvailable) {

        if (percentsAvailable == 100) {
            this.listener.onM3U8ItemDecrypt(new M3U8ProxyCache.CacheItem(url, new M3U8ProxyCache.DecryptInfo("xxxx", 0)));
        }
        //onProgressUpdate(mUrls.indexOf(url), percentsAvailable);
    }
    private void initProxy(){
        if (!m3u8CacheDir.exists())
            m3u8CacheDir.mkdirs();
        else
            for(File file:m3u8CacheDir.listFiles())
                file.delete();
        proxy = new KSYProxyService(context);
        proxy.setMaxFilesCount(MAX_FILES);
        //缓存名字一定要忽略参数，这样可以保证不浪费控件
        proxy.setIgnoreUrlSuffixParam(true);
        proxy.setMaxCacheSize(MAX_CACHE_SIZE);
        proxy.setCacheRoot(m3u8CacheDir);
        proxy.startServer();
    }
    public M3U8ProxyCache(Context context, HttpUrlSource source, FileCache cache) {
        super(source, cache);
        this.context = context;
        baseUrl = source.getUrl();
        int end = baseUrl.indexOf("?");
        if (end > 0)
            baseUrl = baseUrl.substring(0, end);

        end = baseUrl.lastIndexOf("/");
        baseUrl = baseUrl.substring(0, end + 1);

        mDecryptInfo = new DecryptInfo(source.getKey(), 512);
        m3u8CacheDir = new File(cache.file.getAbsolutePath() + ".cache/");
        initProxy();
    }

    final FileDownloadListener queueTarget = new FileDownloadListener() {
        @Override
        protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
        }

        @Override
        protected void connected(BaseDownloadTask task, String etag, boolean isContinue, int soFarBytes, int totalBytes) {
        }

        @Override
        protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
            int percents = 100 * mCurCachePos / elements.size();
            if (totalBytes == 0)
                return;
            int subPercents = (int)(100 * soFarBytes / totalBytes / elements.size());
            if (listener != null) {
                listener.onCacheAvailable(cache.file, source.getUrl(), percents + subPercents);
            }
        }

        @Override
        protected void blockComplete(BaseDownloadTask task) {
        }

        @Override
        protected void retry(final BaseDownloadTask task, final Throwable ex, final int retryingTimes, final int soFarBytes) {

        }

        @Override
        protected void completed(BaseDownloadTask task) {
            if (listener != null) {
                listener.onM3U8ItemDecrypt(new CacheItem(task.getTargetFilePath(), mDecryptInfo));
            }
        }

        @Override
        protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
            if (soFarBytes < totalBytes){
                File file = new File(task.getTargetFilePath());
                file.delete();
            }

        }

        @Override
        protected void error(BaseDownloadTask task, Throwable e) {
            File file = new File(task.getTargetFilePath());
            file.delete();
        }

        @Override
        protected void warn(BaseDownloadTask task) {

        }
    };


    private String getRelativeName(String url) {
        int p = url.lastIndexOf('/');
        if (p > 0)
            return url.substring(p + 1);
        else
            return url;
    }

    private String getRelativeKey(String line) {
        int start = line.indexOf("http");
        int end = line.lastIndexOf('/');
        return line.substring(0, start) + line.substring(end + 1);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        FileDownloader.getImpl().pause(queueTarget);
    }

    private String fetchFileName(String uri){
        uri = ProxyCacheUtils.decode(uri);
        //如果uri是m3u8
        String name = getRelativeName(uri);
        if (uri.equals(source.getUrl()) || ProxyCacheUtils.isM3U8(uri)){
            //取文件名，去掉所有参数
            return name.replaceAll("\\?.*", "");
        }else{
            if (m3U8ItemMap.containsKey(name)){
                //如果是列表内的文件，取列表次序 + 文件名， 去掉所有参数
                M3U8Item item = m3U8ItemMap.get(name);
                int index = elements.indexOf(item.element);
                return "ts_"+ index + "_" + name.replaceAll("\\?.*", "");
            }
            return name.replaceAll("\\?.*", "");
        }
    }


    //只能在sheduleCache中使用，使用content-length
    private boolean isTsFullCached(String url){
        File file = new File(baseCachePath + File.separator + fetchFileName(url));
        if (!file.exists())
            return false;
        HttpUrlSource source = new HttpUrlSource(url, this.source.headerInjector);
        long contentLength = source.getContentLengthAlone();
        long fileLength = file.length();
        if (fileLength >= contentLength)
            return true;
            //如果请求异常，搁置，解码器丢帧
        else if (contentLength < 0)
            return true;
        return false;
    }

    private void scheduleCache() {
        // 从当前正在请求的位置mCurRequestPose往后偏移若干个ts开始下载， 偏移量由mCacheShift控制
        int start = mCurRequestPos + mCacheShift;
        int end = start + M3U8_CACHE_STEP;
        if (end >= elements.size()) {
            end = elements.size();
        }
        FileDownloadQueueSet queueSet = new FileDownloadQueueSet(queueTarget);
        List<BaseDownloadTask> tasks = new ArrayList<>();
        for(int pos = start; pos < end; pos++) {
            Element element = elements.get(pos);
            String url = element.getURI().toString();
            if (!url.startsWith("http"))
                url = baseUrl + url;
            File file = new File(baseCachePath + File.separator + fetchFileName(url));
            if (!file.exists()) {
                BaseDownloadTask downloadTask = FileDownloader.getImpl().create(url);
                Map<String, String> extraHeaders = source.headerInjector.addHeaders(url);
                for (Map.Entry<String, String> header : extraHeaders.entrySet()) {
                    downloadTask.addHeader(header.getKey(), header.getValue());
                }
                downloadTask.setWifiRequired(false)
                        .setPath(file.getAbsolutePath())
                        .setSyncCallback(true)
                        .setCallbackProgressTimes(1000)
                        .setTag(pos);
                tasks.add(downloadTask);
                mCurCachePos = pos + 1;
            }
            //如果url已经完全缓存且在最后面则更新进度
            else if (pos > mCurCachePos) {
                int percents = 100 * mCurCachePos / elements.size();
                listener.onCacheAvailable(file.getAbsoluteFile(), url, percents);
                mCurCachePos = pos + 1;
            }

        }
        queueSet.setAutoRetryTimes(5);
        if (tasks.size() > 0) {
            queueSet.downloadSequentially(tasks);
            queueSet.start();
        }

    }

    private void tryTransformList(File raw) {
        try {
            File ft = new File(raw.getAbsolutePath() + ".t");
            InputStreamReader inputreader = new InputStreamReader(new FileInputStream(raw));
            BufferedReader buffreader = new BufferedReader(inputreader);
            FileOutputStream fos = new FileOutputStream(ft);

            String line;
            //分行读取
            while (( line = buffreader.readLine()) != null) {
                if (line.startsWith("http")) {
                    line = getRelativeName(line);
                }

                line = line + "\n";

                fos.write(line.getBytes());
            }

            fos.flush();
            ft.renameTo(raw);

            cache.reload();

            File fo = new File(raw.getAbsolutePath() + ".o");
            fo.createNewFile();

            buffreader.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mTransformed = true;
    }

    @Override
    protected void onCachePercentsAvailableChanged(int percents) {

        if (cache.isCompleted()) {
            try {
                // 此处的cache代表的是m3u8的清单文件，清单文件下载完成后，将所有ts信息解析出来
                FileInputStream fis = new FileInputStream(cache.file);
                elements = Playlist.parse(fis).getElements();
                String name;
                for (Element element : elements) {
                    // 使用相对名称作为key
                    name = getRelativeName(element.getURI().toString());
                    m3U8ItemMap.put(name, new M3U8Item(element));
                }
                setM3u8CacheStep(30, elements.size());

                // 处理清单文件，将其中ts全部改为相对路径
                tryTransformList(cache.file);

                if (TextUtils.isEmpty(mDecryptInfo.key))
                    mCacheShift = 2;
                else
                    mCacheShift = 0;

                // 创建ts文件下载文件夹
                baseCachePath = cache.file.getAbsolutePath() + "s";
                m3u8CacheDir = new File(baseCachePath);
                m3u8CacheDir.mkdirs();

                // 调度下载第一批文件
                scheduleCache();


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected boolean isUseCache(GetRequest request) throws ProxyCacheException {
        String uri = ProxyCacheUtils.decode(request.uri);
        if (ProxyCacheUtils.isM3U8(uri)) {
            return super.isUseCache(request);
        }

        return isTsDownloaded(request);
    }

    private boolean isTsDownloaded(GetRequest request) {
        if (m3u8CacheDir == null || !m3u8CacheDir.exists())
            return false;

        File ele = new File(m3u8CacheDir.getAbsolutePath() + File.separator + fetchFileName(request.uri));
        return ele.exists();
    }

    private long getTsFileLength(GetRequest request) {
        File ele = new File(m3u8CacheDir.getAbsolutePath() + File.separator + fetchFileName(request.uri));
        if (ele.exists()) {
            return ele.length();
        } else {
            return -1;
        }
    }

    private void responseWithoutCache(OutputStream out, GetRequest request, HttpUrlSource newSourceNoCache) throws ProxyCacheException, IOException {
        try {
            // 在返回从服务器直取文件数据时，同时将数据保存在本地
            File local = new File(m3u8CacheDir.getAbsolutePath() + File.separator + fetchFileName(request.uri) + ".caching");
            if (local.exists()) {
                local.delete();
            }
            local.createNewFile();

            FileOutputStream fos = new FileOutputStream(local);
            long offset = request.rangeOffset;

            newSourceNoCache.open(offset);
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int readBytes;
            while ((readBytes = newSourceNoCache.read(buffer)) != -1) {
                out.write(buffer, 0, readBytes);
                fos.write(buffer, 0, readBytes);
                offset += readBytes;
            }
            out.flush();
            fos.flush();

            // 数据取完后，重命名为正式文件
            local.renameTo(new File(m3u8CacheDir.getAbsolutePath() + File.separator + fetchFileName(request.uri)));

        }  catch (Exception e) {
            e.printStackTrace();
        } finally {
            newSourceNoCache.close();
        }
    }

    private boolean waitFileExists(File dst, int tick) {
        while (tick-- > 0) {
            if (dst.exists())
                return true;

            try {
                Log.i("vcache", "wait " + dst.getName() + " for " + tick);
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    protected void responseWithEncryptedCache(OutputStream out, GetRequest request) throws IOException {
        String path = m3u8CacheDir.getAbsolutePath() + File.separator + fetchFileName(request.uri);
        File origin = new File(path);
        File decrypted = new File(path + ".d");

        waitFileExists(decrypted, 60);

        decrypted.renameTo(origin);

        responseWithCache(out, request);
    }

    protected void responseWithCache(OutputStream out, GetRequest request) throws IOException {
        FileInputStream fis = new FileInputStream(m3u8CacheDir.getAbsolutePath() + File.separator + fetchFileName(request.uri));

        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int readBytes;
        while ((readBytes = fis.read(buffer, 0, buffer.length)) != -1) {
            out.write(buffer, 0, readBytes);
        }
        out.flush();
    }

    // 返回ts文件请求的响应头， 与父类同名文件相比， 主要ts文件长度计算方式不一样
    protected String newResponseHeaders(GetRequest request, HttpUrlSource source) throws IOException, ProxyCacheException {
        String mime = source.getMime();
        long length = getTsFileLength(request);
        if (length <= 0) {
            length = source.length();
        }
        boolean lengthKnown = length >= 0;
        long contentLength = request.partial ? length - request.rangeOffset : length;
        boolean addRange = lengthKnown && request.partial;
        return new StringBuilder()
                .append(request.partial ? "HTTP/1.1 206 PARTIAL CONTENT\n" : "HTTP/1.1 200 OK\n")
                .append("Accept-Ranges: bytes\n")
                .append(lengthKnown ? format("Content-Length: %d\n", contentLength) : "")
                .append(addRange ? format("Content-Range: bytes %d-%d/%d\n", request.rangeOffset, length - 1, length) : "")
                .append(TextUtils.isEmpty(mime) ? "" : format("Content-Type: %s\n", mime))
                .append("\n") // headers end
                .toString();
    }
    private void hookTransformList(OutputStream out) {
        //FileOutputStream out_cache = null;
        try {

            mUrls.clear();
            //打开source
            source.open(0);
            //out_cache = new FileOutputStream(cache.file);
            InputStreamReader inputreader = source.getReader();
            BufferedReader buffreader = new BufferedReader(inputreader);
            String br;
            while (( br = buffreader.readLine()) != null) {
                //直接替换链接
                Log.e("line", br);
                //out_cache.write((br + "\n").getBytes(StandardCharsets.UTF_8));
                if (!br.startsWith("http")){
                    br = baseUrl + br;//相对路径加上基地址
                }
                if (!br.startsWith("#")){
                    //如果列表中还有m3u8-list
                    if (br.replaceAll("\\?.*", "").endsWith(".m3u8")){
                        //
                    }
                    //列表中除了m3u8子列表都是ts文件了
                    else{
                        mUrls.add(br);
                        proxy.registerCacheStatusListener(this, br);
                        br = proxy.getProxyUrl(br);

                    }
                }
                out.write(br.getBytes());
            }

            //out_cache.flush();
            buffreader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            source.closeReader();
            try {
                source.close();

            } catch (Exception e) {
                Log.e("sth. went wrong", "source failed to close");
            }
        }
        M3U8_CACHE_STEP = mUrls.size() * 20 /100;
        if (M3U8_CACHE_STEP == 0)
            M3U8_CACHE_STEP = 1;
        mTransformed = true;
    }

    @Override
    public void processRequest(GetRequest request, Socket socket) throws IOException, ProxyCacheException {
        String uri = ProxyCacheUtils.decode(request.uri);
        Log.e("request", uri);
        if (ProxyCacheUtils.isM3U8(uri)) {
            Log.e("proxy-m3u8", "tackle m3u8 list");
            OutputStream out = new BufferedOutputStream(socket.getOutputStream());
            out.write(newResponseHeaders(request).getBytes(StandardCharsets.UTF_8));
            hookTransformList(out);
            int tick = 60;
            while (tick-- > 0) {
                if (mTransformed)
                    break;

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            //这里都是需要解密的m3u8
            HttpUrlSource newSourceNoCache = new HttpUrlSource(this.source, new SourceInfo(uri, Integer.MIN_VALUE, ProxyCacheUtils.getSupposablyMime(uri)));
            String responseHeaders = newResponseHeaders(request, newSourceNoCache);
            OutputStream out = new BufferedOutputStream(socket.getOutputStream());
            out.write(responseHeaders.getBytes("UTF-8"));
            if (!TextUtils.isEmpty(mDecryptInfo.key)) {
                // 需解密，强制缓存并解密
                responseWithEncryptedCache(out, request);
            }
        }
    }


    /*
    覆盖父类的写法
     */

}
