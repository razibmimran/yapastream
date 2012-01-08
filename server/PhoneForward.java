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

// forwarding happens here
public class PhoneForward extends Thread {
	private byte[] buf = new byte[1500];
	private DatagramSocket streamSocket;
	private DatagramPacket packet = new DatagramPacket(buf, buf.length);
	private Integer port;
	private InetAddress intAdd;
	Integer streamType; // 1=audio, 2=video 
	ConcurrentHashMap<String, PhoneUserS> phoneUsers;
	public PhoneForward(ConcurrentHashMap<String, PhoneUserS> pu, Integer av, Integer p) {
		this.phoneUsers = pu;
		this.port = p; // port to receive and send from
		this.streamType = av;
		try {
			this.intAdd = InetAddress.getByName("0.0.0.0"); // listen on all interfaces
		} catch (Exception ex) {
			this.intAdd = null;
		}
		
	}
	public void run() {
		try {
			streamSocket = new DatagramSocket(this.port, 
					intAdd);		// bind to udp port this.port  InetAddress.getByName(
			System.out.println("Listening on " + intAdd.toString() + ":" + this.port);
		} catch (Exception ex) {
			streamSocket = null;
		}
		PhoneUserS phone_user_tmp; // temp variable used in while loop
		ArrayList <viewUser> subscribedUsers; // temp variable used in while loop
		while (streamSocket != null) {
			try {
				streamSocket.receive(packet);
			} catch (IOException ex) {
				System.out.println("############### Exception packet = null");
				packet = null;
			}
			
			if (packet != null) {
				//System.out.print(".");
				phone_user_tmp = this.phoneUsers.get(this.getPacketInfoStr(packet));
				//System.out.println("####### Number of users:" + this.phoneUsers.size());
				if (phone_user_tmp != null) {
				
					subscribedUsers = phone_user_tmp.getSubscribedUsers();
					if (subscribedUsers != null) {
						for (int i=0; i<subscribedUsers.size(); i++) {
							if (subscribedUsers.get(i).isAvailable()) {
								if (this.streamType == 1) {
									packet.setPort(subscribedUsers.get(i).getResponse().getClientAudioUdpPort()); // set remote port
								} else if (this.streamType == 2) {
									//System.out.println("Setting port: "+ subscribedUsers.get(i).getResponse().getClientVideoUdpPort());
									packet.setPort(subscribedUsers.get(i).getResponse().getClientVideoUdpPort()); // set remote port
								}
								//System.out.println("Setting IP destination: " + subscribedUsers.get(i).getSocket().getInetAddress());
								packet.setAddress(subscribedUsers.get(i).getSocket().getInetAddress()); // set address based on TCP connection, could be changed to be a different address if necessary
								//System.out.println("Set address to " + subscribedUsers.get(i).getSocket().getInetAddress().toString());
								try {
									streamSocket.send(packet);
								} catch (IOException ex) {
									// skip it
								}
							}
						}
					} // else if (phone_user_tmp == null) then user not found
				}
			}
		}
	}
	public String getPacketInfoStr(DatagramPacket p) { // returns string for phoneUsers hashmap, to add, lookup and delete
		return p.getAddress().toString();// + "-" + p.getPort();
	}
}
