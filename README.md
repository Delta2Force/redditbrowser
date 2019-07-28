# RedditBrowser - Browse Reddit in Minecraft [![Build Status](https://travis-ci.org/Delta2Force/redditbrowser.svg?branch=master)](https://travis-ci.org/Delta2Force/redditbrowser)
(For some reason, the branch I made for more interaction is still registered in travis and will always fail since it doesn't exist anymore.)

## Installation
Download the latest version from the releases tab and put it in your plugins tab.

## Usage
When the plugin is first launched, a config file for it will be created. In this file you will have to enter the data from a Reddit application. Google „Reddit application page“ and create a script application. The username and password have to be from your account. Don’t worry, nothing gets sent to me. You can look inside of the code if you are worried. If you still don’t trust me, just create a throwaway. The client id is the text under the application name and the secret is labeled „secret“. I’m not sure if this works without this, but I advise to do it. After this, you don’t have to reload the server. Just do /reddit and it should work.

Use `/reddit subreddit` in chat e.g. `/reddit awww`.

WIP: If you want to join the lobby of another player, type  `/reddit-join playername`.
 
When you are in the room, you can upvote the post by clicking the button left or downvote by clicking the button right.
You can leave a comment by writing a book using your book and quill and putting it in the comment chest.

You can inspect a comment thread by right clicking on a comment. You will then see the comments under that comment. If you right click on those, you'll see the comments of those, and so on.

You can see the post at its highest resolution by right clicking the image.


## Room configuration
You can configure the following properties in the config file:

`screenWidth`: the width of the screen in blocks

`screenHeight`: the height of the screen in blocks

`roomDepth`: how deep the room should be in blocks
