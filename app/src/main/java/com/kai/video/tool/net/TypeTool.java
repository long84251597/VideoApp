package com.kai.video.tool.net;

import android.content.Context;
import android.os.Handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TypeTool {
    private final SearchTool tool;
    public static String[] main_types = new String[] {"电影", "电视剧" , "动漫", "综艺", "纪录片"};
    public static String[] main_typesEN = new String[]{"film", "teleplay", "cartoon","tvshow", "documentary"};
    public static String[] content_types = new String[] {"全部", "剧情","动作","喜剧","冒险","科幻","动画","奇幻","青春","警匪", "爱情", "温情","成长","悬疑","亲情","惊悚","文艺","战争", "魔幻","恐怖", "经典"};
    public static String[] countrys = new String[] {"全部","内地","美国","香港","日本","加拿大","英国","泰国","韩国","俄罗斯","澳大利亚","印度","意大利", "台湾","葡萄牙","欧美","法国"};
    public static String[] years = new String[22];
    public static String[] sorts = new String[]{"最近热播","最新上架","评分最高"};
    public static String[] sortsEN = new String[]{"", "time", "score"};
    private int i1 = 0;
    private int i2 = 0;
    private int i3 = 0;
    private int i4 = 0;
    private int i5 = 0;
    public String[] getTypeByIndex(int index){
        switch (index){
            case 0:return main_types;
            case 1:return content_types;
            case 2:return countrys;
            case 3:return years;
            case 4:return sorts;
        }
        return null;
    }
    public void setTypeByIndex(int index, int i){
        switch (index){
            case 0:selectMain(i);break;
            case 1:selectContent(i);break;
            case 2:selectCountry(i);break;
            case 3:selectYear(i);break;
            case 4:selectSort(i);break;
        }
    }
    public void selectSort(int i){
        i5 = i;
        fetch(false);
    }

    public void selectMain(int i){
        i1 = i;
        i2 = 0;
        i3 = 0;
        i4 = 0;
        i5 = 0;
        fetch(true);
    }
    public void selectContent(int i){
        i2 = i;
        fetch(false);
    }
    public void selectCountry(int i){
        i3 = i;
        fetch(false);
    }
    public void selectYear(int i){
        i4 = i;
        fetch(false);
    }
    static {
        Calendar cal = Calendar.getInstance();
        int y = cal.get(Calendar.YEAR);
        years[0] = "全部";
        for (int i = 0; i < years.length-1; i++){
            years[i+1] = String.valueOf(y-i);
        }
    }


    private int pageOffset = 1;
    private OnFetchListner onFetchListner;

    public void setOnFetchListner(OnFetchListner onFetchListner) {
        this.onFetchListner = onFetchListner;
    }

    public static TypeTool getInstance(Context context){
        return new TypeTool(context);
    }
    private TypeTool(Context context){
        tool = SearchTool.getInstance(context, "");
    }

    public void resetPageOffset() {
        this.pageOffset =1;
    }

    public void fetch(boolean reload){
        new Thread(() -> {
            try {
                List<SearchTool.SearchJtem> SearchJtems = new ArrayList<>();

                Connection.Response response = Jsoup.connect("https://v.sogou.com/api/video/result")
                        .data("order", sortsEN[i5])
                        .data("req", "class")
                        .data("query", main_types[i1])
                        .data("page", pageOffset+"")
                        .data("pagesize", "30")
                        .data("entity", main_typesEN[i1])
                        .data("style", i2==0?"":content_types[i2])
                        .data("zone", i3==0?"":countrys[i3])
                        .data("year", (main_types[i1].equals("综艺") || i4 == 0)?"":years[i4])
                        .method(Connection.Method.GET)
                        .ignoreContentType(true)
                        .execute();
                JSONObject object = JSONObject.parseObject(response.body()).getJSONObject("longVideo");
                JSONArray array = object.getJSONArray("results");
                if (reload){
                    for(Object o:object.getJSONObject("list").getJSONArray("filter_list")){
                        JSONObject obj = (JSONObject)o;
                        switch (obj.getString("name")){
                            case "类型":TypeTool.content_types = (String[]) obj.getJSONArray("option_list").toArray(new Object[0]);break;
                            case "地区":TypeTool.countrys = (String[]) obj.getJSONArray("option_list").toArray(new Object[0]);break;
                            default:break;
                        }
                    }
                }
                for(int i = 0; i < array.size(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    SearchTool.SearchJtem item = tool.createJtem();
                    item.setTitle(obj.getString("name"));
                    item.setHref("https://v.sogou.com"+obj.getString("tiny_url"));
                    item.setRate(obj.getString("docname") + " " +  obj.getString("score"));
                    item.setImg(obj.getString("v_picurl"));
                    item.setYear(obj.getString("year"));
                    SearchJtems.add(item);
                }

                handler.post(() -> {
                    onFetchListner.onFetched(pageOffset, SearchJtems, SearchJtems.size()==30, reload);
                    if (SearchJtems.size() > 0){
                        pageOffset++;
                    }
                });


            }catch (Exception e){
                e.printStackTrace();
                handler.post(() -> onFetchListner.onFetchFail(pageOffset));

            }

        }).start();
    }
    private final Handler handler = new Handler();

    public interface OnFetchListner{
        void onFetched(int page, List<SearchTool.SearchJtem> items, boolean more, boolean reload);
        void onFetchFail(int page);
    }
    public interface OnConnectListner{
        void onConnected(String href);
        void onConnected(List<String> names, List<String> hrefs);
        void onDisConnected();
    }

}
