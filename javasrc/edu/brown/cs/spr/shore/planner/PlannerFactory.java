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
import java.util.List;
import java.util.TreeSet;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.spr.shore.iface.IfaceModel;
import edu.brown.cs.spr.shore.iface.IfaceNetwork;
import edu.brown.cs.spr.shore.iface.IfacePlanner;
import edu.brown.cs.spr.shore.iface.IfaceTrains;

public class PlannerFactory implements PlannerConstants, IfacePlanner
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceModel      layout_model;
private IfaceTrains     train_model;
private IfaceNetwork    network_model;
private int             max_plan_steps;

private List<PlannerActionBase> train_actions;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public PlannerFactory(IfaceNetwork net,IfaceModel mdl,IfaceTrains trns)
{
   layout_model = mdl; 
   train_model = trns;
   network_model = net;
   train_actions = new ArrayList<>();
   
   Element xml0 = layout_model.getModelXml();
   Element xml = IvyXml.getChild(xml0,"PLANNER");
   max_plan_steps = IvyXml.getAttrInt(xml,"STEPS",5);
   for (Element lxml : IvyXml.children(xml,"LOOP")) {
      PlannerLoopAction pl = new PlannerLoopAction(this,lxml,true);
      train_actions.add(pl);
      PlannerLoopAction pl1 = new PlannerLoopAction(this,lxml,false);
      train_actions.add(pl1);
    }
   for (Element sxml : IvyXml.children(xml,"START")) {
      PlannerStartAction ps = new PlannerStartAction(this,sxml);
      train_actions.add(ps);
    }
   for (Element sxml : IvyXml.children(xml,"END")) {
      PlannerEndAction ps = new PlannerEndAction(this,sxml);
      train_actions.add(ps);
    }
   for (PlannerActionBase pact : train_actions) {
      pact.findExits();
    }
}



/********************************************************************************/
/*                                                                              */
/*      Access  methods                                                         */
/*                                                                              */
/********************************************************************************/

IfaceModel getLayoutModel()                     { return layout_model; }

List<PlannerActionBase> getAllActions()
{
   List<PlannerActionBase> rslt = new ArrayList<>(train_actions);
   return rslt;
}


@Override public PlanAction findAction(String name) 
{
   if (name == null) return null;
   
   for (PlannerActionBase pd : getAllActions()) {
      if (pd.getName().equals(name)) return pd;
    }
   
   return null;
}


@Override public Collection<PlanAction> getStartActions() 
{ 
   Collection<PlanAction> rslt = new TreeSet<>();
   for (PlannerActionBase pact : train_actions) {
      if (pact.getActionType() == PlanActionType.START) {
         rslt.add(pact);
       }
    }
   return rslt;
}


@Override public Collection<PlanAction> getNextActions(PlanAction t)
{
   PlannerActionBase pd = (PlannerActionBase) t;
   Collection<PlanAction> rslt = new TreeSet<>();
   for (PlannerExit pe : pd.getExits()) {
      rslt.add(pe.getDestination());
    }
   
   return rslt;
}


@Override public int getMaxSteps() 
{
   return max_plan_steps;
}


/********************************************************************************/
/*                                                                              */
/*      Plan creation methods                                                   */
/*                                                                              */
/********************************************************************************/

@Override public PlannerPlan createPlan(Object... actions) 
{
   PlannerPlan plan = new PlannerPlan(network_model,train_model);
   
   for (int i = 0; i < actions.length; ++i) {
      if (actions[i] instanceof PlanAction) {
         PlanAction pa = (PlanAction) actions[i];
         int ct = 0;
         if (i+1 < actions.length) {
            if (actions[i+1] instanceof Integer) {
               Integer iv = (Integer) (actions[++i]);
               ct = iv;
             }
          }
         plan.addStep(pa,ct);
       }
    }
      
   return plan;
}


}       // end of class PlannerFactory




/* end of PlannerFactory.java */

