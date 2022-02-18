package com.kai.video.tool.net;


import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsoupHelper {
	public static String getValue(Element e, String type, String key) {
		switch (type) {
			case "attr":
				return e.attr(key);
			case "text":
				return e.text();
			case "pattern_all":
				Pattern pattern1 = Pattern.compile(key);
				Matcher matcher1 = pattern1.matcher(e.outerHtml());
				if (matcher1.find()) {
					return matcher1.group();
				}
			case "pattern":
				Pattern pattern = Pattern.compile(key);
				Matcher matcher = pattern.matcher(e.outerHtml());
				if (matcher.find()) {
					return matcher.group(1);
				}
			default:
				return "";
		}
	}
	public static Element search(Element father,String path) {
		if (path.isEmpty()) {
			return father;
		}
		try {
			String[] paths = path.split("/");
			Element next=father;
			for(String p:paths) {
				//拆分类似与元素
				next = getElement(p, next);
				
			}
			return next;
		} catch (Exception e) {
			// TODO: handle exception
			return null;
		}
		
	}
	public static List<Element> filter(List<Element> former,String path) {
		List<Element> later= new ArrayList<>();
		for (Element f : former) {
			later.add(search(f, path));
		}
		return later;
	}


	
	public static Element getElement(String p,Element next) {
		String[] attributes = p.split(":");
		String type = attributes[0];
		String param = attributes[1];
		switch (type) {
		case "class":
			next = next.getElementsByClass(param).first();
			break;
		case "tag":
			next = next.getElementsByTag(param).first();
			break;
		case "id":
			next = next.getElementById(param);
			break;
		case "name":
			next = next.getElementsByAttributeValue("name", param).first();
			break;
		case "action":
			switch (param) {
				case "next":
					next = next.nextElementSibling();
					break;
				case "pre":
					next = next.previousElementSibling();
					break;
				case "parent":
					next = next.parent();
					break;
			}
			break;
		default:
			break;
		}
		return next;
	}
	
	public static List<Element> getElements(String p,Element next) {
		List<Element> elements= new ArrayList<>();
		String[] attributes = p.split(":");
		String type = attributes[0];
		String param = attributes[1];
		switch (type) {
		case "class":
			elements = next.getElementsByClass(param);
			break;
		case "tag":
			elements = next.getElementsByTag(param);
			break;
			default:
			break;
		}
		return elements;
	}
	
	public static List<Element> searchGroup(Element father,String path) {
		try {
			String[] paths = path.split("/");
			Element next=father;
			for (int i = 0; i < paths.length; i++) {
				String p=paths[i];
				if (i==paths.length-1) {
					return getElements(p, next);
				}
				next = getElement(p, next);
			}
		} catch (Exception e) {
			// TODO: handle exception
			return null;
		}
		return null;
		
	}
}
