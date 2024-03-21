/********************************************************************************/
/*                                                                              */
/*              ModelSignal.java                                                */
/*                                                                              */
/*      Representation of a track signal in the model                           */
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
import edu.brown.cs.spr.shore.iface.IfaceSignal;

class ModelSignal implements IfaceSignal, ModelConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String  signal_id;
private ModelPoint at_point;
private ModelPoint to_point;
private ModelBlock entry_block;
private ModelBlock exit_block;
private SignalState signal_state;
private SignalType signal_type; 
private byte tower_id;
private byte tower_index;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ModelSignal(ModelBase model,Element xml)
{
   signal_id = IvyXml.getAttrString(xml,"ID");
   at_point = model.getPointById(IvyXml.getAttrString(xml,"POINT"));
   to_point = model.getPointById(IvyXml.getAttrString(xml,"TO"));
   tower_id = (byte) IvyXml.getAttrInt(xml,"TOWER");
   tower_index = (byte) IvyXml.getAttrInt(xml,"INDEX");
   signal_type = IvyXml.getAttrEnum(xml,"TYPE",SignalType.RG);
   entry_block = null;
   exit_block = null;
   signal_state = SignalState.OFF;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getId()                                  { return signal_id; } 

ModelPoint getAtPoint()                         { return at_point; }

ModelPoint getToPoint()                         { return to_point; }

@Override public ModelBlock getEntryBlock()     { return entry_block; }

@Override public ModelBlock getExitBlock()      { return exit_block; }

@Override public SignalType getSignalType()     { return signal_type; } 

@Override public byte getTowerId()              { return tower_id; } 

@Override public byte getTowerSignal()          { return tower_index; }

@Override public SignalState getSignalState()   { return signal_state; }

@Override public void setSignalState(SignalState state)
{
   signal_state = state;
}




}       // end of class ModelSignal




/* end of ModelSignal.java */

