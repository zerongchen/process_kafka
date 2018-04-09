package com.aotain.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBUtil {
	
	/**
	 * 获取oracle数据库连接 
	 * @return
	 */
	public static Connection getConnection(){
		String driver = CmccConfig.getInstance().getDbDriver();
		String url = CmccConfig.getInstance().getDbUrl(); 
		String user = CmccConfig.getInstance().getDbUser(); 
		String password = CmccConfig.getInstance().getDbPwd();
		
		Connection conn = null;
		try {
			Class.forName(driver);
			conn = DriverManager.getConnection(url, user, password);
		} catch (ClassNotFoundException e) {
			DamsLog.dbLog.error("Not find Driver");
		} catch (SQLException e) {
			DamsLog.dbLog.error("Connect error", e);
		}
		return conn;
	}
	
	
	/**
	 * �ر�һ����ݿ�����
	 * @param conn
	 */
	public static void closeConnection(Connection conn){
		if(conn != null){
			try {
				conn.close();
			} catch (SQLException e) {
				DamsLog.dbLog.error("Close Connect fail");
			}
		}
	}
	
	// �ر���ݿ����ӣ��ͷŻ���ݿ����ӳ�
	public static void closeConnection(Connection conn, java.sql.Statement st,
			ResultSet rs) {
		try {
			if (conn != null) {
				conn.close();
			}
			conn = null;

			if (st != null) {
				st.close();
			}
			st = null;
			if (rs != null) {
				rs.close();
			}
			rs = null;
		} catch (Exception ex) {
		}
	}
	
	// �ر���ݿ����ӣ��ͷŻ���ݿ����ӳ�
	public static void closeConnection(Connection conn, java.sql.Statement st) {
		try {
			if (conn != null) {
				conn.close();
			}
			conn = null;

			if (st != null) {
				st.close();
			}
			st = null;
		} catch (Exception ex) {
		}
	}
}
