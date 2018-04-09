package com.aotain.util;


public class LongUtil {
	public static Long valueOf(String s){
		if(s == null || "".equals(s))return 0L;
		return Long.valueOf(s);
	}
}
