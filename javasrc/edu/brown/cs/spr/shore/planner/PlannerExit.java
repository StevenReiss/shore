/********************************************************************************/
/*                                                                              */
/*              PlannerExit.java                                                */
/*                                                                              */
/*      Implementation of an exit from a loop                                   */
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

import edu.brown.cs.spr.shore.iface.IfaceBlock;

class PlannerExit implements PlannerConstants, Comparable<PlannerExit>
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private PlannerDestination exit_target; 
private List<IfaceBlock> enter_blocks;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

PlannerExit(PlannerDestination pd,List<IfaceBlock> blks)
{
   exit_target = pd;
   enter_blocks = new ArrayList<>(blks);
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

PlannerDestination getDestination()             { return exit_target; }

IfaceBlock geStartBlock()
{
   return enter_blocks.get(0);
}

List<IfaceBlock> getThroughBlocks()
{
   return enter_blocks.subList(1,enter_blocks.size());
}


/********************************************************************************/
/*                                                                              */
/*      Sorting methods                                                         */
/*                                                                              */
/********************************************************************************/

@Override public int compareTo(PlannerExit e)
{
   return exit_target.getName().compareTo(e.exit_target.getName()); 
}

}       // end of class PlannerExit




/* end of PlannerExit.java */

