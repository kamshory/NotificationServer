package com.planetbiru.pushserver.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <strong>Client</strong> is object contains Device
 * @author Kamshory, MT
 *
 */
public class Client 
{
	/**
	 * Client list
	 */
	private static Map<String, ArrayList<Device>> deviceList = new HashMap<>();	
	/**
	 * Default constructor
	 */
	private Client()
	{
		
	}
	public static void add(String deviceID, long apiID, long groupID, Device device, long requestID)
	{
		device.setIndex(requestID);
		String key = deviceID+"_"+apiID+"_"+groupID;
		ArrayList<Device> deviceList;
		deviceList = Client.deviceList.get(key);
		if(deviceList == null)
		{
			deviceList = new ArrayList<>();
		}
		deviceList.add(device);
		Client.deviceList.put(key, deviceList);
	}
	/**
	 * Get client list
	 * @param deviceID Device ID
	 * @param apiID API ID
	 * @param groupID Group ID
	 * @return Array list contains device
	 * @throws ClientException if any errors
	 */
	public static List<Device> get(String deviceID, long apiID, long groupID) throws ClientException
	{
		List<Device> deviceList;
		String key = deviceID+"_"+apiID+"_"+groupID;
		if(Client.deviceList.containsKey(key))
		{
			deviceList = Client.deviceList.get(key);
		}
		else
		{
			throw new ClientException("Device not found");
		}
		return deviceList;

	}
	/**
	 * Get client list
	 * @param deviceID Device ID
	 * @param apiID API ID
	 * @param groupID Group ID
	 * @return Array list contains device
	 * @throws ClientException if any errors
	 */
	public static Device get(String deviceID, long apiID, long groupID, long requestID) throws ClientException
	{
		ArrayList<Device> deviceList;
		String key = deviceID+"_"+apiID+"_"+groupID;
		if(Client.deviceList.containsKey(key))
		{
			deviceList = Client.deviceList.get(key);
			for(Device device : deviceList)
			{
				if(device.getIndex() == requestID)
				{
					return device;
				}
			}
			throw new ClientException("Device not found");
		}
		else
		{
			throw new ClientException("Device not found");
		}
	}
	/**
	 * Remove device from list
	 * @param deviceID Device ID
	 * @param apiID API ID
	 * @param groupID Group ID
	 * @param requestID Request ID
	 */
	public static void remove(String deviceID, long apiID, long groupID, long requestID)
	{
		String key = deviceID+"_"+apiID+"_"+groupID;
		ArrayList<Device> deviceList;
		deviceList = Client.deviceList.get(key);
		Device device;
		if(deviceList != null)
		{
			int size = deviceList.size();
			int i;
			for(i = 0; i < size; i++)
			{
				device = deviceList.get(i);
				if(device.getIndex() == requestID)
				{
					deviceList.remove(i);
					break;
				}
			}
		}
	}
}
