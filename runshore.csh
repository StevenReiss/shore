#! /bin/csh -f

set PRO = /pro
set SHORE = $PRO/shore
set JAVAFX = $PRO/javafx/lib
set OPENCV = /usr/share/java
set IVY = $PRO/ivy/lib

set CP1 = $SHORE/java:$SHORE/javasrc:$SHORE/resources:
set CP2 = $IVY/ivy.jar:$IVY/slf4j-api.jar:
set CP3 = $SHORE/lib/jmdns.jar:$SHORE/lib/medusa.jar:
set CP4 = $OPENCV/opencv.jar:

set CP5 = ""
foreach i ( $JAVAFX/javafx.*.jar)
   set CP5 = ${CP5}${i}:
end

set CP = ${CP1}${CP2}${CP3}${CP4}${CP5}

setenv PATH_TO_FX /pro/javafx/lib

echo $CP

java -classpath $CP --module-path $PATH_TO_FX --add-modules javafx.controls --enable-native-access=ALL-UNNAMED \
       -Djava.library.path=$JAVAFX \
       --enable-native-access=javafx.graphics edu.brown.cs.spr.shore.shore.ShoreMain $PRO/shore/resources/spr_layout.xml






















