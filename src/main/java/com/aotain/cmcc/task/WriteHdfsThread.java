package com.aotain.cmcc.task;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import com.aotain.util.DamsLog;
import com.aotain.util.HiveUtil;

public class WriteHdfsThread extends Thread {
	
	private static final Log LOG = DamsLog.threadLog;
	
	String hdfsUri;
	String hdfsUser;
	String hdfsPath;
	String fileNamePrefix;
	String statDay;
	String cacheFilename;
	List<String> list;
	String tablename;
	String citycode;

	public WriteHdfsThread(String hdfsUri, String hdfsUser, String hdfsPath,
			String fileNamePrefix, String statDay, List<String> list,
			String tablename,String citycode,String cacheFilename) {
		this.hdfsUri = hdfsUri;
		this.hdfsUser = hdfsUser;
		this.hdfsPath = hdfsPath;
		this.fileNamePrefix = fileNamePrefix;
		this.cacheFilename = cacheFilename;
		this.statDay = statDay;
		this.list = list;
		this.tablename = tablename;
		this.citycode = citycode;
	}

	@Override
	public void run() {

		if (list == null || list.size() == 0)
			return;

		FSDataOutputStream out = null;
		FileSystem fs = null;
		try {
			Configuration conf = new Configuration();
			conf.addResource(new Path("/etc/hadoop/conf/core-site.xml"));
			conf.addResource(new Path("/etc/hadoop/conf/yarn-site.xml"));
			conf.addResource(new Path("/etc/hadoop/conf/hdfs-site.xml"));
			conf.set("fs.hdfs.impl",
					"org.apache.hadoop.hdfs.DistributedFileSystem");
			fs = FileSystem.get(conf);
			if(System.getProperty("os.name").toLowerCase().contains("windows")) {
				fs = FileSystem.get(new URI(hdfsUri),new Configuration(),hdfsUser);
			}

			Path path = new Path(hdfsPath + citycode + "/" + statDay);
			if (!fs.exists(path)) {
				int retcode = createHiveTablePartition(statDay);
				if (retcode != 0) {
					fs.mkdirs(path);
				}
			}

			String currDatetime = UUID.randomUUID().toString();// DateUtil.getCurrDateTime();
			// int rnd = new Random().nextInt(10000);
			// first,write a hidden file
			String writingFilename = hdfsPath + citycode + "/" + statDay + "/."
					+ fileNamePrefix + statDay + "_" + currDatetime + ".txt";
			Path writingfile = new Path(writingFilename);
			out = fs.create(writingfile);

			if (fs.exists(writingfile)) {
				LOG.info("Starting to write file: " + writingFilename);

				for (String line : list) {
					out.writeBytes(line + "\r\n");
				}

				out.close();
				out = null;
				LOG.info("Finished writing file: " + writingFilename);
				// rename
				String dstFilename = hdfsPath + citycode + "/" + statDay + "/"
						+ fileNamePrefix + statDay + "_" + currDatetime
						+ ".txt";
				fs.rename(writingfile, new Path(dstFilename));
				LOG.info("Renamed file: " + writingFilename + " to "
						+ dstFilename);
			} else {
				LOG.error("File create fail: " + writingFilename + ","
						+ list.size() + " records lost");
			}

			list.clear();
			list = null;
			
			//删除缓存文件
			File cacheFile = new File(cacheFilename);
			if(cacheFile.exists()){
				cacheFile.delete();
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				out = null;
			}
		}
	}

	private int createHiveTablePartition(String statDay) {
		Connection connection = HiveUtil.getHiveConnection();
		String sql = "alter table " + tablename
				+ " add if not exists partition (citycode='" + citycode + "',partdate=" + statDay
				+ ") location '" + citycode  + "/" + statDay + "'";
		Statement stmt = null;
		try {
			stmt = connection.createStatement();
			stmt.execute(sql);
			return 0;
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				stmt = null;
			}
		}
		return 1;
	}
}