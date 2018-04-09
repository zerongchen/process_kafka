package com.aotain.cmcc.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.aotain.util.DateUtil;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

/**
 * Hello world!
 * 
 */
public class KafkaProducer2 {
	private final Producer<String, String> producer;

	private KafkaProducer2() {
		Properties props = new Properties();
		// �˴����õ���kafka�Ķ˿�
		props.put("metadata.broker.list", "hadoop-r720-3:9092");

		// ����value�����л���
		props.put("serializer.class", "kafka.serializer.StringEncoder");
		// ����key�����л���
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
		/*
		 * int messageNo = 1000; final int COUNT = 5000000;
		 * 
		 * while (messageNo < COUNT) { String key = String.valueOf(messageNo);
		 * String data
		 * =System.currentTimeMillis()/1000+"|"+System.currentTimeMillis
		 * ()/1000+"|"+System.currentTimeMillis()/1000+"|"+
		 * messageNo+"|"+messageNo+"|1|"+messageNo*10+"|"+messageNo*100 ;
		 * producer.send(new KeyedMessage<String, String>(TOPIC, key ,data));
		 * System.out.println(data); messageNo ++; }
		 */
		List<String> list = readFileByLines(file);
		for(int i=0;i<list.size();i++){
			producer.send(new KeyedMessage<String, String>(topic, String.valueOf(i) ,list.get(i)));
		}
	}

	public static void main(String[] argv) {

		if(argv == null || argv.length==0){
			exit_with_help();
		}
		String topic = "";
		String filename = "";
		// parse options
		for(int i=0;i<argv.length;i++){
			
			if(argv[i].charAt(0) != '-') break;
			if(++i>=argv.length)
				exit_with_help();
			switch(argv[i-1].charAt(1)){
				case 't':
					topic = argv[i];
					break;
				case 'f':
					filename = argv[i];
					break;
				default:
					System.err.println("Unknown option: " + argv[i-1] + "\n");
					exit_with_help();
			}
			
		}
		
		
		File file = new File(filename);
		new KafkaProducer2().produce(topic,file);

	}
	
	private static void exit_with_help(){
		System.out.print(
		 "Usage: java -jar process_kafka.jar -p parser_type -i input directory [-o directory] [-n threadpoolNum]\n"
		+"options:\n"
		+"-p parser_type : set type of fetcher\n"
		+"	1 -- shenzhen.qfang.com \n"
		+"	2 -- sz.fang.anjuke.com \n"
		+"	3 -- sz.fang.lianjia.com \n"
		+"	4 -- newhouse.sz.fang.com \n"
		+"-i inputDirectory \n"
		+"-o outputDirectory :default inputDirectory + './output' \n"
		+"-n threadpoolNum : default 5\n"
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