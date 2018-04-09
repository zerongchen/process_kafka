package com.aotain.cmcc.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.aotain.util.CmccConfig;
import com.aotain.util.DateUtil;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

/**
 * Hello world!
 * 
 */
public class AllKafkaProducer {
	private final Producer<String, String> producer;

	private AllKafkaProducer() {
		Properties props = new Properties();
		// DPI-01:2181/kafka_2
		String zc = CmccConfig.getInstance().getZookeeperConnect();
		String broker = zc.substring(0,zc.indexOf("/"));
		broker = broker.replace(":2181", ":9092");
		props.put("metadata.broker.list", broker);

		// 
		props.put("serializer.class", "kafka.serializer.StringEncoder");
		//
		props.put("key.serializer.class", "kafka.serializer.StringEncoder");

		// request.required.acks
		// 0, which means that the producer never waits for an acknowledgement
		// from the broker (the same behavior as 0.7). This option provides the
		// lowest latency but the weakest durability guarantees (some data will
		// be lost when a server fails).
		// 1, which means that the producer gets an acknowledgement after the
		// leader replica has received the data. This option provides better
		// durability as the client waits until the server acknowledges the
		// request as successful (only messages that were written to the
		// now-dead leader but not yet replicated will be lost).
		// -1, which means that the producer gets an acknowledgement after all
		// in-sync replicas have received the data. This option provides the
		// best durability, we guarantee that no messages will be lost as long
		// as at least one in sync replica remains.
		props.put("request.required.acks", "1");

		producer = new Producer<String, String>(new ProducerConfig(props));
	}

	void produce(String topic,File file) {
		List<String> list = readFileByLines(file);
		try{
			System.out.println("Topic:" + topic + ",produce " + list.size() + " records.");
			for(int i=0;i<list.size();i++){
				if(list.get(i) == null || list.get(i).length()<1)
					continue;
				producer.send(new KeyedMessage<String, String>(topic, String.valueOf(i) ,list.get(i)));
			}
			System.out.println("Topic:" + topic + ",finished.");
		}
		catch(Exception e){
			System.out.println(e.toString());
		}
	}

	public static void main(String[] argv) {
		
		/*String sql = "INSERT INTO REPORT_FLOW_QUOTA(TRAFFIC_UP,TRAFFIC_DN,TCPDATAPACKAGE_UP,TCPDATAPACKAGE_DN,TCPCONNECT_COUNT,TCPSUCCESSCONNECT_COUNT,TCPDATAPACKAGE_COUNT,TCPRETRANSMIT_COUNT,SHAKEHANDS_DELAY,CLIENT_DELAY,SERVER_DELAY,FIRSTBYTE_LOADINGTIME,HTTP_DELAY,HTTPGETTIMES,HTTPGETSUCTIMES,HTTPPOSTTIMES,HTTPPOSTSUCTIMES,RESPONSETIMES,RESPONSESUCTIMES,RESPONSEERRORTIMES,RESPONSEREDIRECTTIMES,NODETIME,R_STATTIME,ENDTIME,SERVICE_TYPE) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,to_date('%s', 'yyyy-mm-dd hh24:mi:ss'),to_date('%s', 'yyyy-mm-dd hh24:mi:ss'),to_date('%s', 'yyyy-mm-dd hh24:mi:ss'),%s)";
		String line = "556233|2706298|3966|4601|332|332|7148|537|32|1|31|57|59|97|97|2|2|99|8|90|1|1511807100|1511806500|1511806800|2";
		String[] arr = line.split("\\|");
		long ts = Long.parseLong(arr[22]+"000");
		int[] dateFieldIndex = new int[]{21,22,23};
		for(int idx :dateFieldIndex){
			long timestamp = Long.parseLong(arr[idx]+"000");
			arr[idx] = DateUtil.getDateTime(timestamp);
		}
		
		String e_sql = String.format(sql, arr);
		System.out.print(e_sql);*/
		AllKafkaProducer producer = new AllKafkaProducer();
		
		String dataPath = System.getProperty("user.dir") + "/testdata";
		File path = new File(dataPath);
		if(!path.exists()) System.exit(-1);
		
		for(File subPath: path.listFiles()){
			if(subPath.isDirectory()){
				String topic = subPath.getName();
				for(File file:subPath.listFiles()){
					producer.produce(topic,file);
				}
			}
		}

	}
	
	private static void exit_with_help(){
		System.out.print(
		 "Usage: java -jar process_kafka_producer.jar"
		);
		System.exit(1);
	}

	private  List<String> readFileByLines(File file) {
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