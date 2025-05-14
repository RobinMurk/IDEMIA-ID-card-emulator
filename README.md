# Emulator applet creation and installation

## Current Status:
**working**

This repository is for Robin Mürk's BSc thesis titled *"Estonian ID card’s personal data file emulator"* All materials and resources needed to build the CAP file and load it to a Java Card are included. The only requirement is **Java SE 8** (version 8u442 is confirmed to work). 

The following guide is meant for **Ubuntu 22.04 LTS**.

## How to create and install the emulator applet:
1. Pull the repo to a local machine
2. Install the following package: `ant`
3. You can also install and setup Java SE 8 if needed: `sudo apt install openjdk-8-jdk`
4. Make sure you are in the root directory of the repo.
5. Run the command: `ant -f buildV2.xml`. This should create an `IDappletV2.cap` file in the current directory.
6. Make sure your card reader is connected and the Java Card inserted. If you are using WSL, see more info about `usbipd` for Windows.
7. Run the command: `java -jar gp.jar --install IDappletV2.cap --default`
9. To remove the applet from the card run: `java -jar gp.jar --deletedeps --delete {applet AID}`
10. To find the applet AID run: `java -jar gp.jar --list` while the card is still inserted.

To reinstall a new or modified applet you can use the `reinstall_V2.sh` script. NB: make sure you understand what the script does before executing!

## Managing the card's logs and historic bytes:
1. Make sure you are in the root directory when executing the following steps!
2. The main script used for communication is `send_mock_commands.py`. To use it and see possibilities, run: `python3 scripts/send_mock_commands.py --help`
3. When saving logs, then they will be saved under `logs/testing` as the path is currently hardcoded.

**The packages and JAR files used in this project:**

`ant-javacard.jar`: [ant-javacard.jar](https://github.com/martinpaljak/ant-javacard/releases/download/v20.03.25/ant-javacard.jar)

`jc222_kit`: [jc222_kit](https://github.com/martinpaljak/oracle_javacard_sdks/tree/master/jc222_kit)

`api.jar`: [api.jar](https://github.com/martinpaljak/oracle_javacard_sdks/blob/master/jc222_kit/lib/api.jar)

`gp211.jar`: [gp.jar](https://github.com/martinpaljak/AppletPlayground/tree/master/ext/globalplatform-2_1_1)

`gp.jar`:[GlobalPlatformPro](https://github.com/martinpaljak/GlobalPlatformPro)
