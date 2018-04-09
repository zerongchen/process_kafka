package com.aotain.cmcc.test;

import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConsumerGroupThread implements Runnable{

    private KafkaStream m_streams;
    private int m_threadNumbers;

    public ConsumerGroupThread(KafkaStream m_stream , int m_threadNumber){
        m_streams = m_stream;
        m_threadNumbers = m_threadNumber;
    }

//    @Override
    public void run() {

    ConsumerIterator<byte[], byte[]> it = m_streams.iterator();
    while (it.hasNext()){
        System.out.println("Thread " +m_threadNumbers + " : "+ new String(it.next().message()) );

    }
        System.out.println("Shutting down Thread: " + m_threadNumbers);

    }
}