package com.aotain.cmcc.task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
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

import com.aotain.cmcc.task.OracleTask.MessageFetcher;
import com.aotain.util.CmccConfig;
import com.aotain.util.DateUtil;
import com.aotain.util.MultifileWriter;

public class HdfsTask extends Thread {
		
	MultifileWriter mfw = MultifileWriter.getInstance();
	
	private ExecutorService threadPool;
	private ConsumerConnector consumer;
	
	protected transient Object writeLock;
	private Map<String, List<String>> dataCacheMap = null;
	private Map<String, Integer> countCacheMap = null;
	private String cacheFileDir;
	private String auditFileDir;	

	protected int syncCount = 1000;
	protected int syncTimeInterval = 300;
	private String hdfsPath;
	private String hdfsUri;
	private String hdfsUser;
	private String fileNamePrefix;
	private String tablename;
	private String topic;
	private String citycode;
	private int threadNum = 1;
	private int partdateFieldIndex = 1;
	private ConsumerConfig config;
	
	public HdfsTask(ConsumerConfig config,String citycode,String topic,String hdfsUri, 
			String hdfsUser, String hdfsPath,String tablename,int syncTimeInterval,
			int syncCount,int partdateFieldIndex) {
		this.config = config;
		this.citycode = citycode;
		this.hdfsUri = hdfsUri;
		this.hdfsUser = hdfsUser;
		this.hdfsPath = hdfsPath;
		this.tablename = tablename;
		this.fileNamePrefix = citycode + "_" + topic;
		this.syncTimeInterval = syncTimeInterval * 1000;
		this.syncCount = syncCount;
		this.topic = topic;
		this.partdateFieldIndex = partdateFieldIndex;
		
		this.dataCacheMap = new ConcurrentHashMap<String, List<String>>();
		this.countCacheMap = new ConcurrentHashMap<String, Integer>();
		this.writeLock = new Object();
		this.cacheFileDir = CmccConfig.getInstance().getWorkPath() + "/cache/" + topic + "/";
		this.auditFileDir = CmccConfig.getInstance().getWorkPath() + "/logs/audit/";
		
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
		        public void run() {
		            //System.out.println("HdfsTask TimeSync.");
		            saveData(null,"TimeSync");
		        }
		}, this.syncTimeInterval, this.syncTimeInterval);
		
	}
		
	@Override
	public void run(){
		//处理未入库的缓存数据
		String cacheFilePath = CmccConfig.getInstance().getWorkPath() + "/cache/" + topic + "/";
		File dir = new File(cacheFilePath);
		if(!dir.exists()) dir.mkdirs();
		
		for(File subDir : dir.listFiles()){
			if(!subDir.isDirectory()) continue;
			for(File file : subDir.listFiles()){
				if(file.getName().endsWith(".fin") || file.getName().endsWith(".cache")){
					List<String> list = readFileByLines(file);
					String statDay = file.getName().substring(file.getName().lastIndexOf("_")+1,file.getName().lastIndexOf(".")-2);
					new WriteHdfsThread(hdfsUri, hdfsUser, hdfsPath, fileNamePrefix,
							statDay, list,tablename,citycode,file.getAbsolutePath()).start();
				}
			}
		}
		//处理kafka中数据
		ConsumerConnector consumer = Consumer.createJavaConsumerConnector(config);
		
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
    	//long offset = next.offset();
    	
    	Map<String,String> keyMap = new HashMap<String,String>();
		String[] arr = line.split("\\|");
		long timestamp = Long.parseLong(arr[partdateFieldIndex] + "000");
		String statDateHour = DateUtil.getHourStr(timestamp);
		String partdate = statDateHour.substring(0,8);
		keyMap.put("partdate",partdate);
		keyMap.put("statDateHour",statDateHour);
    	
		//缓存文件
		String cacheFilePath = this.cacheFileDir + (statDateHour.substring(0,8)) + "/";
		File dir = new File(cacheFilePath);
		if(!dir.exists()) dir.mkdirs();
		mfw.writeLine(cacheFilePath + topic + "_" + statDateHour + ".cache", line);
		
    	saveData(keyMap,line);
	}
	
	protected void saveData(Map<String,String> keyMap,String line) {

		boolean forceSync = false;
		String syncStatDay = "";
		if (line.equals("TimeSync")) {
			forceSync = true;
		} else {
			String statDay = keyMap.get("statDateHour");
			if (dataCacheMap.containsKey(statDay)) {
				dataCacheMap.get(statDay).add(line);
			} else {
				List<String> list = new ArrayList<String>();
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
				List<String> list = dataCacheMap.get(syncStatDay);
				int dataSize = list.size();
				
				String cacheFilePath = this.cacheFileDir + (syncStatDay.substring(0,8)) + "/"+ topic + "_" + syncStatDay + ".cache";
				String finishFilePath = this.cacheFileDir + (syncStatDay.substring(0,8)) + "/"+ topic + "_" + syncStatDay + "_" + UUID.randomUUID() + ".fin";
				mfw.closeAndRename(cacheFilePath,finishFilePath);
				
				// output hdfs
				System.out.println("output hdfs");
				new WriteHdfsThread(hdfsUri, hdfsUser, hdfsPath, fileNamePrefix,
						syncStatDay, list,tablename,citycode,finishFilePath).start();

				dataCacheMap.remove(syncStatDay);
				
				//写统计日志
				String audit = DateUtil.getDateTime(System.currentTimeMillis()) + "|" + dataSize;
				String auditFilePath = this.auditFileDir + (syncStatDay.substring(0,8)) + "/";
				File dir = new File(auditFilePath);
				if(!dir.exists()) dir.mkdirs();
				mfw.writeLine(auditFilePath + topic + "_audit_" + syncStatDay + ".log", audit);
				
			} else {
				for (String statDay : dataCacheMap.keySet()) {
					List<String> list = dataCacheMap.get(statDay);
					int dataSize = list.size();
					
					String cacheFilePath = this.cacheFileDir + (statDay.substring(0,8)) + "/"+ topic + "_" + statDay + ".cache";
					String finishFilePath = this.cacheFileDir + (statDay.substring(0,8)) + "/"+ topic + "_" + statDay + "_" + UUID.randomUUID() + ".fin";
					mfw.closeAndRename(cacheFilePath,finishFilePath);
					// output hdfs
					System.out.println("output hdfs");
					new WriteHdfsThread(hdfsUri, hdfsUser, hdfsPath, fileNamePrefix,
							statDay.substring(0,8), list,tablename,citycode,finishFilePath).start();

					dataCacheMap.remove(statDay);
					countCacheMap.remove(statDay);
					
					//写统计日志
					String audit = DateUtil.getDateTime(System.currentTimeMillis()) + "|" + dataSize;
					String auditFilePath = this.auditFileDir + (statDay.substring(0,8)) + "/";
					File dir = new File(auditFilePath);
					if(!dir.exists()) dir.mkdirs();
					mfw.writeLine(auditFilePath + topic + "_audit_" + statDay + ".log", audit);
					
				}
			}
		}
	}
	
	
	public void writeSyncTimeSign(){
		saveData(null,"TimeSync");
	}
	
	
	private List<String> readFileByLines(File file) {  
		List<String> list = new ArrayList<String>();
        BufferedReader reader = null;  
        try {  
            reader = new BufferedReader(new FileReader(file));  
            String line = null;  
            while ((line = reader.readLine()) != null) {  
        		list.add(line);
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
}
