#!/bin/bash

#java -jar gp.jar --delete A000000077010800070000FE00000100 --emv --deletedeps
java -jar gp.jar --delete A011111177010800070000FE111111 --emv --deletedeps

#java -jar gp.jar --delete A000000077 --emv
java -jar gp.jar --delete A011111177 --emv
ant -f buildV2.xml

java -jar gp.jar --install IDapplet_V2.cap --default --emv

java -jar gp.jar --list --emv