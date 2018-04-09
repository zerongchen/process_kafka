package com.aotain.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.PropertyConfigurator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CmccConfig {
	
	private String workPath;
	//#kafka config
	private String zookeeperConnect;
	// group 代表一个消费组
	private String groupId="cmcc";
	// zk连接超时
	private String zookeeperSessionTimeoutMs="4000";
	private String zookeeperSyncTimeMs="200";
	private String autoCommitIntervalMs="1000";
	private String autoOffsetReset="smallest";
	//#hdfs config
	private String hdfsUri;
	private String hdfsUser;
	//#hive config
	private String hiveJdbcUrl;
	private String hiveJdbcDriver;
	private String hiveJdbcUser;
	private String hiveJdbcPasswd;
	//#oracle config
	private String dbDriver;
	private String dbUrl;
	private String dbTns;
	private String dbUser;
	private String dbPwd;
	//citycode
	private String citycode;

	private List<Item> hiveTableList = new ArrayList<Item>();
	private List<Item> oracleTableList = new ArrayList<Item>();
	
	private static CmccConfig instance;

	private CmccConfig() {
		workPath = System.getProperty("user.dir");
		PropertyConfigurator.configure(workPath + "/conf/log4j.properties");
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		InputStream is = null;
		File configFile = new File(workPath + "/conf/config.xml");
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			
			/*if(configFile != null)
				is = new FileInputStream(configFile); 
			else
				is = this.getClass().getClassLoader().getResourceAsStream("conf/config.xml");
			*/	
			//is = this.getClass().getClassLoader().getResourceAsStream();
			
			is = new FileInputStream(configFile); 
			
			Document doc = db.parse(is);
			
			//#kafka config
			zookeeperConnect = doc.getElementsByTagName("zookeeper.connect").item(0).getFirstChild().getNodeValue();
			// group 代表一个消费组
			groupId = doc.getElementsByTagName("group.id").item(0).getFirstChild().getNodeValue();
			// zk连接超时
			zookeeperSessionTimeoutMs = doc.getElementsByTagName("zookeeper.session.timeout.ms").item(0).getFirstChild().getNodeValue();
			zookeeperSyncTimeMs = doc.getElementsByTagName("zookeeper.sync.time.ms").item(0).getFirstChild().getNodeValue();
			autoCommitIntervalMs = doc.getElementsByTagName("auto.commit.interval.ms").item(0).getFirstChild().getNodeValue();
			autoOffsetReset = doc.getElementsByTagName("auto.offset.reset").item(0).getFirstChild().getNodeValue();
			//#hdfs config
			hdfsUri = doc.getElementsByTagName("hdfs.uri").item(0).getFirstChild().getNodeValue();
			hdfsUser = doc.getElementsByTagName("hdfs.user").item(0).getFirstChild().getNodeValue();
			//#hive config
			hiveJdbcUrl = doc.getElementsByTagName("hive.jdbc.url").item(0).getFirstChild().getNodeValue();
			hiveJdbcDriver = doc.getElementsByTagName("hive.jdbc.driver").item(0).getFirstChild().getNodeValue();
			hiveJdbcUser = doc.getElementsByTagName("hive.jdbc.user").item(0).getFirstChild().getNodeValue();
			hiveJdbcPasswd = doc.getElementsByTagName("hive.jdbc.passwd").item(0).getFirstChild()==null?"":doc.getElementsByTagName("hive.jdbc.passwd").item(0).getFirstChild().getNodeValue();
			//#oracle config
			dbDriver = doc.getElementsByTagName("db.driver").item(0).getFirstChild().getNodeValue();
			dbUrl = doc.getElementsByTagName("db.url").item(0).getFirstChild().getNodeValue();
			dbTns = doc.getElementsByTagName("db.tns").item(0).getFirstChild().getNodeValue();
			dbUser = doc.getElementsByTagName("db.user").item(0).getFirstChild().getNodeValue();
			dbPwd = doc.getElementsByTagName("db.pwd").item(0).getFirstChild().getNodeValue();
			//citycode
			citycode = doc.getElementsByTagName("citycode").item(0).getFirstChild().getNodeValue();
			
			NodeList nodeList = doc.getElementsByTagName("hiveItem");
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node site = nodeList.item(i);
				Item item = new Item();
				for (Node node = site.getFirstChild(); node != null; 
						node = node.getNextSibling()) {
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						String name = node.getNodeName();
						String value = node.getFirstChild().getNodeValue();
						//System.out.println(name + ":" + value + "\t");
						
						if(name.equals("topic")) item.setTopic(value); 
						else if(name.equals("threadNum")) item.setThreadNum(Integer.parseInt(value));
						else if(name.equals("partdate.field.index")) item.setPartdateFieldIndex(Integer.parseInt(value));
						else if(name.equals("tablename")) item.setTablename(value);
						else if(name.equals("hdfs.path")) item.setHdfsPath(value);
						else if(name.equals("sync.time.interval")) item.setSyncTimeInterval(Integer.parseInt(value));
						else if(name.equals("sync.count")) item.setSyncCount(Integer.parseInt(value));
						
					}
				}
				hiveTableList.add(item);
			}
			
			nodeList = doc.getElementsByTagName("oraItem");
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node site = nodeList.item(i);
				Item item = new Item();
				for (Node node = site.getFirstChild(); node != null; 
						node = node.getNextSibling()) {
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						String name = node.getNodeName();
						String value = node.getFirstChild().getNodeValue();
						//System.out.println(name + ":" + value + "\t");
						
						if(name.equals("topic")) item.setTopic(value); 
						else if(name.equals("threadNum")) item.setThreadNum(Integer.parseInt(value));
						else if(name.equals("partdate.field.index")) item.setPartdateFieldIndex(Integer.parseInt(value));
						else if(name.equals("date.field.indexs")) item.setDateFieldIndexs(value);
						else if(name.equals("sql")) item.setSql(value);
						else if(name.equals("sync.time.interval")) item.setSyncTimeInterval(Integer.parseInt(value));
						else if(name.equals("sync.count")) item.setSyncCount(Integer.parseInt(value));
						
					}
				}
				oracleTableList.add(item);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally{
			if(is != null){
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public synchronized static CmccConfig getInstance() {
		if (instance == null) {
			instance = new CmccConfig();
		}
		return instance;
	}

	private String getSystemWorkPath(){
		String path = System.getProperty("user.dir");
		if(path == null || path.length()==0){
			path = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();//Thread.currentThread().getContextClassLoader().getResource("./").getPath();
			//path = path + "../";
			if(System.getProperty("os.name").toLowerCase().contains("windows")) {
				path = path.replaceFirst("/", "");
			}
			System.setProperty("work.dir", path);
		}
		
		//logger.info(path);
		return path;
	}

	public static class Item{
		private String topic;
		private int threadNum;
		private int partdateFieldIndex;
		private String tablename;
		private String hdfsPath;
		private int syncTimeInterval;
		private int syncCount;
		private String dateFieldIndexs;
		private String sql;
		
		public String getTopic() {
			return topic;
		}
		
		public String getHdfsPath() {
			return hdfsPath;
		}
		public int getSyncTimeInterval() {
			return syncTimeInterval;
		}
		public int getSyncCount() {
			return syncCount;
		}
		public void setTopic(String topic) {
			this.topic = topic;
		}
		
		public void setHdfsPath(String hdfsPath) {
			this.hdfsPath = hdfsPath;
		}
		public void setSyncTimeInterval(int syncTimeInterval) {
			this.syncTimeInterval = syncTimeInterval;
		}
		public void setSyncCount(int syncCount) {
			this.syncCount = syncCount;
		}
		public String getTablename() {
			return tablename;
		}
		public void setTablename(String tablename) {
			this.tablename = tablename;
		}

		public int getThreadNum() {
			return threadNum;
		}

		public int getPartdateFieldIndex() {
			return partdateFieldIndex;
		}

		public void setThreadNum(int threadNum) {
			this.threadNum = threadNum;
		}

		public void setPartdateFieldIndex(int partdateFieldIndex) {
			this.partdateFieldIndex = partdateFieldIndex;
		}

		public String getDateFieldIndexs() {
			return dateFieldIndexs;
		}

		public String getSql() {
			return sql;
		}

		public void setDateFieldIndexs(String dateFieldIndexs) {
			this.dateFieldIndexs = dateFieldIndexs;
		}

		public void setSql(String sql) {
			this.sql = sql;
		}
		
	}

	public String getZookeeperConnect() {
		return zookeeperConnect;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getZookeeperSessionTimeoutMs() {
		return zookeeperSessionTimeoutMs;
	}

	public String getZookeeperSyncTimeMs() {
		return zookeeperSyncTimeMs;
	}

	public String getAutoCommitIntervalMs() {
		return autoCommitIntervalMs;
	}

	public String getAutoOffsetReset() {
		return autoOffsetReset;
	}

	public String getHdfsUri() {
		return hdfsUri;
	}

	public String getHdfsUser() {
		return hdfsUser;
	}
	
	public String getHiveJdbcUrl() {
		return hiveJdbcUrl;
	}

	public String getHiveJdbcDriver() {
		return hiveJdbcDriver;
	}

	public String getHiveJdbcUser() {
		return hiveJdbcUser;
	}

	public String getHiveJdbcPasswd() {
		return hiveJdbcPasswd;
	}
	
	public String getCitycode() {
		return citycode;
	}

	public List<Item> getHiveTableList() {
		return hiveTableList;
	}

	
	public List<Item> getOracleTableList() {
		return oracleTableList;
	}

	
	public String getWorkPath() {
		return workPath;
	}

	
	public String getDbDriver() {
		return dbDriver;
	}

	public String getDbUrl() {
		return dbUrl;
	}

	public String getDbTns() {
		return dbTns;
	}

	public String getDbUser() {
		return dbUser;
	}

	public String getDbPwd() {
		return dbPwd;
	}

	public static void main(String[] args) {
		CmccConfig.getInstance();
	}
}
