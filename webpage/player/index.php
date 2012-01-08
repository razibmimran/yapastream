<html>
<head>
<script type='text/javascript' src='/test/jwplayer.js'></script> 
</head>
<body>
<div id='mediaspace'>This text will be replaced</div>
<script type='text/javascript'>
  jwplayer('mediaspace').setup({
    'flashplayer': '/test/player.swf',
    'controlbar': 'none',
    'file': 'http://yapastream.com/test/stream/<?php echo $_GET['user']; ?>.flv',
    'autostart': 'true',
    'width': '352',
    'height': '288'
  });
</script>

</html>
