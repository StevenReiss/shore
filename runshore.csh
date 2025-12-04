#! /bin/csh -f

set PRO = /pro
set SHORE = $PRO/shore
set JAVAFX = $PRO/javafx/lib
set OPENCV = /usr/share/java
set IVY = $PRO/ivy/lib
set JAVA = java
set MODS = 1

if (-e /usr/share/openjfx/lib) then
  set JAVAFX = /usr/share/openjfx/lib
  set JAVA = /usr/lib/jvm/java-17-openjdk-amd64/bin/java
  set MODS = 0
endif

set CP1 = $SHORE/java:$SHORE/resources:
set CP2 = $IVY/ivy.jar:$IVY/slf4j-api.jar:
set CP3 = $SHORE/lib/jmdns.jar:$SHORE/lib/medusa.jar:
set CP4 = $OPENCV/opencv.jar:

set CP5 = ""
foreach i ( $JAVAFX/javafx.*.jar)
   set CP5 = ${CP5}${i}:
end

set CP = ${CP1}${CP2}${CP3}${CP4}${CP5}

setenv PATH_TO_FX $JAVAFX
set LDP = $JAVAFX
if ($?LD_LIBRARY_PATH == 0) then
   set LDP = $JAVAFX
else
   set LDP = ${JAVAFX}:$LD_LIBRARY_PATH

endif

if ($MODS == 1) then
   $JAVA -classpath $CP --module-path $PATH_TO_FX --add-modules javafx.controls --enable-native-access=ALL-UNNAMED \
       -Djava.library.path=$LDP \
       --enable-native-access=javafx.graphics edu.brown.cs.spr.shore.shore.ShoreMain \
       -m $PRO/shore/resources/spr_layout.xml
else
   $JAVA -classpath $CP \
       edu.brown.cs.spr.shore.shore.ShoreMain \
       -m $PRO/shore/resources/spr_layout.xml
endif





















