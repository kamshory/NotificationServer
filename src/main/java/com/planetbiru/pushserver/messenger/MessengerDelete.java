package com.planetbiru.pushserver.messenger;

import java.io.IOException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.sql.DataSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.planetbiru.pushserver.client.Client;
import com.planetbiru.pushserver.client.ClientException;
import com.planetbiru.pushserver.client.Device;
import com.planetbiru.pushserver.config.Config;
import com.planetbiru.pushserver.database.DatabaseTypeException;
import com.planetbiru.pushserver.notification.Notification;
import com.planetbiru.pushserver.utility.Encryption;
import com.planetbiru.pushserver.utility.SocketIO;
/**
 * BroadcastDelete
 * @author Kamshory, MT
 *
 */
public class MessengerDelete extends Thread
{
	/**
	 * Data
	 */
	private JSONArray data;
	/**
	 * Device ID
	 */
	private String deviceID;
	/**
	 * API ID
	 */
	private long apiID = 0;
	/**
	 * Group ID
	 */
	private long groupID = 0;
	/**
	 * Notification ID
	 */
	private long notificationID = 0;
	/**
	 * Command
	 */
	private String command = "";
	private long requestID = 0;
	private DataSource dataSource;
	
	private Logger logger = LoggerFactory.getLogger(MessengerDelete.class);
	
	public JSONArray getData() {
		return data;
	}
	public void setData(JSONArray data) {
		this.data = data;
	}
	public String getDeviceID() {
		return deviceID;
	}
	public void setDeviceID(String deviceID) {
		this.deviceID = deviceID;
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
	public String getCommand() {
		return command;
	}
	public void setCommand(String command) {
		this.command = command;
	}
	public long getRequestID() {
		return requestID;
	}
	public void setRequestID(long requestID) {
		this.requestID = requestID;
	}
	/**
	 * Constructor with API ID, device ID, group ID, data, notification ID and database object.
	 * <p>After create an object, caller can invoke start method to run this thread and notification deletion history will sent to the destination device asynchronously.</p>
	 * @param apiID API ID
	 * @param deviceID Device ID
	 * @param requestID Request ID
	 * @param groupID Group ID
	 * @param data Data to send to the client
	 * @param database1 Database 1
	 * @param database2 Database 2
	 * @param database3 Database 3
	 * @param command Command
	 */
	public MessengerDelete(long apiID, String deviceID, long groupID, long requestID, JSONArray data, DataSource dataSource, String command)
	{
		this.apiID = apiID;
		this.data = data;
		this.requestID = requestID;
		this.dataSource = dataSource;
		if(command.equals(""))
		{
			command = "delete-notification";
		}
		this.command = command;		
	}
	/**
	 * Insert delete history
	 * @param notification Notification
	 * @param apiID API ID
	 * @param groupID Group ID
	 * @param data Data to sent to the client
	 * @throws JSONException if any JSON errors
	 * @throws SQLException if any SQL errors
	 * @throws DatabaseTypeException if database type not supported 
	 */
	public void inserDeleteHistory(Notification notification, long apiID, long groupID, JSONArray data) throws JSONException, SQLException, DatabaseTypeException
	{
		notification.insertDeletionLog(this.apiID, groupID, this.data);
	}
	/**
	 * Override run method
	 */
	public void run()
	{
		Notification notification = new Notification(this.dataSource);
		try 
		{
			this.inserDeleteHistory(notification, this.apiID, this.groupID, this.data);
		} 
		catch (JSONException | SQLException | DatabaseTypeException e) 
		{
			logger.error(e.getMessage());
		} 
		try
		{
			List<Device> deviceList = new ArrayList<>();
			try 
			{
				int length = this.data.length();
				int j;
				JSONObject jo;
				String lDeviceID = "";
				long lNotificationID = 0;
				for(j = 0; j < length; j++)
				{
					jo = this.data.optJSONObject(j);
					lDeviceID = jo.optString("deviceID", "");
					lNotificationID = jo.optLong("id", 0);
					try
					{
						deviceList = Client.get(lDeviceID, this.apiID, this.groupID);
						Iterator<Device> iterator = deviceList.iterator();
						Device device;
						Socket socket;
						SocketIO socketIO = null;
						boolean success = false;
						int i = 0;
						String stringNotification = "";
						while(iterator.hasNext())
						{
							device = iterator.next();
							if(device != null)
							{
								if(device.isActive())
								{
									socket = device.getSocket();
									socketIO = new SocketIO(socket);
									try
									{
										socketIO.resetRequestHeader();
										socketIO.addRequestHeader("Content-Type", "application/json");
										socketIO.addRequestHeader("Command", this.command);
										stringNotification = this.data.toString();
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
										if(success && i == 0)
										{
											notification.clearDeleteLog(this.apiID, lDeviceID, lNotificationID);
											i++;
										}
									}
									catch(IOException e)
									{
										device.setActive(false);
										Client.remove(lDeviceID, this.apiID, this.groupID, this.requestID);
										logger.error(e.getMessage());
									}
								}
								else
								{
									Client.remove(lDeviceID, this.apiID, this.groupID, this.requestID);
								}
							}
						}
					}
					catch(ClientException e)
					{
						Client.remove(lDeviceID, this.apiID, this.groupID, this.requestID);
					}
				}			
			} 
			catch (Exception e) 
			{
				logger.error(e.getMessage());
			}
		}
		catch(JSONException e)
		{
			logger.error(e.getMessage());
		}
	}		
}