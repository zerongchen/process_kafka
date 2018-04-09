package com.aotain.cmcc.main;

import java.util.List;
import java.util.Properties;

import kafka.consumer.ConsumerConfig;

import com.aotain.cmcc.task.HdfsTask;
import com.aotain.cmcc.task.OracleTask;
import com.aotain.util.CmccConfig;
import com.aotain.util.CmccConfig.Item;
import com.aotain.util.DamsLog;

public class Bootstrap {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		CmccConfig cmccConfig = CmccConfig.getInstance();
		
		Properties props = new Properties();
		// zookeeper 配置 "hadoop-r720-1:2181,hadoop-r720-3:2181/kafka_2" 
		props.put("zookeeper.connect", cmccConfig.getZookeeperConnect());
		
		// zk连接超时
		props.put("zookeeper.session.timeout.ms", cmccConfig.getZookeeperSessionTimeoutMs());
		props.put("zookeeper.sync.time.ms", cmccConfig.getZookeeperSyncTimeMs());
		props.put("auto.commit.interval.ms", cmccConfig.getAutoCommitIntervalMs());
		props.put("auto.offset.reset", cmccConfig.getAutoOffsetReset());
		// 序列化类
		props.put("serializer.class", "kafka.serializer.StringEncoder");
		
		
		
//		List<Item> hiveTableList = cmccConfig.getHiveTableList();
//		for(Item item : hiveTableList){
//			// group 代表一个消费组
//			props.put("group.id", cmccConfig.getGroupId() + "_hive");
//			ConsumerConfig config = new ConsumerConfig(props);
//
//			HdfsTask hdfsTask = new HdfsTask(
//					config,
//					cmccConfig.getCitycode(),
//					item.getTopic(),
//					cmccConfig.getHdfsUri(),
//					cmccConfig.getHdfsUser(),
//					item.getHdfsPath(),
//					item.getTablename(),
//					item.getSyncTimeInterval(),
//					item.getSyncCount(),
//					item.getPartdateFieldIndex()
//					);
//
//			hdfsTask.start();
//			DamsLog.threadLog.info(item.getTopic() +"-task started.");
//		}
		
		List<Item> oracleTableList = cmccConfig.getOracleTableList();
		for(Item item : oracleTableList){
			// group 代表一个消费组
			props.put("group.id", cmccConfig.getGroupId() + "_db");
			ConsumerConfig config = new ConsumerConfig(props);
			OracleTask oracleTask = new OracleTask(config,
					item.getTopic(),
					item.getSql(),
					item.getSyncTimeInterval(),
					item.getSyncCount(),
					item.getDateFieldIndexs(),
					item.getPartdateFieldIndex());
			oracleTask.start();
			DamsLog.threadLog.info(item.getTopic() +"-task started.");
		}
	}

}
