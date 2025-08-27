/********************************************************************************/
/*                                                                              */
/*              PlannerEvent.java                                               */
/*                                                                              */
/*      Step-by-step events inside a plan                                       */
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


import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceEngine;

abstract class PlannerEvent implements PlannerConstants
{
      


/********************************************************************************/
/*                                                                              */
/*      Static creation methods                                                 */
/*                                                                              */
/********************************************************************************/

static PlannerEvent createEvent(PlannerEventType typ,PlannerPlan plan,
      Object... data)
{
   PlannerEvent evt = null;
   switch (typ) {
      case START :
         evt = new EventStart(plan);
         break;
      case FINISH : 
         evt = new EventFinish(plan);
         break;
      case ACTION_STARTED :
         PlannerActionBase act0 = (PlannerActionBase) data[0];
         evt = new EventActionStart(plan,act0);
         break;
      case ACTION_COMPLETE :
         PlannerActionBase act = (PlannerActionBase) data[0];
         int ct = 0;
         if (data.length > 1) {
            ct = ((Number) data[1]).intValue();
          }
         evt = new EventActionComplete(plan,act,ct);
         break;
      case BLOCK :
         IfaceBlock b0 = (IfaceBlock) data[0];
         IfaceBlock b1 = (IfaceBlock) data[1];
         evt = new EventBlock(plan,b0,b1);
         break;
      case PAUSE :
         break;
    }
   
   return evt;
}



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

protected PlannerPlan   for_plan;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected PlannerEvent(PlannerPlan plan)
{
   for_plan = plan;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

IfaceBlock getBlock()                           { return null; }
IfaceBlock getPriorBlock()                      { return null; }
IfaceBlock getNextBlock()                       { return null; }
void setNextBlock(IfaceBlock blk)               { }


/********************************************************************************/
/*                                                                              */
/*      Generic methods                                                         */
/*                                                                              */
/********************************************************************************/

void waitForAction(IfaceEngine eng) 
{
   // if the event is not immediate -- wait for the event to occur
}


void noteDone()
{
   // take any action associated with the event
   // send messages based on event completion
}


abstract PlannerEventType getEventType();


/********************************************************************************/
/*                                                                              */
/*      Setup switches on block entry                                           */
/*                                                                              */
/********************************************************************************/



/********************************************************************************/
/*                                                                              */
/*      Start plan event                                                        */
/*                                                                              */
/********************************************************************************/

private static class EventStart extends PlannerEvent {
   
   EventStart(PlannerPlan plan) {
      super(plan);
    }
   
   @Override PlannerEventType getEventType() {
      return PlannerEventType.START;
    }
   
   @Override void noteDone() {
      for_plan.firePlanStarted(); 
    }
   
   @Override public String toString() {
      return "START";
    }
   
}       // end of inner class EventStart



/********************************************************************************/
/*                                                                              */
/*      Finish plan event                                                       */
/*                                                                              */
/********************************************************************************/

private static class EventFinish extends PlannerEvent {

   EventFinish(PlannerPlan plan) {
      super(plan);
    }
   
   @Override PlannerEventType getEventType() {
      return PlannerEventType.FINISH;
    }
   
   @Override void waitForAction(IfaceEngine eng) {
      // should wait for train to be exclusively in the finish block
    }
   
   @Override void noteDone() {
      for_plan.getEngine().setThrottle(0.0);
      for_plan.firePlanCompleted(false); 
    }
   
   @Override public String toString() {
      return "FINISH";
    }
   
}       // end of inner class EventFinish



/********************************************************************************/
/*                                                                              */
/*      Action complete event                                                   */
/*                                                                              */
/********************************************************************************/

private static class EventActionStart extends PlannerEvent {

   private PlannerActionBase done_action;

   EventActionStart(PlannerPlan plan,PlannerActionBase act) {
      super(plan);
      done_action = act;
    }
   
   @Override PlannerEventType getEventType() {
      return PlannerEventType.ACTION_COMPLETE;
    }
   
   @Override void noteDone() {
      for_plan.firePlanStepStarted(done_action); 
    } 
   
   @Override public String toString() {
      return "START " + done_action.getName();
    }
   
}       // end of inner class EventActionStart


/********************************************************************************/
/*                                                                              */
/*      Action complete event                                                   */
/*                                                                              */
/********************************************************************************/

private static class EventActionComplete extends PlannerEvent {
   
   private PlannerActionBase done_action;
   private int done_count;
   
   EventActionComplete(PlannerPlan plan,PlannerActionBase act,int ct) {
      super(plan);
      done_action = act;
      done_count = ct;
    }
   
   @Override PlannerEventType getEventType() {
      return PlannerEventType.ACTION_COMPLETE;
    }
   
   @Override void noteDone() {
      for_plan.firePlanStepCompleted(done_action,done_count);
    } 
   
   @Override public String toString() {
      return "DONE " + done_action.getName() + "#" + done_count;
    }
   
}       // end of inner class EventActionComplete



/********************************************************************************/
/*                                                                              */
/*      Enter Block Action                                                      */
/*                                                                              */
/********************************************************************************/

private static class EventBlock extends PlannerEvent {
   
   private IfaceBlock prior_block;
   private IfaceBlock enter_block;
   private IfaceBlock next_block;
   
   EventBlock(PlannerPlan plan,IfaceBlock prior,IfaceBlock enter) {
      super(plan);
      prior_block = prior;
      enter_block = enter;
      next_block = null;
    }
   
   @Override PlannerEventType getEventType() {
      return PlannerEventType.BLOCK;
    }
   
   @Override IfaceBlock getBlock()                      { return enter_block; }
   @Override IfaceBlock getPriorBlock()                 { return prior_block; }
   @Override IfaceBlock getNextBlock()                  { return next_block; }
   @Override void setNextBlock(IfaceBlock b)            { next_block = b; }
   
   @Override void waitForAction(IfaceEngine eng) {
      boolean oncourse = for_plan.waitForBlockEntry(enter_block); 
      if (!oncourse) for_plan.abort();
    }
   
   @Override void noteDone() {
      for_plan.setupSwitches(prior_block,enter_block,next_block);
    }
   
   @Override public String toString() {
      return "BLOCK-" + enter_block.getId();
    }
}


}       // end of class PlannerEvent



/* end of PlannerEvent.java */

