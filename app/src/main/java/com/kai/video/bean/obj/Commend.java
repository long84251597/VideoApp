package com.kai.video.bean.obj;

import android.util.Log;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kai.video.bean.item.CommendItem;
import com.kai.video.bean.item.NaviItem;
import com.danikula.videocache.IPTool;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;

public class Commend {
	private String type;
	private final String action;
	public Commend(String type, String action) {
		this.type = type;
		this.action = action;
	}
	public String get() {
		try {
			Connection.Response response = Jsoup.connect(IPTool.getLocal() + "/HotList")
					.data("action", type)
					.data("type", action)
					.data("commend", "true")
					.timeout(10 * 1000)
					.method(Connection.Method.GET)
					.execute();
			String body = response.body();
			return response.body();
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}
	public NaviItem getDocument(){
		NaviItem item = new NaviItem();
		item.setHasHeader(true);
		item.setHeader(type);
		if (type.equals("bilibili") && action.equals("japanese")) {
			Log.e("tag", "japanese");
			item.setHeader("bilibili1");
		}
		JSONArray data = JSONObject.parseObject(get()).getJSONArray("data");
		item.setExtend(data.toJavaList(CommendItem.class));
		return item;
	}
}
