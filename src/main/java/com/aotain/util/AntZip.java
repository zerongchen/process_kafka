package com.aotain.util;

import java.io.*; 

import org.apache.tools.zip.*; 
import java.util.Enumeration; 
/** 
*����:zipѹ������ѹ(֧�������ļ���) 
*˵��:������ͨ��ʹ��Apache Ant���ṩ��zip����org.apache.tools.zipʵ����zipѹ���ͽ�ѹ����. 
*   ���������java.util.zip����֧�ֺ��ֵ����⡣ 
*   ʹ��java.util.zip��ʱ,��zip�ļ���������Ϊ���ĵ��ļ�ʱ, 
*   �ͻ�����쳣:"Exception  in thread "main " java.lang.IllegalArgumentException  
*               at   java.util.zip.ZipInputStream.getUTF8String(ZipInputStream.java:285) 
*ע��: 
*   1��ʹ��ʱ��ant.jar�ŵ�classpath��,������ʹ��import org.apache.tools.zip.*; 
*   2��Apache Ant ���ص�ַ:http://ant.apache.org/ 
*   3��Ant ZIP API:http://www.jajakarta.org/ant/ant-1.6.1/docs/mix/manual/api/org/apache/tools/zip/ 
*   4��������ʹ��Ant 1.7.1 �е�ant.jar 
* 
*�������ѧϰ�ο�. 
* 
*@author Winty 
*@date   2008-8-3 
*@Usage: 
*   ѹ��:java AntZip -zip "directoryName" 
*   ��ѹ:java AntZip -unzip "fileName.zip" 
*/ 

public class AntZip{ 
    private ZipFile         zipFile; 
    private ZipOutputStream zipOut;     //ѹ��Zip 
    private ZipEntry        zipEntry; 
    private static int      bufSize;    //size of bytes 
    private byte[]          buf; 
    private int             readedBytes; 
     
    public AntZip(){ 
        this(512); 
    } 

    public AntZip(int bufSize){ 
        this.bufSize = bufSize; 
        this.buf = new byte[this.bufSize]; 
    } 
     
    //ѹ���ļ����ڵ��ļ� 
    public void doZip(String zipDirectory){//zipDirectoryPath:��Ҫѹ�����ļ����� 
        File file; 
        File zipDir; 

        zipDir = new File(zipDirectory); 
        String zipFileName = zipDir.getName() + ".zip";//ѹ�������ɵ�zip�ļ��� 

        try{ 
            this.zipOut = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFileName))); 
            handleDir(zipDir , this.zipOut); 
            this.zipOut.close(); 
        }catch(IOException ioe){ 
            ioe.printStackTrace(); 
        } 
    } 

    //��doZip����,�ݹ����Ŀ¼�ļ���ȡ 
    private void handleDir(File dir , ZipOutputStream zipOut)throws IOException{ 
        FileInputStream fileIn; 
        File[] files; 

        files = dir.listFiles(); 
     
        if(files.length == 0){//���Ŀ¼Ϊ��,�򵥶�����֮. 
            //ZipEntry��isDirectory()������,Ŀ¼��"/"��β. 
            this.zipOut.putNextEntry(new ZipEntry(dir.toString() + "/")); 
            this.zipOut.closeEntry(); 
        } 
        else{//���Ŀ¼��Ϊ��,��ֱ���Ŀ¼���ļ�. 
            for(File fileName : files){ 
                //System.out.println(fileName); 

                if(fileName.isDirectory()){ 
                    handleDir(fileName , this.zipOut); 
                } 
                else{ 
                    fileIn = new FileInputStream(fileName); 
                    this.zipOut.putNextEntry(new ZipEntry(fileName.toString())); 

                    while((this.readedBytes = fileIn.read(this.buf))>0){ 
                        this.zipOut.write(this.buf , 0 , this.readedBytes); 
                    } 

                    this.zipOut.closeEntry(); 
                } 
            } 
        } 
    } 
    
    //��ѹָ��zip�ļ� 
    public void unZip(String unZipfileName){//unZipfileName��Ҫ��ѹ��zip�ļ��� 
        FileOutputStream fileOut; 
        File file; 
        InputStream inputStream; 

        try{ 
            this.zipFile = new ZipFile(unZipfileName); 
                
            for(Enumeration entries = this.zipFile.getEntries(); entries.hasMoreElements();){ 
                ZipEntry entry = (ZipEntry)entries.nextElement(); 
                file = new File(entry.getName()); 

                if(entry.isDirectory()){ 
                    file.mkdirs(); 
                } 
                else{ 
                    //���ָ���ļ���Ŀ¼������,�򴴽�֮. 
                    File parent = file.getParentFile(); 
                    if(!parent.exists()){ 
                        parent.mkdirs(); 
                    } 

                    
                    inputStream = zipFile.getInputStream(entry); 

                    fileOut = new FileOutputStream(file); 
                    while(( this.readedBytes = inputStream.read(this.buf) ) > 0){ 
                        fileOut.write(this.buf , 0 , this.readedBytes ); 
                    } 
                    fileOut.close(); 

                    inputStream.close(); 
                }    
            } 
            this.zipFile.close(); 
        }catch(IOException ioe){ 
            ioe.printStackTrace(); 
        } 
    } 

    //���û�������С 
    public void setBufSize(int bufSize){ 
        this.bufSize = bufSize; 
    } 

    //����AntZip�� 
    public static void main(String[] args)throws Exception{ 
        if(args.length==2){ 
            String name = args[1]; 
            AntZip zip = new AntZip(); 

            if(args[0].equals("-zip")) 
                zip.doZip(name); 
            else if(args[0].equals("-unzip")) 
                zip.unZip(name); 
        } 
        else{ 
            System.out.println("Usage:"); 
            System.out.println("ѹ��:java AntZip -zip directoryName"); 
            System.out.println("��ѹ:java AntZip -unzip fileName.zip"); 
            throw new Exception("Arguments error!"); 
        } 
    } 
}