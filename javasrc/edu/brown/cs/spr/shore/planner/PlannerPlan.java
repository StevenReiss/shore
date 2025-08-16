/********************************************************************************/
/*                                                                              */
/*              PlannerPlan.java                                                */
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



package edu.brown.cs.spr.shore.planner;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.brown.cs.ivy.swing.SwingEventListenerList;
import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceConnection;
import edu.brown.cs.spr.shore.iface.IfaceEngine;
import edu.brown.cs.spr.shore.iface.IfaceModel;
import edu.brown.cs.spr.shore.iface.IfacePoint;
import edu.brown.cs.spr.shore.iface.IfaceSafety;
import edu.brown.cs.spr.shore.iface.IfaceSwitch;
import edu.brown.cs.spr.shore.iface.IfaceTrains;
import edu.brown.cs.spr.shore.iface.IfacePlanner.PlanAction;
import edu.brown.cs.spr.shore.iface.IfacePlanner.PlanCallback;
import edu.brown.cs.spr.shore.iface.IfacePlanner.PlanExecutable;
import edu.brown.cs.spr.shore.shore.ShoreLog;

class PlannerPlan implements PlanExecutable, PlannerConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<PlanStep> plan_steps;
private SwingEventListenerList<PlanCallback> plan_listeners; 
private IfaceModel layout_model;
private IfaceSafety safety_model;
private IfaceTrains train_model;
private List<PlanEvent> plan_events;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

