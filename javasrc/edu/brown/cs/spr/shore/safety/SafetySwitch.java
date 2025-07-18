/********************************************************************************/
/*                                                                              */
/*              SafetySwitch.java                                               */
/*                                                                              */
/*      Handle ensuring switches are safe for trains                            */
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
import java.util.TimerTask;

import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceSensor;
import edu.brown.cs.spr.shore.iface.IfaceSwitch;
import edu.brown.cs.spr.shore.shore.ShoreLog;


class SafetySwitch implements SafetyConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

enum SwitchMode {
   NORMAL, SET, DONE
};

private SafetyFactory   safety_factory;
private Map<IfaceSwitch,SwitchData> switch_map;

private static final long SWITCH_DELAY = 2000;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                            
 */
/********************************************************************************/

SafetySwitch(SafetyFactory sf)
{
   safety_factory = sf;
   switch_map = new HashMap<>();
}



/********************************************************************************/
/*                                                                              */
/*     Request change                                                           */
/*                                                                              */
/********************************************************************************/

boolean safelySetSwitch(IfaceSwitch sw,ShoreSwitchState ss)
{
   if (sw.getSwitchState() == ss) return true;
   
   IfaceSensor ns = sw.getNSensor();
   IfaceSensor rs = sw.getRSensor();
   
   if (ns.getSensorState() == ShoreSensorState.ON) {
      return false;
    }
   if (rs.getSensorState() == ShoreSensorState.ON) {
      return false;
    }
   
   triggerSwitch(sw,null,ss);
   
   return false;
}



/********************************************************************************/
/*                                                                              */
/*      Handle sensor changed                                                   */
/*                                                                              */
/********************************************************************************/

void handleSensorChange(IfaceSensor s)
{
   IfaceSwitch swn = s.getSwitchN();
   IfaceSwitch swr = s.getSwitchR();
   if (swn != null) {
      triggerSwitch(swn,s.getSensorState(),ShoreSwitchState.N);
    }
   else if (swr != null) {
      triggerSwitch(swr,s.getSensorState(),ShoreSwitchState.R);
    }
}


void handleBlockChange(IfaceBlock blk)
{
   // nothing needed
}


void handleSwitchChange(IfaceSwitch blk)
{
   // nothing needed
}


private void triggerSwitch(IfaceSwitch sw,ShoreSensorState senst,ShoreSwitchState state)
{
   SwitchData sd = switch_map.get(sw);
   if (sd == null) {
      sd = new SwitchData(sw);
      switch_map.put(sw,sd);
    }
   sd.trigger(senst,state);
}


/********************************************************************************/
/*                                                                              */
/*      Safety infomration about a switch                                       */
/*                                                                              */
/********************************************************************************/

private class SwitchData {
   
   private IfaceSwitch for_switch;
   private SwitchMode  current_mode;
   private ShoreSwitchState last_state;
   private long last_trigger;
   
   SwitchData(IfaceSwitch sw) {
      for_switch = sw;
      current_mode = SwitchMode.NORMAL;
      last_trigger = 0;
    }
   
   void trigger(ShoreSensorState sen,ShoreSwitchState state) {
      ShoreLog.logD("SAFETY","Trigger switch " + for_switch + "=" + state + 
            " @ " + current_mode + " " + sen);
      
      switch (current_mode) {
         case NORMAL :
            if (sen != null && sen != ShoreSensorState.ON) return;
            doTrigger(state);
            break;
         case SET :
            if (sen == null || sen == ShoreSensorState.ON) {
               if (last_state == state) return;
               doTrigger(state);
             }
            else {
               last_trigger = System.currentTimeMillis();
               current_mode = SwitchMode.DONE;
               SwitchTask task = new SwitchTask(this,last_trigger);
               safety_factory.schedule(task,SWITCH_DELAY);
             }
            break;
         case DONE :
            if (sen == null || sen == ShoreSensorState.ON) {
               if (last_state == state) return;
               doTrigger(state);
             }
            break;
       }
    }
   
   void noteDone(long time) {
      ShoreLog.logD("SAFETY","Switch state to normal request " + time + " " + last_trigger);
      if (time != last_trigger) return;
      current_mode = SwitchMode.NORMAL;
      last_trigger = 0;
    }
   
   private void doTrigger(ShoreSwitchState state) {
      safety_factory.getNetworkModel().setSwitch(for_switch,state);
      current_mode = SwitchMode.SET;
      last_trigger = 0;
      last_state = state;
    }
   
}


private class SwitchTask extends TimerTask {
   
   private SwitchData switch_data;
   private long start_time;
   
   SwitchTask(SwitchData sd,long time) {
      switch_data = sd;
      start_time = time;
    }
   
   @Override public void run() {
      switch_data.noteDone(start_time);
    }
   
}



}       // end of class SafetySwitch




/* end of SafetySwitch.java */

