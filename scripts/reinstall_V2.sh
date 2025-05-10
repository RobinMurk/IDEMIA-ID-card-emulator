#!/bin/bash

#java -jar gp.jar --delete A000000077010800070000FE00000100
java -jar gp.jar --delete A011111177010800070000FE111111

#java -jar gp.jar --delete A000000077
java -jar gp.jar --delete A011111177

ant -f buildV2.xml

java -jar gp.jar --install IDapplet_V2.cap --default

java -jar gp.jar --list