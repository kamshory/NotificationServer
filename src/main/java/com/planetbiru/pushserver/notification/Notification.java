package com.planetbiru.pushserver.notification;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.sql.DataSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.support.JdbcUtils;

import com.planetbiru.pushserver.client.Client;
import com.planetbiru.pushserver.client.ClientException;
import com.planetbiru.pushserver.client.Device;
import com.planetbiru.pushserver.config.Config;
import com.planetbiru.pushserver.database.Database;
import com.planetbiru.pushserver.database.QueryBuilder;
import com.planetbiru.pushserver.database.DatabaseTypeException;
import com.planetbiru.pushserver.messenger.MessengerDelete;
import com.planetbiru.pushserver.messenger.MessengerInsert;
import com.planetbiru.pushserver.utility.Cache;
import com.planetbiru.pushserver.utility.Mail;
import com.planetbiru.pushserver.utility.QueryParserException;
import com.planetbiru.pushserver.utility.Utility;

/**
 * Notification class
 * @author Kamshory, MT
 *
 */
public class Notification 
{
	/**
	 * Device ID. It is only used when PushClient connected to the server
	 */
	private String deviceID;
	/**
	 * Group ID registered on the database according to group key when the application push the notifications or PushClient connect to the server
	 */
	private long groupID;
	/**
	 * The API ID registered on the database according to API key when the application push the notifications or PushClient connect to the server
	 */
	private long apiID = 0;
	/**
	 * Array list contains notification ID loaded when PushClient connected to the server after notification pushed
	 */
	private List<String> offlineID = new ArrayList<>();
	/**
	 * Hash password instead of clear password and only used by PushServer. Hash password stored in the database. Both pusher and PushClient never send clear password or hash password
	 */
	private String hashPasswordClient = "";
	/**
	 * Hash password instead of clear password and only used by PushServer. Hash password stored in the database. Both pusher and PushClient never send clear password or hash password
	 */
	private String hashPasswordPusher = "";
	/**
	 * TimeZone object to get time zone of the PushServer. 
	 */
	private TimeZone timeZone;
	/**
	 * Time zone offset of the PushServer. Pusher not necessary to send the local time. All the notification time use the local time of the PushServer
	 */
	private long timeZoneOffset = 0;
	private long requestID = 0;
	private DataSource dataSource;
	/**
	 * Cache to store pusher source
	 */
	private static List<String> cachePusherSource = new ArrayList<>();
	
	
	private Logger logger = LoggerFactory.getLogger(Notification.class);
	
	private Database database = new Database();
	
