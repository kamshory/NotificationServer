package com.planetbiru.pushserver.messenger;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.support.JdbcUtils;

import com.planetbiru.pushserver.client.Client;
import com.planetbiru.pushserver.client.Device;
import com.planetbiru.pushserver.config.Config;
import com.planetbiru.pushserver.database.Database;
import com.planetbiru.pushserver.database.QueryBuilder;
import com.planetbiru.pushserver.database.DatabaseTypeException;
import com.planetbiru.pushserver.utility.Encryption;
import com.planetbiru.pushserver.utility.SocketIO;

/**
 * MessagngerInsert is class to deliver the notification to its destination device. It will search the device from the Device object. If the device is found, it will send notification to its socket and mark it as "sent". If the device not found, it will not mark the notification as "sent". 
 * @author Kamshory, MT
 *
 */
public class MessengerInsert extends Thread
{
	/**
	 * Data to be sent
	 */
	private String data;
	/**
	 * API ID
	 */
	private long apiID = 0;
	/**
	 * Group ID
	 */
	private long groupID = 0;
	/**
	 * Notification
	 */
	private long notificationID = 0;
	/**
	 * Primary Database object
	 */
	private Database database1;
	/**
	 * Secondary Database object
	 */
	private Database database2;
	/**
	 * Tertiary Database object
	 */
	private Database database3;
	/**
	 * Command
	 */
	private String command = "";
	
	private Logger logger = LoggerFactory.getLogger(MessengerInsert.class);
	
	public String getData() {
		return data;
	}
	public void setData(String data) {
		this.data = data;
	}
	public long getApiID() {
		return apiID;
	}
	public void setApiID(long apiID) {
		this.apiID = apiID;
	}
	public long getGroupID() {
		return groupID;
	}
	public void setGroupID(long groupID) {
		this.groupID = groupID;
	}
	public long getNotificationID() {
		return notificationID;
	}
	public void setNotificationID(long notificationID) {
		this.notificationID = notificationID;
	}
	public Database getDatabase1() {
		return database1;
	}
	public void setDatabase1(Database database1) {
		this.database1 = database1;
	}
	public Database getDatabase2() {
		return database2;
	}
	public void setDatabase2(Database database2) {
		this.database2 = database2;
	}
	public Database getDatabase3() {
		return database3;
	}
	public void setDatabase3(Database database3) {
		this.database3 = database3;
	}
	public String getCommand() {
		return command;
	}
	public void setCommand(String command) {
		this.command = command;
	}
	public List<Device> getDeviceList() {
		return deviceList;
	}
	public void setDeviceList(List<Device> deviceList) {
		this.deviceList = deviceList;
	}
	private List<Device> deviceList = new ArrayList<>();
	/**
	 * Constructor with API ID, device list, group ID, data, notification ID and database object.
	 * <p>After create an object, caller can invoke start method to run this thread and notification will sent to the destination device asynchronously.</p>
	 * @param apiID API ID
	 * @param deviceList Device list
	 * @param groupID Group ID
	 * @param data Data to send to the client
	 * @param notificationID Notification ID
	 * @param database1 Primary Database object
	 * @param database2 Secondary Database object
	 * @param database3 Tertiary Database object
	 * @param command Command
	 */
	private DataSource dataSource;
	public MessengerInsert(long apiID, List<Device> deviceList, long groupID, String data, long notificationID, DataSource dataSource, String command)
	{
		this.dataSource = dataSource;
		this.deviceList  = deviceList;
		this.apiID = apiID;
		this.groupID = groupID;
		this.data = data;
		this.notificationID = notificationID;
		if(command.equals(""))
		{
			command = "message";
		}
		this.command  = command;
	}
	/**
	 * Override run method
	 */
	@Override
	public void run()
	{
		Device device;
		String deviceID = "";
		Socket socket;
		int idx = 0;
		boolean success = false;
		Iterator<Device> iterator = this.deviceList.iterator();
		SocketIO socketIO = null;
		String stringNotification = "";
		while(iterator.hasNext())
		{
			if(iterator.hasNext())
			{
				device = iterator.next();
				if(device != null)
				{
					deviceID = device.getDeviceID();
					if(device.isActive())
					{
						socket = device.getSocket();
						socketIO = new SocketIO(socket);
						try
						{
							socketIO.resetRequestHeader();
							socketIO.addRequestHeader("Content-Type", "application/json");
							socketIO.addRequestHeader("Command", "notification");
							stringNotification = this.data;
							if(Config.isContentSecure())
							{
								String tmp = stringNotification;
								Encryption encryption;
								try 
								{
									encryption = new Encryption(device.getKey()+device.getHashPasswordClient());
									stringNotification = encryption.encrypt(tmp, true);
									socketIO.addRequestHeader("Content-Secure", "yes");
								} 
								catch (InvalidKeyException | IllegalArgumentException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) 
								{
									logger.error(e.getMessage());
								} 
							}
							success = socketIO.write(stringNotification);
							if(success && idx == 0 && this.notificationID != 0)
							{
								this.markAsSent(this.notificationID);
								idx++;
							}
							if(!success)
							{
								device.setActive(false);
							}
						}
						catch(SocketException e)
						{
							device.setActive(false);
							Client.remove(deviceID, this.apiID, this.groupID, device.getRequestID());
							logger.error(e.getMessage());
						} 
						catch(IOException e)
						{
							device.setActive(false);
							Client.remove(deviceID, this.apiID, this.groupID, device.getRequestID());
							logger.error(e.getMessage());
						} 
						catch (SQLException | DatabaseTypeException e) 
						{
							logger.error(e.getMessage());
						} 
					}
					else
					{
						Client.remove(deviceID, this.apiID, this.groupID, device.getRequestID());
					}
				}
			}
		}
	}
	/**
	 * Mark notification as "sent"
	 * @param notificationID Notification ID
	 * @throws SQLException if any SQL errors
	 * @throws DatabaseTypeException if database type is not supported
	 */
	public void markAsSent(long notificationID) throws SQLException, DatabaseTypeException 
	{
		QueryBuilder query1 = new QueryBuilder(Config.getDatabaseType());
		String sqlCommand = query1.newQuery()
				.update(Config.getTablePrefix()+"notification")
				.set("is_sent = 1, time_sent = now()")
				.where("notification_id = "+notificationID)
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
}
