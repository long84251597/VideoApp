package com.kai.video.bean;

import java.util.HashMap;

public class StartMap<U> {
	HashMap<String, U> map;
	public StartMap(){
		map = new HashMap<>();
	}
	public void put(String t,U u) {
		map.put(t, u);
	}
	public U get(String t) {
		for(String t1:map.keySet()) {
			if (t.contains(t1)) {
				return map.get(t1);
			}
		}
		return null;
	}
	public String replace(String t) {
		for(String t1:map.keySet()) {
			if (t.contains(t1)) {
				return t.replace(t1, String.valueOf(map.get(t1)));
			}
		}
		return t;
	}
	public boolean exist(String t) {
		for(String t1:map.keySet()) {
			if (t.startsWith(t1) || t.startsWith(String.valueOf(map.get(t1)))) {
				return true;
			}
		}
		return false;
	}
}
