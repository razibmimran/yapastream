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
require("../config.php");
set_time_limit(0);
//register_shutdown_function('shutdown');
ignore_user_abort(false);

if ($_GET['user']) {
$user = @ereg_replace("[^A-Za-z0-9]","", $_GET['user']); 
} else {
  $user = "Justin";
}

header("Expires: Thu, 19 Nov 1981 08:52:00 GMT");
header("Last-Modified: " . gmdate("D, d M Y H:i:s") . " GMT");
header("Cache-Control: no-store, no-cache, must-revalidate");
header("Pragma: no-cache");
header("Content-Type: application/octet-stream");
flush(); 
$fileName = $user . ".flv";
$file = $baseDir . "/" . $flvFolder . "/" . $fileName;

if(file_exists($file) && (strrchr($fileName, '.') == '.flv') && (strlen($fileName) > 2)) {
	$fh = fopen($file, 'rb') or die ('Unable to open ' + $fileName);
	$seekPos = filesize($file);
    echo 'FLV' . pack('C', 1) . pack('C', 1) . pack('N', 9) . pack('N', 9);
	$filesizeNow = filesize($file)+1;
	$filesizePrevious = filesize($file);
	//while ($filesizeNow != $filesizePrevious) {
	while (true) {
		$filesizeNow = trim(`stat -c%s $file`); // because the PHP filesize function does not update 
		if ($filesizePrevious < $filesizeNow) {
			rewind($fh);
			fseek($fh, $filesizePrevious);
			echo fread($fh, ($filesizeNow-$filesizePrevious)); 
		}
		flush();
		sleep(1);
		$filesizePrevious = $filesizeNow;
	}
}

ob_implicit_flush();
ob_end_flush(); 

//while (($p['running']) && (!connection_aborted())) {
//	echo stream_get_contents($pipes[1],1024);
//	flush();
	//ob_flush();
//	if (connection_aborted()) break;
//	if (connection_status() != CONNECTION_NORMAL) break;
	
//}

//posix_kill($p['pid'], SIGINT);
//if ($p['running']) sleep(1);
//if ($p['running']) sleep(2);
//if ($p['running']) posix_kill($p['pid'], SIGKILL);
//proc_close($proc);

//function shutdown() {
//	flush();
//	if ($p['running']) posix_kill($p['pid'], SIGINT);
//	if ($p['running']) sleep(1);
//	if ($p['running']) sleep(2);
//	if ($p['running']) posix_kill($p['pid'], SIGKILL);
//	proc_close($proc);
//}



?>
