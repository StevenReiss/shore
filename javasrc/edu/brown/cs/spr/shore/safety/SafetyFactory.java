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

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import edu.brown.cs.spr.shore.iface.IfaceNetwork;
import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceModel;
import edu.brown.cs.spr.shore.iface.IfaceSensor;
import edu.brown.cs.spr.shore.iface.IfaceSwitch;
import edu.brown.cs.spr.shore.iface.IfaceModel.ModelCallback;
import edu.brown.cs.spr.shore.iface.IfaceSensor.SensorState;

public class SafetyFactory implements SafetyConstants
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

private static final long OFF_TIME = 2000L;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public SafetyFactory(IfaceNetwork net,IfaceModel mdl)
{
   network_model = net;
   layout_model = mdl;
   
   safety_timer = new Timer("SensorTimer",true);
   sensor_map = new HashMap<>();
   
   safety_switch = new SafetySwitch(this); 
   safety_signal = new SafetySignal(this); 
   
   mdl.addModelCallback(new SafetyCallback());
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

IfaceNetwork getNetworkModel()                  { return network_model; }

IfaceModel getLayoutModel()                     { return layout_model; }


void schedule(TimerTask task,long delay)
{
   safety_timer.schedule(task,delay);
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
   safety_switch.handleSensorChange(s);
   safety_signal.handleSensorChange(s);
}



private void handleBlockChange(IfaceBlock blk)
{
   safety_signal.handleBlockChange(blk);
}


private void handleSwitchChange(IfaceSwitch sw)
{
   safety_signal.handleSwitchChange(sw); 
}


/********************************************************************************/
/*                                                                              */
/*      Callback for handing model changes                                      */
/*                                                                              */
/********************************************************************************/

private class SafetyCallback implements ModelCallback {
   
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
   private long off_time;
   private SensorState last_state;
   
   SensorStatus(IfaceSensor sensor) {
      for_sensor = sensor;
      off_time = 0;
      last_state = null;
      setState();
    }
   
   private void setState() {
      SensorState newstate = for_sensor.getSensorState();
      switch (newstate) {
         case ON :
            if (last_state != newstate && off_time == 0) {
               handleActualSensorChange(for_sensor);
             }
            last_state = newstate;
            off_time = 0;
            break;
         case OFF :
            last_state = newstate;
            if (off_time == 0) {
               off_time = System.currentTimeMillis();
               safety_timer.schedule(new SensorTimerTask(this,off_time),OFF_TIME);
             }
            break;
         case UNKNOWN :
            break;
       }
    }
   
   void checkOff(long base) {
      if (off_time == base) {
         off_time = 0;
         handleActualSensorChange(for_sensor);
       }
    }
   
}


private class SensorTimerTask extends TimerTask {
   
   private SensorStatus for_status;
   private long base_time;
   
   SensorTimerTask(SensorStatus sts,long when) {
      for_status = sts;
      base_time = when;
    }
   
   @Override public void run() {
      for_status.checkOff(base_time);
    }
   
}

}       // end of class SafetyFactory




/* end of SafetyFactory.java */

