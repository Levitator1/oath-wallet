# oath-wallet

V0.02 (First release)

This is the first nominal working release. It is for Firefox and Linux.

This is a plugin for Firefox which integrates Yubikey OATH (2FA/HOTP/TOTP) functionality
for logging into Web sites with your smartcard/key without having to switch windows to retrieve
PIN codes, without having to specify the credential name every time, and without cutting and pasting.
You press a hotkey from your Web browser (default is Ctrl-Alt-Insert), and then perform the usual smart
key confirmation (touch and/or PIN activation), and you are done. The PIN number appears in whatever field
is active when you hit the hotkey. The hotkey is rebindable using Firefox's standard key-binding menu.

This addon has a Java backend which should be readily portable to Windows and other operating systems.
At runtime, it requires the "ykman" command-line utility from Yubico. It is packaged in Debian as "yubikey-manager".
In my experience, the stock version of ykman in Debian 10 does not work, and you need to download the latest
version from Yubico and perform a user-local install using the standard Python "pip3" utility.
The graphical/QT version of yubikey-manager is not needed.

# Installation

Installation is a bit ugly and manual right now. If anybody wants to write an installer, I would welcome them to do so.
Onward to installation...

Prerequisites:

1. You will need Firefox, obviously. You will require either Firefox ESR (a standard package in Debian, and maybe elsewhere), or the Developer Edition. The reason for this
   is that you need to enable unsigned add-ons. This should not be a big deal, as you are warned as to whether packages are unsigned and you have the option of rejecting them.
   To enable unsigned add-ons navigate to "about:config" by entering it in the URL bar, as a destination. Then search for the setting: 'xpinstall.signatures.required'.
   If there turns out to be interest, then I may look into what is involved in getting the package signed.

2. The ykman tool must be installed and available in your path. ykman requires Python3 and some other things. See the yubikey-manager documentation.
   
3. The contents of the included bin/ directory can go anywhere, but you will need to update the "path" field in "extension/com.levitator.oath_wallet_service.json"
   to point to the correct path of "com.levitator.oath_wallet_service". That is a shell script, and it expects the jar file to be in the same directory alongside it.
   A typical location is: $HOME/.local/bin. This back-end is built with Java 11, and you will need a suitable Java runtime. It is tested with openjdk-11.

4. Copy your updated com.levitator.oath_wallet_service.json file to a directory named "$HOME/.mozilla/native-messaging-hosts/". It does not exist by default.
   It is used to hold the definition files which Firefox uses to associate back-end programs with javascript-based add-on packages.

5.  You should be able to install the XPI file via the usual method, which is to open the main browser menu (three stacked horizontal bars), and go to "Add-Ons"
   and "Extensions". If you got everything right, then immediately upon installation, you should be notified that the back-end has started, and you will get an
   icon in your system tray which you can click to scroll through a history of notices, or to quit the back-end if you're not using it.

Configuration
=============

Configuration is accomplished using a JSON file which associates OATH credential names (as configured using ykman) with URL glob patterns. Let's say
you do "ykman oath add gizmo", and now you have an oath credential called "gizmo". You intend to use it to access `"https://www.gizmo.narf/"`, so you might
select a URL wildcard such as `"https://www.gizmo.narf/*"`, or maybe `"https://www.gizmo.narf/login/*"`, or whatever you might find to be suitably specific.

The JSON file looks like this:

```javascript
{"mappings":[
	{ "cred":"https://www.facebook.com", "url": "https://www.facebook.com/*" },
	{ "cred":"somewhere", "url": "https://www.somewhere.blah/*" },
	{ "cred":"test", "url": "file:///home/user/project/oath-wallet/test.html" }
]}
```

So, having added gizmo, it would look like this:

```javascript
{"mappings":[
	{ "cred":"https://www.facebook.com", "url": "https://www.facebook.com/*" },
	{ "cred":"somewhere", "url": "https://www.somewhere.blah/*" },
	{ "cred":"test", "url": "file:///home/user/project/oath-wallet/test.html" },
	{ "cred":"gizmo", "url": "https://www.gizmo.narf/*" }
]}
```

This file belongs in: `$HOME/.oath_wallet/mappings.json`
You will need to restart the backend to reread this file, and when it starts back up, the console window from clicking
the system tray icon should tell you how many records were loaded.

You can enter more than one record for the same credential to specify additional wildcards for it. One will probably be enough,
though.

IMPORTANT WARNING: You are strongly advised against placing globs (the star symbol) anywhere within the host portion of the URL.
Or, for that matter, anywere prior to the start of the path. So, in other words, use globs only to describe path wildcards, and
not in the host or protocol.

For example, you might be tempted to do something like `"https://*.gizmo.narf/*"`, to cover multiple subdomains, but don't do that.
The reason not do that is because this URL also matches that pattern: `"https://malice.hax/.gizmo.narf/steal_your_pin_number.php"`


TODO
====

Well, that's it. Please let me know if you enjoy the add-on, and I might add features. Things which would be nice include:

- A user interface for editing the mapping file
- A proper installer so that you don't have to copy files by hand and edit the path in the native-messaging manifest

# Porting
The backend relies on unix fifos so that client-instances of itself can talk to the main server instance. This may or may not
be a total pain under Windows. I haven't tried it. It will probably be necessary to adjust the IO code, or to implement
some alternate transport under Windows. If nothing else, there are paths that need to be adjusted, and there is a call to mkfifo
which needs to be replaced with whatever mechanism it is under windows that creates named pipes or some  other equivalent construct.

Chromium has partial compatibility with Firefox, so porting to Chromium might be doable, and probably easier than porting OS.

Jose Batista
Levitat0r@protonmail.com



