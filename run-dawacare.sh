#!/bin/bash
# Launch DawaCare with HiDPI scaling (change 2 to 1 on normal screens)
cd "$(dirname "$0")"
java -Dsun.java2d.uiScale=2 -jar target/DawaCare.jar