PlannerPlan(IfaceSafety safety,IfaceModel layout,IfaceTrains tm)
{
   layout_model = layout;
   safety_model = safety;
   train_model = tm;
   
   plan_steps = new ArrayList<>();
   plan_listeners = new SwingEventListenerList<>(PlanCallback.class); 
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public void addStep(PlanAction pa,int ct)
{
   PlanStep ps = new PlanStep(pa,ct);
   plan_steps.add(ps);
}


@Override public void addPlanCallback(PlanCallback cb)
{
   plan_listeners.add(cb);
}


@Override public void removePlanCallback(PlanCallback cb)
{
   plan_listeners.remove(cb);
}



/********************************************************************************/
/*                                                                              */
/*      Exeuction methods                                                       */
/*                                                                              */
/********************************************************************************/

@Override public void execute(IfaceEngine eng)
{
   setupPlan();
   
   // add a callback for train position (look at block for current point; check for stopped)
   //    if new block -- change active train block and notifyAll
   //    if train starts after stop (or enters off state -- handle abort)
   // start up engine, set switches for initial block
   // go through plan steps:
   //    if block step -- wait for the block
   //    if event step -- send the proper event as callback
   //    if stop step -- stop the train && set timer to restart
   //    if start step -- start the train
   // possibly create a thread to do the plan execution
   //    it can send the events
   //    it can safely wait for the current block to be exited
   //    it can safely wait for a timer to restart train
   
}


@Override public void abort()
{ }



/********************************************************************************/
/*                                                                              */
/*      One step in a plan                                                      */
/*                                                                              */
/********************************************************************************/

private class PlanStep {

   private PlannerActionBase step_action;
   private int step_count;
   
   PlanStep(PlanAction pd,int ct) {
      step_action = (PlannerActionBase) pd;
      step_count = ct;
    }
   
   PlannerActionBase getAction()                { return step_action; } 
   int getCount()                               { return step_count; }
   
}       // end of inner class PlanStep



/********************************************************************************/
/*                                                                              */
/*      Setup the sequence of blocks for the plan                               */
/*                                                                              */
/********************************************************************************/

private void setupPlan()
{
   plan_events = new ArrayList<>();
   plan_events.add(new EventStart());
   
   IfaceBlock curblk = null;
   PlanStep prior = null;
   for (PlanStep step : plan_steps) {
      curblk = addEvents(step,prior,curblk);
      prior = step;
      plan_events.add(new EventActionComplete(step.getAction()));
    }
   
   plan_events.add(new EventFinish());
   
   // associate next block with each block event
   EventBlock bact = null;
   for (PlanEvent evt : plan_events) {
      if (evt instanceof EventBlock) {
         EventBlock e1 = (EventBlock) evt;
         if (bact != null) {
            bact.setNextBlock(e1.getBlock());
          }
         bact = e1;
       }
    }
}


private IfaceBlock addEvents(PlanStep ps,PlanStep prior,IfaceBlock curblk)
{
   PlannerActionBase act = ps.getAction();
   List<IfaceBlock> actblocks = act.getBlocks();
   
   if (prior != null) {
      // add all the blocks needed from prior to the next block
      PlannerExit pe = findExit(prior.getAction(),ps.getAction());
      if (pe == null) {
         ShoreLog.logE("PLANNER","No exit found for " + prior.getAction() + " " +
               ps.getAction());
       }
      else {
         IfaceBlock sblk = pe.geStartBlock();
         if (sblk != curblk) {
            // handle partial loop
            List<IfaceBlock> lblks = prior.getAction().getBlocks();
            int idx = lblks.indexOf(curblk);
            for (int i = 0; i < lblks.size()-1; ++i) {
               int nidx = (idx+1+i) % lblks.size();
               IfaceBlock blk0 = lblks.get(nidx);
               plan_events.add(new EventBlock(curblk,blk0));
               curblk = blk0;
               if (blk0 == sblk) break;
             }
          }
         for (IfaceBlock blk1 : pe.getThroughBlocks()) {
            plan_events.add(new EventBlock(curblk,blk1));
            curblk = blk1;
          }
       }
    }
   
   switch (act.getActionType()) {
      case START :
         for (int i = 0; i < actblocks.size()-1; ++i) {
            IfaceBlock b = actblocks.get(i);
            plan_events.add(new EventBlock(curblk,b));
            curblk = b;
          }
         break; 
      case END :
         for (IfaceBlock b : actblocks) {
            plan_events.add(new EventBlock(curblk,b));
            curblk = b;
          }
         break;
      case LOOP :
         List<IfaceBlock> loopblks = act.getBlocks();
         int index = 0;
         for (IfaceBlock lblk : loopblks) {
            if (isConnected(curblk,lblk)) break;
            ++index;
          }
         IfaceBlock b2 = loopblks.get(index);
         plan_events.add(new EventBlock(curblk,b2));
         curblk = b2;
         curblk = loopblks.get(index);
         for (int i = 0; i < ps.getCount(); ++i) {
            for (int j = 0; j < loopblks.size(); ++j) {
               int k = (index+ 1 + j) % loopblks.size();
               IfaceBlock b = loopblks.get(k);
               plan_events.add(new EventBlock(curblk,b));
               curblk = b;
             }
            plan_events.add(new EventActionComplete(act,i+1));
          }
         break;
    }
   
   return curblk;
}


private PlannerExit findExit(PlannerActionBase from,PlannerActionBase to)
{
    for (PlannerExit exit : from.getExits()) {
       if (exit.getDestination() == to) {
          return exit;
        }
     }
    
    return null;
}


private boolean isConnected(IfaceBlock b0,IfaceBlock b1)
{
   for (IfaceConnection c : b0.getConnections()) {
      if (c.getOtherBlock(b0) == b1) return true;
    }
   return false;
}


/********************************************************************************/
/*                                                                              */
/*      Setup switches on block entry                                           */
/*                                                                              */
/********************************************************************************/

private void setupSwitches(IfaceBlock prior,IfaceBlock enter,IfaceBlock next) 
{
   IfacePoint gap0 = null;
   IfacePoint p0 = null;
   IfacePoint gap1 = null;
   IfacePoint p1 = null;
   for (IfaceConnection conn : enter.getConnections()) {
      if (conn.getOtherBlock(enter) == next) {
         gap1 = conn.getGapPoint();
         p1 = conn.getExitSensor(enter).getAtPoint();
       }
      else if (prior == null || conn.getOtherBlock(enter) == prior) {
         gap0 = conn.getGapPoint();
         p0 = conn.getEntrySensor(enter).getAtPoint();
       }
    }
   if (gap0 == null || gap1 == null) return;
   
   Set<IfacePoint> allpts = layout_model.findSuccessorPoints(gap0,p0,false);
   Set<IfacePoint> bwdpts = layout_model.findSuccessorPoints(gap1,p1,false);
   allpts.retainAll(bwdpts);
   for (IfaceSwitch sw : layout_model.getSwitches()) {
      if (allpts.contains(sw.getPivotPoint())) {
         if (allpts.contains(sw.getNSensor().getAtPoint())) {
            safety_model.setSwitch(sw,ShoreSwitchState.N);
          }
         else if (allpts.contains(sw.getRSensor().getAtPoint())) {
            safety_model.setSwitch(sw,ShoreSwitchState.R);
          }
       }
    }
}


/********************************************************************************/
/*                                                                              */
/*      Various plan events                                                     */
/*                                                                              */
/********************************************************************************/

private class EventStart implements PlanEvent {
   
   EventStart() { }
   
   @Override public String toString() {
      return "START";
    }
   
}       // end of inner class EventStart


private class EventFinish implements PlanEvent {
   
   EventFinish() { }
   
   @Override public String toString() {
      return "FINISH";
    }
   
}       // end of inner class EventFinish



private class EventActionComplete implements PlanEvent {
   
   private PlannerActionBase done_action;
   
   EventActionComplete(PlannerActionBase act) {
      done_action = act;
    }
   
   EventActionComplete(PlannerActionBase act,int ct) {
      done_action = act;
    }
   
   @Override public String toString() {
      return "DONE " + done_action.getName();
    }
   
}       // end of inner class EventActionComplete



private class EventBlock implements PlanEvent {
   
   private IfaceBlock prior_block;
   private IfaceBlock enter_block;
   private IfaceBlock next_block;
  
   EventBlock(IfaceBlock prior,IfaceBlock enter) {
      prior_block = prior;
      enter_block = enter;
      next_block = null;
    }
   
   IfaceBlock getBlock()                        { return enter_block; }
   IfaceBlock getNextBlock()                    { return next_block; }
   IfaceBlock getPriorBlock()                   { return prior_block; }
   
   void setNextBlock(IfaceBlock b)              { next_block = b; }
   
   @Override public String toString() {
      return enter_block.getId();
    }
   
}       // end of inner class EventBlock

// Add action for stopping the train (at a sensor -- for a station)
// Add action setting thtottle speed



/********************************************************************************/
/*                                                                              */
/*      Callback methods                                                       */
/*                                                                              */
/********************************************************************************/

private void firePlanStarted()
{
   for (PlanCallback cb : plan_listeners) {
      cb.planStarted(this);
    }
}


private void firePlanCompleted()
{
   for (PlanCallback cb : plan_listeners) {
      cb.planCompleted(this);
    }
}


private void firePlanStepCompleted(PlannerActionBase act,int ct) 
{
   for (PlanCallback cb : plan_listeners) {
      cb.planStepCompleted(this,act,ct); 
    }
}



}       // end of class PlannerPlan




/* end of PlannerPlan.java */

