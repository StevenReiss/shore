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

private static final long BLOCK_DELAY = 10;
private static final long VERIFY_DELAY = 10000;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SafetyBlock(SafetyFactory fac) 
{
   safety_factory = fac;
   active_blocks = new HashMap<>();
}



/********************************************************************************/
/*                                                                              */
/*      Checks for signals to determine proper direction                        */
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
   IfaceBlock blk = s.getBlock(); 
   if (blk == null) return;
   BlockData bd = active_blocks.get(blk);
   ShoreLog.logD("SAFETY","SENSOR " + s + " In BLOCK " + blk + " " + (bd != null) +
         " "  + s.getSensorState());
   
   if (bd == null && s.getSensorState() == ShoreSensorState.ON) {
      bd = new BlockData(blk,s);
      active_blocks.put(blk,bd);
      blk.setBlockState(ShoreBlockState.INUSE);
      ShoreLog.logD("SAFETY","Note first use of block " + blk + " " + s);
      checkPendingBlocks(blk);
    }
   else if (bd == null) {
      ShoreLog.logD("SAFETY","Ignore OFF sensor " + s);
    }
   else if (s.getSensorState() == ShoreSensorState.ON) {
      ShoreLog.logD("SAFETY","Note sensor " + s + " in block");
      boolean pend = (bd.getPriorPoint() == null);
      bd.noteBlockSensor(s);
      if (!pend && bd.getPriorPoint() != null) checkPendingBlocks(blk);
    }
   else if (s.getSensorState() == ShoreSensorState.OFF) {
      bd.noteBlockSensor(s);            // handle spurrious sensors if block not verified
      checkBlockExit(s);
    }
}


