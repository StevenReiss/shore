/********************************************************************************/
/*                                                                              */
/*              PlannerLoop.java                                                */
/*                                                                              */
/*      Implementation of a planner loop                                        */
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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceConnection;
import edu.brown.cs.spr.shore.iface.IfaceModel;
import edu.brown.cs.spr.shore.iface.IfacePoint;
import edu.brown.cs.spr.shore.iface.IfaceSensor;


class PlannerLoop extends PlannerDestination
{ 


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private PlannerFactory  plan_model; 
private List<IfaceBlock> loop_blocks;
private String          loop_name;
private List<PlannerExit> possible_exits;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

PlannerLoop(PlannerFactory planner,Element xml,boolean fwd)
{
   super(planner);
   
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



/********************************************************************************/
/*                                                                              */
/*      Setup method to find exits from the loop                                */
/*                                                                              */
/********************************************************************************/

@Override void findExits()
{
   if (loop_blocks.isEmpty()) return;
   
   // first get starting point of first block in the loop
   IfaceBlock blk = loop_blocks.get(0);
   IfaceBlock eblk = loop_blocks.get(loop_blocks.size()-1);
   IfacePoint current = null;
   IfacePoint prior = null;
   for (IfaceConnection c : blk.getConnections()) {
      if (c.getOtherBlock(blk) == eblk) {
         current = c.getExitSensor(blk).getAtPoint();
         prior = c.getGapPoint();
         break;
       }
    }
   
   addExitsForBlock(0,0,prior,current);
}



private void addExitsForBlock(int blkidx,int startidx,IfacePoint prior,IfacePoint cur)
{
   // compute points we might get to in the block in this direction
   Set<IfacePoint> topoints = layout_model.findSuccessorPoints(cur,
         prior,false);
   
   IfaceBlock blk = cur.getBlock();
   for (IfaceConnection c : blk.getConnections()) {
      IfaceSensor s0 = c.getExitSensor(blk);
      IfacePoint p0 = s0.getAtPoint();
      if (topoints.contains(p0)) {
         IfaceBlock nxtblk = c.getOtherBlock(blk);
         if (loop_blocks.contains(nxtblk)) {
            int nidx = (blkidx + 1) % loop_blocks.size();
            if (nxtblk == loop_blocks.get(nidx)) {
               if (nidx == startidx) continue;            // all done here
               else addExitsForBlock(nidx,startidx,c.getGapPoint(),
                     c.getEntrySensor(blk).getAtPoint());
             }
          }
         else {
            findPlannerExit(blk,c,null);
          }
       }
    }
}



private void findPlannerExit(IfaceBlock from,IfaceConnection conn,List<IfaceBlock> thrublocks)
{
   IfaceBlock blk = conn.getOtherBlock(from);
   
   for (PlannerDestination pd : planner_model.getDestinations()) {  
      if (pd.isRelevant(from,conn)) {
         addExit(pd,thrublocks);
         return;
       }
    }
   
   if (blk != null) {
      if (thrublocks == null) {
         thrublocks = new ArrayList<>();
         thrublocks.add(from);
       }
      if (!thrublocks.contains(blk)) {
         thrublocks = new ArrayList<>(thrublocks);
         thrublocks.add(blk);
       }
    }
   
   // we might be passing thru this block
   IfacePoint pt = conn.getEntrySensor(from).getAtPoint();
   Set<IfacePoint> topoints = layout_model.findSuccessorPoints(pt,conn.getGapPoint(),false);
   for (IfaceConnection c : blk.getConnections()) {
      IfaceSensor s0 = c.getExitSensor(blk);
      IfacePoint p0 = s0.getAtPoint();
      if (topoints.contains(p0)) {
         findPlannerExit(blk,c,thrublocks);
       }
    }
}


/********************************************************************************/
/*                                                                              */
/*      Check if this loop is relevant to an incoming position                  */
/*                                                                              */
/********************************************************************************/

@Override boolean isRelevant(IfaceBlock from,IfaceConnection conn)
{
   // find set of point from gap into block
   // for all connections of new block, if connection exit is in set
   //    if target block of connection is successor of this block in block list
   //   return true
   return false;
}

}       // end of class PlannerLoop




/* end of PlannerLoop.java */

