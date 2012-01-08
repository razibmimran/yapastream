<?php
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
if (!(empty($_GET['signup']))) {
        $signup = $_GET['signup'];
} else {
        $signup = 0;
}
if (!(empty($_GET['forgotpassword']))) {
        $forgotpassword = $_GET['forgotpassword'];
} else {
        $forgotpassword = 0;
}
if (!(empty($_GET['login']))) {
        $login = $_GET['login'];
} else {
        $login = 0;
}
if (!(empty($_GET['deviceinfo']))) {
        $deviceinfo = $_GET['deviceinfo'];
} else {
        $deviceinfo = 0;
}
if (!(empty($_GET['view']))) {
        $view = $_GET['view'];
} else {
        $view = 0;
}


if ($signup == 1) {
	require("signup.php");
	if ((isset($_GET['username'])) && (isset($_GET['password'])) && (isset($_GET['email']))) {// sign user up
		$user = $_GET['username'];
		$pass = $_GET['password'];
		$mail = $_GET['email'];
		$signupUser = new Signup($user, $pass, $mail);
		$signupUser->submit();
	} else {// load sign up page
		$signupPage = new signupPage();
		$signupPage->loadSignupPage();
	}
  
   
}  else if ($forgotpassword == 1) {
  require("forgotpassword.php");
  if (isset($_GET['username'])) {
	$user = $_GET['username'];
	$fpUser = new ForgotPassword($user);
	$fpUser->submit();
  } else {
	$fpPage = new ForgotPasswordPage();
	$fpPage->loadForgotPasswordPage();
  }
  
} else if ($login == 1) {
  require("login.php");
} else if ($deviceinfo == 1) {
  require("deviceinfo.php");
  $deviceInfo = new DeviceInfo();
  $deviceInfo->getDeviceInfo();
} else if ($view) { // main page for iframe
  require("view.php"); // view the streams
  $view = $_GET['view'];
  $page = $_GET['page'];
  $username = $_GET['username'];
  $viewer = new View();
  
  if ($view == "all") {
	if (isset($page)) $viewer->loadAllPage($page);
	else $viewer->loadAllPage(1);
  } else if ($view == "user") {
	require("settings.php");
	$settings = new Settings();
	$privacy = $settings->getPrivacy($username);
	if ($privacy == 2) {  // password required
		require("auth.php");
		$auth = new Auth();
		if (empty($_SERVER['PHP_AUTH_DIGEST'])) {
			$auth->prompt();
			$digest = $_SERVER['PHP_AUTH_DIGEST'];
			$verified = $auth->verifyUser($digest);
		} else {
			$digest = $_SERVER['PHP_AUTH_DIGEST'];
			$verified = $auth->verifyUser($digest);
			if ($verified == 0) {
				$auth->prompt();
				$digest = $_SERVER['PHP_AUTH_DIGEST'];
				$verified = $auth->verifyUser($digest);
			}
			$memberUsername = $auth->getUsername($digest);
		}
		if ($verified == 1) { // valid password, look up authorization list
			$member = $auth->verifyMembership($username, $memberUsername);
			if ($member == 1) { 
				$viewer->loadUserPage($username);
			} else {
				$viewer->unauthorized($username);
			}
		} 		
	} else {
		if ($username != "") $viewer->loadUserPage($username); 
		else $viewer->loadAllPage(1);
	}
  } else {
	if (isset($page)) $viewer->loadAllPage($page);
	else $viewer->loadAllPage(1);
  }

} else {// to load main wordpress site
  define('WP_USE_THEMES', true);
  require('./wp/wp-blog-header.php');
// require("view.php"); // view the streams
//  $viewer = new View();
//  $viewer->loadAllPage(1);

}
?>
