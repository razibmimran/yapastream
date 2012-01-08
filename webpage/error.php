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
class Error {
  private $level=1;
  
  function getMessage($errCode) {
  
  }
  function setLogLevel($lev) {
	$this->level = $lev;
  }
  function Log($message) {
	if ($level == 4) {// debug extremely verbose to database
		require("sql.php");
		
	
	} else if ($level == 3) {// debug extremely verbose to file
	
	} else if ($level == 2) {// debug verbose to database
		require("sql.php");
		
	} else if ($level == 1) {// debug verbose to file
	
	} else { //no logging
	
	}
  }


}

?>