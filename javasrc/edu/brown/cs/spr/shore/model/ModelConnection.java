/********************************************************************************/
/*                                                                              */
/*              ModelConnection.java                                            */
/*                                                                              */
/*      description of class                                                    */
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



package edu.brown.cs.spr.shore.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceConnection;
import edu.brown.cs.spr.shore.iface.IfaceSensor;

class ModelConnection implements IfaceConnection, ModelConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ModelBlock      from_block;
private ModelBlock      to_block;
private ModelSensor     from_sensor;
private ModelSensor     to_sensor;
private ModelPoint      gap_point;
private ModelSignal     from_signal;
private ModelSignal     to_signal;
private ModelSwitch     from_switch;
private ModelSwitch     to_switch;
private ShoreSwitchState     from_switch_state;
private ShoreSwitchState     to_switch_state;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ModelConnection(ModelPoint gap,ModelBlock from,ModelBlock to)
{
   from_block = from;
   to_block = to;
   from_sensor = null;
   to_sensor = null;
   gap_point = gap;
   from_signal = null;
   to_signal = null;
   from_switch = null;
   to_switch = null;
   from_switch_state = ShoreSwitchState.UNKNOWN;
   to_switch_state = ShoreSwitchState.UNKNOWN;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public ModelBlock getOtherBlock(IfaceBlock blk)             
{ 
   if (blk == from_block) return to_block;
   else if (blk == to_block) return from_block;
   return null; 
} 

@Override public ModelSensor getExitSensor(IfaceBlock inblock)
{
   if (inblock == from_block) return from_sensor;
   else if (inblock == to_block) return to_sensor;
   return null;
} 

@Override public ModelSensor getEntrySensor(IfaceBlock inblock)
{
   if (inblock == from_block) return to_sensor;
   else if (inblock == to_block) return from_sensor;
   return null;
}


@Override public ModelSignal getStopSignal(IfaceBlock inblock)
{
   if (inblock == from_block) return from_signal;
   else if (inblock == to_block) return to_signal;
   return null;
}




@Override public List<IfaceSensor> getStopSensors(IfaceBlock inblock)  
{
   ModelSignal sig = getStopSignal(inblock);
   if (sig == null) return null;
   return sig.getStopSensors();
}

@Override public ModelSwitch getExitSwitch(IfaceBlock inblock)
{
   if (inblock == from_block) return from_switch;
   else if (inblock == to_block) return to_switch;
   return null;
}



@Override public ShoreSwitchState getExitSwitchState(IfaceBlock inblock)
{
   if (inblock == from_block) return from_switch_state;
   else if (inblock == to_block) return to_switch_state;
   return null;
}








/********************************************************************************/
/*                                                                              */
/*      Normalizationn methods                                                  */
/*                                                                              */
/********************************************************************************/

void normalizeConnection(ModelBase mdl)
{
   for (ModelPoint pt : gap_point.getModelConnectedTo()) {
      Set<ModelPoint> done = new HashSet<>();
      done.add(gap_point);
      followPath(mdl,pt,done);
    }
   if (from_sensor != null) from_sensor.setConnection(this); 
   if (to_sensor != null) to_sensor.setConnection(this);
   if (from_signal != null) {
      for (ModelSensor ss: from_signal.getModelStopSensors()) {
         ss.setConnection(this);
       }
    }
   if (to_signal != null) {
      for (ModelSensor ss : to_signal.getModelStopSensors()) {
         ss.setConnection(this);
       }
    }
   
   from_block.addConnection(this);
   to_block.addConnection(this);
}


private void followPath(ModelBase mdl,ModelPoint pt,Set<ModelPoint> done)
{
   while (pt != null) {
      if (done.contains(pt)) return;
      
      ModelBlock b0 = pt.getBlock();
      switch (pt.getType()) {
         case SENSOR :
            if (b0 == from_block && from_sensor == null) {
               from_sensor = mdl.findSensorForPoint(pt);
             }
            else if (b0 == to_block && to_sensor == null) {
               to_sensor = mdl.findSensorForPoint(pt);
             }
            break;
         case SIGNAL :
            if (b0 == from_block && from_signal == null) {
               ModelSignal sig = mdl.findSignalForPoint(pt); 
               if (sig != null && sig.getFromBlock() == b0) {
                  from_signal = sig;
                  sig.addConnection(this);
                } 
             }
            else if (b0 == to_block && to_signal == null) {
               ModelSignal sig = mdl.findSignalForPoint(pt);
               if (sig != null && sig.getFromBlock() == b0) {
                  to_signal = sig;
                  sig.addConnection(this);
                }
             }
            break;
         case GAP :
         case END :
            return;
         case SWITCH :
            ModelSwitch sw = mdl.findSwitchForPoint(pt);
            if (b0 == from_block && from_switch == null) {
               from_switch = sw;
               if (done.contains(sw.getNPoint())) {
                  from_switch_state = ShoreSwitchState.N;
                }
               else if (done.contains(sw.getRPoint())) {
                  from_switch_state = ShoreSwitchState.R;
                }
             }
            else if (b0 == to_block && to_switch == null) {
               to_switch = sw;
               if (done.contains(sw.getNPoint())) {
                  to_switch_state = ShoreSwitchState.N;
                }
               else if (done.contains(sw.getRPoint())) {
                  to_switch_state = ShoreSwitchState.R;
                }
             }
            if (done.contains(sw.getEntryPoint())) return;
            done.add(sw.getNPoint());
            done.add(sw.getRPoint());
            break;
         default :
            break;
       }
      
      if (b0 == from_block && from_signal != null && from_sensor != null) return;
      else if (b0 == to_block && to_signal != null && to_sensor != null) return;
      
      done.add(pt);
      
      ModelPoint nxt = null;
      for (ModelPoint npt : pt.getModelConnectedTo()) {
         if (done.contains(npt)) continue;
         if (nxt == null) nxt = npt;
         else return;
       }
   
      pt = nxt;
    }
}


}       // end of class ModelConnection




/* end of ModelConnection.java */

