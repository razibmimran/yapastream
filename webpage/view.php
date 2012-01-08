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

class View {
	// viewAll
	private $title = "Yapastream active streams";
	private $player;
	private $width;
	private $height;
	private $server;
	private $streamsFolder;
	private $onload;
	private $extraScripts;
	
	function View() {
		require("config.php");
		$this->server = $webServer;
		$this->streamsFolder = $streamsFolder;
		$this->player = $flashPlayer;
		$this->width = $videoWidth;
		$this->height = $videoHeight;
		$this->jpgFolder = $jpgFolder;
	}
	function loadAllPage($page) {
		// 3 video stream x 3 video stream newest order (SELECT username FROM streams WHERE active=1 SORT BY create_date LIMIT 0,9) 

		$streamsPageWidth = 3;
		$streamsPageHeight = 3;
		$streamsPageTotal = $streamsPageWidth * $streamsPageHeight;
		if (($page <= 0) || (!(isset($page)))) $page = 1; 
		$m = 1;
		
		require("sql.php");
		$users = array();
		$id = array();
		$privacy = array();
		$handle = mysql_connect($sqlserver, $sqluser, $sqlpass) or die("SQL connection failed");
		mysql_select_db($sqldb);
		$epageStart = ($page-1)*$streamsPageTotal;
		$epageEnd = $page*$streamsPageTotal;
		$query = sprintf("SELECT streams.id id,streams.username username, settings.privacy privacy FROM streams, settings WHERE streams.active=1 AND streams.username = settings.username AND settings.privacy = 0 ORDER BY streams.create_date LIMIT %d,%d", $epageStart, $epageEnd);
		$result = mysql_query($query);
		if ($result) {
			while ($row=mysql_fetch_assoc($result)) {
					$users[$m] = $row['username'];
					$id[$m] = $row['id'];
					//$this->onload .= $this->getEmbedLoadVideo($users[$m]);
					$m++;
			}
			$this->extraScripts .= $this->getEmbedLoadPopup();
			$g = 1;
			//$query = sprintf("SELECT privacy FROM settings WHERE username=%s", $users[$g]);
			//$result = mysql_query($query);
			//if ($result) {
			//	while ($row=mysql_fetch_assoc($result)) {
			//		$privacy[$g] = $row['privacy'];
					//$this->onload .= $this->getEmbedLoad($users[$m]);
			//		$g++;
			//	}
			//}
		} else { // No users
			echo $this->getHeader();
			echo "No users are currently logged in. Login now!";
			echo $query;
			$this->getFooter();
			mysql_close();
			exit;
		}
		
		echo $this->getHeader();
		echo $this->getVideoDiv();
		echo '<table id="streams">';
		$l = 0;
		for ($i=1; $i<=$streamsPageWidth; $i++) {
			echo "<tr>";
			for ($j=1; $j<=$streamsPageHeight; $j++) {
				echo '<td>';
				$l++;
				if (isset($users[$l])) {
					
					echo $this->getJpegImg($users[$l]);
					echo "<br>User: " . $users[$l] . " <br><br>";
				}
				echo '</td>';
			}
			echo '</tr>';
		}
		echo '</table>';
		$this->getFooter();
		mysql_close($handle);
	}
	function getPreviousNext($page) {
		if ($page > 1) echo "<a href=\"view=all&page=" . $page-1 . "\">Previous</a>";
		echo "<a href=\"" . $page+1 . "\">Next</a>";
	}
	function getHeader() {
		$ret = "<html><head><title>" . $this->title . "</title>";
		$ret .= "<script type='text/javascript' src='/player/jwplayer.js'></script>\n";
		$ret .= "<script type='text/javascript' src='/js/view.js'></script>\n";
		$ret .= $this->extraScripts;
		$ret .= "</head><body onload=\"" . $this->onload . "\">";
		return $ret;
	}
	function getVideoDiv() {
		$ret =  "<div id=\"videoViewDialog\" title=\"User video\">User<br><br><br><br><br><br><br><br><br><br><br><br></div>";
		return $ret;
	}
	
