/********************************************************************************/
/*                                                                              */
/*              ModelSensor.java                                                */
/*                                                                              */
/*      Representation of a sensor in the model                                 */
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
import edu.brown.cs.spr.shore.iface.IfaceSensor;
import edu.brown.cs.spr.shore.iface.IfaceSwitch;

class ModelSensor implements IfaceSensor, ModelConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String sensor_id;
private ModelPoint sensor_point;
private ModelBlock sensor_block;
private SensorState sensor_state;
private ModelSwitch n_switch;
private ModelSwitch r_switch;
private byte tower_id;
private byte tower_index;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ModelSensor(ModelBase mdl,Element xml)
{
   sensor_id = IvyXml.getAttrString(xml,"ID");
   sensor_point = mdl.getPointById(IvyXml.getAttrString(xml,"POINT"));
   tower_id = (byte) IvyXml.getAttrInt(xml,"TOWER");
   tower_index = (byte) IvyXml.getAttrInt(xml,"INDEX");
   n_switch = null;
   r_switch = null;
   sensor_block = null;
   sensor_state = SensorState.UNKNOWN;
}




/********************************************************************************/
/*                                                                              */
/*      Access Methods                                                          */
/*                                                                              */
/********************************************************************************/

String getId()                                  { return sensor_id; }

ModelPoint getAtPoint()                         { return sensor_point; }

@Override public IfaceSwitch getSwitchN()       { return n_switch; }

@Override public IfaceSwitch getSwitchR()       { return r_switch; }

@Override  public IfaceBlock getBlock()         { return sensor_block; }

@Override public SensorState getSensorState()   { return sensor_state; }

@Override public void setSensorState(SensorState st)
{
   sensor_state = st;
}

@Override public byte getTowerId()              { return tower_id; } 

@Override public byte getTowerSensor()          { return tower_index; }

}       // end of class ModelSensor




/* end of ModelSensor.java */

