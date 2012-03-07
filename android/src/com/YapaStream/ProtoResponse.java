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
	private boolean terminate;
	static boolean debug = false;

	public ProtoResponse(BufferedReader i) {
		this.input = i;
		this.audioPort = 0;
		this.videoPort = 0;
		this.pong = false;
		this.terminate=false;
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
	public boolean getTerminate() {
		return this.terminate;
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
					//} else if (this.statusCode == 601) { // ping
					//		this.terminate= true;
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
