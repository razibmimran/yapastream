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
class Signup {
  private $username;
  private $password;
  private $email;
  private $location;
  private $id;
  function Signup($user, $pass, $mail) {
    $this->username = $user;
    $this->password = $pass;
    $this->email = $mail;
  }
  function setLocation($loc) {
    $this->location = $loc;
	if ($this->id != null) {
		require("sql.php");
		$handle = mysql_connect($sqlserver, $sqluser, $sqlpass) or die("SQL connection failed");
		mysql_select_db($sqldb, $handle);
		$elocation = mysql_real_escape_string($this->location);
		$eid = mysql_real_escape_string($this->id);
		$query = sprintf("UPDATE `users` SET location=%s WHERE id=%s;", $elocation, $eid);
		$result = mysql_query($query, $handle);
		
		if (mysql_affected_rows() > 0) { // success
		
		} else { // error
			$id = 0;
		}
	   
		mysql_close($handle);
	}
  }
  function validateUser() {
    require("sql.php");
	require("config.php");
	$retval = 0;
	$handle = mysql_connect($sqlserver, $sqluser, $sqlpass) or die("SQL connection failed");
	mysql_select_db($sqldb);
	$eusername = mysql_real_escape_string(strtolower($this->username));
	$query = sprintf("SELECT id FROM users WHERE username='%s'", $eusername);
	$result = mysql_query($query);
	if ($result == null) {
		$this->sendError(754, "SQL Database error validating the user.");
		$retval = 754;
	} else if (mysql_num_rows($result) > 0) {
		$this->sendError(544, "Username taken.");
		$retval = 544;
	} else if (strlen($this->username) > $MAX_USERNAME_LENGTH) {
		$this->sendError(546, "Username too long. It must contain no more than " . $MAX_USERNAME_LENGTH . " characters." .	 $this->username);
		$retval = 546;
	} else if (strlen($this->username) < $MIN_USERNAME_LENGTH) {
		$retval = 547;
		$this->sendError(547, "Username too short. It must contain at least " . $MIN_USERNAME_LENGTH . " characters.");
	} else if (preg_match("/[^A-Za-z0-9]/", $this->username)) {
		$retval = 548;
		$this->sendError(548, "Username contains invalid characters.  It must only contain alphanumeric symbols. A-Z and 0-9.");
	} else {
		$retval = 1;
	}
	mysql_close($handle);
	return $retval;
  }
  function validatePassword() {
	$retval = 0;
	require("config.php");
	if (strlen($this->password) > $MAX_PASSWORD_LENGTH) {
		$this->sendError(546, "Password too long. It must contain no more than " . $MAX_PASSWORD_LENGTH . " characters.");
		$retval = 546;
	} else if (strlen($this->password) < $MIN_PASSWORD_LENGTH) {
		$retval = 547;
		$this->sendError(547, "Password too short. It must contain at least " . $MIN_PASSWORD_LENGTH . " characters." . strlen($this->password));
	} else if (preg_match("/[^A-Za-z0-9]/", $this->password)) {
		$retval = 548;
		$this->sendError(548, "Password contains invalid characters.  It must only contain alphanumeric symbols. A-Z and 0-9.");
	} else {
		$retval = 1;
	}
	return $retval;
  
  }
  function validateEmail() {
	if (filter_var($this->email, FILTER_VALIDATE_EMAIL) != $this->email) {
		// invalid email
			$retval = 551;
			$this->sendError(551, "Invalid Email address.");
	} else {// valid email
		$retval = 1;
	}
	return $retval;
  }
  function validate() {
	if (($this->validateUser() == 1) && ($this->validatePassword() == 1) && ($this->validateEmail() == 1)) {
		return true;
	} else {
		return false;
	}
  }
  function submit() {
    $valid = $this->validate();
	if ($valid != true) return $valid; // be sure username and password are valid.
	
    require("sql.php");
     $handle = mysql_connect($sqlserver, $sqluser, $sqlpass) or die("SQL connection failed");
     mysql_select_db($sqldb, $handle);
	 $eusername = mysql_real_escape_string(strtolower($this->username)); // always insert lower case names
	 $epassword = mysql_real_escape_string($this->password);
	 $eemail = mysql_real_escape_string($this->email);
    $query = sprintf("INSERT INTO users (username, password, email) VALUES ('%s', '%s', '%s')", $eusername, $epassword, $eemail);
    $result = mysql_query($query, $handle);
    
	if (mysql_affected_rows() > 0) { // success
		$id = mysql_insert_id();
		$this->sendSuccess("Signup auccessful.", "You may now login.");
	} else { // error
		$id = 0;
	}
   
    mysql_close($handle);
	$this->id = $id;
	return $id;
  }
  function sendError($errCode, $errMsg) {
		echo "ERROR|" . $errCode . "|Error|" . $errMsg;
	}
	function sendSuccess($title, $msg) {
		echo "SUCCESS|" . $title . "|" . $msg;
	}
  
}

class signupPage {
	private $title = "Yapastream Sign up";
	private $username = "";
	private $email = null;
	private $location = null;
	private $header = "";
	function signupPage() {
	
	}
	function loadSignupPage() { // Load with ajax??  / INCLUDE SUBMIT/CANCEL button
		
		$this->getHeader();
		
		echo "<script language=\"javascript\" type=\"text/javascript\" src=\"js/signup.js\"></script>\n";
		echo "<script language=\"javascript\" type=\"text/javascript\" src=\"js/ajaxlib.js\"></script>\n";

		echo "<table id=\"signupTable\">\n";
		echo "<tr><td>Username:</td><td><input type=\"text\" value=\"" . $username . "\" id=\"username\" name=\"username\"></td></tr>\n";		
		echo "<tr><td>Password:</td><td><input type=\"password\" value=\"\" id=\"password\" name=\"password\"></td></tr>\n";
		echo "<tr><td>Confirm Password:</td><td><input type=\"password\" value=\"\" id=\"confirmPassword\" name=\"confirmPassword\"></td></tr>\n";
		echo "<tr><td>Email:</td><td><input type=\"text\" value=\"" . $email . "\" id=\"email\" name=\"email\"></td></tr>\n";
		echo "<tr><td>Location:</td><td><input type=\"text\" value=\"" . $location . "\" id=\"location\" name=\"location\"></td></tr>\n";
		echo "<tr><td></td><td><input type=\"button\" name=\"signup\" value=\"Signup\" onclick=\"submitSignup()\"></td></tr>\n";
		echo "</table>\n";
		
		$this->getFooter();
	}
	function getHeader() {
		echo "<html><head><title>" . $this->title . "</title>";
		if ($this->header != "") echo $this->header;
		echo "</head><body>";
	}
	function addHeader($headerText) {
		$this->header .= $headerText;
	}
	function getFooter() {
		echo "</html>";
	}
	
}
?>
