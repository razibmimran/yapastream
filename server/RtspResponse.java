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
import java.text.*;
import java.security.SecureRandom;
import java.math.BigInteger;

// parse and response to RTSP protocol
public class RtspResponse
{
	private static String capabilities = "DESCRIBE, SETUP, TEARDOWN, PLAY, PAUSE, OPTIONS";
	private int sequenceNumber = 0;
	private int sessionId;
	private static String rtn = "\r\n";
	private boolean setupComplete;
	private boolean playing;
	private Integer clientTcpPort;
	private Integer clientVideoUdpPort;
	private Integer clientAudioUdpPort;
	private Integer serverVideoUdpPort;
	private Integer serverAudioUdpPort;
	private int[] serverUdpPorts;
	private Integer serverserverUdpPortUdpPort;
	private Integer serverTcpPort;
	private String url;
	private String requestedStream;
	private boolean terminated;
	private Integer endReason;
	private String serverIp;
	
	public RtspResponse() {
		this.generateSessionId();
		this.setupComplete = false;
		this.playing = false;
		this.clientTcpPort = 0;
		this.clientAudioUdpPort = 0;
		this.clientVideoUdpPort = 0;
		this.serverAudioUdpPort = 0; //9566
		this.serverVideoUdpPort = 0; //9566
		this.serverTcpPort = 0; // 9567
		this.url = null;
		this.terminated = false;
	}
	public String getResponse(String input) {
		BufferedReader lineReader = new BufferedReader(new StringReader(input)); 
		String line;
		StringTokenizer lineToken = null;
		String request;
		String response = null;
		String protocol;
		
		try {
			line = lineReader.readLine();
		} catch (IOException ex) {
			line = "";
			System.out.println("Unable to assign line");
		}
		if (line != null) {
			lineToken = new StringTokenizer(line);
		} else {
			return null;
		}
		if (lineToken.hasMoreTokens()) {
			request = lineToken.nextToken().toUpperCase(); // request type
		} else {
			return null;
		}
		if (lineToken.hasMoreTokens()) {
			this.url = lineToken.nextToken(); // parse URL
		} else {
			return null;
		}
		if (lineToken.hasMoreTokens()) {
			protocol = lineToken.nextToken(); // not sure we care?
		} else {
			return null;
		}
		try {
			line = lineReader.readLine();
		} catch (IOException ex) {
			return null;
		}
		
		if (request.compareTo("OPTIONS") == 0) {
			response = this.options(input);
		} else if (request.compareTo("DESCRIBE") == 0) {
			response = this.describe(input);
		} else if (request.compareTo("SETUP") == 0) {
			response = this.setup(input);
		} else if (request.compareTo("PLAY") == 0) {
			response = this.play(input);
		} else if (request.compareTo("PAUSE") == 0) {
			response = this.pause(input);
		} else if (request.compareTo("RECORD") == 0) {
			response = Response(551) + rtn; // operation not supported
		} else if (request.compareTo("TEARDOWN") == 0) {
			response = this.teardown(input);
		}

		request = null;
		line = null;
		return response;
	}
	public String describe(String input) {
		// input: DESCRIBE rtsp://host/stream.sdp RTSP/1.0
		// 	  CSeq: sequence#\r\n
		// 	  User-Agent: WebStream  \r\n
		//	  Accept: application/sdp\r\n\r\n
		BufferedReader lineReader = new BufferedReader(new StringReader(input)); 
		String response;
		StringTokenizer lineToken = null;
		String line;
		
		try {
			line = lineReader.readLine();
		} catch (IOException ex) {
			line = null;
		}
		
		if (line != null) lineToken = new StringTokenizer(line);
		String describe = null;
		while (line != null) {
			if (lineToken != null) {
				if (lineToken.hasMoreTokens()) {
					describe = lineToken.nextToken().toLowerCase();
					if (describe.compareTo("cseq:") == 0) {
						if (lineToken.hasMoreTokens()) {
							this.sequenceNumber = Integer.parseInt(lineToken.nextToken());
						} else {
							this.sequenceNumber = 0;
						}
							
					} else if (describe.compareTo("accept:") == 0) {
						if (lineToken.nextToken().toLowerCase().compareTo("application/sdp") == 0) {
							
							
						}
					}
				}
				try {
					line = lineReader.readLine();
				} catch (IOException ex) {
					line = null;
				}
				if (line != null) lineToken = new StringTokenizer(line);
				
			}
		}
		response = describeResponse();
		return response;
	}
	public String describeResponse() {
		// output: RTSP/1.0 200 OK\r\n
		//	  Server: WebStream\r\n
		// 	  Cseq: sequence#
		// 	  Cache-Control: no-cache\r\n
		//	  Content-length: sizeof(session_description)
		// 	  Date: date();
		//	  Expires: date();
		//	  Content-type: application/sdp
		//	  X-Accept-Retransmit: our-retransmit\r\n
		// 	  X-Accept-Dynamic-Rate: 1\r\n
		//	  Content-Base: rtsp://host/stream.sdp\r\n\r\n
		//
		// 	  getSessionDescription(stream);
		String response;
		String sessDesc = getSessionDescription(this.url);
		Date today;
		String dateOut;
		DateFormat formatter = 
		    DateFormat.getDateTimeInstance(DateFormat.LONG,
		                                   DateFormat.LONG);	
		today = new Date();
		dateOut = formatter.format(today);
		
		response = Response(200) + rtn + 
		"Cache-Control: no-cache" + rtn +
		"Content-length: " + sessDesc.length() + rtn + 
		"Date: " + dateOut + rtn +
		"Expires: " + dateOut + rtn +
		"Content-type: application/sdp" + rtn +
		"X-Accept-Retransmit: our-retransmit" + rtn + 
		"X-Accept-Dynamic-Rate: 1" + rtn +
		"Content-Base: " + url + rtn + rtn + 
		sessDesc;
		
		return response;
	}
	public String teardown(String input) {
		// input: TEARDOWN rtsp://host/stream RTSP/1.0
		//	  CSeq: sequence#
		//	  Session: <session id>
		BufferedReader lineReader = new BufferedReader(new StringReader(input)); 
		String response;
		StringTokenizer lineToken = null;
		String line;
		
		try {
			line = lineReader.readLine();
		} catch (IOException ex) {
			line = null;
		}
		
		if (line != null) lineToken = new StringTokenizer(line);
		if (this.endReason == null) this.endReason = 454;
		String options = null;
		while (lineToken.hasMoreTokens()) {
			options = lineToken.nextToken().toLowerCase();
			if (options.compareTo("cseq:") == 0) {
				this.sequenceNumber = Integer.parseInt(lineToken.nextToken());
			} else if (options.compareTo("session:") == 0) {
				if (lineToken.hasMoreTokens()) {
					String clientSessionId = new String(lineToken.nextToken());
					if (clientSessionId.compareTo(String.valueOf(sessionId)) == 0) {
						if (this.endReason == 454) this.endReason = null;
					} else {
						this.endReason = 454; // session not found
					}
					
				}
			}
			try {
				line = lineReader.readLine();
			} catch (IOException ex) {
				line = null;
			}
			if (line != null) lineToken = new StringTokenizer(line);
		}

		// output: RTSP/1.0 200 OK\r\n
		// 	CSeq: sequence#
		if (this.endReason == null) {
			response = Response(200)+ rtn + rtn;
		} else {
			response = Response(this.endReason) + rtn + rtn;
		}
		this.terminated = true;
		return response;
	}
	public String options(String input) {
		// input: OPTIONS rtsp://host/stream.dsp RTSP/1.0
		// 		  CSeq: sequence #
		// 	  	  User-Agent: WebStream \r\n\r\n
		BufferedReader lineReader = new BufferedReader(new StringReader(input)); 
		String response;
		StringTokenizer lineToken = null;
		String line;
		
		try {
			line = lineReader.readLine();
		} catch (IOException ex) {
			line = null;
		}
		if (line != null) lineToken = new StringTokenizer(line);
		String options = null;
		while (lineToken.hasMoreTokens()) {
			options = lineToken.nextToken().toLowerCase();
			if (options.compareTo("cseq:") == 0) {
				this.sequenceNumber = Integer.parseInt(lineToken.nextToken());
			} 
			try {
				line = lineReader.readLine();
			} catch (IOException ex) {
				line = null;
			}
			if (line != null) lineToken = new StringTokenizer(line);
		}
		
		
		// output: RTSP/1.0 200 OK\r\n
		//         Server: WebStream  \r\n
		//         Cseq: sequence#
		// 	   Public: DESCRIBE, SETUP, TEARDOWN, PLAY, PAUSE, OPTIONS\r\n

		if (this.endReason == null) {
			response = Response(200) + rtn + 
						"Public: " + capabilities + rtn + rtn;
		} else {
			response = Response(this.endReason) + rtn + rtn;
			
		}
		return response;
	}
	public String pause(String input) {
		// input: PAUSE rtsp://host/stream RTSP/1.0
		// 	  CSeq: sequence#
		//	  Session: <session id>
		BufferedReader lineReader = new BufferedReader(new StringReader(input)); 
		String response;
		StringTokenizer lineToken = null;
		String line;
		
		try {
			line = lineReader.readLine();
		} catch (IOException ex) {
			line = null;
		}
		if (this.endReason == null) this.endReason = 454;
		String options = null;
		if (line != null) lineToken = new StringTokenizer(line);
		if (lineToken != null) {
			while (lineToken.hasMoreTokens()) {
				options = lineToken.nextToken().toLowerCase();
				if (options.compareTo("cseq:") == 0) {
					this.sequenceNumber = Integer.parseInt(lineToken.nextToken());
				} else if (options.compareTo("session:") == 0) {
					if (lineToken.hasMoreTokens()) {
						String clientSessionId = new String(lineToken.nextToken());
						if (clientSessionId.compareTo(String.valueOf(sessionId)) == 0) {
							if (this.endReason == 454) this.endReason = null;
						} else {
							this.endReason = 454; // session not found
						}
						
					}
				}
				try {
					line = lineReader.readLine();
				} catch (IOException ex) {
					line = null;
				}
				if (line != null) lineToken = new StringTokenizer(line);
				
			}
		}
		// output: RTSP/1.0 200 OK
		// 	  CSeq: sequence#
		// 	  Date: date
		if (this.endReason == null) {
			Date today = new Date();
			DateFormat formatter = 
			    DateFormat.getDateTimeInstance(DateFormat.LONG,
			                                   DateFormat.LONG);	
			String dateOut = formatter.format(today);
			response = Response(200)+ rtn +
					"Date: " + dateOut + rtn + rtn;
		} else {
			response = Response(this.endReason) + rtn + rtn;
			
		}
		return response;
	}
	public String setup(String input) {
		// input: SETUP rtsp://host/stream.dsp RTSP/1.0\r\n
		// 	  CSeq: sequence# \r\n
		//	  User-Agent: blah\r\n
		// 	  Transport: RTP/AVP;unicast;client_port=udp-tcp\r\n\r\n
		BufferedReader lineReader = new BufferedReader(new StringReader(input)); 
		String response;
		StringTokenizer lineToken = null;
		String line;
		
		try {
			line = lineReader.readLine();
		} catch (IOException ex) {
			line = null;
		}
		String options = null;
		if (line != null) lineToken = new StringTokenizer(line);
		while (line != null) {
			if (lineToken != null) {
				if (lineToken.hasMoreTokens()) {
					options = lineToken.nextToken().toLowerCase();
					if (options.compareTo("cseq:") == 0) {
						if (lineToken.hasMoreTokens()) {
							this.sequenceNumber = Integer.parseInt(lineToken.nextToken());
						}
					} else if (options.compareTo("transport:") == 0) {
						if (lineToken.hasMoreTokens()) {
							 String transportOptions = lineToken.nextToken().toString();
							 String transportArray[] = transportOptions.split(";");
							 for (int i=0; i<transportArray.length; i++) {
								 if (transportArray[i].toLowerCase().contains("client_port=")) {
									 try {
										 clientTcpPort =  Integer.parseInt(transportArray[i].toLowerCase().replaceAll("client_port=[0-9]{1,}-", ""));
									 } catch (NumberFormatException ex) {
										 clientTcpPort = 0;
									 }
									 try {
										 clientVideoUdpPort = Integer.parseInt(transportArray[i].toLowerCase().replaceAll("client_port=", "").replaceAll("-[0-9]{1,}$", ""));
									 } catch (NumberFormatException ex) {
										 clientVideoUdpPort = 0;
									 }
								 }
							 }
						}
					}
				} else { 
					lineToken = null;
				}
			} else {
				try {
					line = lineReader.readLine();
				
				} catch (IOException ex) {
					line = null;
				}
				if (line != null) lineToken = new StringTokenizer(line);
				
			}
		}
		// output: RTSP/1.0 200 OK\r\n
		// 	  Server: blah \r\n
		// 	  CSeq: sequence#
		// 	  Cache-Control: no-cache\r\n
		//	  Session: <newly created session identifier>\r\n
		// 	  Date: date();
		// 	  Expires: date();
		// 	  Transport: RTP/AVP;unicast;source=<ip>;client_port=<client_ports>;server_port=<server_ports>\r\n

		Date today = new Date();
		DateFormat formatter = 
		    DateFormat.getDateTimeInstance(DateFormat.LONG,
		                                   DateFormat.LONG);	
		String dateOut = formatter.format(today);
		//String serverIp = new String("97.96.72.164");
		
		if (this.endReason == null) {	
			response = Response(200) + rtn +
			"Cache-control: no-cache" + rtn +
			"Session: " + sessionId + rtn +
			"Date: " + dateOut + rtn +
			"Expires: " + dateOut + rtn +
			"Transport: RTP/AVP;unicast;source=" + serverIp + ";client_port=" + clientVideoUdpPort.toString() + "-" + clientTcpPort.toString() +
				";server_port=" +  serverVideoUdpPort.toString() + "-" + Integer.toString(serverVideoUdpPort+1) + rtn + rtn;
			this.setupComplete = true;
		} else {
			response = Response(this.endReason) + rtn + rtn;
		}
		System.out.println("Sending response: " + response);
		return response;
	}
	public String play(String input) {
		// be sure SETUP has completed
		BufferedReader lineReader = new BufferedReader(new StringReader(input)); 
		String response;
		StringTokenizer lineToken = null;
		String line;
		
		try {
			line = lineReader.readLine();
		} catch (IOException ex) {
			line = null;
		}
		if (line != null) lineToken = new StringTokenizer(line);
		if (this.endReason == null) this.endReason = 454;
		if (setupComplete == true) {
		// input: PLAY rtsp://host/audio RTSP/1.0
		//	  CSeq: sequence#
		// 	  User-Agent: blah\r\n
		//	  Session: <session id>
		// 	  Range: npt=0.00-\r\n
			String options = null;
			String range;
			if (lineToken != null) {
				while (lineToken.hasMoreTokens()) {
					options = lineToken.nextToken().toLowerCase();
					if (options.compareTo("cseq:") == 0) {
						this.sequenceNumber = Integer.parseInt(lineToken.nextToken());
					} else if (options.compareTo("session:") == 0) {
						if (lineToken.hasMoreTokens()) {
							String clientSessionId = new String(lineToken.nextToken());
							if (clientSessionId.compareTo(String.valueOf(sessionId)) == 0) {
								if (this.endReason == 454) this.endReason = null;
							} else {
								this.endReason = 454; // session not found
							}
							
						}
					} else if (options.compareTo("range:") == 0) {
						if (lineToken.hasMoreTokens()) {
							range = lineToken.nextToken();
						}
					}
					try {
						line = lineReader.readLine();
					} catch (IOException ex) {
						line = null;
					}
					if (line != null) lineToken = new StringTokenizer(line);
					
				}
			}
		// output: RTSP/1.0 200 OK\r\n
		// 	  Server: blah\r\n
		// 	  Cseq: sequence #\r\n
		// 	  Session: <session id>
		// 	  Range: npt=now-\r\n
		//
		} else {
			this.endReason = 412; // precondition failed
		}
		if (this.endReason == null) {
			playing = true;
			response = Response(200)+ rtn +
					"Session: " + sessionId + rtn +
					"Range: npt=now-" + rtn + rtn;
		} else {
			response = Response(this.endReason) + rtn + rtn;
			
		}
		return response;
	}
	public String getSessionDescription(String stream) { // returns the SDP protocol for a particular stream/user
		String response = null;
		InetAddress localHost;
		String localHostStr;
		try {
			localHost = InetAddress.getLocalHost();
			//localHostStr = localHost.getHostName();
			localHostStr = localHost.getHostAddress();
		} catch (UnknownHostException ex) {
			localHostStr = "nohost";
		}
		localHostStr = "yapastream.com:554";
		long version = 1522452;
		
		// arrays for expandability in stream support later
		int number_stream = 1;
		int audio_payload_type[] = new int[number_stream];
		String audio_encoding_name[] = new String[number_stream];
		int audio_clockrate[] = new int[number_stream];
		int audio_channels[] = new int[number_stream];
		int audio_port[] = new int[number_stream];
		
		int video_payload_type[] = new int[number_stream];
		String video_encoding_name[] = new String[number_stream];
		int video_clockrate[] = new int[number_stream];
		int video_channels[] = new int[number_stream];
		int video_port[] = new int[number_stream];
		
		audio_payload_type[0] = 97;
		audio_encoding_name[0] = "MP4A-LATM";
		audio_clockrate[0] = 90000;
		video_clockrate[0] = 90000;
		audio_channels[0] = 1;
		audio_port[0]= 0;
		video_port[0] = 0;
		video_payload_type[0] = 96;
		video_encoding_name[0] = "H263-1998";

		//video_encoding_name[0] = "H264";
		//video_encoding_name[0] = "MP4V-ES";
		String username = "blah";
		
		//v=0\r\n
		//o=<username> <session id> <version> IN IP4 " + getlocalhost + "\r\n" +
		//"s=" + stream + "\r\nc=IN IP4 " + getlocalhost + "\r\n" +
		//"t=0 0\r\n" +  // permenant session
		//"m=audio <port> RTP/AVP payload_types(97 or 103?)\r\n" +
		//"a=rtpmap:<payload type> <encoding name>/<clockrate>/<optional # channels>\r\n" +
		//"m=video <port> RTP/AVP <payload_types>\r\n" +
		//"a=rtpmap:<payload type> <encoding name>/<clockrate>\r\n\r\n";
		response = 
			"v=0\r\n" + 
			"o="+ username + " " + sessionId + " " + version + " IN IP4 " + localHostStr + "\r\n" + 
			"s=" + stream + "\r\n" + 
			"c=IN IP4 " + localHostStr + "\r\n" + 
			"t=0 0\r\n" + 
			//"m=audio " + audio_port[0] + " RTP/AVP " + audio_payload_type[0] + "\r\n" + 
			//"a=rtpmap:" + audio_payload_type[0] + " "  + audio_encoding_name[0] + "/" + audio_clockrate[0] + "/" + audio_channels[0] + "\r\n" +
			"m=video " + video_port[0] + " RTP/AVP " + video_payload_type[0] + "\r\n" +
			"a=rtpmap:" + video_payload_type[0] + " " + video_encoding_name[0] + "/" + video_clockrate[0] + "\r\n" + //"/" + video_channels[0] + 
			//"a=framerate:27.0" + "\r\n" + 
			"\r\n";
		return response;
	}
	private String Response(int responseCode) {
		String response = null;
		String productName = "Yapastream";
		String productVersion = "0.1";
		String rtspVersion = "RTSP/1.0";
		switch (responseCode) {
			case 200:
				response = rtspVersion + " 200 OK";
				break;
			case 201:
				response = rtspVersion + " 201 Created";
				break;
			case 250:
				response = rtspVersion + " 250 Low on Storage Space";
				break;
			case 300:
				response = rtspVersion + " 300 Multiple Choices";
				break;
			case 301:
				response = rtspVersion + " 301 Moved Permanently";
				break;
			case 302:
				response = rtspVersion + " 302 Moved Temporarily";
				break;
			case 303:
				response = rtspVersion + " 303 See Other";
				break;
			case 304:
				response = rtspVersion + " 304 Not Modified";
				break;
			case 305:
				response = rtspVersion + " 305 Use Proxy";
				break;
			// 4XX - Client Errors
			case 400:
				response = rtspVersion + " 400 Bad Request";
				break;
			case 401:
				response = rtspVersion + " 401 Unauthorized";
				break;
			case 402:
				response = rtspVersion + " 402 Payment Required";
				break;
			case 403:
				response = rtspVersion + " 403 Forbidden";
				break;
			case 404:
				response = rtspVersion + " 404 Not Found";
				break;
			case 405:
				response = rtspVersion + " 405 Method Not Allowed";
				break;
			case 406:
				response = rtspVersion + " 406 Not Acceptable";
				break;
			case 407:
				response = rtspVersion + " 407 Proxy Authentication Required";
				break;
			case 408:
				response = rtspVersion + " 408 Request Time-out";
				break;
			case 410:
				response = rtspVersion + " 410 Gone";
				break;
			case 411:
				response = rtspVersion + " 411 Length Required";
				break;
			case 412:
				response = rtspVersion + " 412 Precondition Failed";
				break;
			case 413:
				response = rtspVersion + " 413 Request Entity Too Large";
				break;
			case 414:
				response = rtspVersion + " 414 Request-URI Too Large";
				break;
			case 415:
				response = rtspVersion + " 415 Unsupported Media Type";
				break;
			case 451:
				response = rtspVersion + " 451 Parameter Not Understood";
				break;
			case 452:
				response = rtspVersion + " 452 Conference Not Found";
				break;
			case 453:
				response = rtspVersion + " 453 Not Enough Bandwidth";
				break;
			case 454:
				response = rtspVersion + " 454 Session Not Found";
				break;
			case 455:
				response = rtspVersion + " 455 Method Not Valid in This State";
				break;
			case 456:
				response = rtspVersion + " 456 Header Field Not Valid for Resource";
				break;
			case 457:
				response = rtspVersion + " 457 Invalid Range";
				break;
			case 458:
				response = rtspVersion + " 458 Parameter Is Read-Only";
				break;
			case 459:
				response = rtspVersion + " 459 Aggregate operation not allowed";
				break;
			case 460:
				response = rtspVersion + " 460 Only aggregate operation allowed";
				break;
			case 461:
				response = rtspVersion + " 461 Unsupported transport";
				break;
			case 462:
				response = rtspVersion + " 462 Destination unreachable";
				break;
				
			// 5XX - Server Errors
			case 500:
				response = rtspVersion + " 500 Internal Server Error";
				break;
			case 501:
				response = rtspVersion + " 501 Not Implemented";
				break;
			case 502:
				response = rtspVersion + " 502 Bad Gateway";
				break;
			case 503:
				response = rtspVersion + " 503 Service Unavailable";
				break;
			case 504:
				response = rtspVersion + " 504 Gateway Time-out";
				break;
			case 505:
				response = rtspVersion + " 505 RTSP Version not supported";
				break;
			case 551:
				response = rtspVersion + " 551 Option not supported";
				break;
		}
		response = response + rtn + 
				"Server: " + productName + " (" + productVersion + ")" + rtn + 
				"CSeq: " + this.sequenceNumber;

		return response;
	}
	public String getURL() {
		return this.url;
	}
	public boolean isPlaying() {
		return playing;
	}
	public Integer getClientTcpPort() {
		return clientTcpPort;
	}
	public Integer getClientAudioUdpPort() {
		return clientAudioUdpPort;
	}
	public Integer getClientVideoUdpPort() {
		return clientVideoUdpPort;
	}
	public Integer getServerTcpPort() {
		return serverTcpPort;
	}
	public void setServerVideoUdpPort(Integer v) {
		 serverVideoUdpPort = v;
	}
	public void setServerAudioUdpPort(Integer v) {
		 serverAudioUdpPort = v;
	}
	public void setServerTcpPort(Integer v) {
		 this.serverTcpPort = v;
	}
	public boolean isTerminated() { 
		return this.terminated;
	}
	public void setServerIp(String ip) {
		this.serverIp = ip;
	}
	public void setServerUdpPorts(int[] ports) {
		this.serverUdpPorts = ports;
	}
	public void generateSessionId() {
		//SecureRandom random = new SecureRandom();
		//String sessid = new BigInteger(130, random).toString(32);
		//this.sessionId = Integer.parseInt(sessid);
	    Random generator = new Random();
	    this.sessionId = Math.abs(generator.nextInt());
	    //return this.sessionId;
	}
	public String getRequestedStream() {
		if (url == null) {
			return null;
		} else if (requestedStream == null) {
			requestedStream = url.replaceAll("rtsp://[\\w\\d\\.]+[:\\d]*/", "").toLowerCase();
			System.out.println(url + " Requesting Stream: " + requestedStream);
		}
		
		return requestedStream; // username of stream being requested
	}
	public Integer getEndReason() {
		if ((this.endReason == null) || (this.endReason == 0)) { // should always be null unless we are ending the session
			this.endReason = 503;
		}
		return this.endReason;
	}
	public void setEndReason(Integer val) {
		this.endReason = val;
	}
}
