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

import java.net.*;
import android.app.AlertDialog;
import java.io.*;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.MediaRecorder;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.content.Context.*;
import android.content.*;
import android.view.SurfaceHolder;

// TCP connection to server for managing RTP UDP streams
// Spawns 2 threads for sending Audio and Video to Server
import android.widget.RelativeLayout;
//import android.net.rtp.AudioStream;
//import android.net.rtp.RtpStream;

public class ProtocolConnection extends Thread {
	private Socket serverSock;
	private PrintWriter output;
	private BufferedReader input;
	private String remote_server;
	private int remote_port;
	private PhoneUserP phoneUser;
	private static String rtn = "\r\n";
	private boolean authenticated;
	private boolean ported;
	private boolean setting;
	private boolean connected;
	private boolean playing;
	private SurfaceHolder sHolder;
	private boolean ended;
	private Handler handler;
	// from original SecureConn
	LocalSocket receiver, sender;
	LocalServerSocket lss;
	int obuffering;
	int fps;
	SipdroidSocket socket;
	RtpSocket rtp_socket = null;
	Thread t;
	boolean change;
	String errMsg;
	MediaRecorder recorder;
	boolean recording = false;
	private Camera camera;
	static boolean debug = false;
	int vidQuality;
	int privacy; 
	// open TCP connection to server
	//
	public ProtocolConnection(PhoneUserP pup, SurfaceHolder shold) {
		this.phoneUser = pup;
		this.serverSock = null;
		this.sHolder = shold;
		this.connected = false;
		this.playing = false;
		this.authenticated = false;
		this.ported = false;
		this.setting = false;
		this.ended = false;
		this.vidQuality = 0;
		this.remote_server = "yapastream.com";
		this.remote_port = 10083;
	}

	public void run() {
		ProtoResponse response = null;
		this.connected = false;
		this.playing = false;
		this.authenticated = false;		
		this.serverSock = null;
		this.ported = false;
		this.ended = false;
		int attempts = 0;
		// Initialize connection
		while (this.ended == false) {
			if (this.connected == false)  {
				if ((this.connect() == 1) && (attempts++ < 10)) {
					Log.v("S", "connect");
					attempts = 0;
				} else {
					this.errMsg = "Unable to connect.";
					this.end();
					this.alertboxToLogin("Error", this.errMsg);
				}
			} else if (this.authenticated == false) {
				if ((this.authenticate() == 1) && (attempts++ < 10)) {
					Log.v("S", "authenticated");

					attempts = 0;
					response = new ProtoResponse(input);

					Log.v("S",
							"Status code: "
									+ Integer.toString(response.getStatusCode()));
					if (response.getStatusCode() == 200) {
						this.authenticated = true;
						this.phoneUser.setVerified(true);
						this.phoneUser.setSessionId(response.getSessionId());
						this.phoneUser.setRemoteAudioPort(response
								.getAudioPort());
						this.phoneUser.setRemoteVideoPort(response
								.getVideoPort());
					} else if (response.getStatusCode() == 401) {
						// unauthorized
						this.errMsg = "Invalid username and/or password";
						this.authenticated = false;
						this.ended = true;						Log.v("S", "Invalid password");
						this.alertboxInvalidPassword("Invalid password", "Please reenter the password or recover the password if you have forgotten it.");

						//this.alertboxToLogin("Error", this.errMsg);
					//	postError(this.errMsg);
					//	
					}
					Log.v("S",
							"SC: " + Integer.toString(response.getStatusCode())
									+ "Port: " + response.getVideoPort());
					response = null;
				} else {
					Log.v("S", "Unable to authenticate");
					this.errMsg = "Unable to authenticate.";
					this.end();
					this.alertboxToLogin("Error", this.errMsg);
				}
			} else if (this.ported == false) {
				this.port();
				response = new ProtoResponse(this.input);
				if (response.getStatusCode() == 200) {
					this.ported = true;
				}
			} else if (this.setting == false) {
				this.settings();
				response = new ProtoResponse(this.input);
				if (response.getStatusCode() == 200) {
					this.setting = true;
				}
			} else if (this.playing == false) {
				this.play();
				response = new ProtoResponse(this.input);
				if (response.getStatusCode() == 200) {
					this.startCamera();
					this.beginVideoForward();
					this.beginAudioForward();
					this.playing = true;
				}
			}
		}
		Log.v("S", "Ending protocol connection");
	}
	public void alertboxToLogin(String title, String text) {
		this.end();
		if (this.handler != null) {
			Message msg = handler.obtainMessage();
			msg.obj = "alertboxToLogin|" + title + "|" + text;
			handler.sendMessage(msg);
		}
	}
	public void alertboxInvalidPassword(String title, String text) {
		this.end();
		if (this.handler != null) {
			Message msg = handler.obtainMessage();
			msg.obj = "alertboxInvalidPassword|" + title + "|" + text;
			handler.sendMessage(msg);
		}
	}
	// returns 1: success
	// 0: failed
	public int connect() {
		int retval = 0;
		if (this.serverSock == null) {
			try {
				this.serverSock = new Socket(
						InetAddress.getByName(this.remote_server),
						this.remote_port);
				this.output = new PrintWriter(
						this.serverSock.getOutputStream(), true);
				this.input = new BufferedReader(new InputStreamReader(
						this.serverSock.getInputStream()));
				this.connected = true;
				retval = 1;
			} catch (Exception ex) {
				this.serverSock = null;
				ex.printStackTrace();
			}
		} else {
			if (this.serverSock.isClosed()) {
				try {
					this.serverSock = new Socket(
							InetAddress.getByName(this.remote_server),
							this.remote_port);
					this.output = new PrintWriter(
							this.serverSock.getOutputStream(), true);
					this.input = new BufferedReader(new InputStreamReader(
							this.serverSock.getInputStream()));
					this.connected = true;
					retval = 1;
				} catch (Exception ex) {
					this.serverSock = null;
					ex.printStackTrace();
				}
			} else	if (this.serverSock.isConnected()) {
				this.connected = true;
			} else if (this.serverSock.isBound()) {
				Log.v("S", "Unable to bind to server socket.");
			}
		}
		return retval;
	}

