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
import edu.brown.cs.spr.shore.iface.IfaceBlock;
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
private IfaceTrains     train_base;
private IfaceNetwork    network_model;
private int             max_plan_steps;

private List<PlannerLoop> train_loops;
private List<PlannerStart> train_starts;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public PlannerFactory(IfaceNetwork net,IfaceModel mdl,IfaceTrains trns)
{
   layout_model = mdl; 
   train_base = trns;
   network_model = net;
   train_loops = new ArrayList<>();
   train_starts = new ArrayList<>();
   
   Element xml0 = layout_model.getModelXml();
   Element xml = IvyXml.getChild(xml0,"PLANNER");
   max_plan_steps = IvyXml.getAttrInt(xml,"STEPS",5);
   for (Element lxml : IvyXml.children(xml,"LOOP")) {
      PlannerLoop pl = new PlannerLoop(this,lxml,true);
      train_loops.add(pl);
      PlannerLoop pl1 = new PlannerLoop(this,lxml,false);
      train_loops.add(pl1);
    }
   for (Element sxml : IvyXml.children(xml,"START")) {
      PlannerStart ps = new PlannerStart(this,sxml);
      train_starts.add(ps);
    }
   
   for (PlannerLoop pl : train_loops) {
      pl.findExits();
    }
   for (PlannerStart ps : train_starts) {
      ps.findExits();
    }
}



/********************************************************************************/
/*                                                                              */
/*      Access  methods                                                         */
/*                                                                              */
/********************************************************************************/

IfaceModel getLayoutModel()                     { return layout_model; }

List<PlannerDestination> getDestinations()
{
   List<PlannerDestination> rslt = new ArrayList<>();
   rslt.addAll(train_loops);
   rslt.addAll(train_starts);
   return rslt;
}


@Override public PlanTarget findTarget(String name) 
{
   if (name == null) return null;
   
   for (PlannerDestination pd : getDestinations()) {
      if (pd.getName().equals(name)) return pd;
    }
   
   return null;
}


@Override public Collection<PlanTarget> getStartTargets() 
{
   return new TreeSet<>(train_starts);
}


@Override public Collection<PlanTarget> getNextTargets(PlanTarget t)
{
   PlannerDestination pd = (PlannerDestination) t;
   Collection<PlanTarget> rslt = new TreeSet<>();
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
/*      PlannerPlan -- given plan                                               */
/*                                                                              */
/********************************************************************************/

private class PlannerPlan {
   
  private List<PlannerStep> plan_steps;
  
  PlannerPlan() {
     plan_steps = new ArrayList<>();
   }
  
  void addStep(PlannerStep step) {
     plan_steps.add(step);
   }
  
  List<IfaceBlock> getBlockSequence() {
     return null;
   }
  
}       // end of inner class PlannerPlan


private class PlannerStep implements IfacePlanner.PlanStep {

   private PlannerDestination step_target;
   private int step_count;
   
   PlannerStep(PlannerDestination pd,int ct) {
      step_target = pd;
      step_count = ct;
    }
   
   @Override public PlannerDestination getTarget()              { return step_target; } 
   @Override public int getCount()                              { return step_count; }
   
}       // end of inner class Planner Step




}       // end of class PlannerFactory




/* end of PlannerFactory.java */

