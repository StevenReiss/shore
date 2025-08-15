/********************************************************************************/
/*                                                                              */
/*              PlannerDestination.java                                         */
/*                                                                              */
/*      Planner loop or entry/exit (destination for planning)                   */
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
import java.util.Set;
import java.util.TreeSet;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceConnection;
import edu.brown.cs.spr.shore.iface.IfaceModel;
import edu.brown.cs.spr.shore.iface.IfacePlanner;
import edu.brown.cs.spr.shore.iface.IfacePoint;
import edu.brown.cs.spr.shore.iface.IfaceSensor;
import edu.brown.cs.spr.shore.iface.IfaceSignal;
import edu.brown.cs.spr.shore.iface.IfaceSwitch;
import edu.brown.cs.spr.shore.iface.IfacePlanner.PlanActionType;

abstract class PlannerActionBase implements PlannerConstants, IfacePlanner.PlanAction,
      Comparable<PlannerActionBase>
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

protected PlannerFactory planner_model;
protected IfaceModel    layout_model;
private Set<PlannerExit> planner_exits;
private String destination_name;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected PlannerActionBase(PlannerFactory mdl,Element xml) 
{
   planner_model = mdl;
   layout_model = mdl.getLayoutModel();
   planner_exits = new TreeSet<>();
   destination_name = IvyXml.getAttrString(xml,"NAME");
} 



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String getName()
{
   return destination_name;
}

protected void setName(String nm) 
{
   destination_name = nm;
}

Collection<PlannerExit> getExits()
{
   return planner_exits;
}

 
@Override public abstract PlanActionType getActionType();

@Override public IfaceSignal getSignal()                { return null; }

abstract List<IfaceBlock> getBlocks();



/********************************************************************************/
/*                                                                              */
/*      Abstract methods                                                        */
/*                                                                              */
/********************************************************************************/

/**
 *      Compute the set of exits from this destination
 **/

abstract void findExits();


/**
 *     Check if this destination is relevant for this entry
 **/

abstract boolean isRelevant(IfaceBlock from,IfaceConnection c);



/********************************************************************************/
/*                                                                              */
/*      Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

protected IfaceBlock findBlockById(String bid)
{
   for (IfaceBlock bb : layout_model.getBlocks()) {
      if (bb.getId().equals(bid)) {
         return bb;
       }
    } 
   return null;
}


protected IfacePoint getSingleSuccessor(IfaceModel prior,IfacePoint cur) 
{
   for (IfacePoint nxt : cur.getConnectedTo()) {
      if (nxt == prior) continue;
      return nxt;
    }
   
   return null;
}


protected IfaceSwitch findSwitchForPoint(IfacePoint pt)
{
   if (pt == null) return null;
   
   for (IfaceSwitch ms : layout_model.getSwitches()) { 
      if (ms.getPivotPoint() == pt) {
         return ms;
       }
    }
   
   return null;
}


/********************************************************************************/
/*                                                                              */
/*      Exit finding methods                                                    */
/*                                                                              */
/********************************************************************************/

protected void findPlannerExit(IfaceBlock from,IfaceConnection conn,List<IfaceBlock> thrublocks)
{
   IfaceBlock blk = conn.getOtherBlock(from);
   
   if (thrublocks == null) {
      thrublocks = new ArrayList<>();
      thrublocks.add(from);
    }
   
   for (PlannerActionBase pd : planner_model.getAllActions()) {    
      if (pd.isRelevant(from,conn)) {
         addExit(pd,thrublocks);
         return;
       }
    }
   
   if (blk != null) {
      if (!thrublocks.contains(blk)) {
         thrublocks = new ArrayList<>(thrublocks);
         thrublocks.add(blk);
       }
    }
   
   // we might be passing thru this block
   IfaceSensor s = conn.getEntrySensor(from);
   if (s == null) return;
   IfacePoint pt = s.getAtPoint();
   Set<IfacePoint> topoints = layout_model.findSuccessorPoints(pt,conn.getGapPoint(),false);
   topoints.remove(pt);
   for (IfaceConnection c : blk.getConnections()) {
      IfaceSensor s0 = c.getExitSensor(blk);
      IfacePoint p0 = s0.getAtPoint();
      if (topoints.contains(p0)) {
         findPlannerExit(blk,c,thrublocks);
       }
    }
}



protected void addExit(PlannerActionBase pd,List<IfaceBlock> thru)
{
   PlannerExit pe = new PlannerExit(pd,thru);
   planner_exits.add(pe);
}



/********************************************************************************/
/*                                                                              */
/*      Comparison methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override public int compareTo(PlannerActionBase pd)
{
   return getName().compareTo(pd.getName());
}


}       // end of class PlannerDestination




/* end of PlannerDestination.java */

