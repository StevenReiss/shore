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
import java.util.TimerTask;

import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceConnection;
import edu.brown.cs.spr.shore.iface.IfacePoint;
import edu.brown.cs.spr.shore.iface.IfaceSensor;
import edu.brown.cs.spr.shore.iface.IfaceSwitch;
import edu.brown.cs.spr.shore.shore.ShoreLog;

class SafetyBlock implements SafetyConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private SafetyFactory   safety_factory;
private Map<IfaceBlock,BlockData> active_blocks;
private Map<IfaceSensor,BlockData> wait_sensors;

private static final long BLOCK_DELAY = 2000;
private static final long VERIFY_DELAY = 30000;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SafetyBlock(SafetyFactory fac) 
{
   safety_factory = fac;
   active_blocks = new HashMap<>();
   wait_sensors = new HashMap<>();
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

boolean checkPriorSensors(Collection<IfaceSensor> prior)
{
   for (IfaceSensor s : prior) {
      IfaceBlock blk = s.getBlock();
      BlockData bd = active_blocks.get(blk);
      if (bd != null) {
         if (bd.pastSensor(s)) return true;
       }
    }
   return false;
}



/********************************************************************************/
/*                                                                              */
/*      Handle sensor changes                                                   */
/*                                                                              */
/********************************************************************************/

void handleSensorChange(IfaceSensor s)
{
   IfaceBlock blk = s.getAtPoint().getBlock(); 
   if (blk == null) return;
   BlockData bd = active_blocks.get(blk);
   ShoreLog.logD("SAFETY","SENSOR " + s + " In BLOCK " + blk + " " + bd +  " "  +
         s.getSensorState());
   
   if (bd == null && s.getSensorState() == ShoreSensorState.ON) {
      bd = new BlockData(blk,s);
      active_blocks.put(blk,bd);
      blk.setBlockState(ShoreBlockState.INUSE);
      ShoreLog.logD("SAFETY","Note first use of block " + blk);
      checkPendingBlocks(blk);
    }
   else if (bd == null) {
      ShoreLog.logD("SAFETY","Ignore sensor " + s);
    }
   else if (s.getSensorState() == ShoreSensorState.ON) {
      ShoreLog.logD("SAFETY","Note sensor in block");
      if (bd.noteSensor(s)) {
         for (IfaceConnection conn : blk.getConnections()) {
            if (conn.getExitSensor(blk) == s) {
               IfaceSensor ent = conn.getEntrySensor(blk);
               ShoreLog.logD("SAFETY","Set exit sensor " + s + " " + ent);
               bd.setExitSensor(s,ent);
             }
          }
       }
    }
   else if (s.getSensorState() == ShoreSensorState.OFF && s == bd.getExitSensor()) {
      boolean checkexit = true;
      for (IfaceSensor chksen : bd.getAllSensors()) {
         if (chksen.getSensorState() == ShoreSensorState.ON) {
            checkexit = false;
            break;
          }
       }
      if (checkexit) {
         IfaceSensor chk = bd.getExitCheck();
         ShoreLog.logD("SAFETY","Check block exit " + chk + " " + blk);
         if (chk != null) wait_sensors.put(chk,bd);
         else {
            bd.checkEmptyBlock();
          }
       }
    }
   
   BlockData bdw = wait_sensors.get(s);
   if (bdw != null && s.getSensorState() == ShoreSensorState.OFF) {
      wait_sensors.remove(s);
      bdw.checkEmptyBlock();
    }
}


void handleSwitchChange(IfaceSwitch sw)
{
   IfaceBlock blk = sw.getPivotPoint().getBlock();
   checkPendingBlocks(blk);
}


void handleBlockChange(IfaceBlock blk)
{
   switch (blk.getBlockState()) {
      case EMPTY :
      case UNKNOWN :
         active_blocks.remove(blk);
         break;
      default :
        break;
    }
   
   checkPendingBlocks(blk);
}
           

private void checkPendingBlocks(IfaceBlock blk)
{
   BlockData bd = active_blocks.get(blk);
   
   if (bd == null || bd.getAtPoint() == null ||  bd.getPriorPoint() == null) return;
   
   IfaceBlock toblk = safety_factory.getLayoutModel().findNextBlock(
         bd.getPriorPoint(),
         bd.getAtPoint());
   
   for (IfaceConnection conn : blk.getConnections()) {
      IfaceBlock nblk = conn.getOtherBlock(blk);
      if (nblk == null) continue;
      if (nblk == toblk) {
         nblk.setPendingFrom(blk);
       }
      else if (nblk.getPendingFrom() == blk) nblk.setPendingFrom(null);
    }
}




/********************************************************************************/
/*                                                                              */
/*      Data for active blocks                                                  */
/*                                                                              */
/********************************************************************************/

private class BlockData {
   
   private IfaceBlock for_block;
   private IfaceSensor first_sensor;
   private IfacePoint current_point;
   private IfacePoint prior_point;
   private IfaceSensor exit_sensor;
   private IfaceSensor exit_check;
   private Set<IfaceSensor> hit_sensors;
   private long exit_time;
   private boolean is_verified;
   
   BlockData(IfaceBlock blk,IfaceSensor s) {
      for_block = blk;
      first_sensor = null;
      current_point = null;
      exit_sensor = null;
      exit_check = null;
      hit_sensors = new HashSet<>();
      noteSensor(s);
      exit_time = 0;
    }
   
   boolean noteSensor(IfaceSensor s) {
      if (s.getSensorState() == ShoreSensorState.ON && first_sensor == null) {
         first_sensor = s;
         current_point = s.getAtPoint();
         prior_point = null;
         for (IfaceConnection conn : for_block.getConnections()) {
            if (conn.getEntrySensor(for_block) == s) {
               IfaceBlock prev = conn.getOtherBlock(for_block);
               BlockData bd = active_blocks.get(prev);
               if (bd != null && bd.is_verified) is_verified = true;
             }
          }
         for (IfacePoint p : current_point.getConnectedTo()) {
            if (p.getType() == ShorePointType.GAP) { 
               prior_point = p;
               break;
             }
          }
         ShoreLog.logD("SAFETY","Enter block " + for_block + " " +
               first_sensor + " " + prior_point);
       }
      else if (s.getSensorState() == ShoreSensorState.ON && prior_point == null) { 
         findPriorPoint(s.getAtPoint()); 
         if (!is_verified && prior_point != null) {
             if (checkHitSensor(current_point,prior_point,0)) is_verified = true;
          }
       }
      exit_time = 0;
      
      if (s.getSensorState() == ShoreSensorState.OFF && !is_verified) {
         ShoreLog.logD("SAFETY","Begin verification delay for " + for_block);
         safety_factory.schedule(new VerifyTask(this),VERIFY_DELAY);
       }
      
      for_block.setBlockState(ShoreBlockState.INUSE);
      
      return hit_sensors.add(s); 
    }
   
   IfaceSensor getExitSensor()                  { return exit_sensor; }
   IfaceSensor getExitCheck()                   { return exit_check; }
   IfacePoint getAtPoint()                      { return current_point; } 
   IfacePoint getPriorPoint()                   { return prior_point; }
   
   void setExitSensor(IfaceSensor s,IfaceSensor check) {
      exit_sensor = s;
      exit_check = check;
    }
      
   Collection<IfaceSensor> getAllSensors()      { return hit_sensors; }
   
   boolean pastSensor(IfaceSensor s) {
      if (s == null) return false;
      return hit_sensors.contains(s);
    }
   
   void checkEmptyBlock() {
      exit_time = System.currentTimeMillis();
      safety_factory.schedule(new BlockTask(this,exit_time),BLOCK_DELAY);
    }
   
   void checkVerified() {
      if (is_verified) return;
      for (IfaceSensor s : hit_sensors) {
         if (s.getSensorState() == ShoreSensorState.ON) return;
       }
      ShoreLog.logD("SAFETY","Remove spurious block " + for_block);
      checkEmptyBlock();
    }
   
   void noteDone(long time) {
      ShoreLog.logD("SAFETY","Done with block " + time + " " + exit_time);
      if (time != 0 && exit_time != time) return;
      active_blocks.remove(for_block);
      for_block.setBlockState(ShoreBlockState.EMPTY);
    }
   
   private void findPriorPoint(IfacePoint at) {
      if (current_point == null) return;
      for (IfacePoint pt : at.getConnectedTo()) {
         if (safety_factory.getLayoutModel().goesTo(at,pt,current_point)) {
            prior_point = pt;
            current_point = at;
          }
       }
    }
   
   private boolean checkHitSensor(IfacePoint cur,IfacePoint prev,int ct) {
      if (prev == null) return false;
      // if previous was hit then we are okay
      if (prev.getType() == ShorePointType.SENSOR) {
         for (IfaceSensor sen : hit_sensors) {
            if (sen.getAtPoint() == prev) return true;
          }
         if (++ct >= 2) return false;           // can skip one sensor, not two
       }
      for (IfacePoint pt : prev.getConnectedTo()) {
         // check proper direction
         if (safety_factory.getLayoutModel().goesTo(cur,prev,pt)) {
            // check if it was hit
            if (checkHitSensor(prev,pt,ct)) return true;
          }
       }
      
      return false;
    }
   
}       // end of inner class BlockData



private class VerifyTask extends TimerTask {

   private BlockData block_data;
   
   VerifyTask(BlockData bd) {
      block_data = bd;
    }
   
   @Override public void run() {
      block_data.checkVerified();
    }
   
}       // end of inner class VerifyTask

private class BlockTask extends TimerTask {
   
   private BlockData block_data;
   private long start_time;
   
   BlockTask(BlockData bd,long time) {
      block_data = bd;
      start_time = time;
    }
   
   @Override public void run() {
      block_data.noteDone(start_time);
    }
}


}       // end of class SafetyBlock




/* end of SafetyBlock.java */

