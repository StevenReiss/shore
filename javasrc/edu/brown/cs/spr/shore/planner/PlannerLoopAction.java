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
import edu.brown.cs.spr.shore.iface.IfacePoint;
import edu.brown.cs.spr.shore.iface.IfaceSensor;
import edu.brown.cs.spr.shore.iface.IfacePlanner.PlanActionType;


class PlannerLoopAction extends PlannerActionBase
{ 


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<IfaceBlock> loop_blocks;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

PlannerLoopAction(PlannerFactory planner,Element xml,boolean fwd)
{
   super(planner,xml);
   
   String nm = getName();
   if (fwd) nm += " Forward";
   else nm += " Backward";
   setName(nm); 
   
   loop_blocks = new ArrayList<>();
   String blks = IvyXml.getAttrString(xml,"BLOCKS");
   for (StringTokenizer tok = new StringTokenizer(blks); tok.hasMoreTokens(); ) {
      String bid = tok.nextToken();
      IfaceBlock bfnd = findBlockById(bid);
      if (bfnd == null) {
         layout_model.noteError("Block " + bid + " not found for loop " + getName());
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
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public PlanActionType getActionType() 
{
   return PlanActionType.LOOP;
}


@Override List<IfaceBlock> getBlocks()          
{
   return loop_blocks;
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
   topoints.remove(cur);
   
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

 

/********************************************************************************/
/*                                                                              */
/*      Check if this loop is relevant to an incoming position                  */
/*                                                                              */
/********************************************************************************/

@Override boolean isRelevant(IfaceBlock from,IfaceConnection conn)
{
   IfaceBlock blk = conn.getOtherBlock(from);
   int idx = loop_blocks.indexOf(blk);
   if (idx < 0) return false;
   int nidx = (idx + 1) % loop_blocks.size();
   IfaceBlock nxtblk = loop_blocks.get(nidx);
   
   IfaceSensor s0 = conn.getEntrySensor(from);
   IfacePoint p0 = s0.getAtPoint();
   
   Set<IfacePoint> topoints = layout_model.findSuccessorPoints(p0,
         conn.getGapPoint(),false);
   for (IfaceConnection c1 : blk.getConnections()) {
      IfaceSensor s1 = c1.getExitSensor(blk);
      IfacePoint p1 = s1.getAtPoint();
      if (c1.getOtherBlock(blk) == nxtblk && topoints.contains(p1)) {
         return true;
       }
    }
   
   return false;
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String toString() 
{
   StringBuffer buf = new StringBuffer();
   buf.append(getName());
   buf.append(" (");
   for (int i = 0; i < loop_blocks.size(); ++i) {
      IfaceBlock b = loop_blocks.get(i);
      if (i != 0) buf.append("-");
      buf.append(b.getId());
    }
   buf.append(")");
   return buf.toString();
}

}       // end of class PlannerLoop




/* end of PlannerLoop.java */

