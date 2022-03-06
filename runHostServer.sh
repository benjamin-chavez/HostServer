#!/bin/sh

# chmod 700 ./runHostServer.sh

javac -d bin src/HostServer.java
java -cp bin HostServer
