package com.aotain.cmcc.task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import kafka.serializer.StringDecoder;
import kafka.utils.VerifiableProperties;

import com.aotain.util.CmccConfig;
import com.aotain.util.DBUtil;
import com.aotain.util.DamsLog;
import com.aotain.util.DateUtil;
import com.aotain.util.MultifileWriter;
import com.sun.xml.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

import org.apache.commons.lang.StringUtils;

public class OracleTask extends Thread {
		
	MultifileWriter mfw = MultifileWriter.getInstance();
	
	private ExecutorService threadPool;
	private ConsumerConnector consumer;
	protected transient Object writeLock;
	private Map<String, List<String[]>> dataCacheMap = null;
	private Map<String, Integer> countCacheMap = null;
	private String cacheFileDir;
	private String auditFileDir;

	protected int syncCount = 1000;
	protected static int syncTimeInterval = 300;
	private String sql;
	private String topic;
	private int threadNum = 1;
	private int partdateFieldIndex;
	private int[] dateFieldIndex;
	private ConsumerConfig config;
	
	public OracleTask(ConsumerConfig config,String topic,String sql,int syncTimeInterval,
			int syncCount,String dateFieldIndexs,int partdateFieldIndex) {
		this.config = config;

		this.sql = sql.replace("?", "%s");
		this.syncTimeInterval = syncTimeInterval * 1000;
		this.syncCount = syncCount;
		this.topic = topic;
		String [] idx = dateFieldIndexs.split(",");
		int len = idx.length;
		int[] arr = new int[len];
		for(int i=0;i<len;i++){
			arr[i] = Integer.parseInt(idx[i]);
		}
		this.dateFieldIndex = arr;
		
		this.partdateFieldIndex = partdateFieldIndex;
		
		this.dataCacheMap = new ConcurrentHashMap<String, List<String[]>>();
		this.countCacheMap = new ConcurrentHashMap<String, Integer>();
		this.writeLock = new Object();
		
		this.cacheFileDir = CmccConfig.getInstance().getWorkPath() + "/cache/" + topic + "/";
		this.auditFileDir = CmccConfig.getInstance().getWorkPath() + "/logs/audit/";
		
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
		        public void run() {
		            //System.out.println("OracleTask TimeSync.");
		            saveData("",new String[]{"TimeSync"});
		        }
		}, this.syncTimeInterval, this.syncTimeInterval);
		
	}
	
	
	@Override
	public void run(){
		//处理未入库的缓存数据
		
		File dir = new File(cacheFileDir);
		if(!dir.exists()) dir.mkdirs();
		
		for(File subDir : dir.listFiles()){
			if(!subDir.isDirectory()) continue;
			for(File file : subDir.listFiles()){
				if(file.getName().endsWith(".fin") || file.getName().endsWith(".cache")){
					List<String[]> list = readFileByLines(file);
					String statDateHour = file.getName().substring(file.getName().lastIndexOf("_") + 1,file.getName().lastIndexOf("."));
					importDB(file.getPath(),statDateHour,list);
				}
			}
		}
		//处理kafka中数据
		consumer = Consumer.createJavaConsumerConnector(config);
		consumer.commitOffsets();
		Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
        topicCountMap.put(topic, new Integer(threadNum));

        StringDecoder keyDecoder = new StringDecoder(new VerifiableProperties());
        StringDecoder valueDecoder = new StringDecoder(new VerifiableProperties());

        Map<String, List<KafkaStream<String, String>>> consumerMap = 
                consumer.createMessageStreams(topicCountMap,keyDecoder,valueDecoder);
        if(threadNum > 1){
	        List<KafkaStream<String, String>> streamList = consumerMap.get(topic); 
	        threadPool = Executors.newFixedThreadPool(threadNum);  
	        
	        for (KafkaStream<String, String> stream : streamList) {  
	            threadPool.execute(new MessageFetcher(stream));  
	        } 
        }
        else {
	        KafkaStream<String, String> stream = consumerMap.get(topic).get(0);
	        ConsumerIterator<String, String> it = stream.iterator();
	        while (it.hasNext()){
	        	dealMessage(it);
	        }
        }
	}

	public void close() {  
        try {  
            if(threadPool != null )
            	threadPool.shutdownNow();  
        } 
        finally {  
        	consumer.shutdown();  
        }  
    }  
	
	class MessageFetcher implements Runnable {  
        private KafkaStream<String, String> stream;  
  
        MessageFetcher(KafkaStream<String, String> stream) {  
            this.stream = stream;  
        }  
  
        public void run() {  
        	ConsumerIterator<String, String> it = stream.iterator();
        	while (it.hasNext()){
        		dealMessage(it);
            }
        }  
    }  
	
	private void dealMessage(ConsumerIterator<String, String> it) {
		MessageAndMetadata<String, String> next = it.next();
		String line = next.message();
		long offset = next.offset();
		if(line == null || line.length()<1) return;
		
		String[] arr = line.split("\\|");
		long ts = Long.parseLong(arr[partdateFieldIndex]+"000");
		for(int idx :dateFieldIndex){
			long timestamp = Long.parseLong(arr[idx]+"000");
			arr[idx] = DateUtil.getDateTime(timestamp);
		}
//		StringBuffer sb = new StringBuffer();
		for(int i = 0;i < arr.length;i++){
//			sb.append("\"").append(arr[i]).append("\",");
			if(StringUtils.isEmpty(arr[i])){
				arr[i] = "0";
			}

		}
//		String columns = sb.toString();

		String statDateHour = DateUtil.getHourStr(ts);
		
		//缓存文件
		String cacheFilePath = this.cacheFileDir + (statDateHour.substring(0,8)) + "/";
		File dir = new File(cacheFilePath);
		if(!dir.exists()) dir.mkdirs();
		mfw.writeLine(cacheFilePath + topic + "_" + statDateHour + ".cache", line);
		
		saveData(statDateHour,arr);
	}

	
	protected void saveData(String statDateHour,String[] line) {

		boolean forceSync = false;
		String syncStatDay = "";
		if (line[0].equals("TimeSync")) {
			forceSync = true;
		} else {
			if(statDateHour== null || statDateHour.length() == 0) return;
			String statDay = statDateHour;
			if (dataCacheMap.containsKey(statDay)) {
				dataCacheMap.get(statDay).add(line);
			} else {
				List<String[]> list = new ArrayList<String[]>();
				list.add(line);
				dataCacheMap.put(statDay, list);
			}
			// count
			if (countCacheMap.containsKey(statDay)) {
				Integer cnt = countCacheMap.get(statDay);
				cnt++;
				if (cnt < syncCount) {
					countCacheMap.put(statDay, cnt);
				} else {
					countCacheMap.remove(statDay);
					forceSync = true;
					syncStatDay = statDay;
				}
			} else {
				countCacheMap.put(statDay, 1);
			}
		}
		// write data
		if (forceSync) {
			if (syncStatDay.length() != 0) {
				String cacheFilePath = this.cacheFileDir + (syncStatDay.substring(0,8)) + "/"+ topic + "_" + syncStatDay + ".cache";
				String finishFilePath = this.cacheFileDir + (syncStatDay.substring(0,8)) + "/"+ topic + "_" + syncStatDay + ".fin";
				mfw.closeAndRename(cacheFilePath,finishFilePath);
				
				// output oracle
				List<String[]> list = dataCacheMap.get(syncStatDay);
				System.out.println("output oracle");
				importDB(finishFilePath,syncStatDay,list);

				dataCacheMap.remove(syncStatDay);
			} else {
				for (String statDay : dataCacheMap.keySet()) {
					System.out.println(statDay);
					String cacheFilePath = this.cacheFileDir + (statDay.substring(0,8)) + "/"+ topic + "_" + statDay + ".cache";
					String finishFilePath = this.cacheFileDir + (statDay.substring(0,8)) + "/"+ topic + "_" + statDay + ".fin";
					mfw.closeAndRename(cacheFilePath,finishFilePath);
					
					// output oracle
					List<String[]> list = dataCacheMap.get(statDay);
					System.out.println("output oracle");
					importDB(finishFilePath,statDay,list);

					dataCacheMap.remove(statDay);
					countCacheMap.remove(statDay);
				}
			}
		}
	}
	
	
	/**
	 * 批量插入流量统计数据
	 * 
	 * @param dataList
	 * @throws Exception
	 */
	private void importDB(String cacheFilename,String statDateHour,List<String[]> dataList){
		if(dataList == null || dataList.size()<=0) return;
		int dataSize = dataList.size();
		
		Connection conn = null;
		Statement stmt = null;
		try {
			conn = DBUtil.getConnection();
			conn.setAutoCommit(false);
			stmt = conn.createStatement();  
			int count = 0;
			for (String[] data : dataList) {
				String e_sql = "";
				try{
					e_sql = String.format(sql, data);
					System.out.print(e_sql);
				}
				catch(Exception e){
					e_sql = "";
					DamsLog.dbLog.error("format sql error: " + sql + " ,data=" + Arrays.toString(data) ,e);
				}
				if(e_sql.length()>0){
					stmt.addBatch(e_sql);
				}
				count++;
				if (count % 1000 == 0) {
					stmt.executeBatch();
					stmt.clearBatch();
					count = 0;
				}
			}
			// 执行批量更新
			stmt.executeBatch();
			// 语句执行完毕，提交本事务
			conn.commit();
			conn.setAutoCommit(true);
			//删除缓存文件
			File cacheFile = new File(cacheFilename);
			if(cacheFile.exists()){
				cacheFile.delete();
			}
				
			//写统计日志
			String audit = DateUtil.getDateTime(System.currentTimeMillis()) + "|" + dataSize;
			String auditFilePath = this.auditFileDir + (statDateHour.substring(0,8)) + "/";
			File dir = new File(auditFilePath);
			if(!dir.exists()) dir.mkdirs();
			mfw.writeLine(auditFilePath + topic + "_audit_" + statDateHour + ".log", audit);
			
		} catch (Exception ex) {
			DamsLog.dbLog.error("batch insert table " + sql + " error",ex);
		} finally {
			DBUtil.closeConnection(conn, stmt);
			
			dataList.clear();
			dataList = null;
		}
		
	}

	
	public void writeSyncTimeSign(){
		saveData("",new String[]{"TimeSync"});
	}
	
	private List<String[]> readFileByLines(File file) {  
		List<String[]> list = new ArrayList<String[]>();
        BufferedReader reader = null;  
        try {  
            reader = new BufferedReader(new FileReader(file));
            String line = null;  
            while ((line = reader.readLine()) != null) {  
            	String[] arr = line.split("\\|");
            	if(arr.length-1<dateFieldIndex[dateFieldIndex.length-1]) continue;
        		for(int idx :dateFieldIndex){
        			long timestamp = Long.parseLong(arr[idx] + "000");
        			arr[idx] = DateUtil.getDateTime(timestamp);
        		}
        		//String columns = Arrays.toString(arr);
        		//columns = columns.substring(1,columns.length()-1);
        		list.add(arr);
            }  
            reader.close();  
        } catch (IOException e) {  
            e.printStackTrace();  
        } finally {  
            if (reader != null) {  
                try {  
                    reader.close();  
                } catch (IOException e1) {  
                }  
            }  
        } 
        return list;
    }

	private boolean isNumeric(String str){
		for (int i = str.length();--i>=0;){
			if (!Character.isDigit(str.charAt(i))){
				return false;
			}
		}
		return true;
	}
	
}