	public String getDeviceID() {
		return deviceID;
	}
	public void setDeviceID(String deviceID) {
		this.deviceID = deviceID;
	}
	public long getGroupID() {
		return groupID;
	}
	public void setGroupID(long groupID) {
		this.groupID = groupID;
	}
	public long getApiID() {
		return apiID;
	}
	public void setApiID(long apiID) {
		this.apiID = apiID;
	}
	public List<String> getOfflineID() {
		return offlineID;
	}
	public void setOfflineID(List<String> offlineID) {
		this.offlineID = offlineID;
	}
	public String getHashPasswordClient() {
		return hashPasswordClient;
	}
	public void setHashPasswordClient(String hashPasswordClient) {
		this.hashPasswordClient = hashPasswordClient;
	}
	public String getHashPasswordPusher() {
		return hashPasswordPusher;
	}
	public void setHashPasswordPusher(String hashPasswordPusher) {
		this.hashPasswordPusher = hashPasswordPusher;
	}
	public TimeZone getTimeZone() {
		return timeZone;
	}
	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}
	public long getTimeZoneOffset() {
		return timeZoneOffset;
	}
	public void setTimeZoneOffset(long timeZoneOffset) {
		this.timeZoneOffset = timeZoneOffset;
	}
	public long getRequestID() {
		return requestID;
	}
	public void setRequestID(long requestID) {
		this.requestID = requestID;
	}
	public static List<String> getCachePusherSource() {
		return cachePusherSource;
	}
	public static void setCachePusherSource(List<String> cachePusherSource) {
		Notification.cachePusherSource = cachePusherSource;
	}
	/**
	 * Constructor with database object from the "main"
	 * @param database1 Primary Database object
	 * @param database2 Secondary Database object
	 * @param database3 Tertiary Database object
	 * @param requestID Request ID
	 */
	public Notification(DataSource dataSource, long requestID)
	{
		this.dataSource = dataSource;
		this.requestID  = requestID;
		this.timeZone = TimeZone.getDefault();
		this.timeZone.getID();
		this.timeZoneOffset = (this.timeZone.getRawOffset() / 60000);
	}
	/**
	 * Constructor with database initialization
	 * @param database1 Primary Database object
	 * @param database2 Secondary Database object
	 * @param database3 Tertiary Database object
	 */
	public Notification(DataSource dataSource) 
	{
		this.dataSource = dataSource;
		this.timeZone = TimeZone.getDefault();
		this.timeZone.getID();
		this.timeZoneOffset = (this.timeZone.getRawOffset() / 60000);
	}
	/**
	 * PushClient authentication
	 * @param authorization URL encoded string contains the authentication sent by PushClient
	 * @return true if valid and false if invalid
	 * @throws SQLException if any SQL errors
	 * @throws QueryParserException if any errors while parsing 
	 * @throws DatabaseTypeException if database type not supported 
	 * @throws NoSuchAlgorithmException if algorithm not found
	 */
	public boolean authentication(String authorization) throws SQLException, QueryParserException, DatabaseTypeException, NoSuchAlgorithmException
	{
		Map<String, String> queryString = Utility.parseQuery(authorization);
		String key = queryString.getOrDefault("key", "").trim();
		String token = queryString.getOrDefault("token", "").trim();
		String hash = queryString.getOrDefault("hash", "").trim();			
		String time = queryString.getOrDefault("time", "0").trim();
		String groupKey = queryString.getOrDefault("group", "").trim();	
		if(time.equals(""))
		{
			time = "0";
		}
		long unixTime = Long.parseLong(time);
		String token1 = Utility.sha1(unixTime+key);
		String token2 = token;
		if(token1.equals(token2))
		{
			long unixTime1 = Utility.unixTime();
			long unixTime2 = unixTime;
			if(Math.abs(unixTime1 - unixTime2) < 86400)
			{
				QueryBuilder query1 = new QueryBuilder(Config.getDatabaseType());					
				key = query1.escapeSQL(key);
				token = query1.escapeSQL(token);
				hash = query1.escapeSQL(hash);
				time = query1.escapeSQL(time);
				groupKey = query1.escapeSQL(groupKey);
				this.groupID = this.getGroupID(key, groupKey);
				String sqlCommand = query1.newQuery()
						.select("*")
						.from(Config.getTablePrefix()+"api")
						.where("api_key = '"+key+"' and active = 1 ")
						.toString();

				Connection conn = null;
				PreparedStatement stmt = null;
				ResultSet rs = null;		
				try 
				{
					conn = this.dataSource.getConnection();
					stmt = conn.prepareStatement(sqlCommand);
					rs = stmt.executeQuery();			
					if(rs.isBeforeFirst())
					{
						rs.next();
						this.apiID = rs.getLong("api_id");
						this.hashPasswordClient = rs.getString("hash_password_client");	
						String hash1 = Utility.sha1(this.hashPasswordClient+"-"+token+"-"+key);
						String hash2 = hash;
						return hash1.equals(hash2);					
					}
				}
				catch (SQLException e) 
				{
					logger.error(e.getMessage());
				}
				finally 
				{
					JdbcUtils.closeResultSet(rs);
					JdbcUtils.closeStatement(stmt);
					JdbcUtils.closeConnection(conn);			
				}
			}
		}
		return false;	
	}
	/**
	 * Pusher authentication
	 * @param authorization URL encoded string contains the authentication sent by PushClient
	 * @param serverAddress IP address of the pusher server. It must be public static IP address. If IP address is not registered, PushServer ask administrator to validate that IP address is correct and allow to send the notification
	 * @param applicationName Application version of the pusher
	 * @param applicationVersion Application name of the pusher
	 * @param userAgent User agent of the pusher
	 * @return true if valid and false if invalid
	 * @throws SQLException if any SQL errors
	 * @throws QueryParserException if any errors while parsing
	 * @throws DatabaseTypeException if database type not supported 
	 * @throws NoSuchAlgorithmException if algorithm not found
	 * @throws ClassCastException if any class cast errors
	 * @throws NullPointerException if any null value
	 * @throws MessagingException if any errors occurred while send message
	 * @throws IllegalArgumentException if any illegal arguments
	 * @throws AddressException if any invalid address
	 */
	public boolean authentication(String authorization, String serverAddress, String applicationName, String applicationVersion, String userAgent) throws SQLException, QueryParserException, DatabaseTypeException, NoSuchAlgorithmException, ClassCastException, NullPointerException, AddressException, IllegalArgumentException, MessagingException  
	{
		Map<String, String> queryString;
		queryString = Utility.parseQuery(authorization);
		String key = queryString.getOrDefault("key", "").trim();
		String token = queryString.getOrDefault("token", "").trim();
		String hash = queryString.getOrDefault("hash", "").trim();			
		String time = queryString.getOrDefault("time", "0").trim();
		String groupKey = queryString.getOrDefault("group", "0").trim();
		if(time.equals(""))
		{
			time = "0";
		}
		long unixTime = Long.parseLong(time);
		String token1 = Utility.sha1(unixTime+key);
		String token2 = token;
		if(token1.equals(token2))
		{
			long unixTime1 = Utility.unixTime();
			long unixTime2 = unixTime;
			if(Math.abs(unixTime1 - unixTime2) < 86400)
			{
				QueryBuilder query1 = new QueryBuilder(Config.getDatabaseType());	
				key = query1.escapeSQL(key);
				token = query1.escapeSQL(token);
				hash = query1.escapeSQL(hash);
				time = query1.escapeSQL(time);
				groupKey = query1.escapeSQL(groupKey);
				serverAddress = query1.escapeSQL(serverAddress).trim();				
				this.groupID = this.getGroupID(key, groupKey);				
				String sqlCommand = "";				
				sqlCommand = query1.newQuery()
						.select(Config.getTablePrefix()+"api.api_id, coalesce("+Config.getTablePrefix()+"api.hash_password_pusher, '') as hash_password_pusher, coalesce("+Config.getTablePrefix()+"pusher_address.address, '') as address")
						.from(Config.getTablePrefix()+"api")
						.leftJoin(Config.getTablePrefix()+"pusher_address")
						.on(Config.getTablePrefix()+"pusher_address.address = '"+serverAddress+"' and "+Config.getTablePrefix()+"pusher_address.api_id = "+Config.getTablePrefix()+"api.api_id and "+Config.getTablePrefix()+"pusher_address.active = 1 and "+Config.getTablePrefix()+"pusher_address.blocked = 0")
						.where(Config.getTablePrefix()+"api.api_key = '"+key+"' and "+Config.getTablePrefix()+"api.active = 1 ")
						.toString();

				Connection conn = null;
				PreparedStatement stmt = null;
				ResultSet rs = null;		
				try 
				{
					conn = this.dataSource.getConnection();
					stmt = conn.prepareStatement(sqlCommand);
					rs = stmt.executeQuery();			
					if(rs.isBeforeFirst())
					{
						rs.next();
						this.apiID = rs.getLong("api_id");
						this.hashPasswordPusher = rs.getString("hash_password_pusher");					
						String hash1 = Utility.sha1(this.hashPasswordPusher+"-"+token+"-"+key);
						String hash2 = hash;
						String serverAddress1 = rs.getString("address").trim();
						String serverAddress2 = serverAddress;
						if(hash1.equals(hash2))
						{							
							if(serverAddress1.equals(serverAddress2) || !Config.isFilterSource())
							{
								return true;
							}
							else
							{
								this.addPusherAddress(serverAddress, applicationName, applicationVersion, userAgent);
								return false;
							}
						}
						else
						{
							return false;
						}
					}
					else
					{
						return false;
					}
				}
				catch (SQLException e) 
				{
					logger.error(e.getMessage());
				}
				finally 
				{
					JdbcUtils.closeResultSet(rs);
					JdbcUtils.closeStatement(stmt);
					JdbcUtils.closeConnection(conn);			
				}

			}
		}
		return false;	
	}
	/**
	 * Create group
	 * @param body Data sent by the client
	 * @param remoteAddress Remote address
	 * @param applicationName Application version of the pusher
	 * @param applicationVersion Application name of the pusher
	 * @param userAgent User agent of the pusher
	 * @return JSONObject contains group creation information
	 * @throws JSONException if any JSON errors
	 * @throws DatabaseTypeException if database is unsupported
	 * @throws SQLException if any SQL errors
	 * @throws AddressException if any invalid address
	 * @throws NullPointerException if any NULL pointer
	 * @throws IllegalArgumentException if any illegal arguments
	 * @throws MessagingException if any errors occurred while send message
	 * @throws NoSuchAlgorithmException if algorithm is not found
	 */
	public JSONObject createGroup(JSONObject requestJSON, String remoteAddress, String applicationName, String applicationVersion, String userAgent) throws DatabaseTypeException, SQLException, AddressException, NullPointerException, IllegalArgumentException, MessagingException, NoSuchAlgorithmException
	{
		JSONObject requestData = requestJSON.optJSONObject("data");
		if(requestData == null)
		{
			requestData = new JSONObject();
		}
		return this.createGroup(this.apiID, requestData.optString("groupKey", ""), requestData.optString("groupName", ""), requestData.optString("groupDescription", ""), remoteAddress, applicationName, applicationVersion, userAgent);
	}
	/**
	 * Create group
	 * @param apiID API ID
	 * @param groupKey Group key
	 * @param groupName Group name
	 * @param description Description
	 * @param remoteAddress Remote address
	 * @param applicationName Application version of the pusher
	 * @param applicationVersion Application name of the pusher
	 * @param userAgent User agent of the pusher
	 * @return JSONObject contains group creation information
	 * @throws DatabaseTypeException if database is unsupported
	 * @throws SQLException if any SQL errors
	 * @throws AddressException if any invalid address
	 * @throws NullPointerException if any NULL pointer
	 * @throws IllegalArgumentException if any illegal arguments
	 * @throws MessagingException if any errors occurred while send message
	 * @throws NoSuchAlgorithmException if algorithm is not found
	 */
	public JSONObject createGroup(long apiID, String groupKey, String groupName, String description, String remoteAddress, String applicationName, String applicationVersion, String userAgent) throws DatabaseTypeException, SQLException, AddressException, NullPointerException, IllegalArgumentException, MessagingException, NoSuchAlgorithmException
	{
		
		QueryBuilder query1 = new QueryBuilder(Config.getDatabaseType());
		JSONObject jo = new JSONObject();
		int active = 1;
		if(Config.isGroupCreationApproval())
		{
			active = 0;
		}
		groupKey = query1.escapeSQL(groupKey);
		groupName = query1.escapeSQL(groupName);
		description = query1.escapeSQL(description);
		remoteAddress = query1.escapeSQL(remoteAddress);
		long userCreate = 0;
		String sqlCommand = "";
		sqlCommand = query1.newQuery()
				.select("*")
				.from(Config.getTablePrefix()+"api")
				.where("api_id = "+apiID+" ")
				.toString();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;		
		try 
		{
			conn = this.dataSource.getConnection();
			stmt = conn.prepareStatement(sqlCommand);
			rs = stmt.executeQuery();			
			if(rs.isBeforeFirst())
			{
				rs.next();
				userCreate = rs.getLong("user_create");
				
				sqlCommand = query1.newQuery()
						.select("*")
						.from(Config.getTablePrefix()+"client_group")
						.where("api_id = "+apiID+" and group_key = '"+groupKey+"' ")
						.toString();
				
				JdbcUtils.closeResultSet(rs);	
				JdbcUtils.closeStatement(stmt);	
				
				stmt = conn.prepareStatement(sqlCommand);
				rs = stmt.executeQuery();			
				
				if(rs.isBeforeFirst())
				{
					jo.put("success", false);
					jo.put("message", "Group already exists");
				}
				else
				{
					sqlCommand = query1.newQuery()
							.insert()
							.into(Config.getTablePrefix()+"client_group")
							.fields("(api_id, name, group_key, description, blocked, time_create, time_edit, ip_create, ip_edit, user_create, user_edit, active)")
							.values("("+apiID+", '"+groupName+"', '"+groupKey+"', '"+description+"', 0, "+query1.now(6)+", "+query1.now(6)+", '"+remoteAddress+"', '"+remoteAddress+"', "+userCreate+", "+userCreate+", "+active+")")
							.toString();
					
					JdbcUtils.closeResultSet(rs);	
					JdbcUtils.closeStatement(stmt);	

					stmt = conn.prepareStatement(sqlCommand);
					stmt.execute();			
					
					
					long lGroupID = this.database.getLastAutoIncrement(conn);
					this.groupID = lGroupID;
					
					if(!Config.isGroupCreationApproval())
					{
						this.addPusherAddress(remoteAddress, applicationName, applicationVersion, userAgent, false, true);
					}					
					
					JSONObject jdata = new JSONObject();
					jdata.put("groupName", groupName);
					jdata.put("groupKey", groupKey);
					jdata.put("groupID", lGroupID);
					
					jo.put("success", true);
					jo.put("message", "");
					jo.put("data", jdata);
				}
			}
			else
			{
				jo.put("success", false);
				jo.put("message", "Invalid API");
			}
		}
		catch (SQLException e) 
		{
			logger.error(e.getMessage());
		}
		finally 
		{
			JdbcUtils.closeResultSet(rs);
			JdbcUtils.closeStatement(stmt);
			JdbcUtils.closeConnection(conn);			
		}
		return jo;
	}
	/**
	 * Get group ID
	 * @param apiID API ID
	 * @param groupKey Group key
	 * @return Group ID
	 * @throws SQLException if any SQL errors
	 * @throws DatabaseTypeException if database type not supported 
	 */
	public long getGroupID(long apiID, String groupKey) throws SQLException, DatabaseTypeException 
	{
		if(groupKey.length() == 0)
		{
			return 0;
		}
		QueryBuilder query1 = new QueryBuilder(Config.getDatabaseType());
		groupKey = query1.escapeSQL(groupKey).trim();
		long lGroupID = 0;
		String sqlCommand = "";
		sqlCommand = query1.newQuery()
				.select("*")
				.from(Config.getTablePrefix()+"client_group")
				.where("api_id = '"+apiID+"' and group_key = '"+groupKey+"'")
				.toString();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;		
		try 
		{
			conn = this.dataSource.getConnection();
			stmt = conn.prepareStatement(sqlCommand);
			rs = stmt.executeQuery();			
			if(rs.isBeforeFirst())
			{
				rs.next();
				lGroupID = rs.getLong("client_group_id");
			}
		}
		catch (SQLException e) 
		{
			logger.error(e.getMessage());
		}
		finally 
		{
			JdbcUtils.closeResultSet(rs);
			JdbcUtils.closeStatement(stmt);
			JdbcUtils.closeConnection(conn);			
		}
		return lGroupID;
	}
	/**
	 * Get group ID
	 * @param apiKey API key
	 * @param groupKey Group key
	 * @return Group ID
	 * @throws SQLException if any SQL errors
	 * @throws DatabaseTypeException if database type not supported 
	 */
	public long getGroupID(String apiKey, String groupKey) throws SQLException, DatabaseTypeException
	{
		if(groupKey.length() == 0)
		{
			return 0;
		}
		QueryBuilder query1 = new QueryBuilder(Config.getDatabaseType());
		apiKey = query1.escapeSQL(apiKey).trim();
		groupKey = query1.escapeSQL(groupKey).trim();
		long lGroupID = 0;
		String sqlCommand = "";
		sqlCommand = query1.newQuery()
				.select(Config.getTablePrefix()+"client_group.*")
				.from(Config.getTablePrefix()+"client_group")
				.innerJoin(Config.getTablePrefix()+"api")
				.on(Config.getTablePrefix()+"api.api_id = "+Config.getTablePrefix()+"client_group.api_id")
				.where(Config.getTablePrefix()+"api.api_key = '"+apiKey+"' and "+Config.getTablePrefix()+"client_group.group_key = '"+groupKey+"'")
				.toString();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;		
		try 
		{
			conn = this.dataSource.getConnection();
			stmt = conn.prepareStatement(sqlCommand);
			rs = stmt.executeQuery();			
			if(rs.isBeforeFirst())
			{
				rs.next();
				lGroupID = rs.getLong("client_group_id");
			}
		}
		catch (SQLException e) 
		{
			logger.error(e.getMessage());
		}
		finally 
		{
			JdbcUtils.closeResultSet(rs);
			JdbcUtils.closeStatement(stmt);
			JdbcUtils.closeConnection(conn);			
		}
		return lGroupID;
	}
	/**
	 * Confirm address in order the pusher can send the notification. This method will insert an IP address of the pusher and to be confirmed manually
	 * @param serverAddress IP address of the pusher
	 * @param applicationName Application version of the pusher
	 * @param applicationVersion Application name of the pusher
	 * @param userAgent User agent of the pusher
	 * @throws SQLException if any SQL errors
	 * @throws DatabaseTypeException if database type not supported 
	 * @throws MessagingException if any errors occurred while send message
	 * @throws IllegalArgumentException if any illegal arguments
	 * @throws NullPointerException if any NULL pointer
	 * @throws AddressException if any invalid address
	 * @throws NoSuchAlgorithmException if algorithm is not found
	 */
	public void addPusherAddress(String serverAddress, String applicationName, String applicationVersion, String userAgent) throws SQLException, DatabaseTypeException, AddressException, NullPointerException, IllegalArgumentException, MessagingException, NoSuchAlgorithmException
	{
		this.addPusherAddress(serverAddress, applicationName, applicationVersion, userAgent, true, false);
	}
	/**
	 * Confirm address in order the pusher can send the notification. This method will insert an IP address of the pusher and to be confirmed manually
	 * @param serverAddress IP address of the pusher
	 * @param applicationName Application version of the pusher
	 * @param applicationVersion Application name of the pusher
	 * @param userAgent User agent of the pusher
	 * @param needConfirmation Need confirmation
	 * @param active Active
	 * @throws SQLException if any SQL errors
	 * @throws DatabaseTypeException if database type not supported 
	 * @throws MessagingException if any errors occurred while send message
	 * @throws IllegalArgumentException if any invalid arguments
	 * @throws NullPointerException if any NULL pointer
	 * @throws AddressException if any invalid address 
	 * @throws NoSuchAlgorithmException if algorithm is not found
	 */
	public void addPusherAddress(String serverAddress, String applicationName, String applicationVersion, String userAgent, boolean needConfirmation, boolean active) throws SQLException, DatabaseTypeException, AddressException, NullPointerException, IllegalArgumentException, MessagingException, NoSuchAlgorithmException 
	{
		QueryBuilder query1 = new QueryBuilder(Config.getDatabaseType());
		String sqlCommand = "";
		String auth = "";
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;		
		try 
		{
			conn = this.dataSource.getConnection();
	
			if(this.checkInactiveSource(this.apiID, serverAddress))
			{
				sqlCommand = query1.newQuery()
						.update(Config.getTablePrefix()+"pusher_address")
						.set("need_confirmation = 1")
						.where("api_id = '"+this.apiID+"' and address = '"+serverAddress+"' ")
						.toString();
				stmt = conn.prepareStatement(sqlCommand);
				stmt.execute();			
			}
			else
			{
				auth = Utility.sha1(serverAddress+"-"+this.apiID+"-"+Utility.now("yyyy-MM-dd HH:mm:ss.SSS"))+Utility.sha1(this.hashPasswordPusher+"-"+Utility.random(111111, 999999));
				String applicationName2 = query1.escapeSQL(applicationName);
				String applicationVersion2 = query1.escapeSQL(applicationVersion);
				String userAgent2 = query1.escapeSQL(userAgent);
				sqlCommand = query1.newQuery()
						.insert()
						.into(Config.getTablePrefix()+"pusher_address")
						.fields("(api_id, address, application_name, application_version, user_agent, first_access, last_access, auth, need_confirmation, blocked, active)")
						.values("("+this.apiID+", '"+serverAddress+"', '"+applicationName2+"', '"+applicationVersion2+"', '"+userAgent2+"', "+query1.now(6)+", "+query1.now(6)+", '"+auth+"', "+((needConfirmation)?1:0)+", 0, "+((active)?1:0)+")")
						.toString();
				stmt = conn.prepareStatement(sqlCommand);
				stmt.execute();		
				long pusherAddressID = this.database.getLastAutoIncrement(conn);
				if(needConfirmation && !active)
				{
					this.sendMail(pusherAddressID, auth, serverAddress, applicationName, applicationVersion, userAgent, Utility.now("yyyy-MM-dd HH:mm:ss"));
				}
			}
		}
		catch (SQLException e) 
		{
			logger.error(e.getMessage());
		}
		finally 
		{
			JdbcUtils.closeResultSet(rs);
			JdbcUtils.closeStatement(stmt);
			JdbcUtils.closeConnection(conn);			
		}
	}
	/**
	 * Send mail when new pusher address need confirmation
	 * @param pusherAddressID Pusher address ID
	 * @param auth Authentication key
	 * @param remoteAddress Pusher address
	 * @param applicationName Application version of the pusher
	 * @param applicationVersion Application name of the pusher
	 * @param userAgent User agent of the pusher
	 * @param time Time sent
	 * @throws DatabaseTypeException if database type is not supported
	 * @throws SQLException if any SQL errors
	 * @throws AddressException if any invalid address 
	 * @throws MessagingException if any errors occurred while send message
	 */
	public void sendMail(long pusherAddressID, String auth, String remoteAddress, String applicationName, String applicationVersion, String userAgent, String time) throws DatabaseTypeException, SQLException, AddressException, MessagingException 
	{
		String message = "";
		String template = "";
		template = this.loadMailTemplate(Config.getMailTemplate());
		if(template.equals(""))
		{
			template = Config.getApprovalURLTemplate();
		}
		QueryBuilder query1 = new QueryBuilder(Config.getDatabaseType());
		String sqlCommand = "";
		String subQuery = "";	
		subQuery = query1.newQuery()
				.select(Config.getTablePrefix()+"api.name")
				.from(Config.getTablePrefix()+"api")
				.where(Config.getTablePrefix()+"api.api_id = "+Config.getTablePrefix()+"user.api_id")
				.toString();	
		sqlCommand = query1.newQuery()
				.select(Config.getTablePrefix()+"user.*, ("+subQuery+") as api")
				.from(Config.getTablePrefix()+"user")
				.innerJoin(Config.getTablePrefix()+"api_user")
				.on(Config.getTablePrefix()+"api_user.user_id = "+Config.getTablePrefix()+"user.user_id")
				.where(Config.getTablePrefix()+"user.active = 1 and "+Config.getTablePrefix()+"api_user.api_id = "+this.apiID+" ")
				.toString();

		String recipient = "";
		String userFullName = "";
		String api = "";

		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;		
		try 
		{
			conn = this.dataSource.getConnection();
			stmt = conn.prepareStatement(sqlCommand);
			rs = stmt.executeQuery();	

			if(rs.isBeforeFirst())
			{
				while(rs.next())
				{
					recipient = rs.getString("email").trim();
					userFullName = rs.getString("name").trim();
					Mail mail = new Mail();				
					if(recipient.length() > 3 && mail.isValidMailAddress(recipient))
					{
						api = rs.getString("api");						
						mail.setFrom(Config.getMailSender());		
						mail.setTo(recipient);	
						message = template;
						// Format message
						message = message.replace("{id}", pusherAddressID+"");
						message = message.replace("{api}", api);
						message = message.replace("{auth}", auth);
						message = message.replace("{user}", userFullName);
						message = message.replace("{remote_address}", remoteAddress);
						message = message.replace("{app_name}", applicationName);
						message = message.replace("{app_version}", applicationVersion);
						message = message.replace("{user_agent}", userAgent);
						message = message.replace("{time}", time);				
						mail.send(Config.getMailSubject(), message, "text/html");
					}
				}
			}
		}
		catch (SQLException e) 
		{
			logger.error(e.getMessage());
		}
		finally 
		{
			JdbcUtils.closeResultSet(rs);
			JdbcUtils.closeStatement(stmt);
			JdbcUtils.closeConnection(conn);			
		}
	}
	/**
	 * Load mail template from file
	 * @param path Template file path
	 * @return File content
	 * @throws IOException if any IO errors
	 * @throws FileNotFoundException if file not found or permission denied
	 */
	public String loadMailTemplate(String path)
	{
		FileReader fileReader = null;
		BufferedReader bufferedReader = null;
		StringBuilder data = new StringBuilder();
		try
		{
			fileReader = new FileReader(path);
			bufferedReader = new BufferedReader(fileReader);
			String line;
			int i = 0;
			while((line = bufferedReader.readLine()) != null) 
			{
				if(i > 0)
				{
					data.append("\r\n");
				}
				data.append(line);
				i++;
			}
		}
		catch(IOException e)
		{
			logger.error(e.getMessage());
		}
		finally {
			if(fileReader != null)
			{
				try
				{
					fileReader.close();
				}
				catch(Exception e)
				{
					logger.error(e.getMessage());
				}
			}
			if(bufferedReader != null)
			{
				try
				{
					bufferedReader.close();
				}
				catch(Exception e)
				{
					logger.error(e.getMessage());
				}
			}
		}
		return data.toString();
	}
	/**
	 * Check inactive source
	 * @param apiID API ID
	 * @param serverAddress Server address
	 * @return true if source is exists
	 * @throws SQLException if any SQL errors
	 * @throws DatabaseTypeException if database type not supported 
	 */
	public boolean checkInactiveSource(long apiID, String serverAddress) throws SQLException, DatabaseTypeException
	{
		QueryBuilder query1 = new QueryBuilder(Config.getDatabaseType());	
		String sqlCommand = query1.newQuery()
				.select("*")
				.from(Config.getTablePrefix()+"pusher_address")
				.where("api_id = '"+apiID+"' and address = '"+serverAddress+"' ")
				.toString();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;		
		try 
		{
			conn = this.dataSource.getConnection();
			stmt = conn.prepareStatement(sqlCommand);
			rs = stmt.executeQuery();	

			if(rs.isBeforeFirst())
			{
				return true;
			}
		}
		catch (SQLException e) 
		{
			logger.error(e.getMessage());
		}
		finally 
		{
			JdbcUtils.closeResultSet(rs);
			JdbcUtils.closeStatement(stmt);
			JdbcUtils.closeConnection(conn);			
		}
		return false;
	}
	public JSONObject registerDevice(String body) throws SQLException, DatabaseTypeException
	{
		JSONObject jo;
		jo = new JSONObject(body);
		return this.registerDevice(jo);
	}
	public JSONObject registerDevice(JSONObject jo) throws SQLException, DatabaseTypeException
	{
		QueryBuilder query1 = new QueryBuilder(Config.getDatabaseType());
		JSONObject data = jo.optJSONObject("data");
		if(data == null)
		{
			data = new JSONObject();
		}
		String lDeviceID = data.optString("deviceID", "");
		JSONObject response = new JSONObject();

		String sqlSelect = query1.newQuery()
				.select(Config.getTablePrefix()+"client.*")
				.from(Config.getTablePrefix()+"client")
				.where("device_id = '"+lDeviceID+"' and api_id = "+this.apiID+" ")
				.toString();

		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;		
		try 
		{
			conn = this.dataSource.getConnection();
			stmt = conn.prepareStatement(sqlSelect);
			rs = stmt.executeQuery();	

			if(!rs.isBeforeFirst())
			{
			
				String time = Utility.now("yyyy-MM-dd HH:mm:ss");
				String sqlInsert = query1.newQuery()
						.insert()
						.into(Config.getTablePrefix()+"client")
						.fields("(api_id, device_id, last_time, time_create, blocked, active)")
						.values("('"+this.apiID+"', '"+lDeviceID+"', '"+time+"', '"+time+"', 0, 1)")
						.toString();
				
				JdbcUtils.closeResultSet(rs);
				JdbcUtils.closeStatement(stmt);
				stmt = conn.prepareStatement(sqlInsert);
				stmt.execute();	
			}
		}
		catch (SQLException e) 
		{
			logger.error(e.getMessage());
		}
		finally 
		{
			JdbcUtils.closeResultSet(rs);
			JdbcUtils.closeStatement(stmt);
			JdbcUtils.closeConnection(conn);			
		}
		data.put("deviceID", this.deviceID);
		data.put("apiID", this.apiID);
		response.put("command", "register-device");
		response.put("data", data);
		return response;
	}	
	public JSONObject unregisterDevice(String body) throws SQLException, DatabaseTypeException
	{
		JSONObject jo;
		jo = new JSONObject(body);
		return this.unregisterDevice(jo);
	}
	public JSONObject unregisterDevice(JSONObject jo) throws SQLException, DatabaseTypeException
	{
		QueryBuilder query1 = new QueryBuilder(Config.getDatabaseType());
		JSONObject data = jo.optJSONObject("data");
		if(data == null)
		{
			data = new JSONObject();
		}
		String lDeviceID = data.optString("deviceID", "");
		JSONObject response = new JSONObject();

		String sqlDelete = query1.newQuery()
				.delete()
				.from(Config.getTablePrefix()+"client")
				.where("device_id = '"+lDeviceID+"' and api_id = "+this.apiID+" ")
				.toString();

		Connection conn = null;
		PreparedStatement stmt = null;
		try 
		{
			conn = this.dataSource.getConnection();
			stmt = conn.prepareStatement(sqlDelete);
			stmt.execute();	
		}
		catch (SQLException e) 
		{
			logger.error(e.getMessage());
		}
		finally 
		{
			JdbcUtils.closeStatement(stmt);
			JdbcUtils.closeConnection(conn);			
		}
		data.put("deviceID", this.deviceID);
		data.put("apiID", this.apiID);
		response.put("command", "register-device");
		response.put("data", data);
		return response;
	}	
	/**
	 * Insert notification
	 * @param body String contains JSONObject of the notification. Pusher only send one notification message to PushServer but possible to send it to several devices
	 * @return JSONArray of JSONObject contains notification ID and destination device ID
	 * @throws SQLException if any SQL errors
	 * @throws JSONException if any JSON errors
	 * @throws DatabaseTypeException if database type not supported 
	 */
	public JSONObject insert(String body) throws SQLException, JSONException, DatabaseTypeException
	{
		JSONObject jo = new JSONObject(body);
		return this.insert(jo);
	}
	/**
	 * Insert notification
	 * @param jo JSONObject of the notification. Pusher only send one notification message to PushServer but possible to send it to several devices
	 * @return JSONArray of JSONObject contains notification ID and destination device ID
	 * @throws JSONException if any JSON errors
	 * @throws SQLException if any SQL errors
	 * @throws DatabaseTypeException if database type not supported 
	 */
	public JSONObject insert(JSONObject request) throws SQLException, DatabaseTypeException
	{
		JSONObject requestData = request.optJSONObject("data");
		if(requestData == null)
		{
			requestData = new JSONObject();
		}
		QueryBuilder query1 = new QueryBuilder(Config.getDatabaseType());
		int i;
		JSONArray devices = requestData.optJSONArray("deviceIDs");
		JSONArray ja1 = new JSONArray();
		JSONObject jo1;	
		JSONObject dataToSent;		
		
		Connection conn = null;
		PreparedStatement stmt = null;
		try 
		{
			conn = this.dataSource.getConnection();			
			if(devices.length() > 0)
			{
				JSONObject notificationData = requestData.optJSONObject("data");			
				String type = notificationData.optString("type", "");
				String title = notificationData.optString("title", "");
				String subtitle = notificationData.optString("subtitle", "");
				String message = notificationData.optString("message", "");
				String tickerText = notificationData.optString("tickerText", "");
				String clickAction = notificationData.optString("clickAction", "");
				String vibrate = notificationData.optString("vibrate", "");
				String color = notificationData.optString("color", "");
				String sound = notificationData.optString("sound", "");
				String badge = notificationData.optString("badge", "");
				String largeIcon = notificationData.optString("largeIcon", "");
				String smallIcon = notificationData.optString("smallIcon", "");
				String uri = notificationData.optString("uri", "");
				String miscData = notificationData.optString("miscData", "");
				String timeCreate = Utility.now("yyyy-MM-dd HH:mm:ss.SSSSSS");
				String timeGMT = Utility.now("yyyy-MM-dd HH:mm:ss.SSSSSS", "UTC");
				String clientGroupID = notificationData.optString("client_group_id", "");			
				
				vibrate = vibrate.replaceAll("[^\\d.]", " ");
				vibrate = vibrate.replaceAll("\\s+", " ").trim();
				
				dataToSent = new JSONObject();
				dataToSent.put("type", type);
				dataToSent.put("title", title);
				dataToSent.put("subtitle", subtitle);
				dataToSent.put("message", message);
				dataToSent.put("tickerText", tickerText);
				dataToSent.put("uri", uri);
				dataToSent.put("clickAction", clickAction);
				dataToSent.put("color", color);
				dataToSent.put("vibrate", vibrate.split(" "));
				dataToSent.put("sound", sound);
				dataToSent.put("badge", badge);
				dataToSent.put("largeIcon", largeIcon);
				dataToSent.put("smallIcon", smallIcon);
				dataToSent.put("time", timeCreate);
				dataToSent.put("timeGMT", timeGMT);
				dataToSent.put("timeZone", this.timeZoneOffset);
				dataToSent.put("miscData", miscData);
				dataToSent.put("channelID", clientGroupID);
				type = query1.escapeSQL(type);
				color = query1.escapeSQL(color);
				title = query1.escapeSQL(title);
				subtitle = query1.escapeSQL(subtitle);
				message = query1.escapeSQL(message);
				tickerText = query1.escapeSQL(tickerText);
				largeIcon = query1.escapeSQL(largeIcon);
				smallIcon = query1.escapeSQL(smallIcon);
				uri = query1.escapeSQL(uri);
				timeCreate = query1.escapeSQL(timeCreate);
				clickAction = query1.escapeSQL(clickAction);
				miscData = query1.escapeSQL(miscData);
				String sqlCommand = "";
				String lDeviceID = "";
				boolean registered = false;			
				for(i = 0; i < devices.length(); i++)
				{
					lDeviceID = devices.getString(i).trim();	
					registered = Cache.isRegisteredDevice(this.apiID, lDeviceID);
					if(!registered)
					{
						registered = this.checkDevice(this.apiID, lDeviceID);
					}
					lDeviceID = query1.escapeSQL(lDeviceID);
					if(registered)
					{
						jo1 = new JSONObject();
						Cache.registerDevice(this.apiID, lDeviceID);
						this.deviceID = lDeviceID;
						sqlCommand = query1.newQuery()
								.insert()
								.into(Config.getTablePrefix()+"notification")
								.fields("(api_id, client_group_id, device_id, type, title, subtitle, message, ticker_text, uri, click_action, color, vibrate, sound, badge, large_icon, small_icon, misc_data, time_create, time_gmt)")
								.values("("+this.apiID+", "+this.groupID+", '"+lDeviceID+"', '"+type+"', '"+title+"', '"+subtitle+"', '"+message+"', '"+tickerText+"', '"+uri+"', '"+clickAction+"', '"+color+"', '"+vibrate+"', '"+sound+"', '"+badge+"', '"+largeIcon+"', '"+smallIcon+"', '"+miscData+"', '"+timeCreate+"', '"+timeGMT+"')")
								.toString();
						JdbcUtils.closeStatement(stmt);
						stmt = conn.prepareStatement(sqlCommand);
						stmt.execute();	
						
						long notificationID = this.database.getLastAutoIncrement(conn);
						dataToSent.put("id", notificationID);
						List<Device> deviceList = new ArrayList<>();
						int deviceOn = 0;
						try 
						{
							deviceList = Client.get(this.deviceID, this.apiID, this.groupID);
							deviceOn = deviceList.size();
							if(deviceOn > 0)
							{
								MessengerInsert messengerInsert = new MessengerInsert(this.apiID, deviceList, this.groupID, "["+dataToSent.toString()+"]", notificationID, this.dataSource, "message");
								messengerInsert.start();
							}
						} 
						catch (ClientException e) 
						{
							/**
							 * Do nothing
							 */
						}
						jo1.put("id", notificationID);
						jo1.put("deviceID", lDeviceID);
						jo1.put("deviceOn", deviceOn);
						ja1.put(jo1);
					}
				}
			}
		}
		catch (SQLException e) 
		{
			logger.error(e.getMessage());
		}
		finally 
		{
			JdbcUtils.closeStatement(stmt);
			JdbcUtils.closeConnection(conn);			
		}
		JSONObject responseData = new JSONObject();
		responseData.put("notification", devices);
		JSONObject response = new JSONObject();
		response.put("command", "push-notification");
		response.put("data", responseData);
		return response;
	}
	/**
	 * Check whether device is registered on an API ID or not
	 * @param apiID API ID
	 * @param deviceID Device ID
	 * @return true is yes and false if not
	 * @throws SQLException if any SQL errors
	 * @throws DatabaseTypeException if database type not supported 
	 */
	public boolean checkDevice(long apiID, String deviceID) throws SQLException, DatabaseTypeException 
	{
		QueryBuilder query1 = new QueryBuilder(Config.getDatabaseType());	
		String sqlCommand = query1.newQuery() 
			.select("client_id")
			.from(Config.getTablePrefix()+"client")
			.where("api_id = '"+apiID+"' and device_id = '"+deviceID+"' and active = 1 and blocked = 0")
			.toString();
		boolean found = false;
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;		
		try 
		{
			conn = this.dataSource.getConnection();
			stmt = conn.prepareStatement(sqlCommand);
			rs = stmt.executeQuery();	
			if(rs.isBeforeFirst())
			{
				found = true;
			}
		}
		catch (SQLException e) 
		{
			logger.error(e.getMessage());
		}
		finally 
		{
			JdbcUtils.closeResultSet(rs);
			JdbcUtils.closeStatement(stmt);
			JdbcUtils.closeConnection(conn);			
		}
		return found;
	}
	/**
	 * Select offline notification stored on the database with limit.
	 * @param apiID API ID
	 * @param deviceID Device ID
	 * @param groupID Group ID
	 * @param limit Maximum notification selected
	 * @return JSONArray contains notification
	 * @throws SQLException if any SQL errors 
	 * @throws DatabaseTypeException if database type not supported
	 * @throws JSONException if any JSON errors 
	 */
	public JSONArray select(long apiID, String deviceID, long groupID, long limit) throws SQLException, DatabaseTypeException
	{
		this.apiID = apiID;
		this.deviceID = deviceID;
		this.offlineID = new ArrayList<>();
		QueryBuilder query1 = new QueryBuilder(Config.getDatabaseType());	
		query1.newQuery() 
			.select("notification_id, coalesce(type, '') as type, coalesce(title, '') as title, coalesce(subtitle, '') as subtitle, coalesce(message, '') as message, coalesce(uri, '') as uri,  coalesce(click_action, '') as click_action, coalesce(ticker_text, '') as ticker_text, coalesce(color, '0') as color,  coalesce(vibrate, '0') as vibrate, coalesce(sound, '0') as sound, coalesce(badge, '0') as badge, coalesce(large_icon, '') as large_icon, coalesce(small_icon, '') as small_icon, coalesce(misc_data, '') as misc_data, time_create, time_gmt ")
			.from(Config.getTablePrefix()+"notification")
			.where("api_id = "+this.apiID+" and device_id = '"+this.deviceID+"' and client_group_id = "+groupID+" and is_sent = 0")
			.orderBy("notification_id asc");
		if(limit != 0)
		{
			query1.limit(limit)
				.offset(0);
		}		
		String sqlCommand = query1.toString();

		JSONArray ja = new JSONArray();
		JSONObject jo;			
		long notificationID;
		String type = "";
		String title = "";
		String subtitle = "";
		String message = "";
		String tickerText = "";
		String uri = "";
		String clickAction = "";
		String color = "";
		String vibrate = "";
		String sound = "";
		String badge = "";
		String largeIcon = "";
		String smallIcon = "";
		String miscData = "";
		String time = "";
		String timeGMT = "";

		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;		
		try 
		{
			conn = this.dataSource.getConnection();
			stmt = conn.prepareStatement(sqlCommand);
			rs = stmt.executeQuery();	

			if(rs.isBeforeFirst())
			{
				while(rs.next())
				{
					type = rs.getString("type");
					title = rs.getString("title");
					subtitle = rs.getString("subtitle");
					message = rs.getString("message");
					tickerText = rs.getString("ticker_text");
					uri = rs.getString("uri");
					clickAction = rs.getString("click_action");
					color = rs.getString("color");
					vibrate = rs.getString("vibrate");
					sound = rs.getString("sound");
					badge = rs.getString("badge");
					largeIcon = rs.getString("large_icon");
					smallIcon = rs.getString("small_icon");
					miscData = rs.getString("misc_data");
					time = rs.getString("time_create");
					timeGMT = rs.getString("time_gmt");
					notificationID = rs.getLong("notification_id");	
					
					time = time.replace(".000", ".");
					if(time.endsWith("."))
					{
						time = time.substring(0, time.length()-1);
					}				
					jo = new JSONObject();
					jo.put("id", notificationID);
					jo.put("type", type);
					jo.put("title", title);
					jo.put("subtitle", subtitle);
					jo.put("message", message);
					jo.put("tickerText", tickerText);
					jo.put("uri", uri);
					jo.put("clickAction", clickAction);
					jo.put("color", color);
					jo.put("vibrate", vibrate);
					jo.put("sound", sound);
					jo.put("badge", badge);
					jo.put("largeIcon", largeIcon);
					jo.put("smallIcon", smallIcon);
					jo.put("miscData", miscData);
					jo.put("time", time);
					jo.put("timeGMT", timeGMT);
					jo.put("timeZone", this.timeZoneOffset);
					ja.put(jo);
					this.offlineID.add(String.valueOf(notificationID));
				}
			}
		}
		catch (SQLException e) 
		{
			logger.error(e.getMessage());
		}
		finally 
		{
			JdbcUtils.closeResultSet(rs);
			JdbcUtils.closeStatement(stmt);
			JdbcUtils.closeConnection(conn);			
		}
		return ja;
	}
	/**
	 * Select offline notification stored on the database with no limit.
	 * @param apiID API ID
	 * @param deviceID Device ID
	 * @param groupID Group ID
	 * @return JSONArray contains notification
	 * @throws JSONException if any JSON errors
	 * @throws SQLException if any SQL errors 
	 * @throws DatabaseTypeException if database type not supported 
	 */
	public JSONArray select(long apiID, String deviceID, long groupID) throws JSONException, SQLException, DatabaseTypeException
	{
		return this.select(apiID, deviceID, groupID, 0);
	}
	/**
	 * Get offline notification count
	 * @param apiID API ID
	 * @param deviceID Device ID
	 * @param groupID Group ID
	 * @return Offline notification count
	 * @throws SQLException if any SQL errors 
	 * @throws DatabaseTypeException if database type not supported 
	 */
	public long countNotification(long apiID, String deviceID, long groupID) throws SQLException, DatabaseTypeException
	{
		this.apiID = apiID;
		this.deviceID = deviceID;
		this.offlineID = new ArrayList<String>();
		long count = 0;
		QueryBuilder query1 = new QueryBuilder(Config.getDatabaseType());	
		String sqlCommand = query1.newQuery() 
				.select("count(notification_id) as numrows")
				.from(Config.getTablePrefix()+"notification")
				.where("api_id = '"+this.apiID+"' and device_id = '"+this.deviceID+"' and client_group_id = '"+groupID+"' and is_sent = 0")
				.toString();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;		
		try 
		{
			conn = this.dataSource.getConnection();
			stmt = conn.prepareStatement(sqlCommand);
			rs = stmt.executeQuery();	

			if(rs.isBeforeFirst())
			{
				rs.next();
				count = rs.getLong("numrows");
			}
			else
			{
				count = 0;
			}
		}	
		catch (SQLException e) 
		{
			logger.error(e.getMessage());
		}
		finally 
		{
			JdbcUtils.closeResultSet(rs);
			JdbcUtils.closeStatement(stmt);
			JdbcUtils.closeConnection(conn);			
		}
		return count;
	}
	/**
	 * Delete notification stored in the database
	 * @param request String contains JSONArray of pairs of notification ID and device ID. Pusher must send notification ID and device that will be deleted. PushServer only will delete the notification if notification ID, device ID and API ID is match
	 * @return JSONArray contains notification ID and device ID of the deletion
	 * @throws JSONException if any JSON errors
	 * @throws SQLException if any SQL errors
	 * @throws DatabaseTypeException if database type not supported 
	 */
	public JSONObject delete(JSONObject request) throws SQLException, DatabaseTypeException
	{
		JSONObject data = request.optJSONObject("data");
		JSONObject jo;
		JSONArray result = new JSONArray();
		JSONArray tmp;
		int i;
		int j;
		long id;
		String lDeviceID;
		
		JSONArray ids = data.optJSONArray("notification");
		
		j = ids.length();
		for(i = 0; i<j; i++)
		{
			jo = ids.getJSONObject(i);
			id = jo.optLong("id", 0);
			lDeviceID = jo.optString("deviceID", "");
			if(id != 0)
			{
				tmp = this.delete(this.apiID, lDeviceID, this.groupID, id);
				result = this.concatArray(result, tmp);
			}
		}			
		JSONObject responseJSON = new JSONObject();
		JSONObject responseData = new JSONObject();
		responseJSON.put("command", "delete-notifivication");
		responseData.put("notification", result);
		responseJSON.put("data", responseData);
		return responseJSON;
	}
	/**
	 * Delete notification stored in the database
	 * @param apiID API ID
	 * @param deviceID Device ID
	 * @param id Notification ID
	 * @param groupID Group ID
	 * @return JSONArray contains notification ID and device ID of the deletion
	 * @throws JSONException if any JSON errors
	 * @throws SQLException if any SQL errors
	 * @throws DatabaseTypeException if database type not supported 
	 */
	public JSONArray delete(long apiID, String deviceID, long groupID, long id) throws SQLException, DatabaseTypeException
	{
		long[] ids = new long[1];
		ids[0] = id;
		return this.delete(apiID, deviceID, groupID, ids);
	}
	/**
	 * Delete notification stored in the database
	 * @param apiID API ID
	 * @param deviceID Device ID
	 * @param groupID Group ID
	 * @param ids Array long contains notification ID
	 * @return JSONArray contains notification ID and device ID of the deletion
	 * @throws JSONException if any JSON errors
	 * @throws SQLException if any SQL errors
	 * @throws DatabaseTypeException if database type not supported 
	 */
	public JSONArray delete(long apiID, String deviceID, long groupID, long[] ids) throws SQLException, DatabaseTypeException
	{
		JSONArray data = new JSONArray();	
		JSONObject jo;
		int i;
		long j;
		String filterID = "";
		
		Connection conn = null;
		PreparedStatement stmt = null;
		try 
		{
			conn = this.dataSource.getConnection();
			if(ids.length > 0)
			{
				QueryBuilder query1 = new QueryBuilder(Config.getDatabaseType());		
				for(i = 0; i < ids.length; i++)
				{
					j = ids[i];
					if(i > 0)
					{
						filterID += ", ";
					}
					filterID += j;				
					jo = new JSONObject();
					jo.put("deviceID", deviceID);
					jo.put("id", j);
					data.put(jo);
				}
				String sqlCommand = query1.newQuery()
						.delete()
						.from(Config.getTablePrefix()+"notification")
						.where("api_id = '"+apiID+"' and device_id = '"+deviceID+"' and client_group_id = '"+groupID+"' and notification_id in ("+filterID+") ")
						.toString();
				stmt = conn.prepareStatement(sqlCommand);
				MessengerDelete broadcastCloseLoop = new MessengerDelete(this.apiID, this.deviceID, this.groupID, this.requestID, data, this.dataSource, "delete-notification");
				broadcastCloseLoop.start();
			}
		}
		catch (SQLException e) 
		{
			logger.error(e.getMessage());
		}
		finally 
		{
			JdbcUtils.closeStatement(stmt);
			JdbcUtils.closeConnection(conn);			
		}
		return data;
	}
	/**
	 * Concatenate two arrays of JSONArray
	 * @param arr1 First JSONArray 
	 * @param arr2 Second JSONArray
	 * @return JSONArray which is a combination of both inputs
	 * @throws JSONException if any JSON errors
	 */
	public JSONArray concatArray(JSONArray arr1, JSONArray arr2) 
	{
	    JSONArray result = new JSONArray();
	    for (int i = 0; i < arr1.length(); i++) 
	    {
	        result.put(arr1.get(i));
	    }
	    for (int i = 0; i < arr2.length(); i++) 
	    {
	        result.put(arr2.get(i));
	    }
	    return result;
	}
	/**
	 * Insert notification deletion log to be sent to its device
	 * @param apiID API ID
	 * @param groupID Group ID
	 * @param data JSONArray contains device ID and notification ID to be deleted
	 * @throws SQLException if any SQL errors
	 * @throws JSONException if any JSON errors
	 * @throws DatabaseTypeException if database type not supported 
	 */
	public void insertDeletionLog(long apiID, long groupID, JSONArray data) throws SQLException, DatabaseTypeException  
	{
		QueryBuilder query1 = new QueryBuilder(Config.getDatabaseType());
		String sqlCommand = "";
		int i;
		String lDeviceID = null;
		long notificationID = 0;
		JSONObject jo;
		int length = data.length();

		Connection conn = null;
		PreparedStatement stmt = null;
		try 
		{
			conn = this.dataSource.getConnection();
			for(i = 0; i < length; i++)
			{
				jo = data.getJSONObject(i);
				lDeviceID = jo.optString("deviceID", "");
				notificationID = jo.optLong("id", 0);
				lDeviceID = query1.escapeSQL(lDeviceID);
				sqlCommand = query1.newQuery()
						.insert()
						.into(Config.getTablePrefix()+"trash")
						.fields("(api_id, device_id, notification_id, time_delete)")
						.values("("+apiID+", '"+lDeviceID+"', "+notificationID+", "+query1.now(6)+")")
						.toString();
				stmt = conn.prepareStatement(sqlCommand);
				stmt.execute();	
				
				JdbcUtils.closeStatement(stmt);
				JdbcUtils.closeConnection(conn);
			}
		}
		catch (SQLException e) 
		{
			logger.error(e.getMessage());
		}
		finally 
		{
			JdbcUtils.closeStatement(stmt);
			JdbcUtils.closeConnection(conn);			
		}
	}
	/**
	 * Select notification deletion log to be sent to its device with no limit
	 * @param apiID API ID
	 * @param deviceID Device ID
	 * @param groupID Group ID
	 * @return JSONArray contains device ID and notification ID
	 * @throws SQLException if any SQL errors
	 * @throws DatabaseTypeException if database type not supported 
	 */
	public JSONArray selectDeletionLog(long apiID, String deviceID, long groupID) throws SQLException, DatabaseTypeException 
	{
		return this.selectDeletionLog(apiID, deviceID, groupID, 0);
	}
	/**
	 * Select notification deletion log to be sent to its device with limit
	 * @param apiID API ID
	 * @param deviceID Device ID
	 * @param groupID Group ID
	 * @param limit Maximum deletion log selected
	 * @return JSONArray contains device ID and notification ID
	 * @throws JSONException if any JSON errors
	 * @throws SQLException if any SQL errors 
	 * @throws DatabaseTypeException if database type not supported 
	 */
	public JSONArray selectDeletionLog(long apiID, String deviceID, long groupID, long limit) throws SQLException, DatabaseTypeException 
	{
		QueryBuilder query1 = new QueryBuilder(Config.getDatabaseType());
		String sqlCommand = "";
		JSONArray ja = new JSONArray();
		JSONObject jo;
		deviceID = query1.escapeSQL(deviceID);
		query1.newQuery()
				.select("coalesce(device_id, '') as device_id, coalesce(notification_id, 0) as notification_id")
				.from(Config.getTablePrefix()+"trash")
				.where("api_id = '"+apiID+"' and device_id = '"+deviceID+"' and client_group_id = '"+groupID+"' ");
		if(limit > 0)
		{
			query1.limit(Config.getLimitTrash())
			.offset(0);
		}
		sqlCommand = query1.toString();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;		
		try 
		{
			conn = this.dataSource.getConnection();
			stmt = conn.prepareStatement(sqlCommand);
			rs = stmt.executeQuery();	
			if(rs.isBeforeFirst())
			{
				while(rs.next())
				{
					jo = new JSONObject();
					jo.put("deviceID", rs.getString("device_id"));
					jo.put("id", rs.getString("notification_id"));
					ja.put(jo);
				}
			}
		}
		catch (SQLException e) 
		{
			logger.error(e.getMessage());
		}
		finally 
		{
			JdbcUtils.closeResultSet(rs);
			JdbcUtils.closeStatement(stmt);
			JdbcUtils.closeConnection(conn);			
		}
		return ja;
	}
	/**
	 * Count notification deletion log
	 * @param apiID API ID
	 * @param deviceID Device ID
	 * @param groupID Group ID
	 * @return Count the notification deletion log
	 * @throws SQLException if any SQL errors 
	 * @throws DatabaseTypeException if database type not supported 
	 */
	public long countDeletionLog(long apiID, String deviceID, long groupID) throws SQLException, DatabaseTypeException
	{
		this.apiID = apiID;
		this.deviceID = deviceID;
		this.offlineID = new ArrayList<>();
		QueryBuilder query1 = new QueryBuilder(Config.getDatabaseType());	
		String sqlCommand = query1.newQuery() 
				.select("count(trash_id) as numrows")
				.from(Config.getTablePrefix()+"trash")
				.where("api_id = '"+this.apiID+"' and device_id = '"+this.deviceID+"' and client_group_id = '"+groupID+"'")
				.toString();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;		
		long countDeletion = 0;
		try 
		{
			conn = this.dataSource.getConnection();
			stmt = conn.prepareStatement(sqlCommand);
			rs = stmt.executeQuery();	
			if(rs.isBeforeFirst())
			{
				rs.next();
				countDeletion = rs.getLong("numrows");
			}
			else
			{
				countDeletion = 0;
			}
		}
		catch (SQLException e) 
		{
			logger.error(e.getMessage());
		}
		finally 
		{
			JdbcUtils.closeResultSet(rs);
			JdbcUtils.closeStatement(stmt);
			JdbcUtils.closeConnection(conn);			
		}
		return countDeletion;
	}
	/**
	 * Clear notification deletion log
	 * @param apiID API ID
	 * @param deviceID Device ID
	 * @param notificationID Notification ID
	 * @throws SQLException if any SQL errors
	 * @throws DatabaseTypeException if database type not supported 
	 */
	public void clearDeleteLog(long apiID, String deviceID, long notificationID) throws SQLException, DatabaseTypeException 
	{
		QueryBuilder query1 = new QueryBuilder(Config.getDatabaseType());
		deviceID = query1.escapeSQL(deviceID);
		String sqlCommand = "";
		sqlCommand = query1.newQuery()
				.delete()
				.from(Config.getTablePrefix()+"trash")
				.where("api_id = '"+apiID+"' and device_id = '"+deviceID+"' and notification_id = '"+notificationID+"'")
				.toString();
		Connection conn = null;
		PreparedStatement stmt = null;
		try 
		{
			conn = this.dataSource.getConnection();
			stmt = conn.prepareStatement(sqlCommand);
			stmt.execute();	
		}
		catch (SQLException e) 
		{
			logger.error(e.getMessage());
		}
		finally 
		{
			JdbcUtils.closeStatement(stmt);
			JdbcUtils.closeConnection(conn);			
		}
	}
	/**
	 * Clear notification deletion log
	 * @param apiID API ID
	 * @param data JSONArray contains device ID and notification ID to be deleted
	 * @throws SQLException if any SQL errors
	 * @throws DatabaseTypeException if database type not supported 
	 */
	public void clearDeleteLog(long apiID, JSONArray data) throws SQLException, DatabaseTypeException 
	{
		QueryBuilder query1 = new QueryBuilder(Config.getDatabaseType());
		String sqlCommand = "";
		int i;
		String deviceID = null;
		long notificationID = 0;
		JSONObject jo;
		int length = data.length();
		
		Connection conn = null;
		PreparedStatement stmt = null;
		try 
		{
			conn = this.dataSource.getConnection();
			for(i = 0; i < length; i++)
			{
				jo = data.optJSONObject(i);
				deviceID = jo.optString("deviceID", "");
				notificationID = jo.optLong("id", 0);
				sqlCommand = query1.newQuery()
						.delete()
						.from(Config.getTablePrefix()+"trash")
						.where("api_id = '"+this.apiID+"' and device_id = '"+deviceID+"' and notification_id = '"+notificationID+"'")
						.toString();
				stmt = conn.prepareStatement(sqlCommand);
				stmt.execute();	
				
				JdbcUtils.closeStatement(stmt);
				JdbcUtils.closeConnection(conn);
			}	
			
		}
		catch (SQLException e) 
		{
			logger.error(e.getMessage());
		}
		finally 
		{
			JdbcUtils.closeStatement(stmt);
			JdbcUtils.closeConnection(conn);			
		}

	
	}
}
