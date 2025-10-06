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
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceConnection;
import edu.brown.cs.spr.shore.iface.IfacePoint;
import edu.brown.cs.spr.shore.iface.IfaceSensor;
import edu.brown.cs.spr.shore.iface.IfaceSignal;
import edu.brown.cs.spr.shore.shore.ShoreLog;
import javafx.application.Platform;

class ModelSignal implements IfaceSignal, ModelConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ModelBase for_model;
private String  signal_id;
private List<ModelPoint> at_points;
private ModelPoint gap_point;
private ModelPoint next_point;
private Set<ModelConnection> for_connections;
private ShoreSignalState signal_state;
private ShoreSignalType signal_type; 
private List<ModelSensor> stop_sensors;
private Set<ModelSensor> prior_sensors;
private byte tower_id;
private byte tower_index;
private boolean is_unused;
private Set<String> to_blocks;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ModelSignal(ModelBase model,Element xml)
{
   for_model = model;
   signal_id = IvyXml.getAttrString(xml,"ID");
   at_points = model.getPointListById(IvyXml.getAttrString(xml,"POINT")); 
   next_point = null;
   gap_point = model.getPointById(IvyXml.getAttrString(xml,"TO"));
   for_connections = new HashSet<>(); 
   tower_id = (byte) IvyXml.getAttrInt(xml,"TOWER");
   tower_index = (byte) IvyXml.getAttrInt(xml,"INDEX");
   is_unused = IvyXml.getAttrBool(xml,"UNUSED",false);
   signal_type = IvyXml.getAttrEnum(xml,"TYPE",ShoreSignalType.RG);
   boolean anode = IvyXml.getAttrBool(xml,"ANODE",false);
   
   to_blocks = null;
   String toblk = IvyXml.getAttrString(xml,"TOBLOCK");
   if (toblk != null) {
      to_blocks = new HashSet<>();
      for (StringTokenizer tok = new StringTokenizer(toblk,",; ");
            tok.hasMoreTokens(); ) {
          String bid = tok.nextToken();
          to_blocks.add(bid);
       }
      if (to_blocks.isEmpty()) to_blocks = null;
    }
   
   if (anode) {
      switch (signal_type) {
         case RG :
            signal_type = ShoreSignalType.RG_ANODE;
            break;
         case RGY :
            signal_type = ShoreSignalType.RGY_ANODE;
            break;
         case ENGINE :
            signal_type = ShoreSignalType.ENGINE_ANODE;
            break;
       }
    }
   stop_sensors = null;
   prior_sensors = new HashSet<>();
   signal_state = ShoreSignalState.OFF;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String getId()                 { return signal_id; } 

@Override public List<IfacePoint> getAtPoints() 
{
   return new ArrayList<>(at_points); 
}  

@Override public ModelPoint getNextPoint()      { return next_point; }

@Override public ModelBlock getFromBlock()      
{
   if (at_points.isEmpty()) {
      return null;
    }
   return at_points.get(0).getBlock();
}

@Override public boolean isBlockRelevant(IfaceBlock blk)   
{
   if (to_blocks == null) return true;
   if (to_blocks.contains(blk.getId())) return true;
   return false;
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

@Override public List<IfaceSensor> getStopSensors()    
{
   if (stop_sensors == null) return new ArrayList<>();
   
   return new ArrayList<>(stop_sensors);
} 

List<ModelSensor> getModelStopSensors()
{
   return stop_sensors;
}

@Override public Collection<IfaceSensor> getPriorSensors()
{
   return new ArrayList<>(prior_sensors);
}


@Override public void setSignalState(ShoreSignalState state)
{
   Platform.runLater(() -> actualSetSignal(state));
}


void actualSetSignal(ShoreSignalState state) 
{
   if (signal_state == state || is_unused) return;
   
   ShoreLog.logD("MODEL","Set signal " + signal_id + "=" + state);
   
   signal_state = state;
   for_model.fireSignalChanged(this);
}


ModelPoint getToGapPoint()
{
   return gap_point;
}


@Override public boolean isUnused()
{
   return is_unused;
}



/********************************************************************************/
/*                                                                              */
/*      Normalization code                                                      */
/*                                                                              */
/********************************************************************************/

void normalizeSignal(ModelBase mdl)
{
   if (is_unused) return;
   
   if (at_points.isEmpty()) {
      mdl.noteError("Missing POINT for signal " + getId());
      return;
    }
   if (gap_point == null) {
      mdl.noteError("Missing TO for signal " + getId());
      return;
    }
   
   for (ModelPoint pt0 : at_points) {
      for (ModelPoint pt : pt0.getModelConnectedTo()) {
         if (goesTo(pt0,pt,gap_point)) {
            next_point = pt;
          }
         else addPriorSensors(pt0,pt);
       }
    }
   
   stop_sensors = new ArrayList<>();
   for (ModelPoint pt0 : at_points) {
      ModelSensor ms = mdl.findSensorForPoint(pt0);
      if (ms == null) {
         mdl.noteError("No sensor found for signal " + signal_id);
       }
      else {
         stop_sensors.add(ms);
         ms.addSignal(this);
       }
    }
}


private boolean goesTo(ModelPoint prev,ModelPoint pt0,ModelPoint tgt)
{
   ModelPoint pt = pt0;
   if (pt == null) return false;
   if (pt == tgt) return true;
   while (pt != null) {
      if (pt == tgt) return true;
      Collection<ModelPoint> next = pt.getModelConnectedTo();
      if (next.size() != 2) break;
      ModelPoint cur = pt;
      pt = null;
      for (ModelPoint npt : next) {
         if (npt != prev) {
            pt = npt;
            prev = cur;
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


/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String toString()
{
   return "SIGNAL[" + signal_id + "=" + signal_state + "]";
}



}       // end of class ModelSignal




/* end of ModelSignal.java */

