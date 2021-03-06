/*
 * Protocol.java
 * 
 * The wanhive protocol implementation
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

package com.wanhive.iot.protocol;

import java.net.ProtocolException;

import com.wanhive.iot.protocol.bean.IdentificationResponse;
import com.wanhive.iot.protocol.bean.MessageContext;

/**
 * The wanhive protocol implementation
 * 
 * @author amit
 *
 */
public class Protocol {
	private static final String BAD_REQUEST = "Invalid request";
	private static final String BAD_RESPONSE = "Invalid response or request denied";
	private short sequenceNumber;
	private byte session;

	/**
	 * Verifies message's context
	 * 
	 * @param message   The message to check
	 * @param command   The expected command classifier
	 * @param qualifier The expected command qualifier
	 * @return true if the message matched the context, false otherwise
	 */
	public static boolean checkContext(Message message, byte command, byte qualifier) {
		return (message.getCommand() == command && message.getQualifier() == qualifier);
	}

	/**
	 * Verifies message's context
	 * 
	 * @param message   The message to check
	 * @param command   The expected command classifier
	 * @param qualifier The expected command qualifier
	 * @param status    The expected status code
	 * @return true if the message matched the context, false otherwise
	 */
	public static boolean checkContext(Message message, byte command, byte qualifier, byte status) {
		return checkContext(message, command, qualifier) && (message.getStatus() == status);
	}

	/**
	 * Verifies message's context
	 * 
	 * @param message The message to check
	 * @param ctx     The MessageContext object
	 * @return true if the message matched the context, false otherwise
	 */
	public static boolean checkContext(Message message, MessageContext ctx) {
		return checkContext(message, ctx.getCommand(), ctx.getQualifier(), ctx.getStatus());
	}

	/**
	 * Increments the counter and returns the next sequence number
	 * 
	 * @return The next sequence number
	 */
	public short nextSequenceNumber() {
		++sequenceNumber;
		if (sequenceNumber <= 0) {
			sequenceNumber = 1;
		}
		return sequenceNumber;
	}

