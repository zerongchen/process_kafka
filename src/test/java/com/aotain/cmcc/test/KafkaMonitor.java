package com.aotain.cmcc.test;

import kafka.tools.ConsumerOffsetChecker;

/**
 * Created by hadoop on 2017/11/23.
 */
public class KafkaMonitor {



    public static void PrintMess(String[] args)
    {
        //适用于kafka0.8.2.0
        String[] arr = new String[]{"--zookeeper=hadoop-r720-4:2181,hadoop-r720-3:2181/kafka_2","--group=cmcc_db"};
        //适用于kafka0.8.1
//      String[] arr = new String[]{"--zkconnect=h71:2181,h72:2181,h73:2181","--group=test-consumer-group"};
        if(args.length==2){
            arr = args;
        }
        ConsumerOffsetChecker.main(arr);
    }
    public static void main(String[] args){
        PrintMess(args);
    }
}
