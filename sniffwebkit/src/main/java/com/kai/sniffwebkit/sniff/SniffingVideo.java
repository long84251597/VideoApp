package com.kai.sniffwebkit.sniff;

import android.os.Parcel;
import android.os.Parcelable;



import org.jsoup.Connection;

import java.util.HashMap;
import java.util.Map;

public class SniffingVideo implements Parcelable  {
    public static SniffingVideo createNullSniffVideo(){
        return new SniffingVideo(null, null, -1, "filtered");
    }
    public boolean isNull(){
        return url == null;
    }

    public boolean hasResponse(){
        return response != null;
    }

    public boolean isVideo(){
        return type != null && !type.equals("filtered");
    }

    public static SniffingVideo createNoneVideoSniffVideo(String url, String charset, String contentType, Connection.Response response){
        SniffingVideo video = new SniffingVideo(url, contentType, 0, "filtered");
        video.setResponse(response);
        return video;
    }


    public void setCharset(String charset) {
        this.charset = charset;
    }

    public Connection.Response getResponse() {
        return response;
    }

    public void setResponse(Connection.Response response) {
        this.response = response;
    }

    public String getCharset() {
        return charset;
    }

    private String charset = "utf-8";
    private Connection.Response response;
    private Map<String, String> headers = new HashMap<>(0);//附带的请求头，一定要记得加上以防无法连接

    private final String contentType;//content类型

    private final String type; // 文件类型，不一定能获取成功

    private String url; //视频连接

    private int length = 0;//文件长度，不一定能获取成功
    private boolean isSuffix = true;//.xxx 后缀是否在最后
    private boolean isRedirect = false;// 是否为重定向的url  如 http://xxx.xxx.xxx?url=http://xx.xx.xx


    public SniffingVideo(String url, String contentType) {
        this(url, contentType, -1, "");
    }

    public SniffingVideo(String url, String contentType, int length, String type) {
        this.url = url;
        this.length = length;
        this.type = type;
        this.contentType = contentType;
        this.isRedirect = url != null && url.contains("=") && url.lastIndexOf("http") != 0;
        this.isSuffix = url != null && url.lastIndexOf(contentType) == url.length() - contentType.length();
    }

    public void addHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    public Map<String, String> getHeaders() {
        return headers;
    }

    protected SniffingVideo(Parcel in) {
        contentType = in.readString();
        type = in.readString();
        url = in.readString();
        length = in.readInt();
        isSuffix = in.readByte() != 0;
        isRedirect = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(contentType);
        dest.writeString(type);
        dest.writeString(url);
        dest.writeInt(length);
        dest.writeByte((byte) (isSuffix ? 1 : 0));
        dest.writeByte((byte) (isRedirect ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SniffingVideo> CREATOR = new Creator<SniffingVideo>() {

        @Override
        public SniffingVideo createFromParcel(Parcel in) {
            return new SniffingVideo(in);
        }

        @Override
        public SniffingVideo[] newArray(int size) {
            return new SniffingVideo[size];
        }

    };

    public boolean isHtml() {
        return type != null && type.contains("text/html");
    }

    public String getType() {
        return type;
    }

    public String getUrl() {
        if (url == null) {
            return "";
        }
        return url;
    }

    public long getLength() {
        return length;
    }

    public boolean isSuffix() {
        return isSuffix;
    }

    public boolean isRedirect() {
        return isRedirect;
    }

    public String getContentType() {
        return contentType;
    }


    @Override
    public String toString() {
        return "SniffingVideo{" +
                "contentType='" + contentType + '\'' +
                ", type='" + type + '\'' +
                ", url='" + url + '\'' +
                ", length=" + length +
                ", isSuffix=" + isSuffix +
                ", isRedirect=" + isRedirect +
                '}';
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void addCookie(String cookie){
        HashMap<String, String> hashMap = new HashMap<>(headers.size() + 1);
        hashMap.putAll(headers);
        hashMap.put("Cookie", cookie);
    }
}
