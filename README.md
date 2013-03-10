DavSync (or net.zekjur.davsync to be specific)
==============================================

<img src="https://github.com/mstap/android-davsync/raw/master//screenshots/davsync-2013-03-10-200731.png" width="240" align="right" alt="DavSync screenshot">

After installing this Android App, you can share images from your gallery to
your WebDAV server. Furthermore, every picture you take with the camera will
automatically be uploaded to your WebDAV server — unless you turn that off, of
course.

Why would you want this? If you don’t have automatic Picasa uploads enabled due
to privacy concerns or other reasons, you still might want to backup/archive
every picture you take. Also, if you run a WebDAV server on your workstation,
automatically synchronizing your pictures is much better than having to import
them manually with gphoto or other programs.

Project status and contributions
================================

This app works for me. That means, I will most likely not touch it, except
when I want to add a feature that I want.

In case you want to have a specific feature implemented, *DO NOT FILE A FEATURE
REQUEST*. I will close such tickets immediately, unless they come with code.
The rule is: you want it, you send a pull request. I might still decline it if
it doesn’t fit into my vision of what the app should do.

Installation and configuration
==============================

Install the APK. Then, chose “DavSync” in your application drawer. Set up the
following fields:

* WebDAV URL: the full URL to your WebDAV server, including the trailing slash.
  Example:

      http://my-dav-server.zekjur.net/pictures/

* WebDAV username/password: It is strongly recommended to protect your WebDAV
  server with a username/password. DavSync supports basic authentication only.

Ideas for enhancements
======================

For the following features, pull requests will be accepted:

* Implement strong SSL certificate validation. I have no clue how the Apache
  HTTP Client API verifies SSL by default (I suspect not at all).
* Implement SSL certificate pinning. Instead of trusting certificate
  hierarchies, allow the user to pin a specific certificate by fingerprint, no
  matter whether it is self-signed or not.
* Enhance the Preferences activity to allow configuration of multiple WebDAV
  servers, add a popup dialog with server selection in the ShareActivity.
* Beautify the Preferences activity to show the current values of WebDAV
  URL/username underneath the label (see comment in
  [SettingsActivity.java](https://github.com/mstap/android-davsync/blob/master/src/net/zekjur/davsync/SettingsActivity.java).
* Add an option to resize images before uploading to save bandwidth.
