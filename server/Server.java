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
public class Server {
	// read from configuration file...	
	static int MAX_CONNECTIONS=100;
	static int _MAX_IP_CONNECTIONS_ = 15; // max connections from one ip (max connections from NAT)
	static int tcpListenerPhonePort = 10083;
	static int tcpListenerViewPort = 554;


	public static void main(String args[]) {	
		int[] udpPort = new int[_MAX_IP_CONNECTIONS_*2]; // phone listening video UDP port to receive 
		//int[] audioUdpPort =  new int[_MAX_IP_CONNECTIONS_]; // phone listening audio UDP port to receive 
		// set two random ports which will listen for audio and video UDP packets from Phone
		SQLDatabase db = new SQLDatabase();
		Random generator = new Random();

		Integer p;
		for (int j=0; j<_MAX_IP_CONNECTIONS_; j++) {
			p = ((Math.abs(generator.nextInt()) % 64510) + 1024);
			udpPort[j*2] =  (p%2==0 ? p : p+1); // between 1025-65535 // odd numbered / AUDIO
			udpPort[j*2+1] = udpPort[j*2]+2; // increase 2 / VIDEO
			//System.out.println("Setting ports: " + udpPort[j*2]  + " and " + udpPort[j*2+1]);
		}
			
		db.removeStreams();
		ConcurrentHashMap<String, PhoneUserS> phoneUsers = new ConcurrentHashMap<String, PhoneUserS>(); // key = username/streamname, value=user_object (PhoneUserS)
		ArrayList<viewUser> viewUsers = new ArrayList(); // key = username/streamname, value=user_object (viewUser)

		//Thread pl = new PhoneListener(phoneUsers, viewUsers, tcpListenerPhonePort, new int[] { audioUdpPort, videoUdpPort } ); // open TCP listener for incoming phone connections
		//Thread vl = new viewListener(phoneUsers, viewUsers, tcpListenerViewPort,  new int[] { audioUdpPort, videoUdpPort }); // open TCP listener for incoming viewer connections
		PhoneListener device = new PhoneListener(phoneUsers, viewUsers, tcpListenerPhonePort, udpPort );
		viewListener decoder = new viewListener(phoneUsers, viewUsers, tcpListenerViewPort,  udpPort );
		Thread pl = device; // open TCP listener for incoming phone connections
		Thread vl = decoder; // open TCP listener for incoming viewer connections
		device.setListenerViewPort(tcpListenerViewPort);
		pl.start();
		vl.start();
	}
}

	
	/*
	 * 
	 *  // need multiple // DatagramSocket udpListenerView = null; // connection to viewing user(rtsp), outgoing
	// need multiple //DatagramSocket udpListenerPhone = null; // connection from phone, incoming 
	// need multiple //Socket tcpSenderView = null;
	//????? // port to send packets to (server->viewer)	// this changes based on response in SETUP client_port
	
	BufferedReader tcpListenerViewReader;
	RtspConnection rtspThreads[] = new RtspConnection[10]; // 10 threads/connections viewers
	PhoneConnection phoneThreads[] = new PhoneConnection[10]; // 10 phones
	
	InetAddress intAdd;
	byte[] internal = new byte[] {(byte)10,(byte)0,(byte)0,(byte)1};

		try {
			intAdd = InetAddress.getByAddress(internal);
			// intAdd = InetAdress.getByName(ip or host);
			
		    udpListenerPhone = new DatagramSocket(udpListenerPhonePort, intAdd); // this should be a seperate class later
		    (phoneThreads[0] = new PhoneConnection(udpListenerPhone, phoneThreads, rtspThreads)).start();
		    tcpListenerView = new ServerSocket(tcpListenerViewPort);
		} catch (UnknownHostException ex) {
			intAdd = null;
	 	} catch (SocketException ex) {
	 		System.out.println("Error binding to socket on port udp:" + udpListenerPhonePort);
	 	} catch (IOException ex) {
	 		System.out.println("Error binding tcp socket on port " + tcpListenerViewPort);
	 	}
	
	while ((tcpListenerView != null) && (udpListenerPhone != null)) {
      try {
    	  tcpSenderView = tcpListenerView.accept();
    	  for (int i=0; i<rtspThreads.length; i++) {
    		  if (rtspThreads[i] == null) {
    			  (rtspThreads[i] = new RtspConnection(tcpSenderView, rtspThreads)).start();
    			  break;
    		  }
    	  }
    	  //System.out.println(packet.getAddress().toString() + ":" + packet.getLength() + ":" + packet.getData().toString());
  	  } catch (IOException ex) {
 	    System.out.println("error receiving data. exiting..");
	    break;
   	  }
   	}
	System.out.println("No connection");
	tcpListenerPhone = null;
	tcpListenerView = null;
    }*/

