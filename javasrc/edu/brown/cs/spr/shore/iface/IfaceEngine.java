/********************************************************************************/
/*                                                                              */
/*              IfaceTrain.java                                                 */
/*                                                                              */
/*      Information about a train in SHORE                                      */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2023 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2023, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.spr.shore.iface;

import java.net.SocketAddress;
import java.util.EventListener;

import javafx.scene.paint.Color;

/**
 *      representation of an engine, possibly pulling a train.  This tracks the
 *      current location of thr train and provides hooks for controlling the
 *      behavior of the train.
 **/
public interface IfaceEngine
{

enum EngineState {
   UNKNOWN,
   OFF,
   STARTUP,
   READY,
   RUNNING,
   SHUTDOWN,
   IDLE,
}

IfaceBlock getEngineBlock();

boolean isEmergencyStopped();
boolean isBellOn();
boolean isHornOn();
boolean isReverse();
double getRpm();
boolean isFrontLightOn();
boolean isRearLightOn();
boolean hasRearLight();
boolean isMuted();

EngineState getEngineState();
Color getEngineColor();
String getEngineId();

String getEngineName();
SocketAddress getEngineAddress();

void setHorn(boolean on);
void setBell(boolean on); 
void setFrontLight(boolean on);
void setRearLight(boolean on);
void setMute(boolean on);
void setReverse(boolean fg);
void setState(EngineState state);
void setEmergencyStop(boolean on);

void doReboot();

void setupEngine(boolean fwdlight,boolean revlight,
      boolean bell,boolean reverse,int status,
      int speedstep,int rpmstep,int speed,boolean estop,boolean mute);

IfacePoint getCurrentPoint();
IfacePoint getPriorPoint();

void setSpeedParameters(int start,int max,int nstep,double maxdisplay,boolean kmph);
double getThrottleMax();
double getThrottle();
void setThrottle(double v);
int getThrottleSteps();
double getStartSpeed();

double getSpeed();
boolean isSpeedKMPH();
double getSpeedMax();




/********************************************************************************/
/*                                                                              */
/*      Callback handling                                                       */
/*                                                                              */
/********************************************************************************/


void addEngineCallback(EngineCallback cb);
void removeEngineCallback(EngineCallback cb);


/**
 *      Callback that is invoked when information about an engine
 *      or train is changed.
 **/
interface EngineCallback extends EventListener {

   default void engineChanged(IfaceEngine engine)        { }
   default void enginePositionChanged(IfaceEngine e)   { }
   
}


}       // end of interface IfaceTrain




/* end of IfaceTrain.java */

