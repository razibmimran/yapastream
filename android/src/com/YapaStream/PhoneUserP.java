package com.YapaStream;

import java.util.*;
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
		this.password = p;
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
