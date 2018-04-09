package com.aotain.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.Clob;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tools {
	/**
	 * 判断是否是IP地址
	 * 
	 * @param s
	 * @return
	 */
	public static boolean isIpAddress(String s) {
		if (s == null)
			return false;
		String regex = "(((2[0-4]\\d)|(25[0-5]))|(1\\d{2})|([1-9]\\d)|(\\d))[.](((2[0-4]\\d)|(25[0-5]))|(1\\d{2})|([1-9]\\d)|(\\d))[.](((2[0-4]\\d)|(25[0-5]))|(1\\d{2})|([1-9]\\d)|(\\d))[.](((2[0-4]\\d)|(25[0-5]))|(1\\d{2})|([1-9]\\d)|(\\d))";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(s);
		return m.matches();
	}

	/**
	 * 获取ICP注册域名
	 * 
	 * @param domain
	 * @return
	 */
	public static String getICPDomain(String domain) {
		String root = "";
		String[] temp = domain.replace(".com.cn", ".com_cn").replace(".net.cn",
				".net_cn").replace(".org.cn", ".org_cn").replace(".gov.cn",
				".gov_cn").replace(".vnet.cn", ".vnet_cn").replace(".edu.cn",
				".edu_cn").replace(".", "/").split("/");
		if (temp.length > 2) {
			// root = domain.substring(domain.indexOf(".")+1);
			root = temp[temp.length - 2] + "." + temp[temp.length - 1];
			root = root.replace("_", ".");
		} else
			root = domain;

		return root;
	}

	/**
	 * ip地址转成整数.
	 * 
	 * @param ip
	 * @return
	 */
	public static long ip2long(String ip) {
		if (!isIpAddress(ip))
			return -1;

		String[] ips = ip.split("[.]");
		long num = 16777216L * Long.parseLong(ips[0]) + 65536L
				* Long.parseLong(ips[1]) + 256 * Long.parseLong(ips[2])
				+ Long.parseLong(ips[3]);

		return num;
	}

	/**
	 * 整数转成ip地址.
	 * 
	 * @param ipLong
	 * @return
	 */
	public static String long2ip(long ipLong) {
		long mask[] = { 0x000000FF, 0x0000FF00, 0x00FF0000, 0xFF000000 };
		long num = 0;
		StringBuffer ipInfo = new StringBuffer();
		for (int i = 0; i < 4; i++) {
			num = (ipLong & mask[i]) >> (i * 8);
			if (i > 0)
				ipInfo.insert(0, ".");
			ipInfo.insert(0, Long.toString(num, 10));
		}
		return ipInfo.toString();
	}

	// oracle.sql.Clob类型转换成String类型
	public static String ClobToString(Clob clob) throws SQLException,
			IOException {
		String reString = "";
		Reader is = clob.getCharacterStream();// 得到流
		BufferedReader br = new BufferedReader(is);
		String s = br.readLine();
		StringBuffer sb = new StringBuffer();
		// 执行循环将字符串全部取出付值给StringBuffer由StringBuffer转成String
		while (s != null) {
			sb.append(s);
			s = br.readLine();
		}
		reString = sb.toString();
		return reString;
	}

	public static boolean isNumber(String num) {
		return (num!=null && num.matches("[0-9]+"));
	}
	
	public static boolean isInt(String num){
		try{
			int m=Integer.parseInt(num);
		}
		catch(Exception ex){
			return false;
		}
		return true;
	}
	
	public static long getBatchId(){
		String mBatchId="";
		Calendar cal=Calendar.getInstance();
		mBatchId=""+cal.get(Calendar.YEAR);
		mBatchId+=format2Two(cal.get(Calendar.MONTH)+1+"");
		mBatchId+=format2Two(cal.get(Calendar.DAY_OF_MONTH)+"");
		mBatchId+=format2Two(cal.get(Calendar.HOUR_OF_DAY)+"");
		mBatchId+=format2Two(cal.get(Calendar.MINUTE)+"");
		mBatchId+=format2Two(cal.get(Calendar.SECOND)+"");
		//mBatchId+=format2Two(cal.get(Calendar.MILLISECOND)+"");
		return Long.parseLong(mBatchId);
	}
	
	public static String format2Two(String num){
		if(num.length()<2){
			return "0"+num;
		}
		return num;
	}
	
	public static String UrlDecode(String str){
		
		String result = "";
		try {
			if(str.toLowerCase().indexOf("%u")>=0) 
				result = unescape(str);
			else{
				result = URLDecoder.decode(str, "UTF-8");		
				String encode = URLEncoder.encode(result, "UTF-8");
		    
			    if (!str.toLowerCase().equals(encode.toLowerCase()))
		            result = URLDecoder.decode(str, "gb2312");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			DamsLog.threadLog.error("decode error, str="+str, e);
			result = str;
		}
		
        return result;
	}
	
	public static String unescape(String src) {
		
		StringBuffer tmp = new StringBuffer();
		tmp.ensureCapacity(src.length());
		int lastPos = 0, pos = 0;
		char ch;
		while (lastPos < src.length()) {
			pos = src.indexOf("%", lastPos);
			if (pos == lastPos) {
				if (src.charAt(pos + 1) == 'u') {
					ch = (char) Integer.parseInt(src.substring(pos + 2, pos + 6), 16);
					tmp.append(ch);
					lastPos = pos + 6;
				} else {
					ch = (char) Integer.parseInt(src.substring(pos + 1, pos + 3), 16);
					tmp.append(ch);
					lastPos = pos + 3;
				}
			} else {
				if (pos == -1) {
					tmp.append(src.substring(lastPos));
					lastPos = src.length();
				} else {
					tmp.append(src.substring(lastPos, pos));
					lastPos = pos;
				}
			}
	   }
	   return tmp.toString();
	}
	
	public static String getDataString(String time){
		Date date = new Date(LongUtil.valueOf(time)*1000l);
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(date);
	}
	
	public static String getDateString(long timeStamp){
		Date date = new Date(timeStamp);
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(date);
	}
	
	public static double getPercent(long fenzi,long fenmu, int scale){
		if(fenmu == 0)return 0;
		double value = fenzi/(fenmu*1.0);
		value = value *100;
    	BigDecimal b = new BigDecimal(String.valueOf(value));  
        BigDecimal divisor = BigDecimal.ONE; 
        MathContext mc = new MathContext(scale);  
        return b.divide(divisor, mc).doubleValue();
	}
	
	
}
