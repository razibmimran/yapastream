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
class Settings {
	function Settings() {
	}
	function getPrivacy($username) {
 		require("sql.php");
                $handle = mysql_connect($sqlserver, $sqluser, $sqlpass) or die("SQL connection failed");
                mysql_select_db($sqldb);
                $eusername = mysql_real_escape_string($username);
		$query = sprintf("SELECT privacy FROM settings WHERE username='%s'", $eusername);
                $result = mysql_query($query);
		$privacy = 0;
		if ($row = mysql_fetch_assoc($result)) {
			$privacy = $row['privacy'];
		}
		mysql_close($handle);
		return $privacy;
	}


}




?>
