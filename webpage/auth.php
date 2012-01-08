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
class Auth {
	private $realm = "yapastream";
	function Auth() {
	}
	function prompt() {
		header("HTTP/1.0 401 Unauthorized");  
		//header('WWW-Authenticate: Basic realm="Enter your username and password for access."'); 
		header('WWW-Authenticate: Digest realm="'.$this->realm.'",qop="auth",nonce="'.uniqid().'",opaque="'.md5($this->realm).'"');
		// Display message if user cancels dialog  <br>
		$this->unauthorized("");
	}
	// $digest = $_SERVER['PHP_AUTH_DIGEST']
	function verifyUser($digest) {
		$data = $this->http_digest_parse($digest);
	        require("sql.php");
                $handle = mysql_connect($sqlserver, $sqluser, $sqlpass) or die("SQL connection failed");
                mysql_select_db($sqldb);
                $eusername = mysql_real_escape_string($data['username']);
		$query = sprintf("SELECT password FROM users WHERE username='%s'", $eusername);
                $result = mysql_query($query);
		$pass = "";
		if ($row = mysql_fetch_assoc($result)) {
			$pass = $row['password'];
		}
		if ($pass != "") { 
			$A1 = md5($data['username'] . ':' . $this->realm . ':' . $pass);
			$A2 = md5($_SERVER['REQUEST_METHOD'].':'.$data['uri']);
			$valid_response = md5($A1.':'.$data['nonce'].':'.$data['nc'].':'.$data['cnonce'].':'.$data['qop'].':'.$A2);
			if ($data['response'] == $valid_response) {
				$retval = 1;
			} else {
				$retval = 0; // invalid password
			}
		} else {
			$retval = 0; // No password set
		}
		mysql_close($handle);
		return $retval;
	}
	function verifyGroup($user, $group) {

	}
	function getUsername($digest) {
		$data = $this->http_digest_parse($digest);
		return $data['username'];
	}
	function verifyMembership($username, $memberUsername) { // is $username a member of member's list
		$uid = $this->getUserId($username);
		$mid = $this->getUserId($memberUsername);
		$retval = 0;
		if (($mid != 0) && ($uid != 0)) {
		        require("sql.php");
	                $handle = mysql_connect($sqlserver, $sqluser, $sqlpass) or die("SQL connection failed");
                	mysql_select_db($sqldb);
			$euid = mysql_real_escape_string($uid);
			$emid = mysql_real_escape_string($mid);
			$query = sprintf("SELECT id FROM access_list WHERE username_id=%d AND member_id=%d", $euid, $emid);
	                $result = mysql_query($query, $handle);
			$id = 0;
			if ($result) {
				if ($row = mysql_fetch_assoc($result)) {
					$id = $row['id'];	
				}
				if ($id != 0) { // user has access
					$retval = 1;
				} else { // user does not have access
					$retval = 0;
				}
			} else {
				$retval = 0;
			}
		} else {
			$retval = -1; // User not found
		}
		mysql_close($handle);
		return $retval;
	}
	function unauthorized($msg) {
		print <<<EOF
		<HTML>
		<HEAD><TITLE>Authorization Failed</TITLE></HEAD>
		<BODY>
		<H1>Authorization Failed</H1>  
		<P>
		You must provide a valid username and password in order to access this page.
		<BR><BR>
		$msg
		</P>
		</BODY> 
		</HTML>
EOF;
	}
	function http_digest_parse($txt) {
    		$needed_parts = array('nonce'=>1, 'nc'=>1, 'cnonce'=>1, 'qop'=>1, 'username'=>1, 'uri'=>1, 'response'=>1);
    		$data = array();
    		$keys = implode('|', array_keys($needed_parts));
    		preg_match_all('@(' . $keys . ')=(?:([\'"])([^\2]+?)\2|([^\s,]+))@', $txt, $matches, PREG_SET_ORDER);
		foreach ($matches as $m) {
        		$data[$m[1]] = $m[3] ? $m[3] : $m[4];
        		unset($needed_parts[$m[1]]);
    		}
    		return $needed_parts ? false : $data;
	}
	function getUserId($username) {
	        require("sql.php");
                $handle = mysql_connect($sqlserver, $sqluser, $sqlpass) or die("SQL connection failed");
                mysql_select_db($sqldb);
		$eusername = mysql_real_escape_string($username);
		$query = sprintf("SELECT id FROM users WHERE username='%s'", $eusername);
                $result = mysql_query($query);
		$pass = "";
		$id = 0;
		if ($row = mysql_fetch_assoc($result)) {
			$id = $row['id'];
		}
		mysql_close($handle);
		return $id;

	}
}
?>
