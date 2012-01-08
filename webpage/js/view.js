function loadUserVideo(username) {
	var videoViewText = document.getElementById('videoViewDialog');
	videoViewText.innerHTML = "<div id='vid_" + username + "'>Error loading video</div>";
	videoViewText.innerHTML += "<a href=\"?view=user&username=" + username + "\">Click here to load in new window</a><br><br><br><br>";
	var userVideo = 'vid_' + username;
	var flashFile = 'streams/' + username + '.flv';
	//flashFile = 'player/video.mp4';
	jwplayer(userVideo).setup({
	'flashplayer': '/player/player.swf',
	'controlbar': 'none',
	'file': flashFile,
	'autostart': 'true',
	'bufferlength': '0',
	'width': '352',
	'allowFullScreen': 'true',
	'height': '288'});
	$( "#videoViewDialog" ).dialog( "open" );
}
function closeUserVideo() {
	$( "#videoViewDialog" ).dialog( "close" );
}