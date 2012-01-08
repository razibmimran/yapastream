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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import com.YapaStream.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.*;
import android.widget.*;
import android.util.Log;
import android.view.*;
import java.util.StringTokenizer;

public class YapaStream extends Activity {// implements SurfaceHolder.Callback{
	private SurfaceView surfaceView;
	private Button loginButton;
	private Button loginSignup;
	private Button loginSettings;
	private Button signupSubmit;
	private Button signupCancel;
	private Button fpSubmit;
	private Button fpCancel;
	//private Button settingsCancel;
	private Button settingsSave;
	private SurfaceHolder sHolder;
	private PhoneUserP phoneUser;
	private ProtocolConnection protoConn;
	private SharedPreferences yapaPref;
	private EditText etUsername;
	private EditText etPassword;
	private Spinner spinVideoQuality;
	private Spinner spinPrivacy;
	private ToggleButton tbAudio;
	private Handler handler;
	private EditText etWebAddress;
	private String serverAddress;
	private int serverPort;
	private int privacy; // 0=broadcast;1=public,2=private
	private String webAddress;
	//http://mindtherobot.com/blog/159/android-guts-intro-to-loopers-and-handlers/
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		surfaceView = (SurfaceView) findViewById(R.id.surface_camera);
		sHolder = surfaceView.getHolder();
		sHolder.addCallback(surfaceCallback);
		sHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		this.showLogin();
		this.serverAddress = null;
		this.serverPort = 0;
		this.phoneUser = new PhoneUserP();
		//this.protoConn = new ProtocolConnection(this.phoneUser, this.sHolder);
		this.loginButton = (Button) this.findViewById(R.id.btnLogin);
		this.loginSignup = (Button) this.findViewById(R.id.btnSignup);
		this.loginSettings = (Button) this.findViewById(R.id.btnSettings);
		this.signupSubmit = (Button) this.findViewById(R.id.btnSignupSubmit);
		this.signupCancel = (Button) this.findViewById(R.id.btnSignupCancel);
		this.fpSubmit = (Button) this.findViewById(R.id.btnFpSubmit);
		this.fpCancel = (Button) this.findViewById(R.id.btnFpCancel);
		this.settingsSave = (Button) this.findViewById(R.id.btnSettingsSave);

		//this.settingsCancel = (Button) this.findViewById(R.id.btnSettingsCancel);
		handler = new Handler() {
				public void handleMessage(Message msg) {
					// set with 
					//Message msg = handler.obtainMessage();
					//msg.obj = "ERROR";
					//handler.sendMessage(msg);
					String text = (String)msg.obj;
					String[] textSplit = text.split("[|]");
					if (textSplit[0].compareTo("alertboxToLogin") == 0) {
						Log.v("S", "Alertbox to login");
						alertboxToLogin(textSplit[1], textSplit[2]);
					} else if (textSplit[0].compareTo("alertboxInvalidPassword") == 0) {
						alertboxInvalidPassword(textSplit[1], textSplit[2]);
					}
				}
		};
			
