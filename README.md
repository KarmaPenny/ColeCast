# ColeCast
ColeCast lets you control your HTPC with your smartphone over the local network.

## Setup

### Installation
1.) Download and install the latest ColeCast service on your HTPC. [Download Link](https://github.com/KarmaPenny/ColeCast/raw/master/release/CurrentVersion/ColeCast-Installer.msi)

2.) Install the ColeCast app on your android smartphone via the [Google Play Store Link](https://github.com/KarmaPenny/ColeCast/raw/master/release/CurrentVersion/ColeCast.apk) or [Direct APK Download](https://github.com/KarmaPenny/ColeCast/raw/master/release/CurrentVersion/ColeCast.apk)

### ExLink (OPTIONAL)
ExLink allows ColeCast to control the Power and Volume of your TV. To setup ExLink:

1.) Connect your HTPC to your TV with an ExLink cable. 

2.) Open Task Manager (Ctrl + Shift + ESC).

3.) Kill ColeCast.exe

4.) Press start. Type "notepad". Right click on notepad.exe and click Run as Administrator. 

5.) Open the ColeCast config located at "C:\Program Files (x86)\ColeCast\ColeCast.exe.config"

6.) Set the following config values:

* exlink_enabled = "true"

* portName = "com3" NOTE: Set this to the com port number of your ExLink Cable which can be found in the Device Manager under Ports (COM & LPT)

7.) Lookup the ExLink codes for your TV make/model then enter them into the following ColeCast config fields:

* exlink_power_on

* exlink_power_off

* exlink_mute

* exlink_volume_down

* exlink_volume_up

8.) Save the config changes

9.) Start the ColeCast service located at "C:\Program Files (x86)\ColeCast\ColeCast.exe"

## Usage

### Pairing
1.) Open the ColeCast app on your smartphone.

2.) Open the Devices menu by pressing the three horizontal lines in the top left corner.

3.) Select your HTPC from the list of available Devices. Your phone is now paired to your HTPC. You do not need to pair again unless your HTPC's ip address changes or you switch the Paired device to another HTPC.

NOTE: You can forget/remove devices. Long press each device you wish to remove then press the trash icon to remove them.

### Controls
Once paired with your HTPC you can control your HTPC with the control buttons. From left to right, top to bottom the control buttons are:

* Media Previous - Press/Hold to go back one music track or rewind video.

* Media Play/Pause - Press to play or pause current song or video.

* Media Next - Press/Hold to skip forward on music track or fast forward video.

* Favorites - Press to open favorites menu.

* Power - Press to wake/suspend the HTPC. Waking the HTPC requires a network card with Wake On Lan (WoL) enabled. Addtionally if you've setup ExLink the TV will turn on/off with the HTPC.

* Volume Mute - Press to mute the HTPC. If ExLink is enabled the TV will be muted instead of the HTPC.

* Volume Down - Press/Hold to lower the HTPC's volume. The TV's volume will be lowered instead of the HTPC if ExLink is setup.

* Volume Up - Press/Hold to increase the HTPC's volume. The TV's volume will be increased instead of the HTPC if ExLink is setup.

* Close Window - Press to close the currently active window.

* Fullscreen - Press to fullscreen the currently active video.

### Sharing
You can open URLs from other apps on your HTPC by sharing the url to the ColeCast app.

1.) In a different app on your smartphone, press Share.

2.) Select the ColeCast app. The URL will automatically open on your HTPC.

### Favorites
The favorites menu lets you create shortcuts to open on the HTPC. Open the Favorites menu by clicking the favorites button. To add or edit a favorite:

1.) Press the "+" button in the top right corner of the favorites menu to create a new favorite or long press an existing favorite to edit it.

2.) Press the title and enter the name of you new favorite.

3.) Press the icon image to select an image to use for the new favorite.

4.) Press the "Enter URL" text and enter the URL or File Path the favorite will open on the HTPC when pressed.

5.) Press save to create the favorite.

NOTE: You can remove favorites by editting the favorite and pressing the trash icon.
