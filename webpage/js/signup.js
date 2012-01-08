function submitSignup() {
	var minUsername = 4;
	var maxUsername = 16;
	var minPassword = 6;
	var maxPassword = 30;
	var minEmail = 5;
	var maxEmail = 60;
	var username = document.getElementById("username");
	var password = document.getElementById("password");
	var confirmPassword = document.getElementById("confirmPassword");
	var email = document.getElementById("email");
	var location = document.getElementById("location");
	
	var errorMsg = "";
	var errorOccured = false;
	if (username == null) {
		errorMsg += "Username is missing.\n";
		errorOccured = true;
	} else if (username.value.length < minUsername) {
		errorMsg  +=  "Username is not long enough.  Please use a username with more than " + minUsername + " characters.\n";
		errorOccured = true;
	} else if (username.value.length > maxUsername) {
		errorMsg  +=  "Username is too long.  Please use a username with less than " + minUsername + " characters.\n";
		errorOccured = true;
	}
	if (password == null) {
		errorMsg += "Password is missing.\n";
		errorOccured = true;
	} else if (password.value.length < minPassword) {
		errorMsg += "The password does not meet the minumum length requirements.  Please use a password with at least " + minPassword + " characters.\n";
		errorOccured = true;
	} else if (password.value.length > maxPassword) {
		errorMsg += "Your password is too long.  Please use a password less than " + maxPassword + " characters.\n";
		errorOccured = true;
	} else if (confirmPassword.value != password.value) {
		errorMsg += "Passwords do not match.\n";
		errorOccured = true;
	}
	if (confirmPassword == null) {
		errorMsg += "Confirmation password is missing.\n";
		errorOccured = true;
	} 
	if (email == null) {
		errorMsg += "Email is missing.\n";
		errorOccured = true;
	} else if (email.value.length < minEmail) {
		errorMsg += "Please enter a valid email address.\n";
		errorOccured = true;
	}
	if (errorOccured == true) {
		//errorMsg += "Please completely fill out the form.";
		alert(errorMsg);
	}
	if (errorOccured == false) {
		var url = "?signup=1&username=" + username.value + "&password=" + password.value + "&email=" + email.value;
		if (location != null) {
			if (location.length > 0) {
					url += "&location" + location.value;
				}
		}
		getRequest(url, signupCallback);
	}
}
function signupCallback(resp) {	
	var respSplit = resp.split("|");
	if ((respSplit[0] != null) && (respSplit[1] != null)) {
		if (respSplit[0] == "ERROR") {
			if (respSplit[3] != null) {
				alert(respSplit[3]);
			}
		} else if (respSplit[0] == "SUCCESS") {
			if (respSplit[2] != null) {
				alert(respSplit[2]);
			}
		}
	}
}