package com.aotain.cmcc.test;

import java.util.Properties;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

/**
 * Hello world!
 *
 */
public class KafkaProducer 
{
    private final Producer<String, String> producer;
    public final static String TOPIC = "testroy";

    private KafkaProducer(){
        Properties props = new Properties();
        //�˴����õ���kafka�Ķ˿�
        props.put("metadata.broker.list", "hadoop-r720-3:9092");

        //����value�����л���
        props.put("serializer.class", "kafka.serializer.StringEncoder");
        //����key�����л���
        props.put("key.serializer.class", "kafka.serializer.StringEncoder");

        //request.required.acks
        //0, which means that the producer never waits for an acknowledgement from the broker (the same behavior as 0.7). This option provides the lowest latency but the weakest durability guarantees (some data will be lost when a server fails).
        //1, which means that the producer gets an acknowledgement after the leader replica has received the data. This option provides better durability as the client waits until the server acknowledges the request as successful (only messages that were written to the now-dead leader but not yet replicated will be lost).
        //-1, which means that the producer gets an acknowledgement after all in-sync replicas have received the data. This option provides the best durability, we guarantee that no messages will be lost as long as at least one in sync replica remains.
        props.put("request.required.acks","1");

        producer = new Producer<String, String>(new ProducerConfig(props));
    }

    void produce() {
        int messageNo = 1000;
        final int COUNT = 5000000;

        while (messageNo < COUNT) {
            String key = String.valueOf(messageNo);
            String data =System.currentTimeMillis()/1000+"|"+System.currentTimeMillis()/1000+"|"+System.currentTimeMillis()/1000+"|"+
                    messageNo+"|"+messageNo+"|1|"+messageNo*10+"|"+messageNo*100 ;
            producer.send(new KeyedMessage<String, String>(TOPIC, key ,data));
            System.out.println(data);
            messageNo ++;
        }
    }

    public static void main( String[] args )
    {

        new KafkaProducer().produce();

        System.out.println(System.getenv("HADOOP_PROXY_USER"));

        System.out.println( System.getProperty("HADOOP_PROXY_USER"));
    }
}