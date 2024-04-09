/********************************************************************************/
/*                                                                              */
/*              SafetyBlock.java                                                */
/*                                                                              */
/*      Handle automatic block state setting and checking                       */
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceConnection;
import edu.brown.cs.spr.shore.iface.IfaceSensor;
import edu.brown.cs.spr.shore.iface.IfaceBlock.BlockState;
import edu.brown.cs.spr.shore.iface.IfaceSensor.SensorState;

class SafetyBlock implements SafetyConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

// private SafetyFactory   safety_factory;
private Map<IfaceBlock,BlockData> active_blocks;
private Map<IfaceSensor,BlockData> wait_sensors;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SafetyBlock(SafetyFactory fac) 
{
// safety_factory = fac;
   active_blocks = new HashMap<>();
   wait_sensors = new HashMap<>();
}



/********************************************************************************/
/*                                                                              */
/*      Handle sensor changes                                                   */
/*                                                                              */
/********************************************************************************/

void handleSensorChange(IfaceSensor s)
{
   IfaceBlock blk = s.getBlock();
   BlockData bd = active_blocks.get(blk);
   if (bd == null && s.getSensorState() == SensorState.ON) {
      bd = new BlockData(blk,s);
      active_blocks.put(blk,bd);
      blk.setBlockState(BlockState.INUSE);
    }
   else if (s.getSensorState() == SensorState.ON) {
      if (bd.noteSensor(s)) {
         for (IfaceConnection conn : blk.getConnections()) {
            if (conn.getExitSensor(blk) == s) {
               IfaceSensor ent = conn.getEntrySensor(blk);
               bd.setExitSensor(s,ent);
             }
          }
       }
    }
   else if (s.getSensorState() == SensorState.OFF && s == bd.getExitSensor()) {
      for (IfaceSensor chksen : bd.getAllSensors()) {
         if (chksen.getSensorState() == SensorState.ON) return;
       }
      IfaceSensor chk = bd.getExitCheck();
      if (chk != null) wait_sensors.put(chk,bd);
      else {
         active_blocks.remove(bd.getBlock());
         blk.setBlockState(BlockState.EMPTY);
       }
    }
   
   BlockData bdw = wait_sensors.get(s);
   if (bdw != null && s.getSensorState() == SensorState.OFF) {
      wait_sensors.remove(s);
      active_blocks.remove(bdw.getBlock());
      bdw.getBlock().setBlockState(BlockState.EMPTY);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Data for active blocks                                                  */
/*                                                                              */
/********************************************************************************/

private class BlockData {
   
   private IfaceBlock for_block;
   private IfaceSensor exit_sensor;
   private IfaceSensor exit_check;
   private Set<IfaceSensor> hit_sensors;
   
   BlockData(IfaceBlock blk,IfaceSensor s) {
      for_block = blk;
      exit_sensor = null;
      exit_check = null;
      hit_sensors = new HashSet<>();
      hit_sensors.add(s);
    }
   
   boolean noteSensor(IfaceSensor s) {
      return hit_sensors.add(s);
    }
   
   IfaceBlock getBlock()                        { return for_block; }
   IfaceSensor getExitSensor()                  { return exit_sensor; }
   IfaceSensor getExitCheck()                   { return exit_check; }
   
   void setExitSensor(IfaceSensor s,IfaceSensor check) {
      exit_sensor = s;
      exit_check = check;
    }
      
   Collection<IfaceSensor> getAllSensors()      { return hit_sensors; }
   
}


}       // end of class SafetyBlock




/* end of SafetyBlock.java */

