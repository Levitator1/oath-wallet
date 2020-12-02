# oath-wallet

WORK IN PROGRESS

This is going to be a plugin for Firefox which integrates Yubikey OATH (2FA/HOTP/TOTP) functionality
for logging into Web sites with your smartcard/key without having to switch windows to retrieve
PIN codes, without having to specify the credential id every time, and without cutting and pasting.
You press a hotkey from your Web browser, and then perform the usual smart key confirmation (touch and/or
PIN activation), and you are done.

It has a Java backend which should be readily portable to Windows and other operating systems.
At runtime, it requires the "ykman" command-line utility from Yubico. It is packaged in Debian as "yubikey-manager".
In my experience, the stock version of ykman in Debian 10 does not work, and you need to download the latest
version from Yubico and perform a user-local install using the standard Python "pip3" utility.
The graphical/QT version of yubikey-manager is not needed.

Current progress is very early stage, and this project does not accomplish anything usable yet.


