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

import java.util.List;

import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceConnection;
import edu.brown.cs.spr.shore.iface.IfaceModel;
import edu.brown.cs.spr.shore.iface.IfacePoint;
import edu.brown.cs.spr.shore.iface.IfaceSwitch;

abstract class PlannerDestination implements PlannerConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

protected PlannerFactory planner_model;
protected IfaceModel    layout_model;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected PlannerDestination(PlannerFactory mdl) 
{
   planner_model = mdl;
   layout_model = mdl.getLayoutModel();
} 


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


protected void addExit(PlannerDestination pd,List<IfaceBlock> thru)
{
   
}


}       // end of class PlannerDestination




/* end of PlannerDestination.java */

