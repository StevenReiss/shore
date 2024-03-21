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
import java.util.List;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceSensor;

class ModelBlock implements ModelConstants, IfaceBlock
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String block_id;
private ModelPoint at_point;
private BlockState block_state;
private List<ModelSensor> entry_sensors;
private List<ModelSensor> exit_sensors;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ModelBlock(ModelBase model,Element xml)
{
   block_id = IvyXml.getAttrString(xml,"ID");
   String ptname = IvyXml.getAttrString(xml,"POINT");
   at_point = model.getPointById(ptname);
   block_state = BlockState.UNKNOWN;
   entry_sensors = new ArrayList<>();
   exit_sensors = new ArrayList<>();
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getId()                                  { return block_id; }
ModelPoint getAtPoint()                         { return at_point; }

@Override public BlockState getBlockState()     { return block_state; }

@Override public void setBlockState(BlockState st)
{
   block_state = st;
}



@Override public List<IfaceSensor> getEntrySensors()
{
   return new ArrayList<IfaceSensor>(entry_sensors);
}

@Override public List<IfaceSensor> getExitSensors()
{
   return new ArrayList<IfaceSensor>(exit_sensors);
}



}       // end of class ModelBlock




/* end of ModelBlock.java */

