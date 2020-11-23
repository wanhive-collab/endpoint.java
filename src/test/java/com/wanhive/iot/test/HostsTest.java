/*
 * HostsTest.java
 * 
 * Testing hosts database implementation
 * 
 * This program is part of Wanhive IoT Platform.
 * 
 * Apache-2.0 License
 * Copyright 2020 Wanhive Systems Private Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package com.wanhive.iot.test;

import java.util.Arrays;

import com.wanhive.iot.protocol.hosts.Hosts;
import com.wanhive.iot.protocol.hosts.WanhiveHosts;

/**
 * @author amit
 *
 */
public class HostsTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try (Hosts hosts = new WanhiveHosts(args[0])) {
			long[] list = hosts.list(0, 5);
			Arrays.sort(list);
			for (long l : list) {
				System.out.println(l);
			}
		} catch (Exception e) {
			System.out.println("Exception: " + e.getMessage());
		}
	}
}