		this.loginButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				btnLogin(v);
			}
		});
		this.loginSignup.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				btnSignup(v);
			}
		});
		this.loginSettings.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				btnSettings(v);
			}
		});
		this.signupSubmit.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				btnSignupSubmit(v);
			}
		});
		this.signupCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				btnCancel(v);
			}
		});
		this.fpSubmit.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				btnFpSubmit(v);
			}
		});

		this.fpCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) { 
				btnCancel(v);
			}
		});
		/*this.settingsCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) { 
				btnCancel(v);
			}
		});*/
		this.settingsSave.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) { 
				btnSettingsSave(v);
			}
		});
		try {
			Spinner spinner = (Spinner) findViewById(R.id.spinVideoQuality);
			ArrayAdapter<CharSequence> vidQualityAdapter = ArrayAdapter.createFromResource(
		            this, R.array.spinVideoQuality, android.R.layout.simple_spinner_item);
			
			vidQualityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinner.setAdapter(vidQualityAdapter);
			
			spinner = (Spinner) findViewById(R.id.spinPrivacy);
			ArrayAdapter<CharSequence> privacyAdapter = ArrayAdapter.createFromResource(
		            this, R.array.spinPrivacy, android.R.layout.simple_spinner_item);
			
			privacyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinner.setAdapter(privacyAdapter);
			this.loadUser();
			this.loadPref();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		//this.alertbox("Test", "Error connecting.");
		
		
	}

	@Override
	public void onResume() {
		super.onResume();
		this.sHolder = surfaceView.getHolder();
		Log.v("S", "Resuming");
		if (this.protoConn != null) {
			this.protoConn.end();
			this.protoConn.stop();
			this.protoConn = null;
			//if (this.protoConn.isPlaying() == true) {
			//	Log.v("S", "PLAYING");
			//	this.Login(); // this.protoConn.startCamera();
			//} else {
			//}
		} else {
			Log.v("S", "Stopped");
			this.hideCamera();
			this.showLogin();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		try {
			this.protoConn.end();
			this.protoConn.stop();
			this.protoConn = null;
		} catch (Exception ex) {

		}
	}
	@Override
	public void onStop() {
		super.onStop();
		Log.v("S", "Stopping");
		try {
			this.protoConn.end();
			this.protoConn.stop();
			this.protoConn = null;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
		public void surfaceCreated(SurfaceHolder holder) {
			/*
			 * if (camera != null) { try { camera.setPreviewDisplay(holder); }
			 * catch (IOException e) { // TODO Auto-generated catch block
			 * e.printStackTrace(); } catch (Throwable t) { Log.e("callback",
			 * "setPreview() exception."); } }
			 */
			Display display = ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
			
			Log.v("S", "Surface created");
		}

		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			/*
			 * if (camera != null) { Camera.Parameters params =
			 * camera.getParameters(); params.setPreviewSize(width, height);
			 * camera.setParameters(params); camera.startPreview();
			 * displayPreview = true; }
			 */
			//protoConn.cameraChange(width, height, display);
			Log.v("S", "Surface changed");
		}

		public void surfaceDestroyed(SurfaceHolder holder) {
			Log.v("S", "Surface detroyed");
			if (protoConn != null) {
				protoConn.end();
			}
		}
	};
	public boolean onPrepareOptionsMenu(Menu menu) {
		RelativeLayout camera_layout = (RelativeLayout)findViewById(R.id.camera_layout);
		menu.clear();
		if (camera_layout.isShown()) menu.add(0, 0, 0, "Logout");
		return  super.onPrepareOptionsMenu(menu);
	}
	public boolean onCreateOptionsMenu(Menu menu) {
		RelativeLayout camera_layout = (RelativeLayout)findViewById(R.id.camera_layout);
		//menu.add(0, 1, 0, "Start");
		if (camera_layout.isShown()) menu.add(0, 0, 0, "Logout");
		return super.onCreateOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0: // stream
		//	break;
		//case 1: // options
			this.Logout();
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	public boolean loadPref() {
		this.yapaPref = getSharedPreferences("yapaPref", MODE_PRIVATE);
		this.etWebAddress = (EditText) findViewById(R.id.etWebAddress);
		this.spinVideoQuality = (Spinner) findViewById(R.id.spinVideoQuality);
		this.spinPrivacy = (Spinner) findViewById(R.id.spinPrivacy);
		this.tbAudio = (ToggleButton) findViewById(R.id.tbAudio);
		this.tbAudio = (ToggleButton) findViewById(R.id.tbAudio);
		this.etWebAddress.setText(yapaPref.getString("webaddress", ""));
		this.spinVideoQuality.setSelection(yapaPref.getInt("videoquality", 0));
		this.spinPrivacy.setSelection(yapaPref.getInt("privacy", 0));
		this.tbAudio.setChecked(yapaPref.getBoolean("audio", true));
		this.webAddress = yapaPref.getString("webaddress", "");
		
		return true;
	}
	public boolean loadUser() {
		this.yapaPref = getSharedPreferences("yapaPref", MODE_PRIVATE);
		this.etUsername = (EditText) findViewById(R.id.etUsername);
		this.etPassword = (EditText) findViewById(R.id.etPassword);
		this.etUsername.setText(yapaPref.getString("username", ""));
		this.etPassword.setText(yapaPref.getString("password", ""));
		return true;
	}
	public boolean saveUser(String user, String pass) {
		SharedPreferences.Editor e = yapaPref.edit();
		e.putString("username", user);
		e.putString("password", pass);
		e.commit(); 
		return true;
	}
	// user, pass, audio[0=off,1=on], quality[0=high,1=low]
	public boolean savePref(boolean aud, int quality, String server, int privacy) {
		SharedPreferences.Editor e = yapaPref.edit();
		e.putBoolean("audio", aud);
		e.putInt("videoquality", quality);
		e.putString("webaddress", server);
		e.putInt("privacy", privacy);
		e.commit(); 
		return true;
	}
	void btnLogin(View v) {
		Login();
		return;

	}
	void btnSignup(View v) {
		showSignup();
		
	}
	void btnFpSubmit(View v) {
		try {
		EditText fpUser = (EditText) findViewById(R.id.etFpUsername);
		forgotPassword(fpUser.getText().toString());
		
		} catch (Exception ex) {
			Log.v("S", ex.getMessage());
		}
		
	}
	void btnForgotPassword(View v) {
		showForgotPassword();
	}
	void btnCancel(View v) {
		showLogin();
	}
	void btnLogout(View v) {
		this.Logout();
		try {
			this.protoConn.end();
		} catch (Exception ex) {
			
		}
	}
	public void btnSettings(View v) {
		showSettings();
	}
	public void btnSettingsSave(View v) {
		boolean aud = this.tbAudio.isChecked();
		int vidQuality = this.spinVideoQuality.getSelectedItemPosition();
		String server = this.etWebAddress.getText().toString();
		this.webAddress = server;
		this.privacy =  this.spinPrivacy.getSelectedItemPosition();
		this.savePref(aud, vidQuality, server, this.privacy);
		this.getServerInfo();
		Log.v("S", "asking for server info.");
		showLogin();
	}
	public void btnSettingsCancel(View v) {
		this.loadPref();
		showLogin();
	}
	public void btnSignupSubmit(View v) {
		EditText signupUsername = (EditText) findViewById(R.id.etSignupUsername);
		EditText signupPassword = (EditText) findViewById(R.id.etSignupPassword);
		EditText signupConfirmPassword = (EditText) findViewById(R.id.etSignupConfirmPassword);
		EditText signupEmail = (EditText) findViewById(R.id.etSignupEmail);
		
		if (signupPassword.getText().toString().compareTo(signupConfirmPassword.getText().toString()) == 0) {
			this.signup(signupUsername.getText().toString(), signupPassword.getText().toString(), signupEmail.getText().toString());
			
			
		} else {
			// passwords do not match
			signupPassword.setText("");
			signupConfirmPassword.setText("");
			this.alertboxToSignup("Error", "Passwords do not match. Please retype the passwords then try again.");
		}
	}
	void showLogin() {
		RelativeLayout options_layout = (RelativeLayout)findViewById(R.id.login_layout);
		options_layout.setVisibility(View.VISIBLE);
		hideCamera();
		hideSignup();
		hideForgotPassword();
		hideSettings();
		return;
	}
	void hideLogin() {
		RelativeLayout login_layout = (RelativeLayout)findViewById(R.id.login_layout);
		login_layout.setVisibility(View.GONE);
		
	}
	void showSettings() {
		RelativeLayout settings_layout = (RelativeLayout)findViewById(R.id.settings_layout);
		settings_layout.setVisibility(View.VISIBLE);
		hideCamera();
		hideSignup();
		hideLogin();
		hideForgotPassword();
		return;
	}
	void hideSettings() {
		RelativeLayout options_layout = (RelativeLayout)findViewById(R.id.settings_layout);
		options_layout.setVisibility(View.GONE);
		
	}
	void showCamera() {
		RelativeLayout camera_layout = (RelativeLayout)findViewById(R.id.camera_layout);
		camera_layout.setVisibility(View.VISIBLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		hideLogin();
		hideSignup();
		hideSettings();
		hideForgotPassword();
	}
	void hideCamera() {
		RelativeLayout camera_layout = (RelativeLayout)findViewById(R.id.camera_layout);
		camera_layout.setVisibility(View.GONE);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		if (this.protoConn != null) {
			if (this.protoConn.isPlaying()) {
				this.protoConn.end();
			}
		}
	}

	void showSignup() {
		RelativeLayout signup_layout = (RelativeLayout)findViewById(R.id.signup_layout);
		signup_layout.setVisibility(View.VISIBLE);
		hideCamera();
		hideLogin();
		hideSettings();
		hideForgotPassword();
	}
	void hideSignup() {
		RelativeLayout signup_layout = (RelativeLayout)findViewById(R.id.signup_layout);
		signup_layout.setVisibility(View.GONE);
	}

	void showForgotPassword() {
		hideCamera();
		hideLogin();
		hideSettings();
		hideSignup();
		RelativeLayout forgotpassword_layout = (RelativeLayout)findViewById(R.id.forgotpassword_layout);
		forgotpassword_layout.setVisibility(View.VISIBLE);
	}
	void hideForgotPassword() {
		RelativeLayout forgotpassword_layout = (RelativeLayout)findViewById(R.id.forgotpassword_layout);
		forgotpassword_layout.setVisibility(View.GONE);
	}
	public void Login() {
		String username = etUsername.getText().toString();
		String password = etPassword.getText().toString();
		boolean aud = this.tbAudio.isChecked();
		int vidQuality = this.spinVideoQuality.getSelectedItemPosition();
		
		if ((username.length() > 0) && (password.length() > 0) && (username.matches("[a-zA-Z0-9]*"))) {
			this.phoneUser.setUsername(username);
			this.phoneUser.setPassword(password);
			if ((this.serverAddress == null) || (this.serverPort == 0)) {
				this.getServerInfo();
				this.phoneUser.setServerAddress(this.serverAddress);
				this.phoneUser.setServerPort(this.serverPort);
			}
			this.saveUser(username, password);
			//this.savePref(aud, vidQuality);	
			this.showCamera();
			this.protoConn = new ProtocolConnection(this.phoneUser, this.sHolder);
			this.protoConn.setupVideoSocket();
			this.protoConn.setVideoQuality(vidQuality);
			this.protoConn.setHandler(handler);
			this.protoConn.setPrivacy(this.privacy);
			this.protoConn.setServerAddress(this.serverAddress);
			this.protoConn.setServerPort(this.serverPort);
			this.begin();
		} else {
			if (!(username.matches("[a-zA-Z0-9]*"))) {
				this.alertboxToLogin("Error", "Username contains invalid characters.");
			}
			
		}
	}
	
	public void Logout() {
		if (this.protoConn != null) {
			this.protoConn.end();
			this.protoConn.stop();
			this.protoConn = null;
		}
		this.showLogin();
	}

	public void begin() {
		// Open TCP connection to server
		//setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		try {

			if (protoConn != null)	{
				if (this.protoConn.getState() == Thread.State.NEW) {
					Log.v("S", "Protocol Connection thread starting");
					protoConn.start();
				} else if (this.protoConn.getState() == Thread.State.TERMINATED) {
					Log.v("S", "Protocol Connection thread resuming");
					if (this.protoConn != null) this.protoConn.start();
					
			//		this.protoConn.run();
				} else {// else thread is still running
					
					Log.v("S", "Protocol Connection thread already running");
				}
			} 
		
		} catch (Exception ex) {
			Log.v("S", ex.getMessage());
		}
	} 
	public void postError(String error) {
		final String errMsg = this.protoConn.getErrorMsg();
		if (handler != null) {
			handler.post(new Runnable() {
			    public void run() {
			    	alertboxToLogin("Error", errMsg);
			    }
			  });
		}
	
	}
	public void alertboxToLogin(String title, String msg) {
		if (msg != null) {
			 new AlertDialog.Builder(this)
		      .setMessage(msg)
		      .setTitle(title)
		      .setCancelable(true)
		      .setNeutralButton(android.R.string.ok,
		    		  new DialogInterface.OnClickListener() {
		    	  public void onClick(DialogInterface dialog, int whichButton){
		        	 showLogin();
		         }
		         }).show();
		}
	}	
	public void alertboxInvalidPassword(String title, String msg) {
		if (msg != null) {
			 new AlertDialog.Builder(this)
		      .setMessage(msg)
		      .setTitle(title)
		      .setCancelable(true)
		      .setPositiveButton(R.string.btnFpReenter,
		    		  new DialogInterface.OnClickListener() {
		    	  public void onClick(DialogInterface dialog, int whichButton){
		        	 showLogin();
		         }
		         }).setNegativeButton(R.string.btnFpRecover,
		    		  new DialogInterface.OnClickListener() {
		    	  public void onClick(DialogInterface dialog, int whichButton){
		        	 showForgotPassword();
		         }
		         }).show();
		}
	}	
	public void alertboxToCamera(String title, String msg) {
		if (msg != null) {
			 new AlertDialog.Builder(this)
		      .setMessage(msg)
		      .setTitle(title)
		      .setCancelable(true)
		      .setNeutralButton(android.R.string.ok,
		    		  new DialogInterface.OnClickListener() {
		    	  public void onClick(DialogInterface dialog, int whichButton){
		        	 showCamera();
		         }
		         }).show();
		}	
	}
	public void alertboxToSignup(String title, String msg) {
		if (msg != null) {
			 new AlertDialog.Builder(this)
		      .setMessage(msg)
		      .setTitle(title)
		      .setCancelable(true)
		      .setNeutralButton(android.R.string.ok,
		    		  new DialogInterface.OnClickListener() {
		    	  public void onClick(DialogInterface dialog, int whichButton){
		        	 showSignup();
		         }
		         }).show();
		}
	}
	public void alertboxToForgotPassword(String title, String msg) {
		if (msg != null) {
			 new AlertDialog.Builder(this)
		      .setMessage(msg)
		      .setTitle(title)
		      .setCancelable(true)
		      .setNeutralButton(android.R.string.ok,
		    		  new DialogInterface.OnClickListener() {
		    	  public void onClick(DialogInterface dialog, int whichButton){
		        	 showForgotPassword();
		         }
		         }).show();
		}	
	}
	// make a thread with a cancel dialog
	public void signup(String username, String password, String email) {
		URL signupUrl = null;
		try {
			signupUrl = new URL("http://" + this.webAddress + "/?signup=1&device=androidapplication&username=" + username + "&password=" + password + "&email=" + email);
			//Log.v("S", "Opening url:  " + signupUrl);
			//signupUrl = new URL("http://" + this.httpServerAddress + ":" + this.httpServerPort + "/" + this.httpPath + "?signup=1&device=androidapplication&username=" + username + "&password=" + password + "&email=" + email);
			//signupUrl = new URL("http://" + this.webAddress + "/?signup=1&device=androidapplication&username=" + username + "&password=" + password + "&email=" + email);
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (signupUrl != null) {
	        URLConnection uc = null;
			try {
				uc = signupUrl.openConnection();
				if (uc != null) {
			        BufferedReader in;
					in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
			        String inputLine;
			        while ((inputLine = in.readLine()) != null) {
			        	Log.v("S", inputLine);
			        	String[] splitInput = inputLine.split("[|]");
			        	if (splitInput[0].compareTo("SUCCESS") == 0) {
			        		// set user name and password in login form
			        		etUsername.setText(username);
			        		etPassword.setText(password);
					        //this.alertboxToLogin("Success", "You may now login to yapastream.");
			        		this.alertboxToLogin(splitInput[1], splitInput[2]);
			        	} else if (splitInput[0].compareTo("ERROR") == 0) {
			        		/*int errCode=0;
			        		errCode = Integer.parseInt(splitInput[1]);
			        		String errStr;
			        		switch(errCode) {
			        			case 544: 
			        				errStr = "Username is taken";
			        				break;
			        			case 547:
			        				errStr = "Username too short.";
			        				break;
			        			case 546:
			        				errStr = "Username too long.";
			        				break;
			        			case 548:
			        				errStr = "Username contains invalid characters.";
			        				break;
			        			case 549:
			        				errStr = "Username error";
			        				break;
			        			case 554:
			        				errStr = "Password invalid";
			        				break;
			        			case 556:
			        				errStr = "Password too long";
			        				break;
			        			case 557:
			        				errStr = "Password too short";
			        				break;
			        			case 558: 
			        				errStr = "Password contains invalid characters.";
			        				break;
			        			case 559:
			        				errStr = "Password error";
			        				break;
			        			default:
			        				errStr = "Unable to parse responses. Please try again later.";
			        				break;
			        		}*/
					        this.alertboxToSignup(splitInput[2], splitInput[3]);
			        	}
			        	  //System.out.println(inputLine);
			        }
			          
			        in.close();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	// this should be a new thread that uses handler to post back; will avoid pause in ui
	public void forgotPassword(String username) {
		URL fpUrl = null;
		try {
			fpUrl = new URL("http://" + this.webAddress + "/?forgotpassword=1&device=androidapplication&username=" + username);
			//fpUrl = new URL("http://" + this.httpServerAddress + ":" + this.httpServerPort + "/" + this.httpPath + "?forgotpassword=1&device=androidapplication&username=" + username);
			//fpUrl = new URL("http://" + this.webAddress + "/?forgotpassword=1&device=androidapplication&username=" + username);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (fpUrl != null) {
	        URLConnection uc = null;
			try {
				uc = fpUrl.openConnection();
				if (uc != null) {
			        BufferedReader in;
					in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
			        String inputLine;
			        while ((inputLine = in.readLine()) != null) {
			        	Log.v("S", inputLine);
			        	String[] splitInput = inputLine.split("[|]");
			        	if (splitInput[0].compareTo("SUCCESS") == 0) {
			        		// set user name and password in login form
			        		etUsername.setText(username);
			        		etPassword.setText("");
			        		this.alertboxToLogin(splitInput[1], splitInput[2]);
					       // this.alertboxToLogin("Success", "An e-mail has been sent to you containing a link to reset your password.");
			        	} else if (splitInput[0].compareTo("ERROR") == 0) {
			        		int errCode = 0;
			        		errCode = Integer.parseInt(splitInput[1]);
			        		String errStr;
			        		switch (errCode) {
			        			case 561: 
			        				errStr = "Unable to find username.";
			        				// would you like to sign up?
			        				break;
			        			default:
			        				errStr = "An unknown error has occured.";
			     
			        		}
			        		this.alertboxToForgotPassword("Error",  splitInput[3]);
			        	}
			        	  //System.out.println(inputLine);
			        }
			        in.close();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}	
	public void getServerInfo() {
		URL webUrl = null;
		try {
			Log.v("S", "Loading url " + "http://" + this.webAddress + "/?deviceinfo=1&device=androidapplication");
			webUrl = new URL("http://" + this.webAddress + "/?deviceinfo=1&device=androidapplication");
			//fpUrl = new URL("http://" + this.httpServerAddress + ":" + this.httpServerPort + "/" + this.httpPath + "?forgotpassword=1&device=androidapplication&username=" + username);
			//fpUrl = new URL("http://" + this.webAddress + "/?forgotpassword=1&device=androidapplication&username=" + username);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (webUrl != null) {
	        URLConnection uc = null;
			try {
				uc = webUrl.openConnection();
				if (uc != null) {
			        BufferedReader in;
					in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
			        String inputLine;
			        while ((inputLine = in.readLine()) != null) {
			        	Log.v("S", inputLine);
			        	String[] splitInput = inputLine.split("[|]");
			        	if (splitInput[0].compareTo("SUCCESS") == 0) {
			        		// 1 = IP/HOST; 2 = PORT
			        		if (splitInput[1] != null) {
			        			this.serverAddress = splitInput[1];
			        			if (splitInput[2] != null) {
			        				this.serverPort = Integer.parseInt(splitInput[2]);
			        			} else {
			        				this.serverPort = 10083; // default port
			        			}
			        		} else {// default values
			        			this.serverAddress = "yapastream.com";
			        			this.serverPort = 10083;
			        		}
		        			Log.v("S", "Server address set to " + this.serverAddress);
		        			Log.v("S", "Server port set to " + this.serverPort);
			        	} else if (splitInput[0].compareTo("ERROR") == 0) {
			        		int errCode = 0;
			        		errCode = Integer.parseInt(splitInput[1]);
			        		String errStr;
			        		switch (errCode) {			        	
			        			default:
			        				errStr = "An unknown error has occured.";
			     
			        		}
			        		this.alertboxToForgotPassword("Error",  splitInput[3]);
			        	}
			        	//System.out.println(inputLine);
			        }
			        in.close();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	/*public void openSignup(String username) {
		Context context = getApplicationContext();  
		String url = "http://www.yapastream.com/?page=signup&device=androidappbrowser";  
		if (username != null) {
			url += "&username=" + username;
		}
		Intent i = new Intent(Intent.ACTION_VIEW);   
		Uri u = Uri.parse(url);   
		i.setData(u);   
		try {  
		  // Start the activity  
		  startActivity(i);  
		} catch (ActivityNotFoundException e) {  
		  // Raise on activity not found  
		  Toast toast = Toast.makeText(context, "Browser not found.", Toast.LENGTH_SHORT);  
		}  
	}
	public void openForgotPassword(String username) {
		Context context = getApplicationContext();  
		String url = "http://www.yapastream.com/?page=forgotpassword&device=androidappbrowser";
		if (username != null) {
			url += "&username=" + username;
		}
		Intent i = new Intent(Intent.ACTION_VIEW);   
		Uri u = Uri.parse(url);   
		i.setData(u);   
		try {  
		  // Start the activity  
		  startActivity(i);  
		} catch (ActivityNotFoundException e) {  
		  // Raise on activity not found  
		  Toast toast = Toast.makeText(context, "Browser not found.", Toast.LENGTH_SHORT);  
		}  
	}
	*/
	protected void alertbox(String title, String msg) {
		if (msg != null) {
		   new AlertDialog.Builder(this)
		      .setMessage(msg)
		      .setTitle(title)
		      .setCancelable(true)
		      .setPositiveButton(android.R.string.ok,
		    		  new DialogInterface.OnClickListener() {
		    	  public void onClick(DialogInterface dialog, int whichButton){
		        	 
		         	}
		         })
		         .setNegativeButton(android.R.string.cancel,
		        		 new DialogInterface.OnClickListener() {
		        	 public void onClick(DialogInterface dialog, int whichButton){
		        	 
		        	 }
		         })
		      .show();
		 }
	}
}
