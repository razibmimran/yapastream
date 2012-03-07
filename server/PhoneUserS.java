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

// responds to Phone Response to set up UDP connection between Phone and Server [server end]
public class PhoneUserS extends Thread {
	private Process decoder_process;
	private static String rtn = "\r\n";
	private String username;
	private String password;
	private Integer local_audio_port; // server port
	private Integer local_video_port; // server port
	private Integer remote_audio_port; // phone port
	private Integer remote_video_port; // phone port
	private String sessionId;
	private boolean available;
	private boolean verified;
	private ConcurrentHashMap<String, PhoneUserS> phoneUsers;
	private ArrayList <viewUser> subscribedUsers; // users
	Timer jpgOutputTimer;
	Timer timeoutTimer; // n
	Timer keepAliveTimer; // n/3
	private Socket phoneSocket;
	SQLDatabase sqldb;
	String flvPath;
	String jpgPath;
	String serverAddress;
	int timeout;

	public PhoneUserS(ConcurrentHashMap<String, PhoneUserS> pu, List<viewUser> vu, Socket sock, int[] ports) {
		this.available = false;
		this.verified = false;
		this.phoneUsers = pu;
		this.phoneSocket = sock; // tcp socket to phone
		this.subscribedUsers = new ArrayList<viewUser>();
		this.generateSessionId();
		this.local_audio_port = ports[0];
		this.local_video_port = ports[1];
		this.flvPath = "/var/www/sites/yapastream/flv";
		this.jpgPath = "/var/www/sites/yapastream/jpg";
		this.serverAddress = "yapastream.com";
		this.sqldb = new SQLDatabase();
		this.jpgOutputTimer = new Timer();
		this.timeoutTimer = new Timer();
		this.keepAliveTimer = new Timer();
		this.timeout = 30; // seconds
	}
	public void run() {
		// read input sequence, store in variables
		BufferedReader receiver;
		PrintStream sender;
		String response = "";
		OutputStream stdin = null;
		InputStream stderr = null;
		InputStream stdout = null;
		BufferedReader bre = null;
		BufferedReader bro = null;
		try {
			receiver = new BufferedReader(new InputStreamReader(phoneSocket.getInputStream()));
			sender = new PrintStream(phoneSocket.getOutputStream());
			String line = null;
			String inputBlock;
			int nullLine = 0;
			
			while (phoneSocket.isConnected()) {
				if ((this.decoder_process != null)  && (bre == null) && (bro == null)){
				//System.out.println("Process not null! Setting output.");
					stderr = this.decoder_process.getErrorStream ();
					stdout = this.decoder_process.getInputStream ();
					bre = 
						new BufferedReader (new InputStreamReader (stderr));
					bro = 
						new BufferedReader (new InputStreamReader (stdout));
				}
				
				inputBlock = "";
				line = receiver.readLine();
				if (line == null) {
					nullLine++;
					Thread.currentThread().sleep(1000);
				} else {
					nullLine = 0;
				}
				if (nullLine > 10) {
					System.out.println("Phone timed out.");
					break;
				}
				while (line != null) {
					if (line.compareTo("") == 0) break;
					if (inputBlock.compareTo("") == 0) {
						inputBlock = line;
					} else {
						inputBlock = inputBlock + rtn + line;
					}
					line = receiver.readLine();
					System.out.println("line: " + line);
				}
				if (inputBlock != "") System.out.println(phoneSocket.getInetAddress().toString()+ ":Phone:Received: " + inputBlock);
				/*if ((this.decoder_process != null)  && (bre != null) && (bro != null)){
					String l;
					if ((l = bre.readLine()) != null) { System.out.println("[decoder-stderr]: " + l); }
					if ((l = bro.readLine()) != null) { System.out.println("[decoder-stdout]: " + l); }
				}*/
				if (inputBlock != null) response = this.getResponse(inputBlock);
				else response = null;

				if (response != null) {
					sender.println(response);
					System.out.println(phoneSocket.getInetAddress().toString() + ":Phone:Sent: " + response);
				}
			}
			try {
				receiver.close();
			} catch (IOException e) {
				
			}
			sender.close();
			if (!(phoneSocket.isClosed())) phoneSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
   		 return;
   		 
	}
	public String getResponse(String input) {
		BufferedReader lineReader = new BufferedReader(new StringReader(input)); 
		String line;
		StringTokenizer lineToken = null;
		String request = null;
		String response = null;
		
		try {
			// read user line
			line = lineReader.readLine();
			if (line != null) {
				lineToken = new StringTokenizer(line);
			}
			if (lineToken != null) {
				if (lineToken.hasMoreTokens()) {
					request = lineToken.nextToken().toLowerCase().toString();
				}
			}
			// USER user + rtn
			// PASS password + rtn
			if (request.compareTo("user") == 0) { // username sent
				if ((this.username == null) && (lineToken.hasMoreTokens())) {
					String tmpUser = lineToken.nextToken().toLowerCase();
					if (tmpUser.matches("^[a-zA-Z0-9]+$")) {
						this.username = tmpUser;
					} 
					
					// read password line
					line = lineReader.readLine();
					lineToken = new StringTokenizer(line);
					
					if (lineToken.hasMoreTokens()) {

						request = lineToken.nextToken().toLowerCase();
						if ((request.compareTo("pass") == 0) && (lineToken.hasMoreTokens()) && (this.username != null)) { // password sent
							this.password = lineToken.nextToken();
							if (this.verifyUser()) {
								System.out.println("Verified user " + this.username);
								if ((this.local_audio_port != null) && (this.local_video_port != null)) {
									response = "200 OK" + rtn + 
												"Session: " + this.sessionId + rtn + 
												"Port: " + this.local_audio_port + "-" + this.local_video_port + rtn
												+ rtn;
								} else {
									// unable to set receive ports [not currently implemented]
								}
							} else {
								response = "401 Unauthorized" + rtn +
								rtn;
								System.out.println("Unauthorized access attempt by " + this.username);
								
							}
						}
					}
				}
			} else if (request.compareTo("port") == 0) {
				System.out.println("Received PORT");
				String rp;
				if (lineToken.hasMoreTokens()) {
					rp = lineToken.nextToken().toString();
					try {
						this.remote_audio_port = Integer.parseInt(rp.replaceAll("-[0-9]{1,}$", "")); // port the phone will send Audio UDP packets
						this.remote_video_port = Integer.parseInt(rp.replaceAll("^[0-9]{1,}-", "")); // port the phone will send Video UDP packets
					} catch (NumberFormatException ex) {
						this.remote_audio_port = null;
						this.remote_video_port = null;
					}
				}
				response = "200 OK" + rtn +
							"Session: " + this.sessionId + rtn;
				
				System.out.println("Ports are " + this.remote_audio_port + " - " + this.remote_video_port);
			} else if (request.compareTo("report") == 0) {
				// check session id...
				response = "200 OK" + rtn + 
							"Session: " + this.sessionId + rtn + 
							"Port: " + this.local_audio_port + "-" + this.local_video_port + rtn
							+ rtn;
			} else if (request.compareTo("pause") == 0) {
				pause();
				response = "200 OK" + rtn +
				"Session: " + this.sessionId + rtn + rtn;
			} else if (request.compareTo("settings") == 0) { 
				line = lineReader.readLine();
				this.settings(input);
				response = "200 OK" + rtn +
				"Session: " + this.sessionId + rtn + rtn;
			} else if (request.compareTo("play") == 0) {
				play();
				response = "200 OK" + rtn +
							"Session: " + this.sessionId + rtn + rtn;
			} else if (request.compareTo("terminate") == 0) {
				this.terminate();
			} else if (request.compareTo("pong") == 0) {
				System.out.println("Received PONG");
				resetTimeout();
				response = "200 OK" + rtn +
							"Session: " + this.sessionId + rtn + rtn;
			} else {
				System.out.println("Unknown command:" + request);
			}
		} catch (IOException ex) {
		} catch (Exception ex) {
		
		}
		return response;
	}	
	public void resetTimeout() { // received ping command, reset timer
		try {
			this.timeoutTimer.cancel();
		} catch (IllegalStateException ex) {
		
		}
		this.timeoutTimer = null;
		this.timeoutTimer = new Timer();
		this.timeoutTimer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				terminate();
			}
		}, this.timeout*1000, this.timeout*1000);	
	}
		
	public void settings(String input) {
	// update database with privacy settings
//		String[] settingSplit = line.split("[|]");
//		for (int i=0; i<settingSplit.length; i+=3) {
//			if ((settingSplit[i+1].compareTo("privacy") == 0) && (settingSplit[i+2] != null)) {
//				this.sqldb.addSetting(this.username, "privacy", settingSplit[i+2]);
//			}
//		}
		String line;
		BufferedReader lineReader = new BufferedReader(new StringReader(input));
		try {
			line = lineReader.readLine();
		} catch (Exception e) {
			line = null;
		}
		String[] settingSplit;
		//settingsSplit[0] is command/key
		//settingsSplit[1] is value
		while (line != null) {
			settingSplit = line.split(":");
                        if (line.compareTo("") == 0) break;
                        if (settingSplit[0].compareTo("privacy") == 0) {
				if (settingSplit[1] != null) {
                      	           	this.sqldb.addSetting(this.username, "privacy", settingSplit[1]);
				} else {
					this.sqldb.addSetting(this.username, "privacy", "1");
				}
                         } else {
                              // invalid setting
                         }
                	try { 
			        line = lineReader.readLine();
			} catch (Exception e) {
				line = null;
			}
                 }
	}
	public void play() {
		if (this.verified) {
			if ((this.remote_audio_port != null) && (this.remote_video_port != null)) {
				System.out.println("Adding " + this.getPacketInfoStr(this.phoneSocket, this.local_video_port));
				this.phoneUsers.putIfAbsent(this.getPacketInfoStr(this.phoneSocket, this.local_video_port), this);
				this.phoneUsers.putIfAbsent(this.username, this); // to find phoneUser by username [viewUser]
				this.available = true;
				final String userpathFlv = this.flvPath + "/" + this.username + ".flv";
				final String userpathJpg = this.jpgPath + "/" + this.username + ".jpg";
				final String ffmpegPath = "/usr/local/bin/ffmpeg";
				final String decoderCommand[] = new String[] {"/bin/sh", "-c", ffmpegPath + " -i rtsp://" + this.serverAddress + "/" + this.username + " -y -f flv " + userpathFlv};
				//final String decoderCommand[] = new String[] {"/bin/sh", "-c", ffmpegPath + " -i rtsp://" + this.serverAddress + "/" + this.username + " -y -f flv -vcodec copy " + userpathFlv};
				final String jpgOutputCommand[] = new String[] {"/bin/sh", "-c", ffmpegPath + " -i rtsp://" + this.serverAddress + "/" + this.username + " -y -vcodec mjpeg -s 176x144 -f image2 -an -ss 00:00:01 " + userpathJpg};
							
				try {
					//System.out.println(decoderCommand[2]);
					//String decoderCommand[] = new String[] {"/usr/bin/sudo", "-u", "nobody", "/bin/sh", "-c", "/mnt/network/programming/vlc/vlc/linux/cvlc rtsp://" + this.serverAddress + "/" + this.username + " --sout='#transcode{vcodec=FLV1,vb=32,width=352,height=288}:standard{access=file,mux=ffmpeg{mux=flv},dst=" + userpath + "}' --sout-mux-caching 1000"};
					//String decoderCommand[] = new String[] {"/usr/bin/sudo", "-u", "nobody", "/bin/sh", "-c", "/mnt/network/programming/vlc/vlc/linux/cvlc" + " rtsp://" + this.serverAddress + "/" + this.username + " --sout='#transcode{vcodec=raw}:standard{access=file,mux=raw,dst=" + userpath + "}' --sout-transcode-hurry-up --sout-transcode-high-priority --color"};
					//String decoderCommand[] = new String[] {"/usr/bin/sudo", "-u", "nobody", "/bin/sh", "-c", "/mnt/network/programming/vlc/vlc/linux/cvlc" + " rtsp://" + this.serverAddress + "/" + this.username + " --sout='#transcode{vcodec=FLV1,scale=2,qmax=1000,qmin=1000}:standard{access=file,mux=ffmpeg{mux=flv},dst=" + userpath + "}' --sout-transcode-hurry-up --sout-transcode-high-priority --color"};
					//String decoderCommand[] = new String[] {"/usr/bin/sudo", "-u", "nobody", "/bin/sh", "-c", this.cvlc_path + " rtsp://" + this.serverAddress + "/" + this.username + " --sout='#transcode{vcodec=FLV1,scale=2,qmax=1000,qmin=1000}:standard{access=file,mux=ffmpeg{mux=flv},dst=" + userpath + "}' --sout-transcode-hurry-up --sout-transcode-high-priority --color"};
					//String decoderCommand[] = new String[] {"/bin/sh", "-c", "/usr/bin/mencoder rtsp://" + this.serverAddress + "/" + this.username + " -o " + userpath +  " -of lavf -ovc lavc -lavcopts vcodec=flv:vbitrate=500:mbd=2:mv0:trell:v4mv:cbp:last_pred=3"};
					//String decoderCommand[] = new String[] {"/bin/sh", "-c", "/usr/bin/mencoder rtsp://" + this.serverAddress + "/" + this.username + " -o " + userpath +  " -of lavf -ovc lavc -lavcopts vcodec=flv:vbitrate=500:mbd=2:mv0:trell:v4mv:cbp:last_pred=3"};
					//String decoderCommand[] = new String[] {"/bin/sh", "-c", openrtspPath + " -b 2000000 -B 20000 -f 15 -v -c rtsp://" + this.serverAddress + "/" + this.username + " | /usr/bin/ffmpeg -i - -y -f flv -an  " + userpath };
					//String decoderCommand[] = new String[] {"/usr/bin/sudo", "-u", "nobody", "/bin/sh", "-c", this.cvlc_path + " rtsp://" + this.serverAddress + "/" + this.username + " --sout='#transcode{vcodec=h264,venc=x264{profile=baseline,asm,tune=psnr}:standard{access=file,mux=ffmpeg{mux=flv},dst=" + userpath + "}' --sout-transcode-hurry-up --sout-transcode-high-priority --color"};
					//String decoderCommand[] = new String[] {"/usr/bin/sudo", "-u", "nobody", "/bin/sh", "-c", this.cvlc_path + " rtsp://" + this.serverAddress + "/" + this.username + " --sout='#transcode{vcodec=FLV1,scale=2,qmax=1000,qmin=90}:standard{access=file,mux=ffmpeg{mux=flv},dst=" + userpath + "}' --sout-transcode-hurry-up --sout-transcode-high-priority --color"};System.out.println("Attempting to run " + this.cvlc_path);
					this.decoder_process = Runtime.getRuntime().exec(decoderCommand);
					this.sqldb.joinUser(this.username, this.sessionId);
					
					
					this.jpgOutputTimer.scheduleAtFixedRate(new TimerTask() {
						Process jpg_grab_process;
						int retry=0;
						public void run() {
							File flvFile;
							try {
								// copy blank image to user's name
								
								
								jpg_grab_process = Runtime.getRuntime().exec(jpgOutputCommand);//run decoder out to jpg
								flvFile = new File(userpathFlv);
								long flvFilesize = flvFile.length();
								Thread.sleep(8000);
								// check file sizeflvFile = new File(userpathFlv);
								flvFile = new File(userpathFlv);
								long flvFilesizeChange = flvFile.length();
								if (flvFilesize == flvFile.length()) {
								//send post to restart decoder process
									if (decoder_process != null) decoder_process.destroy();
									//if (retry > 5) { // terminate connection? }
									System.out.println("Restarting decoder process.");
									decoder_process = Runtime.getRuntime().exec(decoderCommand);
									retry++;
									
								}
								// if it has not updated restart decoder
								
								// get output of process
								//OutputStream conversion_output = jpg_grab_process.getOutputStream();
								//if stream is unable to receive packets after 8 seconds, send Terminate to phone and partUser
								
								jpg_grab_process.destroy();//terminate previous process if it is still running
							} catch (Exception ex) {
								ex.printStackTrace();
							}
						}
					}, 1000, 15000);
					
					// TCP timeout for stream, drop user if we lose TCP stream
					/*try {
						this.keepAliveTimer.cancel();
						this.timeoutTimer.cancel();
					} catch (Exception e) {
					
					}*/
					System.out.println("Starting timer");
					this.keepAliveTimer.scheduleAtFixedRate(new TimerTask() {
						public void run() {
							PrintStream sender;
							try {
								sender = new PrintStream(phoneSocket.getOutputStream());
								sender.println("100 PING" + rtn + "Session: " + sessionId + rtn + rtn);// sends ping command
								
								System.out.println("Sent PING");
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}, this.timeout*1000/5, this.timeout*1000/5);
					
					this.timeoutTimer.scheduleAtFixedRate(new TimerTask() {
						public void run() {
							terminate();
						}
					}, this.timeout*1000, this.timeout*1000);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				
			} else {// else phone user they have not set up remote video and audio ports, unable to proceed.
				System.out.println("NO VIDEO AUDIO SET UP");
			}
		}  else {// else tell phone user they have not verified, unable to proceed.
			System.out.println("NOT VERIFIED");
		
		}
	}
	public void terminate() {
		PrintStream sender;
		try {
			sender = new PrintStream(phoneSocket.getOutputStream());
			sender.println("410 TERMINATE");
			sender.close();
			sender = null;
		} catch (Exception e) {
		}
		// phone terminated connection, send respones to all viewers 410 Gone and Terminate session
		this.sqldb.partUser(this.username, this.sessionId);
		for (int i=0; i<subscribedUsers.size(); i++) {
			subscribedUsers.get(i).getResponse().setEndReason(410);
		}
		this.phoneUsers.remove(this.getPacketInfoStr(this.phoneSocket, this.local_audio_port));
		this.phoneUsers.remove(this.getPacketInfoStr(this.phoneSocket, this.local_video_port));
		this.phoneUsers.remove(this.username); // remove from Phone Users list
		this.decoder_process.destroy(); // kill conversion process
		this.jpgOutputTimer.cancel();
		this.keepAliveTimer.cancel();
		this.timeoutTimer.cancel();
		try {
			this.phoneSocket.close(); // close tcp connection
		} catch (IOException ex) {
			
		}
		this.available = false;
	}
	public void pause() {//skipping, currently unnecessary
		this.available = false;
	}
	@Override
	public void destroy() {
		terminate();
		super.destroy();
	}
	public Integer getLocalVideoPort() {
		return this.local_video_port;
	}
	public Integer getLocalAudioPort() {
		return this.local_audio_port;
	}
	public void generateSessionId() {
	    Random generator = new Random();
	    this.sessionId = Integer.toString(Math.abs(generator.nextInt()));
	   // return this.sessionId;
	}
	public boolean subscribe(viewUser obj) {
		this.subscribedUsers.add(obj);
		return true;
	}
	public boolean unsubscribe(viewUser obj) {
		this.subscribedUsers.remove(obj);
		this.subscribedUsers.trimToSize();
		return true;
	}
	
	public boolean verifyUser() {
		if (this.sqldb.verifyUser(this.username, this.password)) {// valid user, sql verified
			this.verified = true;
			return true; 
		} else { // invalid user
			this.verified = false;
			return false;
		}
	}
	
	public ArrayList getSubscribedUsers() {
		return this.subscribedUsers;
	}
	public String getPacketInfoStr(Socket s, int port) { // returns string for phoneUsers hashmap, to add, lookup and delete
		return s.getInetAddress().toString();// + "-" + port;
	}

}

// from play command
//
// !! IGNORE BELOW !!
// (only begin play once clients have connected? add as option later?)
// open UDP audio port
// spawn thread for UDP audio connection (subscribedUsers); only one connection need be accepted
// listens on this.local_audio_port

//audioReceiverThread = new PhoneForward(subscribedUsers, this.local_audio_port, 1).start(); 

// open UDP video port
// spawn thread for UDP video connection (subscribedUsers); only one connection need be accepted
// listens on this.local_video_port

//videoReceiverThread = new PhoneForward(subscribedUsers, this.local_video_port, 2).start();
//public boolean setReceivePorts() {

//this.local_audio_port = ap;
//	this.local_video_port = vp;
//	return true;
//}
/*	public boolean setReceivePorts() {
// only try certain number of times to prevent DoS?
do { 
	Random generator = new Random();
	Integer p = ((Math.abs(generator.nextInt()) % 64510) + 1024);
	this.local_audio_port =  (p%2==0 ? p : p+1); // between 1025-65535 // odd numbered
	this.local_video_port = this.local_audio_port+2; // increase 2
} while (!(udpListenPhonePort.contains(this.local_audio_port) && (!(udpListenPhonePort.contains(this.local_video_port)))));
this.udpListenPhonePort.add(this.local_audio_port);
this.udpListenPhonePort.add(this.local_video_port);
return true;
}*/
