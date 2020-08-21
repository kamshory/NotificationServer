package com.planetbiru.pushserver.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.support.JdbcUtils;

import com.planetbiru.pushserver.config.Config;

public class Database {
	private Logger logger = LoggerFactory.getLogger(Database.class);
	public long getLastAutoIncrement(Connection conn) {
		long lastID = 0;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try 
		{
			QueryBuilder query = new QueryBuilder(Config.getDatabaseType());
			String sqlCommand = query.lastID().alias("last_id").toString();
			stmt = conn.prepareStatement(sqlCommand);
			rs = stmt.executeQuery();			
			if(rs.isBeforeFirst() && rs.next())
			{
				lastID = rs.getLong("last_id");
			}
		}
		catch (SQLException | DatabaseTypeException e) 
		{
			logger.error(e.getMessage());
		}
		finally 
		{
			JdbcUtils.closeResultSet(rs);
			JdbcUtils.closeStatement(stmt);
		}
		return lastID;
	}

}