	// returns 1: success
	// 0: not connected
	public int authenticate() {
		int retval = 0;
		
		if (this.serverSock != null) {
			if (this.serverSock.isConnected()) {
				if (this.phoneUser.isVerified() == false) {
					this.output.print("USER " + this.phoneUser.getUsername()
							+ rtn);
					this.output.println("PASS " + this.phoneUser.getPassword()
							+ rtn);

					retval = 1;
				} // else already verified
			} else { // else not connected
				this.connected = false;
				this.serverSock = null;
				this.connect();

				Log.v("S", "not connected");
			}
		} else {// else never started
			this.connected = false;
			this.serverSock = null;
			this.connect();
			Log.v("S", "never connected");
		}
		return retval;
	}

	public int disconnect() {
		int retval = 0;
		this.ended = true;
		if (this.serverSock != null) {
			this.sendCommand("TERMINATE");
			try {
				this.serverSock.close();
				this.output.close();
				this.input.close();
				retval = 1;
			} catch (Exception ex) {

			}

		}
		return retval;
	}

	public void play() {
		this.sendCommand("PLAY");
	}

	public void pause() {
		this.sendCommand("PAUSE");
	}

	public void port() {
		this.sendCommand("PORT " + this.phoneUser.getLocalAudioPort() + "-"
				+ this.phoneUser.getLocalVideoPort());
	}
	public void settings() {
		this.sendCommand("SETTINGS " + "|" + "privacy" + "|" + Integer.toString(this.privacy)); 
				//"|" + "SETTINGS" + "|" + );
		
	}

