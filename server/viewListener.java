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

public class viewListener extends Thread {
	private ArrayList<viewUser> viewUsers;
	private ConcurrentHashMap<String, PhoneUserS> phoneUsers;
	private int tcpListenerViewPort;
	private int[] audioVideoPorts;
	private int _MAX_IP_CONNECTIONS_ = 15;
	
	public viewListener(ConcurrentHashMap<String, PhoneUserS> pu, ArrayList<viewUser> vu, int port, int[] avports) {
		this.viewUsers = vu;
		this.phoneUsers = pu;
		this.tcpListenerViewPort = port;
		this.audioVideoPorts = avports;
	}
	public void run() {
		ServerSocket tcpListenerView;
		try {
			tcpListenerView = new ServerSocket(this.tcpListenerViewPort);// open TCP listen port for Viewer -> Server (1554) 554 is default as root/admin
		} catch (IOException ex) {
			tcpListenerView = null;
			System.out.println("Error binding RTSP TCP listening socket on port " + this.tcpListenerViewPort);
		}
		// if viewer connects spawn new thread with hashMap passed
		Socket sock = null;
		Thread t;
		while (tcpListenerView != null) {
			try {
				sock = tcpListenerView.accept();
				t = new viewUser(this.phoneUsers, this.viewUsers, sock,  this.audioVideoPorts);
				t.start();
			} catch (Exception ex) {
				ex.printStackTrace();
			}		
		}
	}
}
	