	function getFooter() {
		return "</html>";
	}
	function getJpegImg($username) {
		$ret = "<img src=\"" . $this->jpgFolder . "/" . $username . ".jpg\" onclick=\"loadUserVideo('" . $username ."')\"><br>";
		return $ret;
	}
	function getEmbedLoadVideo($username) {
		//$ret = "<script type='text/javascript'>";
		//$ret .= "function loadUserVideo() {";
		$ret .= "jwplayer('vid_" . $username . "').setup({";
		$ret .= "'flashplayer': '" . $this->player . "',";
		$ret .= "'controlbar': 'none',";
		$ret .= "'file': 'http://" . $this->server . "/" . $this->streamsFolder . "/" . $username . ".flv',";
		$ret .= "'autostart': 'true',";
		$ret .= "'bufferlength': '0',";
		$ret .= "'width': '" . $this->width . "',";
		$ret .= "'height': '" . $this->height . "'";
		$ret .= "});";
		//$ret .= "}";
		//$ret .= "</script>";
		return $ret;
	}
	function getEmbedLoadPopup() {
		$ret = "<script language=\"javascript\" type=\"text/javascript\"  src=\"js/jquery.min.js\"></script>\n";
		$ret .= "<script language=\"javascript\" type=\"text/javascript\"  src=\"js/jquery-ui.min.js\"></script>\n";
		$ret .= "<link href=\"css/jquery-ui.css\" rel=\"stylesheet\" type=\"text/css\">\n";
		$ret .= "<script type=\"text/javascript\" >\n";
  		$ret .= "$(document).ready(function() {\n";
    	$ret .= "$(\"#" . "videoView" . "Dialog\").dialog({\n";
		$ret .= "autoOpen: false,\n";
		$ret .= "modal:true,\n";
		$ret .= "allowFullScreen: true,";
		$ret .= "width: 400,\n";
		$ret .= "height: 400});\n";
  		$ret .= "});\n";
		$ret .= "</script>\n";
		return $ret;
  }
	function getEmbedDivPopup() {
		//$ret = "<div id='vid_" . $username . "'>Error loading video</div>";
		$ret = "<div id=\"" . "videoView" . "Dialog\" title=\"" . "User video" . "\">" . "</div>\n";
		return $ret;
	}
	function loadUserPage($username) {
		// viewUser
		// stream information (on right of stream)
		// information in blogging style or static 
		require("sql.php");
		$handle = mysql_connect($sqlserver, $sqluser, $sqlpass) or die("SQL connection failed");
		mysql_select_db($sqldb);
		$eusername = mysql_real_escape_string($username);
		$query = sprintf("SELECT id FROM `users` WHERE username='%s'",$eusername);
		$result = mysql_query($query);
		$information = null;
		if ($result) {
			if (mysql_num_rows($result) > 0) {
			/*	$query = sprintf("SELCT information FROM profile WHERE username='%s'", $eusername);
				$result = mysql_query($query);
				if ($row=mysql_fetch_assoc($result)) {
					$information = $row['information'];
				}*/

				$query = sprintf("SELECT username FROM `streams` WHERE username='%s' AND active=%d", $username, 1);
				$result = mysql_query($query);
				if ($row=mysql_fetch_assoc($result)) {
					$this->userPage($username, $information);
				} else {
					$this->UserInactive();
				}
			} else { // user not found
				$this->UserNotFound($username);
			}
		} else {
			echo "SQL error connecting to load view of the users page" ;
		}
		mysql_close($handle);
		
	}
	function UserInactive() {
		echo $this->getHeader();
		echo "The user you request is not currently active.  Please try again later.";
		echo $this->getFooter();
	}
	function getEmbedScript($username) {
		$ret = "<script type='text/javascript' language='javascript'>\n";

		return $ret;
	}
	function userPage($username, $information) {
		$this->onload = $this->getEmbedLoadVideo($username);
		//$this->onload .= "loadUservideo(); ";
		echo $this->getHeader();
		//echo $this->getVideoDiv();
		echo "\n" . '<table id="userTable"><tr><td>Username:</td><td>' . $username . '</td></tr>';
		echo ($information != null) ? ('\n<tr><td>Information </td><td>' . $information . '</td></tr>') : "";
		echo "\n" . '<tr><td>Video:</td><td>';
		echo "\n" . '<div id="vid_' . $username . '"></div>';
		//echo $this->getEmbedLoadVideo($username);
		echo "\n</td></table>";
		echo $this->getFooter();
	}
	function UserNotFound($username) {
		echo "Unable to find user $username.";
	}
	function unauthorized($username) {
		echo "You are not in " . $username . "'s access list."; 
	}
}
?>
