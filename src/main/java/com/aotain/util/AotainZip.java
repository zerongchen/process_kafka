package com.aotain.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.tools.zip.ZipOutputStream;

public class AotainZip {

	 public static void doZip(ByteArrayOutputStream buffer,String dirName,String fileName){

	    	CreateDir(dirName);
	    	try{ 
	    		org.apache.tools.zip.ZipOutputStream zipOut = 
	    			new org.apache.tools.zip.ZipOutputStream(new BufferedOutputStream(new FileOutputStream(dirName + fileName + ".zip"))); 
	            zipOut.putNextEntry(new org.apache.tools.zip.ZipEntry(fileName + ".xml"));
	            zipOut.setEncoding("GBK");
	            //zipOut.write("<?xml version=\"1.0\" encoding=\"GBK\"?>\n".getBytes());
	            zipOut.write(buffer.toByteArray());
	            zipOut.close(); 
	        }
	    	catch(IOException ioe){ 
	            ioe.printStackTrace(); 
	        } 
	    }
	
	    
		private static void CreateDir(String dirName) { 
			File file = new File(dirName); 
			if (!file.exists()){ 
				file.mkdirs(); 
			} 
		} 
		
		public static int unZip(byte[] buf,String dirName){
			int result = 0;
			FileOutputStream fileOut; 
		    File file; 
		        
			CreateDir(dirName);
			java.util.zip.ZipInputStream zis = 
				new java.util.zip.ZipInputStream(new ByteArrayInputStream(buf)); 
	        java.util.zip.ZipEntry entry; 
	        try{
	        	while ((entry = zis.getNextEntry()) != null) { 
		        	file = new File(dirName + entry.getName()); 

		            if(entry.isDirectory()){ 
		            	file.mkdirs(); 
		            } 
		            else{ 
	                    //如果指定文件的目录不存在,则创建之. 
	                    File parent = file.getParentFile(); 
	                    if(parent !=null && !parent.exists()){ 
	                        parent.mkdirs(); 
	                    } 

	                    int readedBytes;
	                    byte[] buffer =  new byte[512]; 
	                    fileOut = new FileOutputStream(file); 
	                    while(( readedBytes = zis.read(buffer) ) > 0){ 
	                        fileOut.write(buffer , 0 , readedBytes ); 
	                    } 
	                    fileOut.close(); 

	                    zis.closeEntry();
		            } 
	        	}
	        }
	        catch(IOException ioe){ 
	            ioe.printStackTrace(); 
	            result = -100;
	        } 
	        finally{
	        	try{
	        		zis.close();
	        	}
	        	 catch(IOException ioe){ 
	 	            ioe.printStackTrace(); 
	 	            result = -200;
	 	        } 
	        }
	        
	        return result;
		}
		
		public static String unZipReturnFileName(byte[] buf,String dirName){
			String result = null;
			FileOutputStream fileOut; 
		    File file; 
		        
			CreateDir(dirName);
			java.util.zip.ZipInputStream zis = 
				new java.util.zip.ZipInputStream(new ByteArrayInputStream(buf)); 
	        
	        try{
	        	java.util.zip.ZipEntry entry; 
	        	while ((entry = zis.getNextEntry()) != null) { 
	        		result = entry.getName();
		        	file = new File(dirName + entry.getName()); 

		            if(entry.isDirectory()){ 
		            	file.mkdirs(); 
		            } 
		            else{ 
	                    //如果指定文件的目录不存在,则创建之. 
	                    File parent = file.getParentFile(); 
	                    if(parent !=null && !parent.exists()){ 
	                        parent.mkdirs(); 
	                    } 

	                    int readedBytes;
	                    byte[] buffer =  new byte[512]; 
	                    fileOut = new FileOutputStream(file); 
	                    while(( readedBytes = zis.read(buffer) ) > 0){ 
	                        fileOut.write(buffer , 0 , readedBytes ); 
	                    } 
	                    fileOut.close(); 

	                    zis.closeEntry();
		            } 
	        	}
	        }
	        catch(Exception e){ 
	            DamsLog.threadLog.error(e.toString());
	            result = null;
	        } 
	        finally{
	        	try{
	        		zis.close();
	        	}
	        	 catch(Exception e){ 
	        		 DamsLog.threadLog.error(e.toString());
	 	        } 
	        }
	        
	      return result;
	}
}
