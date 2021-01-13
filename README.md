# gbdownloader
Lightweight Android app for downloading videos from GiantBomb.com (and playing them via VLC)

I initially started this project for a very specific reason - I have an old android tablet and running almost anything on it is <i>slow</i>. I found it was taking forever to open the video list and individual video pages on giantbomb dot com, much less play the video in the web player. I started downloading the videos and playing them with VLC, which allowed me to actually watch the videos, but getting to the video was still slow. I decided to write a simple android app that uses the giantbomb web api to pull the list of available videos and download selected videos.

I do not consider this a production-level app. There are TODOs, basically no graphic design, no play store signing keys, etc. (I'm running a dev, not release, build on my tablet, for example). Given time, I would like to go back and add automated testing, customizability (e.g. pull different resolution video streams), add comments, clean up code, and resolve the aforementioned issues, but what I have works well enough for my purposes at the moment, so I can't justify the time requirements for many of these fixes, at least presently.

Now that I've expounded on its shortcomings, here is what the app <i>does</i> do:
- Fast RecyclerView-based view for the video list with a cache for the video images.
- Display video image, title, description, and run length. If the text overflows the list item view, clicking on the text opens a pop-up with the full text.
- Functionality for downloading (including pausing and resuming) and deleting a video. Partial downloads utilizes partial http requests.
- Calls VLC via intent to play a downloaded video
- Downloading videos uses a background Service utilizing a queued, single-threaded Executor to keep utilized core counts low
- giantbomb API key prompt and storage in preferences.

As I mentioned previously, I wouldn't publish the app as-is on the play store. However, what it does do, it does well, and it has fulfilled my use case.
