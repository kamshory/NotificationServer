package com.planetbiru.pushserver.evaluator;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.planetbiru.pushserver.config.Config;
import com.planetbiru.pushserver.notification.NotificationHandler;

/**
 * SocketBreaker
 * @author Kamshory, MT
 *
 */
public class SocketkBreaker extends Thread
{
	/**
	 * Notification handler
	 */
	private NotificationHandler notificationHandler;
	
	private Logger logger = LoggerFactory.getLogger(SocketkBreaker.class);
	/**
	 * Default constructor
	 */
	public SocketkBreaker()
	{
	}
	/**
	 * Constructor with notification handler and timeout
	 * @param notificationHandler Notification handler
	 * @param timeout Timeout
	 */
	public SocketkBreaker(NotificationHandler notificationHandler, long timeout)
	{
		this.notificationHandler = notificationHandler;
	}
	/**
	 * Override run method
	 */
	@Override
	public void run()
	{
		try 
		{
			Thread.sleep(Config.getWaitForAnswer());
			if(!this.notificationHandler.isConnected())
			{
				try 
				{
					this.notificationHandler.setRunning(false);
					this.notificationHandler.getSocket().close();
				} 
				catch (IOException e) 
				{
					logger.error(e.getMessage());
				}
			}
		} 
		catch (InterruptedException e) 
		{
			logger.error(e.getMessage());
		}
	}
}
