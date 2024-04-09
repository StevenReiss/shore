/********************************************************************************/
/*                                                                              */
/*              ModelBlock.java                                                 */
/*                                                                              */
/*      Implementation of a track block                                         */
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



package edu.brown.cs.spr.shore.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceConnection;
import edu.brown.cs.spr.shore.shore.ShoreLog;

class ModelBlock implements ModelConstants, IfaceBlock
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ModelBase for_model;
private String block_id;
private ModelPoint at_point;
private BlockState block_state;
private ModelBlock pending_from;
private Set<ModelConnection> block_connects;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ModelBlock(ModelBase model,Element xml)
{
   for_model = model;
   block_id = IvyXml.getAttrString(xml,"ID");
   String ptname = IvyXml.getAttrString(xml,"POINT");
   at_point = model.getPointById(ptname);
   if (at_point == null) {
      model.noteError("Point " + ptname + " not found for block " + block_id);
    }
   block_state = BlockState.UNKNOWN;
   block_connects = new HashSet<>();
   pending_from = null;
} 


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getId()                                  { return block_id; }
ModelPoint getAtPoint()                         { return at_point; }

@Override public BlockState getBlockState()     { return block_state; }
@Override public ModelBlock getPendingFrom()    { return pending_from; }

@Override public void setBlockState(BlockState st)
{
   if (block_state == st) return;
   
   ShoreLog.logD("MODEL","Set Block State " + block_id + "=" + st);
   block_state = st;
   for_model.fireBlockChanged(this);
}

@Override public boolean setPendingFrom(IfaceBlock blk)  
{
   if (block_state != BlockState.EMPTY) return false;
   pending_from = (ModelBlock) blk;
   setBlockState(BlockState.PENDING);
   return true;
}


@Override public Collection<IfaceConnection> getConnections()
{
   return new ArrayList<>(block_connects); 
}

void addConnection(ModelConnection conn) 
{
   block_connects.add(conn);
}



/********************************************************************************/
/*                                                                              */
/*      Normalize the block                                                     */
/*                                                                              */
/********************************************************************************/

/*
 * first pass associated each point with a block
 **/

void normalizeBlock(ModelBase mdl)
{
   ModelPoint pt = getAtPoint();
   if (pt == null) {
      mdl.noteError("No Point specified for block");
      return;
    }
   propagateBlock(mdl,pt);
}



private void propagateBlock(ModelBase mdl,ModelPoint pt)
{
   if (pt.getBlock() != null) { 
      if (pt.getBlock() == this) return;
      mdl.noteError("Point " + pt.getId() + " is in block " +
            pt.getBlock().getId() + " and block " + getId());
      return;
    }
   
   pt.setBlock(this);
   
   for (ModelPoint npt : pt.getAllPoints()) {
      if (npt.getType() == PointType.GAP) continue;
      propagateBlock(mdl,npt);
    }
}




}       // end of class ModelBlock




/* end of ModelBlock.java */

