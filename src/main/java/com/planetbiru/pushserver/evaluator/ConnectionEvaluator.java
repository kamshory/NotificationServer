package com.planetbiru.pushserver.evaluator;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.planetbiru.pushserver.notification.NotificationHandler;
/**
 * ClientEvaluator to evaluate the connection
 * @author Kamshory, MT
 *
 */
public class ConnectionEvaluator extends Thread
{
	/**
	 * Timeout
	 */
	private long timeout = 3600000;
	
	/**
	 * Notification handler
	 */
	private NotificationHandler notificationHandler;
	
	private Logger logger = LoggerFactory.getLogger(ConnectionEvaluator.class);
	
	/**
	 * Default constructor
	 */
	public ConnectionEvaluator()
	{
		
	}
	/**
	 * Constructor with client socket, notification handler and interval
	 * @param notificationHandler Notification handler
	 * @param interval Interval to evaluate client socket
	 */
	public ConnectionEvaluator(NotificationHandler notificationHandler, long interval)
	{
		this.notificationHandler = notificationHandler;
		this.timeout = interval;
	}
	/**
	 * Override run method
	 */
	@Override
	public void run()
	{
		try 
		{
			Thread.sleep(this.timeout);
		} 
		catch (InterruptedException e) 
		{
			logger.error(e.getMessage());
		}
		try 
		{
			this.notificationHandler.sendQuestion();
		} 
		catch (IOException | JSONException | NoSuchAlgorithmException | NullPointerException | IllegalArgumentException e) 
		{
			logger.error(e.getMessage());
		}
	}
	
}
