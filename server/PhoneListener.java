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
import java.lang.*;
import java.util.concurrent.*;

// 1 Thread
// open TCP listener for incoming phone connections
// Spawns 2 Threads for listening on UDP ports (audio/video) [PhoneForward]
// Spwans a new thread on each TCP connection [PhoneUserS]
public class PhoneListener extends Thread {
	
	private ArrayList<viewUser> viewUsers;
	private ConcurrentHashMap<String, PhoneUserS> phoneUsers;
	//private int videoUdpPort; // phone listening video UDP port to receive 
	//private int audioUdpPort; // phone listening audio UDP port to receive 
	private int[] udpPort; 
	private int _MAX_IP_CONNECTIONS_ = 15;
	private PhoneForward[] audioPhoneForward = new PhoneForward[_MAX_IP_CONNECTIONS_];
	private PhoneForward[] videoPhoneForward = new PhoneForward[_MAX_IP_CONNECTIONS_];
	private Thread[] audioPhoneThread = new Thread[_MAX_IP_CONNECTIONS_];
	private Thread[] videoPhoneThread = new Thread[_MAX_IP_CONNECTIONS_];
	private int tcpListenerPhonePort;
	private int tcpListenerViewPort;
	public PhoneListener(ConcurrentHashMap<String, PhoneUserS> pu, ArrayList<viewUser> vu, int lport, int[] avports) {
		this.viewUsers = vu;
		this.phoneUsers = pu; 
		this.tcpListenerPhonePort = lport;
		//this.setServerPorts();
		//this.audioUdpPort = avports[0];
		//this.videoUdpPort = avports[1];
		this.udpPort = avports;
		for (int j=0; j<_MAX_IP_CONNECTIONS_; j++) {
			this.audioPhoneForward[j] = new PhoneForward(this.phoneUsers, 1, avports[j*2]);  // opens UDP ports for Audio
			this.audioPhoneThread[j] = new Thread(audioPhoneForward[j]);
			this.videoPhoneForward[j] = new PhoneForward(this.phoneUsers, 2, avports[j*2+1]); // opens UDP ports for Video
			this.videoPhoneThread[j] = new Thread(videoPhoneForward[j]);
		}
		if (!audioPhoneThread[0].isAlive()) audioPhoneThread[0].start();
		if (!videoPhoneThread[0].isAlive()) videoPhoneThread[0].start();
	}
   	public void run() {
   		ServerSocket tcpListenerPhone;
   		try {
   			tcpListenerPhone = new ServerSocket(tcpListenerPhonePort);// open TCP listen port for Phone -> Server (10084)
   		} catch (IOException ex) {
   			System.out.println("Error binding Phone TCP listening socket on port " + tcpListenerPhonePort);
   			tcpListenerPhone = null;
   		}
   		while (true) {
   		// if Phone client connects
   		// create PhoneUser object
   			if (tcpListenerPhone != null) {
	   			try {
		   			// create tcp thread
		   			Socket sock = tcpListenerPhone.accept(); // used to control playback and verify phone user
		   			System.out.println("Received connection.");
					
					int udpPortSelection=0;
					while (udpPortSelection < _MAX_IP_CONNECTIONS_) {

						if ((this.phoneUsers.contains(getPacketInfoStr(sock, this.udpPort[udpPortSelection*2]))) || (this.phoneUsers.contains(getPacketInfoStr(sock, this.udpPort[udpPortSelection*2+1])))) {// go to next port...
							udpPortSelection++;
						} else {
							break;
						}
					}
					if (udpPortSelection > _MAX_IP_CONNECTIONS_) { // too many connections from this ip.  
					
					
					} else {
						if (!audioPhoneThread[udpPortSelection].isAlive()) audioPhoneThread[udpPortSelection].start();
						if (!videoPhoneThread[udpPortSelection].isAlive()) videoPhoneThread[udpPortSelection].start();
						Thread t = new PhoneUserS(phoneUsers, viewUsers, sock, (new int[] { this.udpPort[udpPortSelection*2], this.udpPort[udpPortSelection*2+1] }));
		   			
						t.start(); // open Phone User Thread
						System.out.println("started thread");
					}
	   			} catch (Exception ex) {
	   				System.out.println("Socket error from Phone Connection");
	   			}
   			}
   		}
   	}
	public void setListenerViewPort(int port) {
		this.tcpListenerViewPort = port;
	}
	// returns the two ports audio and video which we are listening for UDP packets on
	public String getPacketInfoStr(Socket s, int port) { // returns string for phoneUsers hashmap, to add, lookup and delete
		return s.getInetAddress().toString() + "-" + port;
	}
}
