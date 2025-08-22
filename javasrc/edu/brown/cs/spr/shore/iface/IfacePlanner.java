/********************************************************************************/
/*                                                                              */
/*              IfacePlanner.java                                               */
/*                                                                              */
/*      Controller for planning                                                 */
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



package edu.brown.cs.spr.shore.iface;

import java.util.Collection;
import java.util.EventListener;



public interface IfacePlanner extends IfaceConstants
{

Collection<PlanAction> getStartActions();
Collection<PlanAction> getNextActions(PlanAction t);
PlanAction findAction(String name);

PlanExecutable createPlan(Object... actions);


int getMaxSteps();

enum PlanActionType {
   START,
   END,
   LOOP,
}

interface PlanExecutable {
   void addStep(PlanAction act,int count);
   default void addStep(PlanAction act)         { addStep(act,0); }
   void execute(IfaceEngine eng);
   void abort();
   void addPlanCallback(PlanCallback cb);
   void removePlanCallback(PlanCallback cb);
}


interface PlanCallback extends EventListener {
   default void planStarted(PlanExecutable p)                                   { }
   default void planStepCompleted(PlanExecutable p,PlanAction act,int ct)       { }
   default void planCompleted(PlanExecutable p,boolean abort)           { }
}



interface PlanAction {
   String getName();
   PlanActionType getActionType();
   IfaceSignal getSignal();
}


}       // end of interface IfacePlanner




/* end of IfacePlanner.java */

