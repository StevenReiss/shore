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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceConnection;
import edu.brown.cs.spr.shore.iface.IfaceSensor;
import edu.brown.cs.spr.shore.iface.IfaceSignal;
import edu.brown.cs.spr.shore.iface.IfaceSwitch;
import edu.brown.cs.spr.shore.shore.ShoreLog;

class ModelSensor implements IfaceSensor, ModelConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ModelBase for_model;
private String sensor_id;
private ModelPoint sensor_point;
private ModelBlock sensor_block;
private ShoreSensorState sensor_state;
private ModelSwitch n_switch;
private ModelSwitch r_switch;
private ModelSwitch entry_switch;
private ModelConnection in_connection;
private Set<ModelSignal> for_signals;
private byte tower_id;
private byte tower_index;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ModelSensor(ModelBase mdl,Element xml)
{
   for_model = mdl;
   sensor_id = IvyXml.getAttrString(xml,"ID");
   String pt = IvyXml.getAttrString(xml,"POINT");
   sensor_point = mdl.getPointById(pt);
   tower_id = (byte) IvyXml.getAttrInt(xml,"TOWER");
   tower_index = (byte) IvyXml.getAttrInt(xml,"INDEX");
   n_switch = null;
   r_switch = null;
   entry_switch = null;
   if (sensor_point != null) {
      sensor_block = sensor_point.getBlock();
    }
   else {
      mdl.noteError("Sensor point " + pt + " not found for " + sensor_id);
      sensor_block = null;
    }
   sensor_state = ShoreSensorState.UNKNOWN;
   for_signals = new HashSet<>();
   in_connection = null;
}




/********************************************************************************/
/*                                                                              */
/*      Access Methods                                                          */
/*                                                                              */
/********************************************************************************/

String getId()                                  { return sensor_id; }

@Override public ModelPoint getAtPoint()                { return sensor_point; }

@Override public IfaceSwitch getSwitchN()       { return n_switch; }

@Override public IfaceSwitch getSwitchR()       { return r_switch; }

IfaceSwitch getSwitchEntry()                    { return entry_switch; }

@Override public Collection<IfaceSignal> getSignals()        
{ 
   return new ArrayList<>(for_signals);
}

@Override public IfaceConnection getConnection()                 
{
   return in_connection; 
} 

void setConnection(ModelConnection conn)
{
   in_connection = conn;
}



void assignSwitch(ModelSwitch sw,ShoreSwitchState state)
{
   switch (state) {
      case N :
         n_switch = sw;
         break;
      case R :
         r_switch = sw;
         break;
      case UNKNOWN :
         entry_switch = sw;
         break;
    }
}


@Override  public IfaceBlock getBlock()         { return sensor_block; }

@Override public ShoreSensorState getSensorState()   { return sensor_state; }

@Override public void setSensorState(ShoreSensorState st)
{
   if (st == sensor_state) return;
   
   ShoreLog.logD("MODEL","Set sensor state " + sensor_id + "=" + st);
   
   sensor_state = st;
   for_model.fireSensorChanged(this);
}

void addSignal(ModelSignal sig)
{
   for_signals.add(sig);
}

@Override public byte getTowerId()              { return tower_id; } 

@Override public byte getTowerSensor()          { return tower_index; }



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String toString()
{
   return "SENSOR[" + sensor_id + "]";
}

}       // end of class ModelSensor




/* end of ModelSensor.java */

