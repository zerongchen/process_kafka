package com.aotain.util;


public class Random {
	
	public static String getRandomString(){
				
		char c[] = new char[62];
		for (int i = 97, j = 0; i < 123; i++, j++) {
			c[j] = (char) i;
		}
		for (int o = 65, p = 26; o < 91; o++, p++) {
			c[p] = (char) o;
		}
		for (int m = 48, n = 52; m < 58; m++, n++) {
			c[n] = (char) m;
		}
		// 取随机产生的认证码(4位数字)
		String randomString = "";
		java.util.Random r = new java.util.Random();		
		for (int i = 0; i < 20; i++) {
			int x = r.nextInt(62);
			randomString += String.valueOf(c[x]);
		}
		return randomString;
	}

}
