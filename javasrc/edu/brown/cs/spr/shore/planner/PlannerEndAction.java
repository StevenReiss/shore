/********************************************************************************/
/*                                                                              */
/*              PlannerEndAction.java                                           */
/*                                                                              */
/*      End action for train                                                    */
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

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceConnection;
import edu.brown.cs.spr.shore.iface.IfaceSignal;
import edu.brown.cs.spr.shore.iface.IfacePlanner.PlanActionType;

class PlannerEndAction extends PlannerActionBase
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceBlock      end_block;
private IfaceSignal     end_signal;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

PlannerEndAction(PlannerFactory planner,Element xml) 
{
   super(planner,xml); 
   
   String sid = IvyXml.getAttrString(xml,"SIGNAL");
   
   end_signal = null;
   for (IfaceSignal sig : layout_model.getSignals()) {
      if (sig.getId().equals(sid)) {
         end_signal = sig;
         break;
       }
    }
   if (end_signal == null) {
      layout_model.noteError("Signal " + sid + " not found for start " + getName());
      return;
    }
   end_block = end_signal.getFromBlock();
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public IfaceSignal getSignal() 
{
   return end_signal;
}

@Override public PlanActionType getActionType()
{ 
   return PlanActionType.END;
}


@Override List<IfaceBlock> getBlocks()
{
   return List.of(end_block);
}


/********************************************************************************/
/*                                                                              */
/*      Setup finding exits from this start                                     */
/*                                                                              */
/********************************************************************************/

@Override void findExits()                      { }


@Override boolean isRelevant(IfaceBlock from,IfaceConnection c)
{
   IfaceBlock blk = c.getOtherBlock(from);
   if (end_block == blk) return true;
   
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


}       // end of class PlannerEndAction


/* end of PlannerEndAction.java */

