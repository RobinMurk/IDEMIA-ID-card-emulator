# Mock EstID Applet Creation and installation

## Current Status:
**Not working**

This repository is for Robin Mürk's BSc thesis titled *"Estonian ID-card authentication to a machine over NFC and its security implications."* All materials and resources needed to build the CAP file and load it to a JavaCard are included. The only requirement is **OpenJDK 8** (OpenJDK 8u442 is confirmed to work). 

The following guide is meant for **Ubuntu 22.04 LTS**.

## How to create:
1. Pull the repo to a local machine
2. Install the following package: `ant`
3. You can also install OpenJDK 8 if needed: `sudo apt install openjdk-8-jdk`
4. Run the command: `ant`. This should create an `IDapplet.cap` file in the current directory.
5. Make sure your card reader is connected and the JavaCard inserted. If you are using WSL, see more info about usbipd for Windows.
6. Run the command: `java -jar lib/gp.jar --install IDapplet.cap`
7. You can add the flag `--default` to make the applet default, but if you want to simulate how the Estonian ID card behaves, this should be avoided.
8. To remove the applet from the card run: `java -jar lib/gp.jar --deletedeps --delete {applet AID}`

**The packages and JAR files used in this project:**

`ant-javacard.jar`: [ant-javacard.jar](https://github.com/martinpaljak/ant-javacard/releases/download/v20.03.25/ant-javacard.jar)

`jc222_kit`: [jc222_kit](https://github.com/martinpaljak/oracle_javacard_sdks/tree/master/jc222_kit)

`gp.jar`: [gp.jar](https://github.com/martinpaljak/GlobalPlatformPro/releases/download/v0.3.5/gp.jar)

`gpapi-globalplatform.jar`: [gpapi-globalplatform.jar](https://github.com/OpenJavaCard/globalplatform-exports/blob/master/org.globalplatform-1.3/gpapi-globalplatform.jar)
