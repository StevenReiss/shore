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

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceConnection;
import edu.brown.cs.spr.shore.iface.IfaceModel;

class PlannerStart extends PlannerDestination
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String  start_name;
private IfaceBlock start_block;
 


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

PlannerStart(PlannerFactory planner,Element xml) 
{
   super(planner);
   
   start_name = IvyXml.getAttrString(xml,"NAME");
   String bid = IvyXml.getAttrString(xml,"BLOCK");
   start_block = findBlockById(bid); 
   if (start_block == null) {
      layout_model.noteError("Block " + bid + " not found for start " + start_name);
    }
}




/********************************************************************************/
/*                                                                              */
/*      Setup finding exits from this start                                     */
/*                                                                              */
/********************************************************************************/

@Override void findExits() 
{
   // find loops we can get to pretty directly
}


@Override boolean isRelevant(IfaceBlock from,IfaceConnection c)
{
   IfaceBlock blk = c.getOtherBlock(from);
   if (start_block == blk) return true;
   
   return false;
}


}       // end of class PlannerStart




/* end of PlannerStart.java */

