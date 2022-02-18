package com.kai.sniffwebkit.ad;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;

public class AdBlocker {
    private static boolean prepared = false;
    private static final List<String> lines = new ArrayList<>();
    public static void init(){
        new Thread(AdBlocker::transListFromWeb).start();
    }


    public static boolean isAd(String url){
        if (!prepared)
            return false;
        int length = lines.size();
        for(int i = 0; i < length -2; i+=3){
            String line = lines.get(i);
            String line1 = lines.get(i+1);
            String line2 = lines.get(i+2);
            if (url.contains(line) || url.contains(line1) || url.contains(line2))
                return true;
        }
        return false;
    }

    private static void transListFromWeb(){
        try {

            Connection.Response response = Jsoup.connect("https://anti-ad.net/surge2.txt")
                    .ignoreContentType(true)
                    .method(Connection.Method.GET)
                    .execute();
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.bodyStream()));
            String line;
            while ((line = reader.readLine())!= null){
                if (line.startsWith("."))
                    lines.add(line.trim());
            }
            prepared = true;
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
