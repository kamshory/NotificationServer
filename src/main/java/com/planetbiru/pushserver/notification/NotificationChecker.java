package com.planetbiru.pushserver.notification;

import java.io.IOException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.planetbiru.pushserver.database.DatabaseTypeException;

/**
 * Notification checker
 * @author Kamshory, MT
 *
 */
public class NotificationChecker extends Thread
{
	/**
	 * Client socket
	 */
	@SuppressWarnings("unused")
	private Socket socket = new Socket();
	/**
	 * Interval
	 */
	private long interval = 3600000;
	/**
	 * Notification handler. The object is carried when constructor is invoked
	 */
	private NotificationHandler notificationHandler;
	private Logger logger = LoggerFactory.getLogger(NotificationChecker.class);
	/**
	 * Default constructor
	 */
	public NotificationChecker()
	{		
	}
	/**
	 * Constructor with client socket, NotificationHandler object and interval
	 * @param socket Client Socket
	 * @param notificationHandler NotificationHandler object
	 * @param interval Interval
	 */
	public NotificationChecker(Socket socket, NotificationHandler notificationHandler, long interval)
	{
		this.socket = socket;
		this.interval = interval;
		this.notificationHandler = notificationHandler;
	}
	/**
	 * Override run method
	 */
	public void run()
	{
		while(this.notificationHandler.isRunning())
		{
			try 
			{
				this.notificationHandler.downloadLastNotification();
				this.notificationHandler.downloadLastDeleteLog();
			} 
			catch (IOException e) 
			{
				logger.error(e.getMessage());
			}
			catch (SQLException e) 
			{
				logger.error(e.getMessage());
			} 
			catch (DatabaseTypeException e) 
			{
				logger.error(e.getMessage());
			} 
			catch (InvalidKeyException e) 
			{
				logger.error(e.getMessage());
			} 
			catch (NoSuchAlgorithmException e) 
			{
				logger.error(e.getMessage());
			} 
			catch (NoSuchPaddingException e) 
			{
				logger.error(e.getMessage());
			} 
			catch (IllegalBlockSizeException e) 
			{
				logger.error(e.getMessage());
			} 
			catch(JSONException e)
			{
				logger.error(e.getMessage());
			}
			catch(BadPaddingException e) 
			{
				logger.error(e.getMessage());
			}
			try 
			{
				Thread.sleep(this.interval);
			} 
			catch (InterruptedException e) 
			{
				logger.error(e.getMessage());
			}
		}
	}	
}