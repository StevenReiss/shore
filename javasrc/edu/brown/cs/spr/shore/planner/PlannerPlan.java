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

import edu.brown.cs.ivy.swing.SwingEventListenerList;
import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceConnection;
import edu.brown.cs.spr.shore.iface.IfaceEngine;
import edu.brown.cs.spr.shore.iface.IfaceNetwork;
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
private IfaceNetwork network_model;
private IfaceTrains train_model;
private List<PlanEvent> plan_events;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

PlannerPlan(IfaceNetwork nm,IfaceTrains tm)
{
   network_model = nm;
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
               plan_events.add(new EventBlock(blk0));
               curblk = blk0;
               if (blk0 == sblk) break;
             }
          }
         for (IfaceBlock blk1 : pe.getThroughBlocks()) {
            plan_events.add(new EventBlock(blk1));
            curblk = blk1;
          }
       }
    }
   
   switch (act.getActionType()) {
      case START :
         for (int i = 0; i < actblocks.size()-1; ++i) {
            IfaceBlock b = actblocks.get(i);
            plan_events.add(new EventBlock(b));
            curblk = b;
          }
         plan_events.add(new EventStepComplete());
         break; 
      case END :
         for (IfaceBlock b : actblocks) {
            plan_events.add(new EventBlock(b));
            curblk = b;
          }
         plan_events.add(new EventStepComplete());
         break;
      case LOOP :
         List<IfaceBlock> loopblks = act.getBlocks();
         int index = 0;
         for (IfaceBlock lblk : loopblks) {
            if (isConnected(curblk,lblk)) break;
            ++index;
          }
         plan_events.add(new EventBlock(loopblks.get(index)));
         curblk = loopblks.get(index);
         for (int i = 0 ; i < ps.getCount(); ++i) {
            for (int j = 0; j < loopblks.size(); ++j) {
               int k = (index+ 1 + j) % loopblks.size();
               IfaceBlock b = loopblks.get(k);
               plan_events.add(new EventBlock(b));
               curblk = b;
             }
            plan_events.add(new EventStepComplete());
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
/*      Various plan events                                                     */
/*                                                                              */
/********************************************************************************/

private class EventStart implements PlanEvent {
   
}


private class EventFinish implements PlanEvent {
   
}

private class EventStopEngine implements PlanEvent {
   
}

private class EventThrottle implements PlanEvent {
   
}

private class EventStepComplete implements PlanEvent {
   
}

private class EventBlock implements PlanEvent {
   
   private IfaceBlock enter_block;
  
   EventBlock(IfaceBlock enter) {
      enter_block = enter;
    }
   
}




}       // end of class PlannerPlan




/* end of PlannerPlan.java */