	public void end() {
		this.disconnect();
		if (recorder != null) {
			try {
				recorder.stop();
				recorder.reset();
				recorder.release();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			recorder = null;
		}
		if (camera != null) {
			camera.stopPreview();
			camera.unlock();
			camera.release();
		}
		try {
			receiver.close();
			lss.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			lss.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			sender.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
		this.phoneUser.setVerified(false);
		recording = false;
		this.serverSock = null;
		this.connected = false;
		this.playing = false;
		this.authenticated = false;
		this.ported = false;
		this.ended = true;
		try {
			this.lss.close();
			this.receiver.close();
			this.sender.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void setHandler(Handler h) {
		this.handler = h;
		
	}
	public void setPrivacy(int priv) {
		this.privacy = priv;
	}
	public boolean isPlaying() {
		return this.playing;
	}
	public String getErrorMsg() {
		return this.errMsg;
	}
	
	public void sendCommand(String command) {
		if (this.serverSock != null) {
			if (this.serverSock.isConnected()) {
				if (this.phoneUser.isVerified() == true) {
					this.output.println(command + rtn + "Session: "
							+ this.phoneUser.getSessionId() + rtn);

				} else {
					// unverified, try verification?, alert user?
				}
			}
		}
	}

	public void setupVideoSocket() {
		receiver = new LocalSocket();
		try {
			if (lss != null) lss.close();
			lss = new LocalServerSocket("yapaStream");
			receiver.connect(new LocalSocketAddress("yapaStream"));
			receiver.setReceiveBufferSize(500000);
			receiver.setSendBufferSize(500000);
			sender = lss.accept();
			sender.setReceiveBufferSize(500000);
			sender.setSendBufferSize(500000);
		} catch (IOException e1) {
			e1.printStackTrace();
			stopCamera();
			
			// finish();
			return;
		}
	}
	public void cameraChange(int width, int height, Display display) {
		if (recording)
        {
			camera.stopPreview();
        }
	
	}
      /* Parameters parameters = camera.getParameters();
        

        if(display.getRotation() == Surface.ROTATION_0)
        {
            parameters.setPreviewSize(height, width);                           
            camera.setDisplayOrientation(90);
        }

        if(display.getRotation() == Surface.ROTATION_90)
        {
            parameters.setPreviewSize(width, height);                           
        }

        if(display.getRotation() == Surface.ROTATION_180)
        {
            parameters.setPreviewSize(height, width);               
        }

        if(display.getRotation() == Surface.ROTATION_270)
        {
            parameters.setPreviewSize(width, height);
            camera.setDisplayOrientation(180);
        }
        
        camera.setParameters(parameters);
        */
		/*
	     android.hardware.Camera.CameraInfo info =
             new android.hardware.Camera.CameraInfo();
             int cameraId = 0; // 0 - getNumberOfCameras() -1
     android.hardware.Camera.getCameraInfo(cameraId, info);
     int rotation = display.getRotation();
     int degrees = 0;
     switch (rotation) {
         case Surface.ROTATION_0: degrees = 0; break;
         case Surface.ROTATION_90: degrees = 90; break;
         case Surface.ROTATION_180: degrees = 180; break;
         case Surface.ROTATION_270: degrees = 270; break;
     }

     int result;
     if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
         result = (info.orientation + degrees) % 360;
         result = (360 - result) % 360;  // compensate the mirror
     } else {  // back-facing
         result = (info.orientation - degrees + 360) % 360;
     }
     camera.setDisplayOrientation(result);
*/
       /* if (playing == true) {
        	startCamera();
        }*/
//	}
	public void setServerAddress(String server) {
		this.remote_server = server;
	}
	public void setServerPort(int port) {
		this.remote_port = port;
	}
	public boolean startCamera() {
		// recording variable may be different here.. check later.. rename if
		// needed
		if (sHolder == null) { // was surfaceView
			return false;
		}
		recording = true;
		recorder = new MediaRecorder();
		if (camera != null) {
			Log.v("S", "Camera not null");
			if (Integer.parseInt(Build.VERSION.SDK) >= 8) {
				// reconnect
			}
			camera.release();
			camera = null;
		} else {
			Log.v("S", "Camera is null");
		}
		if ((this.sender != null) || (this.receiver == null)) {
			if (!(this.sender.isConnected())) {
				this.setupVideoSocket();
			}
		} else {
			this.setupVideoSocket();
		}
		recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		recorder.setOutputFile(sender.getFileDescriptor());
		//recorder.setOutputFile("/sdcard/preview.3gp");
		if (this.vidQuality == 1) {
			recorder.setVideoSize(176, 144); 
			recorder.setVideoFrameRate(30);	
		} else {
			recorder.setVideoSize(352, 288); // CIF for h263, 16 lines for Group of
			recorder.setVideoFrameRate(30);	
		}
											// Blocks (GOB)
		//recorder.setVideoSize(586, 480);
		recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
		
		
	//	recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
	//	recorder.setOrientationHint(180);
		//sHolder.getSurface().setOrientation(display.getDisplayId(), Surface.ROTATION_180);
		recorder.setPreviewDisplay(sHolder.getSurface());

		try {
			recorder.prepare();
			recorder.start();
		} catch (IOException ex) {
			ex.printStackTrace();
			this.end();
			return false;
		}
		return true;
	}

	public void stopCamera() {
		this.end();
		
	}

	public void beginAudioForward() {
		//AudioStream audioStream = new AudioStream()
		//audioStream.setMode(MODE_SEND_ONLY); // from RtpStream
		//audioStream.associate(remote_server, remote_audio_port); // from RtpStream	
	}
	public void setVideoQuality(int vq) {
		this.vidQuality = vq;
		Log.v("S", "Setting video quality to " + vq);
	}
	public void beginVideoForward() {
		try {
			if (rtp_socket == null) {
				rtp_socket = new RtpSocket(new SipdroidSocket(  // using built in android udp results in loss of packets
						this.phoneUser.getLocalVideoPort()),
						InetAddress.getByName(this.remote_server),
						this.phoneUser.getRemoteVideoPort());
				Log.v("S",	"remote video port is : " + this.phoneUser.getRemoteVideoPort());
				
			} else if (rtp_socket.getDatagramSocket().isClosed()) {
					rtp_socket = new RtpSocket(new SipdroidSocket(
							this.phoneUser.getLocalVideoPort()),
							InetAddress.getByName(this.remote_server),
							this.phoneUser.getRemoteVideoPort());
			}
			// local port, remote address, remote port
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		// Thread by SipDroid Project with various changes added
		(t = new Thread() { // thread which will receive the local video packets
			// thread will convert to RTP and send to web server
			public void run() {
				int frame_size = 1400;
				byte[] buffer = new byte[frame_size + 14];
				buffer[12] = 4;
				RtpPacket rtp_packet = new RtpPacket(buffer, 0);
				int seqn = 0;
				int num, number = 0, src, dest, len = 0, head = 0, lasthead = 0, lasthead2 = 0, cnt = 0, stable = 0;
				long now, lasttime = 0;
				double avgrate = 45000;
				double avglen = avgrate / 20;

				InputStream fis = null;
				try {
					fis = receiver.getInputStream();
				} catch (IOException e1) {
					e1.printStackTrace();
					rtp_socket.getDatagramSocket().close();
					return;
				}
				rtp_packet.setPayloadType(96);
				while (rtp_socket != null) {
					num = -1;
					try {
						num = fis
								.read(buffer, 14 + number, frame_size - number);
					} catch (IOException e) {
						e.printStackTrace();
						break;
					}
					if (num < 0) {
						try {
							sleep(20);
						} catch (InterruptedException e) {
							break;
						}
						continue;
					}

					number += num;
					head += num;

					try {
						now = SystemClock.elapsedRealtime();
						if (lasthead != head + fis.available() && ++stable >= 5
								&& now - lasttime > 700) {
							if (cnt != 0 && len != 0)
								avglen = len / cnt;
							if (lasttime != 0) {
								fps = (int) ((double) cnt * 1000 / (now - lasttime));
								avgrate = (double) ((head + fis.available()) - lasthead2)
										* 1000 / (now - lasttime);
							}
							lasttime = now;
							lasthead = head + fis.available();
							lasthead2 = head;
							len = cnt = stable = 0;
						}
					} catch (IOException e1) {
						e1.printStackTrace();
						break;
					}
					// in h264 search for NAL units here
					for (num = 14; num <= 14 + number - 2; num++)
						if (buffer[num] == 0 && buffer[num + 1] == 0)
							break;
					if (num > 14 + number - 2) {
						num = 0;
						rtp_packet.setMarker(false);
					} else {
						num = 14 + number - num;
						rtp_packet.setMarker(true);
					}
					rtp_packet.setSequenceNumber(seqn++);
					rtp_packet.setPayloadLength(number - num + 2);
					if (seqn > 10)
						try {
							if (debug == true) 	Log.v("S", Integer.toString(rtp_packet.getLength()));
							rtp_socket.send(rtp_packet);
							len += number - num;
						} catch (IOException e) {
							e.printStackTrace();
							break;
						}

					if (num > 0) {
						num -= 2;
						dest = 14;
						src = 14 + number - num;
						if (num > 0 && buffer[src] == 0) {
							src++;
							num--;
						}
						number = num;
						while (num-- > 0)
							buffer[dest++] = buffer[src++];
						buffer[12] = 4;

						cnt++;
						try {
							if (avgrate != 0)
								Thread.sleep((int) (avglen / avgrate * 1000));
						} catch (Exception e) {
							break;
						}
						rtp_packet
								.setTimestamp(SystemClock.elapsedRealtime() * 90);
					} else {
						number = 0;
						buffer[12] = 0;
					}
					if (change) {
						change = false;
						long time = SystemClock.elapsedRealtime();

						try {
							while (fis.read(buffer, 14, frame_size) > 0
									&& SystemClock.elapsedRealtime() - time < 3000)
								;
						} catch (Exception e) {
						}
						number = 0;
						buffer[12] = 0;
					}
				}
				rtp_socket.getDatagramSocket().close();
				try {
					while (fis.read(buffer, 0, frame_size) > 0)
						;
				} catch (IOException e) {
				}
			}
		}).start();
	}

}
