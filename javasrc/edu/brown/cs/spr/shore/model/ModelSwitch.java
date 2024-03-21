/********************************************************************************/
/*                                                                              */
/*              ModelSwitch.java                                                */
/*                                                                              */
/*      Representation of a switch                                              */
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

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceSwitch;

class ModelSwitch implements IfaceSwitch, ModelConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ModelBase for_model;
private String switch_id;
private ModelBlock entry_block;
private ModelBlock n_block;
private ModelBlock r_block;
private SwitchState switch_state;
private byte tower_id;
private byte tower_index;
private ModelPoint pivot_point;
private ModelPoint n_point;
private ModelPoint r_point;
private String associated_name;
private ModelSwitch associated_switch;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ModelSwitch(ModelBase model,Element xml)
{
   for_model = model;
   switch_id = IvyXml.getAttrString(xml,"ID");
   pivot_point = model.getPointById(IvyXml.getAttrString(xml,"POINT"));
   n_point = model.getPointById(IvyXml.getAttrString(xml,"N"));
   r_point = model.getPointById(IvyXml.getAttrString(xml,"R"));
   tower_id = (byte) IvyXml.getAttrInt(xml,"TOWER");
   tower_index = (byte) IvyXml.getAttrInt(xml,"INDEX");
   associated_name = IvyXml.getAttrString(xml,"ASSOCIATE");
   entry_block = null;
   n_block = null;
   r_block = null;
   switch_state = SwitchState.UNKNOWN;
   associated_switch = null;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getId()                                  { return switch_id; }

ModelPoint getPivotPoint()                      { return pivot_point; }

ModelPoint getNPoint()                          { return n_point; }

ModelPoint getRPoint()                          { return r_point; }

ModelSwitch getAssociatedSwitch()
{
   if (associated_name == null) return null;
   if (associated_switch != null) return associated_switch;
   associated_switch = for_model.getSwitchById(associated_name); 
   return associated_switch;
}

@Override public IfaceBlock getEntryBlock()     { return entry_block; }

@Override public IfaceBlock getNBlock()         { return n_block; }

@Override public IfaceBlock getRBlock()         { return r_block; }

@Override public SwitchState getSwitchState()   { return switch_state; }

@Override public void setSwitch(SwitchState st)
{
   switch_state = st;
}


@Override public byte getTowerId()              { return tower_id; }

@Override public byte getTowerSwitch()          { return tower_index; }




}       // end of class ModelSwitch




/* end of ModelSwitch.java */

