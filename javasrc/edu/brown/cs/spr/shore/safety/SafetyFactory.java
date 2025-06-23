/********************************************************************************/
/*                                                                              */
/*              SafetyFactory.java                                              */
/*                                                                              */
/*      Factory to setup and maintain safety conditions                         */
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



package edu.brown.cs.spr.shore.safety;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import edu.brown.cs.spr.shore.iface.IfaceNetwork;
import edu.brown.cs.spr.shore.iface.IfaceSafety;
import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceModel;
import edu.brown.cs.spr.shore.iface.IfaceSensor;
import edu.brown.cs.spr.shore.iface.IfaceSignal;
import edu.brown.cs.spr.shore.iface.IfaceSwitch;
import edu.brown.cs.spr.shore.iface.IfaceTrains;
import edu.brown.cs.spr.shore.iface.IfaceModel.ModelCallback;
import edu.brown.cs.spr.shore.shore.ShoreLog;

public class SafetyFactory implements IfaceSafety, SafetyConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceNetwork    network_model;
private IfaceModel      layout_model;
private Timer           safety_timer;
private Map<IfaceSensor,SensorStatus> sensor_map;
private SafetySwitch    safety_switch;
private SafetySignal    safety_signal;
private SafetyBlock     safety_block;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public SafetyFactory(IfaceNetwork net,IfaceModel mdl,IfaceTrains trains)
{
   network_model = net;
   layout_model = mdl;
   
   safety_timer = new Timer("SensorTimer",true);
   sensor_map = new HashMap<>();
   
   safety_switch = new SafetySwitch(this); 
   safety_signal = new SafetySignal(this); 
   safety_block = new SafetyBlock(this);
   
   mdl.addModelCallback(new SafetyCallback());
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

IfaceNetwork getNetworkModel()                  { return network_model; }

IfaceModel getLayoutModel()                     { return layout_model; }

boolean checkPriorSensors(Collection<IfaceSensor> prior)
{
   return safety_block.checkPriorSensors(prior); 
}


void schedule(TimerTask task,long delay)
{
   safety_timer.schedule(task,delay);
}


/********************************************************************************/
/*                                                                              */
/*      Safely Set Switches, Signals, etc.                                      */
/*                                                                              */
/********************************************************************************/

@Override public boolean setSwitch(IfaceSwitch sw,ShoreSwitchState state)
{
   return safety_switch.safelySetSwitch(sw,state);  
}


@Override public boolean setSignal(IfaceSignal ss,ShoreSignalState state)
{
   return safety_signal.safelySetSignal(ss,state);
}


@Override public boolean setSensor(IfaceSensor ss,ShoreSensorState state)
{
   network_model.setSensor(ss,state); 
   
   return true;
}

/********************************************************************************/
/*                                                                              */
/*      Handle sensor changes                                                   */
/*                                                                              */
/********************************************************************************/

private void handleSensorChange(IfaceSensor s)
{
   SensorStatus sts = sensor_map.get(s);
   if (sts == null) {
      sts = new SensorStatus(s);
      sensor_map.put(s,sts);
    }
   sts.setState();
}


private void handleActualSensorChange(IfaceSensor s)
{
   ShoreLog.logD("SAFETY","Actual sensor change " + s + " " + s.getSensorState());
   
   safety_switch.handleSensorChange(s);
   safety_signal.handleSensorChange(s);
   safety_block.handleSensorChange(s);
}



private void handleBlockChange(IfaceBlock blk)
{
   safety_signal.handleBlockChange(blk);
   safety_block.handleBlockChange(blk);  
}


private void handleSwitchChange(IfaceSwitch sw)
{
   safety_signal.handleSwitchChange(sw);
   safety_block.handleSwitchChange(sw); 
}


/********************************************************************************/
/*                                                                              */
/*      Callback for handing model changes                                      */
/*                                                                              */
/********************************************************************************/

private final class SafetyCallback implements ModelCallback {
   
   @Override public void sensorChanged(IfaceSensor sen) {
      handleSensorChange(sen);
    }
   
   @Override public void blockChanged(IfaceBlock blk) {
      handleBlockChange(blk);
    }
   
   @Override public void switchChanged(IfaceSwitch sw) {
      handleSwitchChange(sw);
    }

}       // end of inner class SafetyCallback



/********************************************************************************/
/*                                                                              */
/*      Track sensor states with delay                                          */
/*                                                                              */
/********************************************************************************/

private class SensorStatus {

   private IfaceSensor for_sensor;
   private ShoreSensorState last_state;
   
   SensorStatus(IfaceSensor sensor) {
      for_sensor = sensor;
      last_state = null;
      setState();
    }
   
   private void setState() {
      ShoreSensorState newstate = for_sensor.getSensorState();
      if (last_state != newstate && newstate != ShoreSensorState.UNKNOWN) {
         handleActualSensorChange(for_sensor);
       }
      last_state = newstate;
    }
   
}




}       // end of class SafetyFactory




/* end of SafetyFactory.java */

