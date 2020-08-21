package com.planetbiru.pushserver;

import java.io.IOException;
import java.net.ServerSocket;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.planetbiru.pushserver.notification.NotificationHandler;

public class NotificationServer extends Thread {
	private long requestID = 1;
	private DataSource dataSource;
	private Logger logger = LoggerFactory.getLogger(NotificationServer.class);
	int port;
	public NotificationServer(DataSource dataSource, int port) {
		this.dataSource = dataSource;
		this.port = port;
	}
	@Override
	public void run()
	{
		ServerSocket serverSocket = null;
		try 
		{
		    serverSocket = new ServerSocket(port);		    
	        do 
	        {
	        	NotificationHandler handler;
	        	handler = new NotificationHandler(serverSocket.accept(), this.requestID, this.dataSource);
	        	handler.start(); 
	        	this.requestID++;
	        }
	        while(true);
			
		} 
		catch (IOException e) 
		{
			logger.error(e.getMessage());
		}
		finally 
		{
			if(serverSocket != null)
			{
				try 
				{
					serverSocket.close();
				} 
				catch (IOException e) 
				{
					logger.error(e.getMessage());
				}
			}
		}
	}
}
