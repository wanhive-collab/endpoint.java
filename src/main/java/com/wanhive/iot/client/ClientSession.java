/*
 * ClientSession.java
 * 
 * Management of end-point pairs
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
package com.wanhive.iot.client;

/**
 * Paired end points management
 * 
 * @author amit
 *
 */
public interface ClientSession {
	/**
	 * Set local identity
	 * 
	 * @param identity Unique identity of the local end point
	 */
	void setLocalIdentity(long identity);

	/**
	 * Returns the local end point's identity
	 * 
	 * @return Local end point's identity
	 */
	long getLocalIdentity();

	/**
	 * Set identity of the remote end point
	 * 
	 * @param identity Unique identity of the remote end point
	 */
	void setRemoteIdentity(long identity);

	/**
	 * Returns remote end point's identity
	 * 
	 * @return Remote end point's identity
	 */
	long getRemoteIdentity();

	/**
	 * Associates given value with the given key, replaces on conflict
	 * 
	 * @param key   Key with which the given value will be associated
	 * @param value Value which will be associated with the given key
	 * @throws Exception
	 */
	void setProperty(Object key, Object value);

	/**
	 * Returns value associated with the given key, null if not found
	 * 
	 * @param key Key whose association will be returned
	 * @return Value associated with the given key
	 * @throws Exception
	 */
	Object getProperty(Object key);
}
