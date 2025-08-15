/********************************************************************************/
/*                                                                              */
/*              PlannerStart.java                                               */
/*                                                                              */
/*      Possible starting/ending point for a plan                               */
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

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceConnection;
import edu.brown.cs.spr.shore.iface.IfaceSignal;
import edu.brown.cs.spr.shore.iface.IfacePlanner.PlanActionType;

class PlannerStartAction extends PlannerActionBase 
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceBlock start_block;
private IfaceSignal start_signal;
private List<IfaceBlock> next_blocks;
 


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

PlannerStartAction(PlannerFactory planner,Element xml) 
{
   super(planner,xml); 
   
   String sid = IvyXml.getAttrString(xml,"SIGNAL");
   
   start_signal = null;
   for (IfaceSignal sig : layout_model.getSignals()) {
      if (sig.getId().equals(sid)) {
         start_signal = sig;
         break;
       }
    }
   if (start_signal == null) {
      layout_model.noteError("Signal " + sid + " not found for start " + getName());
      return;
    }
   start_block = start_signal.getFromBlock();
   next_blocks = new ArrayList<>();
   for (IfaceConnection conn : start_signal.getConnections()) {
      IfaceBlock nblk = conn.getOtherBlock(start_block);
      next_blocks.add(nblk);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public IfaceSignal getSignal()
{
   return start_signal;
}

@Override public PlanActionType getActionType()
{
   return PlanActionType.START;
}

@Override List<IfaceBlock> getBlocks()
{
   List<IfaceBlock> rslt = new ArrayList<>();
   rslt.add(start_block);
   rslt.addAll(next_blocks);
   return rslt;
}


/********************************************************************************/
/*                                                                              */
/*      Setup finding exits from this start                                     */
/*                                                                              */
/********************************************************************************/

@Override void findExits() 
{
   for (IfaceConnection conn : start_signal.getConnections()) {
      findPlannerExit(start_block,conn,null);
    }
}


@Override boolean isRelevant(IfaceBlock from,IfaceConnection c)
{
   return false;
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String toString()
{
   return getName();
}


}       // end of class PlannerStart




/* end of PlannerStart.java */

