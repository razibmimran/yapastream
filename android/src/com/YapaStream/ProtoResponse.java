package com.YapaStream;

import java.io.*;
import java.util.*;

import android.util.Log;

// reads response from server

public class ProtoResponse {
	private BufferedReader input;
	private int statusCode;
	private String sessionId;
	private int audioPort;
	private int videoPort;
	private boolean pong;
	static boolean debug = false;
	
	public ProtoResponse(BufferedReader i) {
		this.input = i;
		this.audioPort = 0;
		this.videoPort = 0;
		this.pong = false;
		this.parseInput();
	}

	public int getStatusCode() {
		return this.statusCode;
	}

	public int getAudioPort() {
		return this.audioPort;
	}

	public int getVideoPort() {
		return this.videoPort;
	}

	public String getSessionId() {
		return this.sessionId;
	}

	public boolean getPong() {
		return this.pong;
	}
	private void parseInput() {
		String line;
		StringTokenizer lineToken = null;
		try {
			line = this.input.readLine();
		} catch (IOException ex) {
			line = "";
		}

		if (line != null) {
			lineToken = new StringTokenizer(line);
		} else {
			return;
		}
		if (lineToken.hasMoreTokens()) {
			try {
				this.statusCode = Integer.parseInt(lineToken.nextToken()); // request
																			// type
			} catch (NumberFormatException ex) {
				this.statusCode = 0;
				return;
			}
		} else {
			return;
		}
		String describe = null;
		try {
			line = this.input.readLine();
		} catch (IOException ex) {
			line = null;
		}

		if (line != null) {
			lineToken = new StringTokenizer(line);
		}
		while ((line != null) && (line.length() > 0)) {
			Log.v("S", "read line: " + line);
			if (lineToken != null) {
				if (lineToken.hasMoreTokens()) {
					describe = lineToken.nextToken().toLowerCase();
					Log.v("S", "got describe: " + describe);
					
					if (this.statusCode == 100) { // ping
						this.pong= true;
						Log.d("S", "Received PING");
					} else if (describe.compareTo("session:") == 0) {
						if (lineToken.hasMoreTokens()) {
							this.sessionId = lineToken.nextToken();
							
							Log.v("S", "Got session:" + this.sessionId);
						} else {
							this.sessionId = "";
						}
					} else if (describe.compareTo("port:") == 0) {
						if (lineToken.hasMoreTokens()) {
							String tmpPort;
							tmpPort = lineToken.nextToken();
							try {
								this.audioPort = Integer.parseInt(tmpPort
										.replaceAll("-[0-9]{1,}$", ""));
								this.videoPort = Integer.parseInt(tmpPort
										.replaceAll("[0-9]{1,}-", ""));
							} catch (NumberFormatException ex) {
								this.audioPort = 0;
								this.videoPort = 0;
							}
							Log.v("S",
									"Settings ports to: "
											+ Integer.toString(this.videoPort));
						} else {
							this.audioPort = 0;
							this.videoPort = 0;
						}
					}
					
				}
				try {
					line = this.input.readLine();
				} catch (IOException ex) {
					line = null;
				}
				if (line != null)
					lineToken = new StringTokenizer(line);

			}
		}

	}
}
