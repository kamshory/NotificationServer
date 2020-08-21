package com.planetbiru.pushserver;

import static org.junit.Assert.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.planetbiru.pushserver.client.Device;

@SpringBootTest
class PushServerApplicationTests {

	@Test
	void contextLoads() {
		Device client = new Device();
		assertNull(client.getDeviceID());
	}

}
