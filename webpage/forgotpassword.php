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

class ForgotPassword {
	private $username;
	private $confirmationCode;
	private $email;
	private $password;
	private $errMsg;
	function ForgotPassword($user) {
		$this->username = $user;
		$this->confirmationCode = $this->generateConfirmation();
		$this->errMsg = "";
	
	}
    function submit() {
		require("config.php");
		require("sql.php");
		$retval = 0;
		if (strlen($this->username) > $MAX_USERNAME_LENGTH) {
			$this->sendError(546, "Username too long. It must contain no more than " . $MAX_USERNAME_LENGTH . " characters.");
			$retval = 546;
		} else if (strlen($this->username) < $MIN_USERNAME_LENGTH) {
			$retval = 547;
			$this->sendError(547, "Username too short. It must contain at least " . $MIN_USERNAME_LENGTH . " characters.");
		} else if (preg_match("/[^A-Za-z0-9]/", $this->username)) {
			$retval = 548;
			$this->sendError(548, "Username contains invalid characters.  It must only contain alphanumeric symbols. A-Z and 0-9.");
			
		}
		if ($retval != 0) return $retval;
		
		$handle = mysql_connect($sqlserver, $sqluser, $sqlpass) or die("SQL connection failed");
		mysql_select_db($sqldb, $handle);
		
		$eusername = mysql_real_escape_string(strtolower($this->username));
		$econfirmation = mysql_real_escape_string($this->confirmationCode);
		$etime = mysql_real_escape_string(strtotime("now"));
		
		$query = sprintf("INSERT INTO passwordConfirmation (username, confirmation_code, create_date) VALUES ('%s','%s','%s')", 
			$eusername, $econfirmation, $etime);
			
		$result = mysql_query($query, $handle);
		
		if (mysql_affected_rows() > 0) { // success
			$query = sprintf("SELECT email FROM users WHERE username='%s'", $eusername);
			$result = mysql_query($query, $handle);
			if (!$result) {
				$this->sendError(561,"Unable to find user  " . $this->username);
			} else {
				if ($row = mysql_fetch_assoc($result)) {
					$this->email = $row['email'];
					$this->sendConfirmation();
					$this->sendSuccess("Success", "An e-mail has been sent to you containing a link to reset your password.");
				} else { // error, no rows found
					$this->sendError(561, "Unable to find user " . $this->username);
				}
			}
		} else { // error, unable to insert password confirmation
			$this->sendError(771, "Server database error: It's not your fault. Contact server administrator. ErrMsg: the server does not have write permissions or the database does not exist");
		}
		mysql_close($handle);
	}
	// returns -2 if invalid confirmation
	// returns -1 if confirmation has expired
	// returns 0 on sql error
	// returns > 0 on success
	function resetPassword($pass, $confirmation) {	
		$retval = 0;
		require("sql.php");
		$handle = mysql_connect($sqlserver, $sqluser, $sqlpass) or die("SQL connection failed");
		mysql_select_db($sqldb, $handle);
		
		$eusername = mysql_real_escape_string(strtolower($this->username));
		$econfirmation = mysql_real_escape_string($this->confirmationCode);
		$etime = mysql_real_escape_string(strtotime("now"));
		
		$query = sprintf("SELECT id,create_date,valid FROM passwordConfirmation WHERE username=%s AND confirmationCode=%s", 
			$eusername, $econfirmation, $etime);
			
		$result = mysql_query($query, $handle);
		$id = "";
		$created = "";
		$valid = "";
		if ($row = mysql_fetch_assoc($result)) { // success
			$id = $row['id'];
			$created = $row['create_date'];
			$valid = $row['valid'];
			
			if ((strtotime("-3days") > $created) && ($valid == 1)) {
				$retval = $this->updatePassword($pass);
			} else {// confirmation expired
				$retval = -1;
			}
		} else { // error, unable to select id
			$retval = -2;
		}
	    if ($retval > 0) {
			$query = sprintf("UPDATE `passwordConfirmation` SET valid=%s WHERE id=%s", 1, $id);
			$result = mysql_query($query);
			if (mysql_rows_affected() < 0) { // Log error, unable to invalidate password confirmation code
			}
		}
		mysql_close($handle);
		return $retval;
	 }
	// returns 0 on sql error
	// returns > 0 on success
	function updatePassword($pass) {
		$handle = mysql_connect($sqlserver, $sqluser, $sqlpass) or die("SQL connection failed");
		mysql_select_db($sqldb, $handle);
		
		$eusername = mysql_real_escape_string(strtolower($this->username));
		$epassword = mysql_real_escape_string($this->password);
		
		$query = sprintf("UPDATE `users` SET password=%s WHERE username=%s", $epassword, $eusername);
		mysql_query($query);
		$id = mysql_insert_id();
		mysql_close($handle);
		return $id;
	}
	function sendConfirmation() {
		$to = $this->email;
		$header = "";
		$resetLink = sprintf("http://www.yapastream.com/?forgotpassword=1&username=%s&confirmationCode=%s", $this->username, $this->cofirmationCode); 
		$message = "<a href=\"" . $resetLink . "\">Click here</a> to reset your password.";
	}
	function generateConfirmation() {
		$confirmation = substr(str_shuffle(str_repeat('ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789',9)),0,9);
		return $confirmation;
	}
	function sendError($errCode, $errMsg) {
		echo "ERROR|" . $errCode . "|Error|" . $errMsg;
	}
	function sendSuccess($title, $msg) {
		echo "SUCCESS|" . $title . "|" . $msg;
	}
}
class forgotPasswordPage {
	private $extraScripts;
	private $title = "Yapastream - Forgot password";
	function forgotPasswordPage() {
	
	}
	function getHeader() {
		echo "<html><head><title>" . $this->title . "</title>";
		echo "<script type='text/javascript' src='/player/jwplayer.js'></script>\n" . $this->extraScripts .  " </head><body>";
	}
	function getFooter() {
		echo "</html>";
	}
	function loadForgotPasswordPage() {
		$this->getHeader();
		echo "<table id=\"forgotpassword\">\n";
		echo "<tr><td>Username:</td><td><input type=\"text\" value=\"" . $username . "\" name=\"username\"></td></tr>\n";		
		echo "</table>";
		$this->getFooter();
	}

}

?>
