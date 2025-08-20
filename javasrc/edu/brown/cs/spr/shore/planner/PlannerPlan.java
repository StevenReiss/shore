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
import edu.brown.cs.spr.shore.iface.IfaceSafety;
import edu.brown.cs.spr.shore.iface.IfaceSwitch;
import edu.brown.cs.spr.shore.iface.IfaceTrains;
import edu.brown.cs.spr.shore.iface.IfaceEngine.EngineCallback;
import edu.brown.cs.spr.shore.iface.IfacePlanner.PlanAction;
import edu.brown.cs.spr.shore.iface.IfacePlanner.PlanCallback;
import edu.brown.cs.spr.shore.iface.IfacePlanner.PlanExecutable;
import edu.brown.cs.spr.shore.iface.IfacePoint;
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
private boolean    abort_plan;
private IfaceEngine for_engine;
private IfaceBlock  engine_block;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

PlannerPlan(IfaceSafety safety,IfaceModel layout,IfaceTrains tm)
{
   layout_model = layout;
   safety_model = safety;
   
   plan_steps = new ArrayList<>();
   plan_listeners = new SwingEventListenerList<>(PlanCallback.class); 
   abort_plan = false;
   for_engine = null;
   engine_block = null;
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


IfaceModel getLayoutModel()                     { return layout_model; }
IfaceSafety getSafetyModel()                    { return safety_model; }



/********************************************************************************/
/*                                                                              */
/*      Exeuction methods                                                       */
/*                                                                              */
/********************************************************************************/

@Override public void execute(IfaceEngine eng)
{
   List<PlannerEvent> steps = setupPlan();
   for_engine = eng;
   
   PlanFollower runner = new PlanFollower(steps);
   abort_plan = false;
   runner.start();
}


@Override public void abort()
{
   synchronized (this) {
      abort_plan = true;
      notifyAll();
    }
}


private PlannerEvent createEvent(PlannerEventType type,Object... data)
{
   return PlannerEvent.createEvent(type,this,data);
}



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

private List<PlannerEvent> setupPlan()
{
   List<PlannerEvent> planevents = new ArrayList<>();
   planevents.add(createEvent(PlannerEventType.START));
   
   IfaceBlock curblk = null;
   PlanStep prior = null;
   for (PlanStep step : plan_steps) {
      curblk = addEvents(step,prior,curblk,planevents);
      prior = step;
      planevents.add(createEvent(PlannerEventType.ACTION_COMPLETE,step.getAction()));
    }
   
   planevents.add(createEvent(PlannerEventType.FINISH)); 
   
   // associate next block with each block event
   PlannerEvent bact = null;
   for (PlannerEvent evt : planevents) {
      if (evt.getEventType() == PlannerEventType.BLOCK) {
         if (bact != null) bact.setNextBlock(evt.getBlock());
         bact = evt;
       }
    }
   
   return planevents;
}


private IfaceBlock addEvents(PlanStep ps,PlanStep prior,IfaceBlock curblk,List<PlannerEvent> events)
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
               events.add(createEvent(PlannerEventType.BLOCK,curblk,blk0));
               curblk = blk0;
               if (blk0 == sblk) break;
             }
          }
         for (IfaceBlock blk1 : pe.getThroughBlocks()) {
            events.add(createEvent(PlannerEventType.BLOCK,curblk,blk1));
            curblk = blk1;
          }
       }
    }
   
   switch (act.getActionType()) {
      case START :
         for (int i = 0; i < actblocks.size()-1; ++i) {
            IfaceBlock b = actblocks.get(i);
            events.add(createEvent(PlannerEventType.BLOCK,curblk,b));
            curblk = b;
          }
         break; 
      case END :
         for (IfaceBlock b : actblocks) {
            events.add(createEvent(PlannerEventType.BLOCK,curblk,b));
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
         events.add(createEvent(PlannerEventType.BLOCK,curblk,b2));
         curblk = b2;
         curblk = loopblks.get(index);
         for (int i = 0; i < ps.getCount(); ++i) {
            for (int j = 0; j < loopblks.size(); ++j) {
               int k = (index+ 1 + j) % loopblks.size();
               IfaceBlock b = loopblks.get(k);
               events.add(createEvent(PlannerEventType.BLOCK,curblk,b));
               curblk = b;
             }
            events.add(createEvent(PlannerEventType.ACTION_COMPLETE,act,i+1));
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
/*      Callback methods                                                       */
/*                                                                              */
/********************************************************************************/

void firePlanStarted()
{
   for (PlanCallback cb : plan_listeners) {
      cb.planStarted(this);
    }
}


void firePlanCompleted()
{
   for (PlanCallback cb : plan_listeners) {
      cb.planCompleted(this);
    }
}


void firePlanStepCompleted(PlannerActionBase act,int ct) 
{
   for (PlanCallback cb : plan_listeners) {
      cb.planStepCompleted(this,act,ct); 
    }
}



/********************************************************************************/
/*                                                                              */
/*      Helper methods for plan execution                                       */
/*                                                                              */
/********************************************************************************/

void setupSwitches(IfaceBlock prior,IfaceBlock enter,IfaceBlock next) 
{
   ShoreLog.logD("PLANNER","Set switches " + prior + " " + enter + " " + next);
   
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
   ShoreLog.logD("PLANNER","FWD points " + allpts);
   ShoreLog.logD("PLANNER","BWD points " + bwdpts);
   allpts.retainAll(bwdpts); 
   ShoreLog.logD("PLANNER","Point set: " + allpts);
   for (IfaceSwitch sw : layout_model.getSwitches()) {
      if (allpts.contains(sw.getPivotPoint())) {
         ShoreLog.logD("PLANNER","Check switch " + sw + " " + 
               sw.getNSensor().getAtPoint() +
               " " + sw.getRSensor().getAtPoint());
         if (allpts.contains(sw.getNSensor().getAtPoint())) {
            safety_model.setSwitch(sw,ShoreSwitchState.N);
          }
         else if (allpts.contains(sw.getRSensor().getAtPoint())) {
            safety_model.setSwitch(sw,ShoreSwitchState.R);
          }
       }
    }
}



boolean waitForBlockEntry(IfaceBlock blk)
{
   synchronized (this) {
      for ( ; ; ) {
         IfacePoint pt = for_engine.getCurrentPoint();
         if (pt != null) {
            IfaceBlock atblk = pt.getBlock();
            if (atblk == blk) {
               engine_block = blk;
               return true;
             }
            if (engine_block == null) engine_block = atblk;
            if (atblk != engine_block) {
               ShoreLog.logD("PLANNER","Unexpected block " + atblk + " " +
                     engine_block + " " + blk);
               return false;
             }
          }
         try {
            wait(5000);
          }
         catch (InterruptedException e) { }
       }
    }
}


/********************************************************************************/
/*                                                                              */
/*      Plan Follower Thread                                                    */
/*                                                                              */
/********************************************************************************/

private final class PlanFollower extends Thread implements EngineCallback {

   private List<PlannerEvent> plan_events;
   private int event_index;
   
   PlanFollower(List<PlannerEvent> events) {
       plan_events = events;
       event_index = 0;
       for_engine.addEngineCallback(this);
     }
   
   @Override public void run() {
      while (event_index < plan_events.size()) {
         PlannerEvent event = plan_events.get(event_index);
         ShoreLog.logD("PLANNER","Work on step " + event_index + " " + event);
         if (abort_plan) break;
         event.waitForAction(for_engine); 
         if (abort_plan) break;
         event.noteDone();
         ++event_index;
       }
      for_engine.removeEngineCallback(this);
    }
   
   @Override public void engineChanged(IfaceEngine eng) { 
      // if we need to monitor throttle or other aspects do a notify here
      // should monitor for engine reboot and abort plan
    }
   
   @Override public void enginePositionChanged(IfaceEngine eng) {
      synchronized (PlannerPlan.this) {
         PlannerPlan.this.notifyAll();
       }
    }
   
}


}       // end of class PlannerPlan




/* end of PlannerPlan.java */

