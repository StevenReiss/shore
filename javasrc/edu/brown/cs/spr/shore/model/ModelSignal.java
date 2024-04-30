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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.spr.shore.iface.IfaceConnection;
import edu.brown.cs.spr.shore.iface.IfaceSensor;
import edu.brown.cs.spr.shore.iface.IfaceSignal;
import edu.brown.cs.spr.shore.shore.ShoreLog;

class ModelSignal implements IfaceSignal, ModelConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ModelBase for_model;
private String  signal_id;
private ModelPoint at_point;
private ModelPoint gap_point;
private ModelPoint next_point;
private Set<ModelConnection> for_connections;
private ShoreSignalState signal_state;
private ShoreSignalType signal_type; 
private ModelSensor stop_sensor;
private Set<ModelSensor> prior_sensors;
private byte tower_id;
private byte tower_index;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ModelSignal(ModelBase model,Element xml)
{
   for_model = model;
   signal_id = IvyXml.getAttrString(xml,"ID");
   at_point = model.getPointById(IvyXml.getAttrString(xml,"POINT"));
   next_point = null;
   gap_point = model.getPointById(IvyXml.getAttrString(xml,"TO"));
   for_connections = new HashSet<>(); 
   tower_id = (byte) IvyXml.getAttrInt(xml,"TOWER");
   tower_index = (byte) IvyXml.getAttrInt(xml,"INDEX");
   signal_type = IvyXml.getAttrEnum(xml,"TYPE",ShoreSignalType.RG);
   stop_sensor = null;
   prior_sensors = new HashSet<>();
   signal_state = ShoreSignalState.OFF;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getId()                                  { return signal_id; } 

@Override public ModelPoint getAtPoint()        { return at_point; } 
@Override public ModelPoint getNextPoint()      { return next_point; }

@Override public ModelBlock getFromBlock()      
{
   return at_point.getBlock();
}

@Override public Collection<IfaceConnection> getConnections() 
{
   return new ArrayList<>(for_connections); 
}

void addConnection(ModelConnection conn) 
{
   for_connections.add(conn);
}


@Override public ShoreSignalType getSignalType()     { return signal_type; } 

@Override public byte getTowerId()              { return tower_id; } 

@Override public byte getTowerSignal()          { return tower_index; }

@Override public ShoreSignalState getSignalState()   { return signal_state; }

@Override public ModelSensor getStopSensor()    { return stop_sensor; } 

@Override public Collection<IfaceSensor> getPriorSensors()
{
   return new ArrayList<>(prior_sensors);
}


@Override public void setSignalState(ShoreSignalState state)
{
   if (signal_state == state) return;
   
   ShoreLog.logD("MODEL","Set signal " + signal_id + "=" + state);
   
   signal_state = state;
   for_model.fireSignalChanged(this);
}



/********************************************************************************/
/*                                                                              */
/*      Normalization code                                                      */
/*                                                                              */
/********************************************************************************/

void normalizeSignal(ModelBase mdl)
{
   if (at_point == null) {
      mdl.noteError("Missing POINT for signal " + getId());
      return;
    }
   if (gap_point == null) {
      mdl.noteError("Missing TO for signal " + getId());
      return;
    }
   
   for (ModelPoint pt : at_point.getModelConnectedTo()) {
      if (goesTo(at_point,pt,gap_point)) {
         next_point = pt;
       }
      else addPriorSensors(at_point,pt);
    }
   
   stop_sensor = mdl.findSensorForPoint(at_point);
   if (stop_sensor == null) {
      mdl.noteError("No sensor found for signal " + signal_id);
    }
   stop_sensor.addSignal(this); 
}


private boolean goesTo(ModelPoint prev,ModelPoint pt,ModelPoint tgt)
{
   if (pt == null) return false;
   if (pt == tgt) return true;
   while (pt != null) {
      if (pt == tgt) return true;
      Collection<ModelPoint> next = pt.getModelConnectedTo();
      if (next.size() != 2) break;
      pt = null;
      for (ModelPoint npt : next) {
         if (npt != prev) {
            pt = npt;
            break;
          }
       }
    }
   return false;
}


private void addPriorSensors(ModelPoint prev,ModelPoint pt)
{
   ModelSensor s = for_model.findSensorForPoint(pt);
   if (s != null) prior_sensors.add(s);
   else {
      for (ModelPoint npt : pt.getModelConnectedTo()) {
         if (npt == prev) continue;
         if (npt.getBlock() != prev.getBlock()) continue;
         addPriorSensors(pt,npt);
       }
    }
}


}       // end of class ModelSignal




/* end of ModelSignal.java */

