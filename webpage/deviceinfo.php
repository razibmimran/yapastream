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
class DeviceInfo {
        private $serverAddress;
        private $serverPort;
	private $rtn = "\r\n";
		private $serverTimeout;
        public function DeviceInfo() {
                require("config.php");
                $this->serverAddress = $deviceAddress;
                $this->serverPort = $devicePort;
				$this->serverTimeout = $deviceTimeout;
        }
        public function getDeviceInfo() {
                $this->sendSuccess();
        }
	public function sendError($errCode, $errMsg) {
		echo "ERROR" . $this->rtn . 
			"Code:" . $errCode . $this->rtn . 
			"Message:" . $errMsg . $this->rtn; 
	}
        public function sendSuccess() {
                echo "SUCCESS" . $this->rtn . 
				"Address: " . $this->serverAddress . $this->rtn . 
				"Port: " . $this->serverPort . $this->rtn . 
				"Timeout: " . $this->serverTimeout . $this->rtn;
        }
}
?>