void handleSwitchChange(IfaceSwitch sw)
{
   IfaceBlock blk = sw.getPivotPoint().getBlock();
   if (blk.getBlockState() == ShoreBlockState.INUSE) {
      checkPendingBlocks(blk);
    }
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
           


/********************************************************************************/
/*                                                                              */
/*      Block Exit Checks                                                       */
/*                                                                              */
/********************************************************************************/

/** 
 *      Check if we have exited previous block.  If we have turned off a sensor
 *      at a connector and the other block of the connector is also off, check
 *      if the other block is actually empty and mark it as such.
 **/

private void checkBlockExit(IfaceSensor s)
{
   if (s.getSensorState() != ShoreSensorState.OFF) return;
   
   IfaceBlock prior = findExitingBlock(s);
   if (prior != null) {
      ShoreLog.logD("CHECK BLOCK EXIT " + s.getBlock() + " " + prior);
      BlockData pbd = active_blocks.get(prior);
      boolean checkexit = true;
      if (pbd != null) {
         for (IfaceSensor chksen : pbd.getAllSensors()) {
            if (chksen.getSensorState() == ShoreSensorState.ON) {
               checkexit = false;
               break;
             } 
          }
       }
      if (checkexit) {
         pbd.checkEmptyBlock();
       }
    }
}


private IfaceBlock findExitingBlock(IfaceSensor s) 
{
   IfaceBlock blk = s.getBlock();
   for (IfaceConnection conn : blk.getConnections()) {
      if (conn.getExitSensor(blk) == s) {
         IfaceSensor ent = conn.getEntrySensor(blk);
         ShoreLog.logD("SAFETY","Exit connection " + conn + " " + s + " " + ent);
         if (s.getSensorState() == ShoreSensorState.OFF &&
               ent.getSensorState() == ShoreSensorState.OFF) {
            IfaceBlock xblk = ent.getBlock();
            ShoreLog.logD("SAFETY","Exit check " + s + " " + ent + " " + xblk);
            if (xblk.getBlockState() == ShoreBlockState.INUSE) {
               return xblk;
             }
            else {
               ShoreLog.logD("SAFETY","Prior block " + ent.getBlock() + " not in use");
             }
          }
       }
    }
   
   return null;
}


/********************************************************************************/
/*                                                                              */
/*      Pending block checks                                                    */
/*                                                                              */
/********************************************************************************/

/**
 *      Find the next block we will be going to given current switches and direction.
 *      Set its pending to this block, ensure no other blocks are pending on this block
 **/

private void checkPendingBlocks(IfaceBlock blk)
{
   BlockData bd = active_blocks.get(blk);
   
   if (bd == null || bd.getAtPoint() == null ||  bd.getPriorPoint() == null) {
      if (bd == null) {
         ShoreLog.logD("SAFETY","No pending check  because " + blk + 
               "has no active block");
       }
      else {
         ShoreLog.logD("SAFETY","No pending check for direction " + 
               bd.getAtPoint() + " " + bd.getPriorPoint());
       }
       
      return;
    }
   
   IfaceBlock toblk = safety_factory.getLayoutModel().findNextBlock(
         bd.getPriorPoint(),
         bd.getAtPoint());
   
   for (IfaceConnection conn : blk.getConnections()) {
      IfaceBlock nblk = conn.getOtherBlock(blk);
      if (nblk == null) continue;
      if (nblk == toblk) {
         nblk.setPendingFrom(blk);
       }
      else if (nblk.getPendingFrom() == blk) {
         nblk.setPendingFrom(null);
       }
    }
}




/********************************************************************************/
/*                                                                              */
/*      Data for active blocks                                                  */
/*                                                                              */
/********************************************************************************/

private class BlockData {
   
   private IfaceBlock for_block;
   private IfacePoint current_point;
   private IfacePoint prior_point;
   private Set<IfaceSensor> hit_sensors;
   private long exit_time;
   private boolean is_verified;
   
   BlockData(IfaceBlock blk,IfaceSensor s) {
      for_block = blk;
      current_point = null;
      prior_point = null;
      hit_sensors = new HashSet<>();
      noteBlockSensor(s);
      exit_time = 0;
    }
   
   boolean noteBlockSensor(IfaceSensor s) {
      if (s.getSensorState() == ShoreSensorState.ON && current_point == null) {
         current_point = s.getAtPoint();
         prior_point = null;
         checkPriorBlock(s);
         ShoreLog.logD("SAFETY","Enter block " + for_block + " " +
                current_point + " " + prior_point + " " + is_verified);
       }
      else if (s.getSensorState() == ShoreSensorState.ON) { 
         if (!hit_sensors.contains(s)) {
            // first time at this sensor
            IfacePoint pt = s.getAtPoint();
            if (prior_point == null && current_point != null && current_point != pt) {
               prior_point = current_point;
             }
            current_point = pt;
            if (!is_verified && prior_point != null) {
               ShoreLog.logD("SAFETY","Verified " + for_block + " base on prior sensors " + 
                     hit_sensors);
               is_verified = true;
             }
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
   
   IfacePoint getAtPoint()                      { return current_point; } 
   IfacePoint getPriorPoint()                   { return prior_point; }
   
      
   Collection<IfaceSensor> getAllSensors()      { return hit_sensors; }
   
   boolean pastSensor(IfaceSensor s) {
      if (s == null) return false;
      return hit_sensors.contains(s);
    }
   
   void checkEmptyBlock() {
      ShoreLog.logD("SAFETY","Note block " + for_block + " seems empty");
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
      ShoreLog.logD("SAFETY","Done with block " + for_block + " " +
            time + " " + exit_time);
      if (time != 0 && exit_time != time) return;
      active_blocks.remove(for_block);
      for_block.setBlockState(ShoreBlockState.EMPTY);
      current_point = null;
      prior_point = null;
      is_verified = false;
    }
   
   private void checkPriorBlock(IfaceSensor s) {
      // check if first sensor is a connector with prior verified block
      for (IfaceConnection conn : for_block.getConnections()) {
         if (conn.getEntrySensor(for_block) == s) {
            IfaceBlock prev = conn.getOtherBlock(for_block);
            BlockData bd = active_blocks.get(prev);
            if (bd != null && bd.is_verified) {
               is_verified = true;
               prior_point = conn.getGapPoint();
               ShoreLog.logD("SAFETY","Verified " + for_block + 
                     " based on prior block " + bd.for_block);
               break;
             }
          }
       }
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

