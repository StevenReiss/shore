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
import javafx.application.Platform;

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
private ShoreBlockState block_state;
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
   block_state = ShoreBlockState.UNKNOWN;
   block_connects = new HashSet<>();
   pending_from = null;
}  


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String getId()                 { return block_id; } 
@Override public ModelPoint getAtPoint()        { return at_point; }

@Override public ShoreBlockState getBlockState()     { return block_state; }
@Override public ModelBlock getPendingFrom()    
{
   if (block_state != ShoreBlockState.PENDING) return null;
    
   return pending_from;
}



@Override public void setBlockState(ShoreBlockState st)
{
   Platform.runLater(() -> actualSetBlockState(st));
}



void actualSetBlockState(ShoreBlockState st) 
{
   if (block_state == st) return;
   
   String stnm = st.toString();
   if (st == ShoreBlockState.PENDING) stnm += "[" + pending_from + "]";
   ShoreLog.logD("MODEL","Set Block State " + block_id + "=" + stnm);
   
   block_state = st;
   if (st != ShoreBlockState.PENDING) pending_from = null;
   for_model.fireBlockChanged(this);
}


@Override public void setPendingFrom(IfaceBlock blk)  
{
   Platform.runLater(() -> actualSetPendingFrom(blk));
}


void actualSetPendingFrom(IfaceBlock blk)
{
   if (blk == null) {
      actualSetBlockState(ShoreBlockState.EMPTY);
      return;
    }
   else if (block_state != ShoreBlockState.EMPTY &&
         block_state != ShoreBlockState.UNKNOWN) {
      // do nothing
    }
   else {  
      pending_from = (ModelBlock) blk;
      actualSetBlockState(ShoreBlockState.PENDING);
    }
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
      mdl.noteError("No Point specified for block " + getId());
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
   
   for (ModelPoint npt : pt.getModelConnectedTo()) {
      if (npt.getType() == ShorePointType.GAP) continue;
      propagateBlock(mdl,npt);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String toString()
{
   return "BLOCK[" + block_id + "=" + block_state + "]";
}



}       // end of class ModelBlock




/* end of ModelBlock.java */

