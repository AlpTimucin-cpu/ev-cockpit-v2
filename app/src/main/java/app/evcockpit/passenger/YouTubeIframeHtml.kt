package app.evcockpit.passenger

/** Official YouTube IFrame Player API; only a strict 11-char video ID is interpolated. */
object YouTubeIframeHtml {
    fun render(videoId: String): String {
        require(Regex("^[A-Za-z0-9_-]{11}$").matches(videoId))
        return """
<!doctype html><html lang="tr"><head>
<meta name="viewport" content="width=device-width,initial-scale=1,user-scalable=no">
</head><body style="margin:0;background:#000;height:100vh">
<div id="player"></div>
<script src="https://www.youtube.com/iframe_api"></script>
<script>
  function onYouTubeIframeAPIReady() {
    new YT.Player('player', {
      height:'100%', width:'100%', videoId:'VIDEO_ID',
      playerVars:{playsinline:1}
    });
  }
</script></body></html>""".replace("VIDEO_ID", videoId)
    }
}
