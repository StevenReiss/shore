/********************************************************************************/
/*                                                                              */
/*              PlannerFactory.java                                             */
/*                                                                              */
/*      Main class for managine planning                                        */
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



package edu.brown.cs.spr.shore.planner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceConnection;
import edu.brown.cs.spr.shore.iface.IfaceModel;
import edu.brown.cs.spr.shore.iface.IfaceNetwork;
import edu.brown.cs.spr.shore.iface.IfacePoint;
import edu.brown.cs.spr.shore.iface.IfaceSwitch;
import edu.brown.cs.spr.shore.iface.IfaceTrains;

public class PlannerFactory implements PlannerConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceModel      layout_model;
private IfaceTrains     train_base;
private IfaceNetwork    network_model;

private List<PlannerLoop> train_loops;
private List<PlannerStart> train_starts;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

PlannerFactory(IfaceModel mdl,IfaceTrains trns,IfaceNetwork net)
{
   layout_model = mdl;
   train_base = trns;
   network_model = net;
   train_loops = new ArrayList<>();
   train_starts = new ArrayList<>();
   
   Element xml0 = layout_model.getModelXml();
   Element xml = IvyXml.getChild(xml0,"PLANNER");
   for (Element lxml : IvyXml.children(xml,"LOOP")) {
      PlannerLoop pl = new PlannerLoop(lxml,true);
      train_loops.add(pl);
      PlannerLoop pl1 = new PlannerLoop(lxml,false);
      train_loops.add(pl1);
    }
   for (Element sxml : IvyXml.children(xml,"START")) {
      PlannerStart ps = new PlannerStart(sxml);
      train_starts.add(ps);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Utility methods                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceBlock findBlockById(String bid)
{
   for (IfaceBlock bb : layout_model.getBlocks()) {
      if (bb.getId().equals(bid)) {
         return bb;
       }
    } 
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      PlannerPlan -- given plan                                               */
/*                                                                              */
/********************************************************************************/

private class PlannerPlan {
   
  private List<PlannnerStep> plan_steps;
  
  PlannerPlan() {
     plan_steps = new ArrayList<>();
   }
  
  void addStep(PlannnerStep step) {
     plan_steps.add(step);
   }
  
  List<IfaceBlock> getBlockSequence() {
     return null;
   }
  
}       // end of inner class PlannerPlan



private class PlannnerStep {
   
   private PlannerDestination plan_loop;
   private IfaceBlock start_block;
   private int loop_count;
   
}       // end of inner class PlannerStep



/********************************************************************************/
/*                                                                              */
/*      Generic destination                                                     */
/*                                                                              */
/********************************************************************************/
// should include pointer to Planner Factory or layout_model
private abstract class PlannerDestination {
   
   abstract void findExits();
}


/********************************************************************************/
/*                                                                              */
/*      Information for a loop                                                  */
/*                                                                              */
/********************************************************************************/
// should be its own class
private class PlannerLoop extends PlannerDestination {
   
   private List<IfaceBlock> loop_blocks;
   private String loop_name;
   private List<PlannerExit> possible_exits;
   
   PlannerLoop(Element xml,boolean fwd) {
      loop_name = IvyXml.getAttrString(xml,"NAME");
      if (fwd) loop_name += " Forward";
      else loop_name += " Backward";
      
      loop_blocks = new ArrayList<>();
      String blks = IvyXml.getAttrString(xml,"BLOCKS");
      for (StringTokenizer tok = new StringTokenizer(blks); tok.hasMoreTokens(); ) {
         String bid = tok.nextToken();
         IfaceBlock bfnd = findBlockById(bid);
         if (bfnd == null) {
            layout_model.noteError("Block " + bid + " not found for loop " + loop_name);
          }
         else {
            loop_blocks.add(bfnd);
          }
       }
      if (!fwd) {
         Collections.reverse(loop_blocks);
       }
    }
   
   @Override void findExits() {
      if (loop_blocks.isEmpty()) return;
      IfaceBlock blk = loop_blocks.get(0);
      IfaceBlock eblk = loop_blocks.get(loop_blocks.size()-1);
      IfacePoint current = null;
      IfacePoint prior = null;
      for (IfaceConnection c : blk.getConnections()) {
         if (c.getOtherBlock(blk) == eblk) {
            current = c.getEntrySensor(blk).getAtPoint();
            prior = c.getGapPoint();
          }
       }
      findExits(0,prior,current,null);
    }
   
   private IfacePoint getSingleSuccessor(IfacePoint prior,IfacePoint cur) {
      for (IfacePoint nxt : cur.getConnectedTo()) {
         if (nxt == prior) continue;
         return nxt;
       }
      return null;
    }
   
   private void findExits(int blkidx,IfacePoint prior,IfacePoint cur,List<IfaceBlock> thrublocks) {
      Collection<IfacePoint> nextpts = cur.getConnectedTo();
      IfaceBlock b0 = cur.getBlock();
      if (b0 != null && b0 != loop_blocks.get(blkidx)) {
         if (thrublocks == null) thrublocks = new ArrayList<>();
         if (!thrublocks.contains(b0)) {
            thrublocks = new ArrayList<>(thrublocks);
            thrublocks.add(b0);
          }
       }
      if (nextpts.size() == 1) {
         return;        // dead end
       }
      else if (nextpts.size() == 2) {
         switch (cur.getType()) {
            case END :
               return;
            case GAP :
               IfacePoint pt = getSingleSuccessor(prior,cur);
               IfaceBlock nxtblk = pt.getBlock();
               int nidx = (blkidx + 1) % loop_blocks.size();
               if (nxtblk == loop_blocks.get(nidx)) {
                  findExits(blkidx+1,cur,pt,null);
                  return;
                }
               PlannerExit pe = findPlannerExit(cur,pt,thrublocks); 
               if (pe != null) {
                  possible_exits.add(pe);
                }
               else {
                  findExits(blkidx,cur,pt,thrublocks);
                }
               break;
            default :
               IfacePoint nxt = getSingleSuccessor(prior,cur);
               findExits(blkidx,cur,nxt,thrublocks);
               break;
          }
       }
      else {
         List<IfacePoint> next = findSwitchNext(cur,prior);
         for (IfacePoint trypt : next) {
            findExits(blkidx,cur,trypt,thrublocks);
          }
       }
    }
   
   private List<IfacePoint> findSwitchNext(IfacePoint pivot,IfacePoint prior) {
      List<IfacePoint> rslt = new ArrayList<>();
      IfaceSwitch atsw = null;
      for (IfaceSwitch sw : layout_model.getSwitches()) {
         if (sw.getPivotPoint() == pivot) {
            atsw = sw;
            break;
          }
       }
      Set<IfacePoint> priorset = layout_model.findPriorPoints(prior,pivot);
      if (priorset.contains(atsw.getNSensor().getAtPoint()) || 
            priorset.contains(atsw.getRSensor().getAtPoint())) {
         for (IfacePoint pt : pivot.getConnectedTo()) {
            if (pt == prior) continue;
            if (layout_model.goesTo(pivot,pt,atsw.getNSensor().getAtPoint()) ||
                  layout_model.goesTo(pivot,pt,atsw.getRSensor().getAtPoint())) {
               continue;
             }
            rslt.add(pt);
          }
         return rslt;
       }
      for (IfacePoint pt : pivot.getConnectedTo()) {
         if (pt != prior) {
            rslt.add(pt);
          }
       }
      return rslt;
    }
   
   private PlannerExit findPlannerExit(IfacePoint gap,IfacePoint cur,List<IfaceBlock> thru) {
      // get the block of cur
      // if it matches a start/end block --. return an exit to that
      // else if it matches a block inside a loop:  follow the patch from gap->cur ... to determine next block in loop
      // if it matches the next expected block, return an exit to that, else keep trying other loops
      return null;
    }
   
         
}       // end of inner class PlannerLoop



/********************************************************************************/
/*                                                                              */
/*      Start/End point for a plan                                              */
/*                                                                              */
/********************************************************************************/

private class PlannerStart extends PlannerDestination {
   
   private String start_name;
   private IfaceBlock start_block;
   
   PlannerStart(Element xml) {
      start_name = IvyXml.getAttrString(xml,"NAME");
      String bid = IvyXml.getAttrString(xml,"BLOCK");
      start_block = findBlockById(bid);
      if (start_block == null) {
         layout_model.noteError("Block " + bid + " not found for start " + start_name);
       }
    }
   
   @Override void findExits() { }
   
}       // end of inner class PlannerStart



/********************************************************************************/
/*                                                                              */
/*      Possible exit point for a loop                                          */
/*                                                                              */
/********************************************************************************/

private class PlannerExit {
   
   private IfaceSwitch  exit_switch;
   private PlannerDestination exit_target;
   private List<IfaceBlock> enter_blocks;
   
   PlannerExit(IfaceSwitch sw,PlannerDestination pd,List<IfaceBlock> blks) {
      exit_switch = sw;
      exit_target = pd;
      enter_blocks = new ArrayList<>(blks);
    }
   
}       // end of inner class Planner Exit



}       // end of class PlannerFactory




/* end of PlannerFactory.java */

