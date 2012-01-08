var ajaxreq=false;
var ajaxCallback = false;

function initRequest() {
    ajaxreq = false;
    try {
        ajaxreq = new XMLHttpRequest(); // Firefox, IE7, Others
    } catch (error) {
        try {
            // IE5, IE6
            ajaxreq = new ActiveXObject("Microsoft.XMLHTTP");
        } catch (error)  {
            alert('Error creating Ajax request.  If you continue to receive this message please contact the web administrator.');
            return false;
        }
    }
    return true;
}
function getRequest(url, callback) {
    if (initRequest()) {
        ajaxCallback = callback;
        ajaxreq.open("GET", url, true);
        ajaxreq.onreadystatechange = rsChange;
        ajaxreq.send(null);
    }
}
function rsChange() {
    if (ajaxreq.readyState != 4) return;
    if (ajaxreq.status==200) { // Request succeeded
        if (ajaxCallback) {
            ajaxCallback(ajaxreq.responseText);
            ajaxCallback = false;
        } else {
            alert("Callback missing.");
        }
    } else {
        ajaxCallback = false;
    }
}