	/**
	 * Sets the sequence number
	 * 
	 * @param sequenceNumber The sequence number counter will be set to this value
	 */
	public void setSequenceNumber(short sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	/**
	 * Returns the current sequence number
	 * 
	 * @return The current sequence number value
	 */
	public short getSequenceNumber() {
		return sequenceNumber;
	}

	/**
	 * Sets the session identifier
	 * 
	 * @param session The session identifier will be set to this value
	 */
	public void setSession(byte session) {
		this.session = session;
	}

	/**
	 * Returns the session identifier
	 * 
	 * @return The current session identifier value
	 */
	public byte getSession() {
		return session;
	}

	/**
	 * The default constructor
	 */
	public Protocol() {
		sequenceNumber = 0;
		session = 0;
	}

	// -----------------------------------------------------------------
	/**
	 * Creates an identification request
	 * 
	 * @param uid   Local client's identity
	 * @param nonce The public ephemeral value generated by the client
	 * @return A new identification request
	 */
	public Message createIdentificationRequest(long uid, final byte[] nonce) {
		if (nonce == null || nonce.length == 0 || nonce.length > Message.PAYLOAD_SIZE) {
			throw new IllegalArgumentException(BAD_REQUEST);
		} else {
			Message message = new Message();
			message.prepareHeader(uid, 0, (short) (Message.HEADER_SIZE + nonce.length), nextSequenceNumber(),
					getSession(), RequestContext.IDENTIFY);
			message.setBlob(0, nonce);
			return message;
		}
	}

	/**
	 * Processes an identification response
	 * 
	 * @param message The identification response
	 * @return An IdentificationResponse object
	 * @throws ProtocolException Request denied or invalid response
	 */
	public IdentificationResponse processIdentificationResponse(Message message) throws ProtocolException {
		if (!checkContext(message, ResponseContext.IDENTIFY) || message.getSource() != 0) {
			throw new ProtocolException(BAD_RESPONSE);
		} else if (message.getLength() <= Message.HEADER_SIZE + 4) {
			throw new ProtocolException(BAD_RESPONSE);
		} else {
			short saltLength = message.getShort(0);
			short nonceLength = message.getShort(2);
			if (saltLength <= 0 || nonceLength <= 0 || (saltLength + nonceLength + 4) > Message.PAYLOAD_SIZE) {
				throw new ProtocolException(BAD_RESPONSE);
			}

			IdentificationResponse response = new IdentificationResponse();
			response.setSalt(message.getBlob(4, saltLength));
			response.setNonce(message.getBlob(4 + saltLength, nonceLength));
			return response;
		}
	}

	/**
	 * Creates an authentication request
	 * 
	 * @param proof Client's proof of identity
	 * @return A new authentication request
	 */
	public Message createAuthenticationRequest(final byte[] proof) {
		if (proof == null || proof.length == 0 || proof.length > Message.PAYLOAD_SIZE) {
			throw new IllegalArgumentException(BAD_REQUEST);
		} else {
			Message message = new Message();
			message.prepareHeader(0, 0, (short) (Message.HEADER_SIZE + proof.length), nextSequenceNumber(),
					getSession(), RequestContext.AUTHENTICATE);
			message.setBlob(0, proof);
			return message;
		}
	}

	/**
	 * Processes an authentication response
	 * 
	 * @param message The authentication response
	 * @return Remote hosts's proof of identity
	 * @throws ProtocolException Request denied or invalid response
	 */
	public byte[] processAuthenticationResponse(Message message) throws ProtocolException {
		short msgLen = message.getLength();
		if (!checkContext(message, ResponseContext.AUTHENTICATE) || message.getSource() != 0) {
			throw new ProtocolException(BAD_RESPONSE);
		} else if (msgLen <= Message.HEADER_SIZE) {
			throw new ProtocolException(BAD_RESPONSE);
		} else {
			return message.getBlob(0, msgLen - Message.HEADER_SIZE);
		}
	}

	// -----------------------------------------------------------------
	/**
	 * Creates a registration request
	 * 
	 * @param uid Client's identity
	 * @param hc  The session key
	 * @return A new registration request
	 */
	public Message createRegisterRequest(long uid, byte[] hc) {
		short length = Message.HEADER_SIZE;
		Message message = new Message();
		if (hc != null) {
			message.setBlob(0, hc);
			length += (short) hc.length;
		}
		message.prepareHeader(uid, 0, length, nextSequenceNumber(), getSession(), RequestContext.REGISTER);
		return message;
	}

	/**
	 * Processes a registration response
	 * 
	 * @param message The registration response
	 * @return true if the request was accepted by the remote host
	 * @throws ProtocolException Request denied or invalid response
	 */
	public boolean processRegisterResponse(Message message) throws ProtocolException {
		if (!checkContext(message, ResponseContext.REGISTER) || message.getSource() != 0) {
			throw new ProtocolException(BAD_RESPONSE);
		} else if (message.getLength() != Message.HEADER_SIZE) {
			throw new ProtocolException(BAD_RESPONSE);
		} else {
			return true;
		}
	}

	/**
	 * Creates a Session Key request
	 * 
	 * @param hc 64-bit nonce generated by the client
	 * @return A new session key request
	 */
	public Message createGetKeyRequest(byte[] hc) {
		short length = Message.HEADER_SIZE;
		Message message = new Message();
		if (hc != null) {
			message.setBlob(0, hc);
			length += (short) hc.length;
		}
		message.prepareHeader(0, 0, length, nextSequenceNumber(), getSession(), RequestContext.GETKEY);
		return message;
	}

	/**
	 * Processes a session key response
	 * 
	 * @param message The session key response
	 * @return 64-bytes of the session key returned by the host
	 * @throws ProtocolException Request denied or invalid response
	 */
	public byte[] processGetKeyResponse(Message message) throws ProtocolException {
		short msgLen = message.getLength();
		if (!checkContext(message, ResponseContext.GETKEY) || message.getSource() != 0) {
			throw new ProtocolException(BAD_RESPONSE);
		} else if (msgLen <= Message.HEADER_SIZE) {
			throw new ProtocolException(BAD_RESPONSE);
		} else if (msgLen == (Message.HEADER_SIZE + 64)) {
			return message.getBlob(0, 64);
		} else if (msgLen == (Message.HEADER_SIZE + 128)) {
			return message.getBlob(64, 64);
		} else {
			throw new ProtocolException(BAD_RESPONSE);
		}
	}

	// -----------------------------------------------------------------
	/**
	 * Creates a bootstrap request
	 * 
	 * @param uid Client's identity
	 * @return A new bootstrap request
	 */
	public Message createFindRootRequest(long uid) {
		Message message = new Message();
		message.prepareHeader(0, 0, (short) (Message.HEADER_SIZE + 8), nextSequenceNumber(), getSession(),
				RequestContext.FINDROOT);
		message.setLong(0, uid);
		return message;
	}

	/**
	 * Processes a bootstrap response
	 * 
	 * @param message The bootstrap response
	 * @return Root host's identity
	 * @throws ProtocolException Request denied or invalid response
	 */
	public long processFindRootResponse(Message message) throws ProtocolException {
		if (!checkContext(message, ResponseContext.FINDROOT) || message.getSource() != 0) {
			throw new ProtocolException(BAD_RESPONSE);
		} else if (message.getLength() != (Message.HEADER_SIZE + 16)) {
			throw new ProtocolException(BAD_RESPONSE);
		} else {
			return message.getLong(8);
		}
	}

	// -----------------------------------------------------------------
	/**
	 * Creates a publish request
	 * 
	 * @param topic   The topic identifier
	 * @param payload The bytes of data to be published
	 * @return A new publish request
	 */
	public Message createPublishRequest(byte topic, byte[] payload) {
		Message message = new Message();
		message.prepareHeader(0, 0, (short) (Message.HEADER_SIZE + (payload == null ? 0 : payload.length)),
				nextSequenceNumber(), topic, RequestContext.PUBLISH);
		message.setBlob(0, payload);
		return message;
	}

	/**
	 * Creates a subscription request
	 * 
	 * @param topic The topic identifier
	 * @return A new subscription request
	 */
	public Message createSubscribeRequest(byte topic) {
		Message message = new Message();
		message.prepareHeader(0, 0, (short) Message.HEADER_SIZE, nextSequenceNumber(), topic, RequestContext.SUBSCRIBE);
		return message;
	}

	/**
	 * Processes a subscription response
	 * 
	 * @param message The subscription response
	 * @return The topic identifier
	 * @throws ProtocolException Request denied or invalid response
	 */
	public byte processSubscribeResponse(Message message) throws ProtocolException {
		if (!checkContext(message, ResponseContext.SUBSCRIBE) || message.getSource() != 0) {
			throw new ProtocolException(BAD_RESPONSE);
		} else if (message.getLength() != Message.HEADER_SIZE) {
			throw new ProtocolException(BAD_RESPONSE);
		} else {
			return message.getSession();
		}
	}

	/**
	 * Creates an unsubscription request
	 * 
	 * @param topic The topic to unsubscribe from
	 * @return A new unsubscription request
	 */
	public Message createUnsubscribeRequest(byte topic) {
		Message message = new Message();
		message.prepareHeader(0, 0, (short) Message.HEADER_SIZE, nextSequenceNumber(), topic,
				RequestContext.UNSUBSCRIBE);
		return message;
	}

	/**
	 * Processes an unsubscription response
	 * 
	 * @param message The response to an unsubscription request
	 * @return The topic identifier
	 * @throws ProtocolException Request denied or invalid response
	 */
	public byte processUnsubscribeResponse(Message message) throws ProtocolException {
		if (!checkContext(message, ResponseContext.UNSUBSCRIBE) || message.getSource() != 0) {
			throw new ProtocolException(BAD_RESPONSE);
		} else if (message.getLength() != Message.HEADER_SIZE) {
			throw new ProtocolException(BAD_RESPONSE);
		} else {
			return message.getSession();
		}
	}
}
