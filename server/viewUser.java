/*
Copyright (c) 2002-2011 "Yapastream,"
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
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

// no need to synchronize phoneUsers here, thread should be destroyed and recreated on reconnection
public class viewUser extends Thread {
	private DatagramSocket audioViewSocket;
	private DatagramSocket videoViewSocket;
	private Socket viewSocket; // tcp
	private String username;
	private RtspResponse response;
	private boolean available = false;
	private BufferedReader receiver = null;
	private PrintStream sender = null;
	private ConcurrentHashMap<String, PhoneUserS> phoneUsers;
	private ArrayList<viewUser> viewUsers; // used only to updated list on disconnections/termination
	private PhoneUserS subscribedPhoneUser;
	
	public viewUser(ConcurrentHashMap<String, PhoneUserS> pu, ArrayList<viewUser> vu, Socket sock, int[] avports) {
		this.viewSocket = sock;
		this.response = new RtspResponse();
		this.response.setServerUdpPorts(avports);
		this.response.setServerIp(sock.getLocalAddress().toString().replace("/", ""));
		this.available = true;
		this.phoneUsers = pu;
		this.viewUsers = vu;
		this.subscribedPhoneUser = null;
		
		System.out.println("Running connection.");

	}
	public String getUsername() {
		return this.username;
	}
	public void setUsername(String u) {
		this.username = u;
	}
	public Socket getSocket() {
		return this.viewSocket;
	}
	public RtspResponse getResponse() {
		return this.response;
	}
	public boolean isAvailable() {
		if (viewSocket == null) this.available = false;
		return this.available;
	}
	public void run() {
		String resp = "";
		try {
			receiver = new BufferedReader(new InputStreamReader(viewSocket.getInputStream()));
			sender = new PrintStream(viewSocket.getOutputStream());
			String line = null;
			String inputBlock;
			Integer endReason;
			int nullLine = 0;
			while (this.viewSocket.isConnected()) {
				// read input from TCP RTSP socket
				inputBlock = "";
				line = receiver.readLine();
				if (line == null) {
					nullLine++;
					Thread.currentThread().sleep(1000);
				} else {
					nullLine = 0;
				}
				if (nullLine > 10) {
					System.out.println("Viewer timed out.");
					break;
				}
				while ((line != null) && this.viewSocket.isConnected()) {
					
					if (line.compareTo("") == 0) break;
					if (inputBlock.compareTo("") == 0) {
						inputBlock = line;
					} else {
						inputBlock = inputBlock + "\r\n" + line;
					}
					line = receiver.readLine();
				}
				if (inputBlock != "") System.out.println(viewSocket.getInetAddress().toString()+ ":Received: " + inputBlock);

				// if phone user is not currently playing send play command
				// wait for rtp packets from phone to server before responding OK to viewer
				// if no rtp packets in 8 -seconds [this should be standard protocol and monitored] [on each packet arrival set lastArrival = current seconds, time checks current seconds and lastArrival, if > 8, do below]
					// send TERMINATE response to Phone
					// send respose 452 conference not found to RTSP viewers
				if (this.subscribedPhoneUser == null) {
					if (this.response.getRequestedStream() != null) {
					//	System.out.println("Requesting Stream: " + response.getRequestedStream());
						if (this.phoneUsers.containsKey(response.getRequestedStream())) {// search for requested stream/find "user" in phoneUsers ConcurrentHashMap
							this.subscribedPhoneUser = phoneUsers.get(response.getRequestedStream());
							this.subscribedPhoneUser.subscribe(this); // subscribe to user(viewers list for forwarding packets)
							this.response.setServerAudioUdpPort(this.subscribedPhoneUser.getLocalAudioPort());
							this.response.setServerVideoUdpPort(this.subscribedPhoneUser.getLocalVideoPort());
							// add viewer to viewUsers ConcurrentHashMap
							this.viewUsers.add(this);
						} else {
							// rtsp response stream not found
							this.response.setEndReason(452);
							
						}
					}
				}
				if (this.response.isPlaying()) { //  phone still has live stream to web server
					
				}
				
				
				// send response to user
				resp = this.response.getResponse(inputBlock);
				if (resp != null) {
					sender.print(resp);
					System.out.println(this.viewSocket.getInetAddress().toString() + ":Sent: " + resp);
				}
				if (this.response.isTerminated()) break;
			}
			System.out.println("Closing connection with viewuser");
			receiver.close();
			sender.close();
			this.viewSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	// add itself to PhoneConnection of subscribed stream
		receiver = null;
		sender = null;
		this.viewSocket = null;
	}
	public void terminate() {
		// remove from viewUsers list
		// unsubscribe from phone user
		// trim viewuser list
	}
}



/*
//Connecting viewer to rtsp server
class RtspConnection extends Thread {

	private BufferedReader receiver = null;
	private PrintStream sender = null;
	private Socket clientSocket = null;
	private RtspConnectiono connections[];
	private RtspResponse userResponse;
	public RtspConnection(Socket tcpSenderView, RtspConnection[] rtspThreads) {
		this.clientSocket = tcpSenderView;
		this.connections  = rtspThreads;
		userResponse = new RtspResponse();
	}

	public void run() {
		String response = "";
		try {
			receiver = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			sender = new PrintStream(clientSocket.getOutputStream());
			String line = null;
			String inputBlock;
			
			while (clientSocket.isConnected()) {
				inputBlock = "";
				line = receiver.readLine();
				while (line != null) {
					if (line.compareTo("") == 0) break;
					if (inputBlock.compareTo("") == 0) {
						inputBlock = line;
					} else {
						inputBlock = inputBlock + "\r\n" + line;
					}
					line = receiver.readLine();
				}
				if (inputBlock != "") System.out.println(clientSocket.getInetAddress().toString()+ ":Received: " + inputBlock);
				
				response = userResponse.getResponse(inputBlock);
				if (response != null) {
					sender.print(response);
					System.out.println(clientSocket.getInetAddress().toString() + ":Sent: " + response);
				}
			}
			receiver.close();
			sender.close();
			clientSocket.close();
		} catch (IOException e) {
		}
	// add itself to PhoneConnection of subscribed stream
		receiver = null;
		sender = null;
		clientSocket = null;
	}
	public Socket getClientSocket () {
		return clientSocket;
	}
	public RtspResponse getUserResponse() {
		return userResponse;
	}
	
}*/

