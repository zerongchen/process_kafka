package com.aotain.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;

public class MultifileWriter{
	
	private static MultifileWriter instance;

	private MultifileWriter(){
		Thread thread = new Thread() {
			
			@Override
			public void run() {
				while(true){
					
					Date date = new Date(System.currentTimeMillis());
					//Calendar calendar = Calendar.getInstance();
				    //calendar.setTime(date);
				    //int min = calendar.get(Calendar.MINUTE);
				    
				    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHH");
					String dateString = formatter.format(date);
					int dateHour = Integer.parseInt(dateString);
				    
					flush(dateHour);
					
					try {
						Thread.sleep(30000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		thread.start();
	}
	
	public synchronized static MultifileWriter getInstance() {

		if (instance == null) {
			instance = new MultifileWriter();
		}
		return instance;
	}
	
	// the map's key is path
	private final static ConcurrentHashMap<String, BufferedWriter> writersMap = new ConcurrentHashMap<String, BufferedWriter>();

	public void writeLine(String path, String line) {
		try {
			BufferedWriter bw = writersMap.get(path);
			if (null == bw) {
				
				File file = new File(path);
				bw = new BufferedWriter(new OutputStreamWriter(FileUtils.openOutputStream(file,
						true),"utf-8"));
				writersMap.put(path, bw);
			}
			bw.write(line);
			bw.newLine();
		} catch (Exception e) {
			DamsLog.threadLog.error(e.getMessage());
		} 
	}

	public void flush(int dateHour) {
		try {
			for (Entry<String, BufferedWriter> entry : writersMap.entrySet()) {
				String path = entry.getKey();
				BufferedWriter bw = entry.getValue();
				bw.flush();
				
				String filename = entry.getKey();
				String time = filename.substring(filename.lastIndexOf("_")+1,filename.indexOf("."));
				
				if(dateHour> Integer.parseInt(time)){
					bw.close();	
					writersMap.remove(path);
				}
				
			}
		} catch (Exception e) {
			DamsLog.threadLog.error(e.getMessage(), e);
		}
	}
	
	public void close(String filename) {
		try {
			BufferedWriter bw = writersMap.get(filename);
			if(bw != null) {
				bw.flush();
				bw.close();	
				writersMap.remove(filename);
			}
		} catch (Exception e) {
			DamsLog.threadLog.error(e.getMessage(), e);
		}
	}
	
	public void closeAndRename(String filename,String newFilename) {
		try {
			BufferedWriter bw = writersMap.get(filename);
			if(bw != null) {
				bw.flush();
				bw.close();	
				writersMap.remove(filename);
			}
			File file = new File(filename);
			file.renameTo(new File(newFilename));
			
		} catch (Exception e) {
			DamsLog.threadLog.error(e.getMessage(), e);
		}
	}
	
}
