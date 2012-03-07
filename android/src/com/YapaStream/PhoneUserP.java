/*Copyright (c) 2002-2011 "Yapastream,"
Yapastream [http://yapastream.com]

This file is part of Yapastream.

Yapastream is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
package com.YapaStream;

import java.util.*;
import java.security.*;

//import java.lang.*;
//import java.io.*;
//import java.net.*;
//import java.util.concurrent.*;

//import android.database.sqlite.SQLiteDatabase;
//import android.database.sqlite.SQLiteException;

// phone to rtp server [phone end]
public class PhoneUserP  {
	private String username;
	private String password;
	private int local_audio_port;
	private int local_video_port;
	private int remote_audio_port;
	private int remote_video_port;
	private String sessionId;
	private boolean verified;
	private String deviceServer;
	private int devicePort;

	public PhoneUserP() {
		this.verified = false;
		this.setClientPorts();
	}

	public int getLocalAudioPort() {
		return this.local_audio_port;
	}

	public int getLocalVideoPort() {
		return this.local_video_port;
	}

	public int getRemoteAudioPort() {
		return this.remote_audio_port;
	}

	public int getRemoteVideoPort() {
		return this.remote_video_port;
	}

	public String getSessionId() {
		return this.sessionId;
	}

	public String getUsername() {
		return this.username;
	}

	public String getPassword() {
		
		return this.password;
	}
	public int getServerPort() {
		return this.devicePort;
	}
	public String getServerAddress() {
		return this.deviceServer;
	}
	public boolean isVerified() {
		return this.verified;
	}

	public void setLocalVideoPort(int p) {
		this.local_video_port = p;
	}

	public void setLocalAudioPort(int p) {
		this.local_audio_port = p;
	}

	public void setRemoteVideoPort(int p) {
		this.remote_video_port = p;
	}

	public void setRemoteAudioPort(int p) {
		this.remote_audio_port = p;
	}

	public void setVerified(boolean v) {
		this.verified = v;
	}
	public void setServerAddress(String address) {
		this.deviceServer = address;
	}
	public void setServerPort(int port) {
		this.devicePort = port;
	}
	public void setUsername(String u) {
		this.username = u;
	}

	public void setPassword(String p) {
		MessageDigest md;
        	try {
            		md= MessageDigest.getInstance("SHA-512");
             		md.update(p.getBytes());
            		byte[] mb = md.digest();
            		String out = "";
            		for (int i = 0; i < mb.length; i++) {
                		byte temp = mb[i];
                		String s = Integer.toHexString(new Byte(temp));
                		while (s.length() < 2) {
                    			s = "0" + s;
                		}
                		s = s.substring(s.length() - 2);
                		out += s;
            		}
			this.password = out;
        	} catch (NoSuchAlgorithmException e) {
			this.password = null;
        	}
	}

	public void setSessionId(String s) {
		this.sessionId = s;
	}
	public void setClientPorts() {
		Random generator = new Random();
		Integer p;
		p = ((Math.abs(generator.nextInt()) % 64510) + 1024);
		this.local_audio_port = (p % 2 == 0 ? p : p + 1); // between 1025-65535
															// // odd numbered
		this.local_video_port = this.local_audio_port + 2; // increase 2
	}
}
