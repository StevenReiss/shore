/********************************************************************************/
/*                                                                              */
/*              ModelBase.java                                                  */
/*                                                                              */
/*      Implementation of a model                                               */
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

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.swing.SwingEventListenerList;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceConnection;
import edu.brown.cs.spr.shore.iface.IfaceDiagram;
import edu.brown.cs.spr.shore.iface.IfaceModel;
import edu.brown.cs.spr.shore.iface.IfacePoint;
import edu.brown.cs.spr.shore.iface.IfaceSensor;
import edu.brown.cs.spr.shore.iface.IfaceSignal;
import edu.brown.cs.spr.shore.iface.IfaceSwitch;
import edu.brown.cs.spr.shore.shore.ShoreException;
import edu.brown.cs.spr.shore.shore.ShoreLog;

public class ModelBase implements ModelConstants, IfaceModel
{



/********************************************************************************/
/*                                                                              */
/*      Main program for testing/standalone mode                                */
/*                                                                              */
/********************************************************************************/

public static void main(String [] args)
{
   ShoreLog.setup();
   
   File f = new File("/pro/shore/resources/spr_layout.xml");
   if (args.length > 0) {
      f = new File(args[0]);
    }
   
   ModelBase mb = new ModelBase(f);
   
   ShoreLog.logD("Built model " + mb);
   
   File f1 = new File(System.getProperty("user.home"));
   File f2 = new File(f1,"shore.report");
   mb.createReport(f2);
}


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private File model_file;
private Map<String,ModelPoint> model_points;
private Map<String,ModelSwitch> model_switches;
private Map<String,ModelBlock> model_blocks;
private Map<String,ModelSensor> model_sensors;
private Map<String,ModelSignal> model_signals;
private Map<String,ModelDiagram> model_diagrams;
private List<ModelConnection> block_connections;
private List<String> model_errors;
private SwingEventListenerList<ModelCallback> model_listeners;
private Element model_xml;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public ModelBase(File file)
{
   model_file = file;
   model_points = new HashMap<>();
   model_switches = new TreeMap<>(); 
   model_blocks = new HashMap<>();
   model_sensors = new TreeMap<>();
   model_signals = new TreeMap<>();
   model_diagrams = new HashMap<>();
   block_connections = new ArrayList<>();
   model_listeners = new SwingEventListenerList<>(ModelCallback.class);
   
   model_errors = new ArrayList<>();
   model_xml = null;
   
   try {
      loadModel(file);
    }
   catch (ShoreException e) {
      System.err.println("SHORE: Problem loading model: " + e.toString());
      System.exit(1);
    }
   
   if (hasErrors()) {
      System.err.println("SHORE: Model is inconsistent:");
      for (String s : model_errors) {
         System.err.println("   " + s);
       }
      System.exit(1);
    }
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public Collection<IfaceSensor> getSensors()
{
   return new ArrayList<>(model_sensors.values());
}

Collection<ModelSensor> getModelSensors()
{
   return model_sensors.values();
}

@Override public Collection<IfaceSignal> getSignals()
{
   return new ArrayList<>(model_signals.values());
}

Collection<ModelSignal> getModelSignals()
{
   return model_signals.values();
}


@Override public Collection<IfaceConnection> getConnections() 
{
   return new ArrayList<IfaceConnection>(block_connections);
}


@Override public Collection<IfaceSwitch> getSwitches()
{
   return new ArrayList<>(model_switches.values());
}


Collection<ModelSwitch> getModelSwitches()
{
   return model_switches.values(); 
}


@Override public Collection<IfaceBlock> getBlocks() 
{
   return new ArrayList<>(model_blocks.values());
}


@Override public Collection<IfaceDiagram> getDiagrams()
{
   return new ArrayList<>(model_diagrams.values());  
}



ModelSensor findSensorForPoint(ModelPoint pt)
{
   if (pt == null) return null;
   
   for (ModelSensor ms : getModelSensors()) { 
      if (ms.getAtPoint() == pt) {
         return ms;
       }
    }
   
   return null;
}


ModelSignal findSignalForPoint(ModelPoint pt)
{
   if (pt == null) return null;
   
   for (ModelSignal ms : getModelSignals()) { 
      if (ms.getAtPoints().contains(pt)) {
         return ms;
       }
    }
   
   return null;
}


ModelSwitch findSwitchForPoint(IfacePoint pt)
{
   if (pt == null) return null;
   
   for (ModelSwitch ms : getModelSwitches()) { 
      if (ms.getPivotPoint() == pt) {
         return ms;
       }
    }
   
   return null;
}


ModelDiagram findDiagram(String id)
{
   return model_diagrams.get(id);
}


ModelPoint getPointById(String id)
{
   if (id == null) return null;
   
   return model_points.get(id);
}

List<ModelPoint> getPointListById(String ids)
{
   List<ModelPoint> rslt = new ArrayList<>();
   if (ids != null) {
      StringTokenizer tok = new StringTokenizer(ids," \t,;");
      while (tok.hasMoreTokens()) {
         String pid = tok.nextToken();
         ModelPoint pt = getPointById(pid);
         if (pt != null) rslt.add(pt);
       }
    }
   return rslt;
}

ModelSwitch getSwitchById(String id)
{
   return model_switches.get(id);
}

@Override public Element getModelXml()          { return model_xml; }


void noteError(String msg)
{
   model_errors.add(msg);
}


boolean hasErrors()
{
   return !model_errors.isEmpty();
}


/********************************************************************************/
/*                                                                              */
/*       Path methods                                                           */
/*                                                                              */
/********************************************************************************/

@Override public boolean goesTo(IfacePoint prev,IfacePoint pt,IfacePoint tgt)
{
   Set<IfacePoint> done = new HashSet<>();
   return goesTo(prev,pt,tgt,done);
   
}



private boolean goesTo(IfacePoint prev,IfacePoint pt,IfacePoint tgt,Set<IfacePoint> done)
{
   if (pt == tgt) return true;
   if (pt == prev) return false;
   if (!done.add(pt)) return false;
   
   Collection<IfacePoint> next = pt.getConnectedTo();
   if (pt.getType() == ShorePointType.SWITCH) {
      ModelSwitch sw = findSwitchForPoint(pt);
      IfacePoint xpt = findSwitchPoint(prev,sw);
      if (xpt == sw.getNPoint()) done.add(sw.getRPoint());
    }
   else if (pt.getType() == ShorePointType.GAP) { 
      return false;
    }
   for (IfacePoint npt : next) {
      if (goesTo(prev,npt,tgt,done)) return true;
    }
   return false;
}



@Override public IfaceBlock findNextBlock(IfacePoint prev,IfacePoint pt)
{
   Set<IfacePoint> done = new HashSet<>();
   IfaceBlock blk = pt.getBlock();
   return findNextBlock(prev,pt,blk,done);
}


private IfaceBlock findNextBlock(IfacePoint prev0,IfacePoint pt0,IfaceBlock blk,Set<IfacePoint> done)
{
   IfacePoint pt = pt0;
   IfacePoint prev = prev0;
   for (IfacePoint pt1 : pt0.getConnectedTo()) {
      if (pt1 == prev) break;
      if (goesTo(pt0,pt1,prev)) {
         prev = pt1;
         break;
       }
    }
   ShoreLog.logD("MODEL","Find next block " + prev + " " + pt + " " + blk);
   
   while (pt != null) {
      if (pt.getBlock() != blk) {
         if (pt.getBlock() == null) {
            for (IfacePoint npt : pt.getConnectedTo()) {
               if (npt == prev) continue;
               if (pt.getBlock() == blk) return null;
               ShoreLog.logD("MODEL","Next block is " + npt.getBlock());
               return npt.getBlock();
             }
          }
         else {
            ShoreLog.logD("MODEL","Next block is " + pt.getBlock());
            return pt.getBlock();
          }
       }
      
      IfacePoint nextpt = null;
      for (IfacePoint npt : pt.getConnectedTo()) {
         if (npt == prev) continue;
         if (nextpt == null) nextpt = npt;
         else {
            ModelSwitch sw = findSwitchForPoint(pt);
            IfacePoint xpt = findSwitchPoint(prev,sw);
            IfacePoint tpt = findSwitchPoint(npt,sw);
            if (xpt == sw.getNPoint() || xpt == sw.getRPoint()) {
               if (tpt == sw.getEntryPoint()) nextpt = npt;
             }
            else {
               switch (sw.getSwitchState()) {
                  case R :
                     if (tpt == sw.getRPoint()) nextpt = npt;
                     break;
                  case N :
                     if (tpt == sw.getNPoint()) nextpt = npt;
                     break;
                }
             }
          }
       }
      prev = pt;
      pt = nextpt;
    }
   
   return null;
}


private IfacePoint findSwitchPoint(IfacePoint pt,ModelSwitch sw)
{
   IfacePoint prev = sw.getPivotPoint();
   while (pt != null) {
      if (sw.getNPoint() == pt) return pt;
      if (sw.getRPoint() == pt) return pt;
      if (sw.getEntryPoint() == pt) return pt;
      for (IfacePoint npt : pt.getConnectedTo()) {
         if (npt == prev) continue;
         if (npt.getType() == ShorePointType.SWITCH) continue;
         prev = pt;
         pt = npt;
         break;
       }
    }
   
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Callback methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public void addModelCallback(ModelCallback cb)
{
   model_listeners.add(cb);
}

@Override public void removeModelCallback(ModelCallback cb)
{
   model_listeners.remove(cb);
}


void fireSensorChanged(ModelSensor sensor)
{
   for (ModelCallback cb : model_listeners) {
      cb.sensorChanged(sensor);
    }
}


void fireSwitchChanged(ModelSwitch sw)
{
   for (ModelCallback cb : model_listeners) {
      cb.switchChanged(sw);
    }
}


void fireSignalChanged(ModelSignal signal)
{
   for (ModelCallback cb : model_listeners) {
      cb.signalChanged(signal);
    }
}


void fireBlockChanged(ModelBlock block)
{
   for (ModelCallback cb : model_listeners) {
      cb.blockChanged(block);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Load the model from description                                         */
/*                                                                              */
/********************************************************************************/

private void loadModel(File file) throws ShoreException
{
   Map<ModelDiagram,Element> xmlmap = new HashMap<>();
   
   Element xml = IvyXml.loadXmlFromFile(file);
   if (xml == null) throw new ShoreException("File " + file + " doesn't contain a model");
   model_xml = xml;
   
   if (IvyXml.getChild(xml,"DIAGRAM") != null) {
      for (Element dxml : IvyXml.children(xml,"DIAGRAM")) {
         ModelDiagram md = new ModelDiagram(dxml);
         model_diagrams.put(md.getId(),md);
         xmlmap.put(md,dxml);
       }
    }  
   else {
      ModelDiagram md = new ModelDiagram(xml);
      model_diagrams.put(md.getId(),md); 
      xmlmap.put(md,xml);
    }
   
   if (hasErrors()) return;
   
   for (ModelDiagram md : xmlmap.keySet()) {
      for (ModelPoint mp : md.getModelPoints()) {  
         if (model_points.put(mp.getId(),mp) != null) {
            noteError("Point " + mp.getId() + " defined twice");
          }
       }
    }
   
   for (Map.Entry<ModelDiagram,Element> ent : xmlmap.entrySet()) {
      ModelDiagram md = ent.getKey(); 
      Element dxml = ent.getValue();
      md.loadDiagram(this,dxml); 
      loadDiagramData(md);
    }
   
   if (hasErrors()) return;
   
   normalizeModel();
   
   if (hasErrors()) return;
   
   checkModel();
}
 


private void loadDiagramData(ModelDiagram md)
{
   for (ModelSwitch ms : md.getModelSwitches()) {
      if (model_switches.put(ms.getId(),ms) != null) {
         noteError("Switch " + ms.getId() + " defined twice");
       } 
    }
   for (ModelBlock mb : md.getModelBlocks()) { 
      if (model_blocks.put(mb.getId(),mb) != null) {
         noteError("Block " + mb.getId() + " defined twice");
       }
    }
   for (ModelSensor ms : md.getModelSensors()) {
      if (model_sensors.put(ms.getId(),ms) != null) {
         noteError("Sensor " + ms.getId() + " defined twice");
       }
    }
   for (ModelSignal ms : md.getModelSignals()) {
      if (model_signals.put(ms.getId(),ms) != null) {
         noteError("Signal " + ms.getId() + " defined twice");
       }
    }
}





/********************************************************************************/
/*                                                                              */
/*      Model normalization methods                                             */
/*                                                                              */
/********************************************************************************/

private void normalizeModel() throws ShoreException
{
   for (ModelBlock blk : model_blocks.values()) {
      blk.normalizeBlock(this); 
    }
   
   for (ModelPoint pt : model_points.values()) { 
      if (pt.getType() == ShorePointType.GAP) {
         setupConnection(pt);
       }
    }
   
   for (ModelSwitch sw : model_switches.values()) {
      sw.normalizeSwitch(this);  
    }
   
   for (ModelSignal sig : model_signals.values()) {
      sig.normalizeSignal(this); 
    }
   
   for (ModelConnection conn : block_connections) { 
      conn.normalizeConnection(this);
    }
   
   for (ModelSensor ms : model_sensors.values()) {
      ms.normalizeSensor(this); 
    }
   normalizePoints();
  
   // now we need to normalize the model 
   //    reset points so that they are linear
}



private void normalizePoints()
{
   DoneMap done = new DoneMap();
   List<List<ModelPoint>> seqs = new ArrayList<>();
   
   for (ModelPoint pt : model_points.values()) {
      if (pt.getType() == ShorePointType.TURNING ||
            pt.getType() == ShorePointType.TURNTABLE) {
         for (ModelPoint npt: pt.getModelConnectedTo()) {
            if (npt.getDiagram() != pt.getDiagram()) continue;
            if (done.isDone(npt,pt)) continue;
            List<ModelPoint> seq = createSequence(pt,npt,done);
            if (seq != null && seq.size() > 2) seqs.add(seq);
          }
       }
    }
   
   for (ModelPoint pt : model_points.values()) {
      if (pt.getType() == ShorePointType.SWITCH) {
         ModelSwitch sw = findSwitchForPoint(pt);
         if (sw == null) {
            noteError("Switch not found for point " + pt.getId());
            continue;
          }
         ModelPoint npt = sw.getRPoint();
         if (done.isDone(npt,pt)) continue;
         if (npt.getDiagram() != pt.getDiagram()) continue;    
         List<ModelPoint> seq = createSequence(pt,npt,done);
         if (seq != null && seq.size() > 2) seqs.add(seq);
       }
    }
   
   for (List<ModelPoint> seq : seqs) {
//    System.err.print("SEQUENCE ");
//    for (ModelPoint pt : seq) {
//       System.err.print(pt.getId() + " ");
//     }
//    System.err.println();
      alignSequence(seq);
    }
}


private List<ModelPoint> createSequence(ModelPoint pt0,ModelPoint pt1,DoneMap done)
{
   List<ModelPoint> rslt = new ArrayList<>();
   rslt.add(pt0);
   rslt.add(pt1);
   ModelPoint prev = pt0;
   ModelPoint pt = pt1;
   done.noteSeq(pt0,pt1);
   
   for ( ; ; ) {
      ModelPoint npt = null;
      switch (pt.getType()) {
         case SWITCH :
            ModelSwitch sw = findSwitchForPoint(pt);
            if (sw == null) {
               noteError("Switch not found for point " + pt.getId());
             }
            else if (prev == sw.getEntryPoint()) npt = sw.getNPoint();
            else if (prev == sw.getNPoint()) npt = sw.getEntryPoint();
            break;
         case END :
         case TURNING :
         case TURNTABLE :
            break;
         default :
            for (ModelPoint conn : pt.getModelConnectedTo()) {
               if (conn == prev) continue;
               else if (npt == null) npt = conn;
               else {
                  noteError("Too many connections for point " + pt.getId());
                }
             }
            break;
       }
      if (npt == null) break;
      if (npt.getDiagram() != pt0.getDiagram()) break;
      rslt.add(npt);
      done.noteSeq(pt,npt);
      prev = pt;
      pt = npt;
    }
   
   if (rslt.size() == 2) return null;
   
   return rslt;
}



private void alignSequence(List<ModelPoint> seq)
{ 
   ModelPoint mpstart = seq.get(0);
   ModelPoint mpend = seq.get(seq.size() - 1);
   Point2D p2start= mpstart.getPoint2D();
   Point2D p2end = mpend.getPoint2D();
   double dist = p2start.distance(p2end);
   
   for (int i = 1; i < seq.size()-1; ++i) {
      ModelPoint pt = seq.get(i);
      Point2D pt2 = pt.getPoint2D();
      double d0 = p2start.distance(pt2);
      double d1 = p2end.distance(pt2);
      double d2 = (d0/d1 * dist) /(1 + d0/d1);
      double x = p2start.getX() + (p2end.getX() - p2start.getX()) * d2/dist;
      double y = p2start.getY() + (p2end.getY() - p2start.getY()) * d2/dist;
      pt.setPoint2D(x,y);
//    System.err.println("MAP " + pt.getId() + " " + pt2 + " " + x + " " + y);
    }
   

}



private class DoneMap extends HashMap<ModelPoint,Set<ModelPoint>> {
   
   private static final long serialVersionUID = 1;
   
   DoneMap() { }
   
   boolean isDone(ModelPoint from,ModelPoint to) {
      Set<ModelPoint> r = get(from);
      if (r == null) return false;
      return r.contains(to);
    }
   
   void noteSeq(ModelPoint from,ModelPoint to) {
      Set<ModelPoint> r = get(from);
      if (r == null) {
         r = new HashSet<>();
         put(from,r);
       }
      r.add(to);
    }
   
}       // end of inner class DoneMap




private void setupConnection(ModelPoint gap)
{
   ModelBlock b0 = null;
   ModelBlock b1 = null;
   if (gap.getModelConnectedTo().size() != 2) {
      noteError("Gap " + gap.getId() + " must have exactly 2 connections");
      return;
    }
   
   for (ModelPoint pt : gap.getModelConnectedTo()) {
      if (b0 == null) b0 = pt.getBlock();
      else if (b1 == null) b1 = pt.getBlock();
    }
   
   if (b0 == b1) {
      noteError("Gap " + gap.getId() + " is not between blocks");
      return;
    }
   
   block_connections.add(new ModelConnection(gap,b0,b1)); 
}






private void checkModel() throws ShoreException 
{
   for (ModelPoint pt : model_points.values()) {
      String rid = pt.getRefId();
      switch (pt.getType()) {
         case SWITCH :
            ModelSwitch sw = findSwitchForPoint(pt);
            if (sw == null) noteError("No switch found for point " + pt.getId());
            else if (rid != null && !rid.equals(sw.getId())) {
               noteError("Point " + pt.getId() + " has the wrong switch"); 
             }
            break;
         case SENSOR :
            ModelSensor sensor = findSensorForPoint(pt);
            if (sensor == null) noteError("No sensor found for point " + pt.getId());
            else if (!rid.equals(sensor.getId())) {
               noteError("Point " + pt.getId() + " has the wrong sensor"); 
             }
            ModelSignal sig0 = findSignalForPoint(pt);
            if (sig0 != null) {
               pt.setSignal(sig0); 
             }
            break;
         case SIGNAL :
            ModelSignal signal = findSignalForPoint(pt);
            if (signal == null) noteError("No signal found for point " + pt.getId());
            else if (!rid.equals(signal.getId())) {
               noteError("Point " + pt.getId() + " has the wrong signal"); 
             }
            break;
       }
      switch (pt.getType()) {
         case LABEL :
         case GAP :
         case DIAGRAM :
            break;
         default :
            ModelBlock blk = pt.getBlock();
            if (blk == null) {
               noteError("Point " + pt.getId() + " is not connected");
             }
            break;
       }
      int tgt = 2;
      switch (pt.getType()) {
         case SWITCH :
            tgt = 3;
            break;
         case END :
            tgt = 1;
            break;
         case LABEL :
         case TURNTABLE :
         case GAP :
            tgt = 0;
            break;
         default :
            tgt = 2;
            break;
       }
      if (tgt > 0) {
         int sz = pt.getModelConnectedTo().size();
         if (sz != tgt) {
            noteError("Point " + pt.getId() + " has the wrong number of connections");
          }
       }
    }
   
   Set<Integer> done = new HashSet<>();
   for (ModelSensor sen : model_sensors.values()) {
      int idx = sen.getTowerId() * 1024 + sen.getTowerSensor();
      if (!done.add(idx)) {
         noteError("Sensor tower conflict for " + sen.getId());
       }
    }
   done.clear();
   for (ModelSwitch sw : model_switches.values()) {
      int idx = sw.getTowerId() * 1024 + sw.getTowerSwitch();
      if (!done.add(idx)) {
         ModelSwitch swa = sw.getAssociatedSwitch();
         if (swa == null || swa.getTowerId() != sw.getTowerId() ||
               swa.getTowerSwitch() != sw.getTowerSwitch()) {
            noteError("Switch tower conflict for " + sw.getId());
          }
       }
      int idx1 = sw.getTowerRSwitch();
      if (idx1 >= 0) {
         idx1 += sw.getTowerId() * 1024;
         if (!done.add(idx1)) {
            noteError("Switch tower conflict for " + sw.getId());
          }
       }
      if (sw.getNSensor() == null) {
         noteError("No N sensor for switch " + sw.getId());
       }
      if (sw.getRSensor() == null && sw.getAssociatedSwitch() == null) {
         noteError("No R sensor for switch " + sw.getId());
       }
      
    }
   done.clear();
   for (ModelSignal sig : model_signals.values()) {
      int idx = sig.getTowerId() * 1024 + sig.getTowerSignal();
      if (!done.add(idx)) {
         noteError("Signal tower conflict for " + sig.getId());
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Output Methods                                                          */
/*                                                                              */
/********************************************************************************/

public void createReport(File output)
{
   PrintStream ps = System.out;
   try {
      if (output != null) ps = new PrintStream(output);
    }
   catch (IOException e) {
      System.err.println("Can't open report file " + output);
      return;
    }
   
   ps.println("HO TRAIN Setup for " + model_file);
   ps.println();
   
   ps.println("SWITCHES:");
   for (ModelSwitch sw : model_switches.values()) {
      ps.println("   Switch " + sw.getId() + 
            " (" + sw.getTowerId() + "-" + sw.getTowerSwitch() + ")");
      ps.println("      N: " + sw.getNPoint() + " " + sw.getNSensor());
      ps.println("      R: " + sw.getRPoint() + " " + sw.getRSensor());
      ps.println("      E: " + sw.getEntryPoint());
    }
   ps.println("\f");
   
   ps.println("SIGNALS:");
   for (ModelSignal sg : model_signals.values()) {
      if (sg.getModelStopSensors() == null) continue;
      ps.println("   Signal " + sg.getId() + "  " + sg.getSignalType() + 
            " (" + sg.getTowerId() + "-" + sg.getTowerSignal() + ")");
      ps.print("      Connections:");
      for (IfaceConnection conn : sg.getConnections()) {
         ps.print(" " + conn);
       }
      ps.println();
      ps.print("      At:  ");
      List<IfacePoint> pts = sg.getAtPoints();
      List<IfaceSensor> sns = sg.getStopSensors();
      for (int i = 0; i < pts.size(); ++i) {
         ps.print(" " + pts.get(i));
         ps.print(" " + sns.get(i));
       }
      ps.println();
      ps.println("      Next: " + sg.getGapPoint()); 
    }
   ps.println("\f");
   
   ps.println("CONNECTIONS:");
   for (ModelConnection conn : block_connections) {
      ModelBlock fblk = conn.getFromBlock();
      ModelBlock tblk = conn.getOtherBlock(fblk); 
      ps.println("   CONNECT " + fblk + " <--> " + tblk + "  at " + conn.getGapPoint()); 
      ps.println("      SENSORS: " + conn.getExitSensor(fblk) + " " +
            conn.getEntrySensor(fblk));
      ps.print("      FROM: " + conn.getStopSignal(fblk));
      if (conn.getStopSensors(fblk) != null) {
         for (IfaceSensor sen : conn.getStopSensors(fblk)) {
            ps.print(" " + sen);
          }
       }
      ModelSwitch sw0 = conn.getExitSwitch(fblk);
      if (sw0 != null) {
         ps.print(" " + sw0 + " = " + conn.getExitSwitchState(fblk));
       }
      ps.println();
      ps.print("      TO:   " + conn.getStopSignal(tblk));
      if (conn.getStopSensors(tblk) != null) {
         for (IfaceSensor sen : conn.getStopSensors(tblk)) {
            ps.print(" " + sen);
          }
       }
      sw0 = conn.getExitSwitch(tblk);
      if (sw0 != null) {
         ps.print(" " + sw0 + " = " + conn.getExitSwitchState(tblk));
       }
      ps.println();
    }
   ps.println("\f"); 
   
   for (int i = 0; ; ++i) {
      Map<Byte,ModelSensor> senmap = new TreeMap<>();
      Map<Byte,ModelSignal> sigmap = new TreeMap<>();
      Map<Byte,ModelSwitch> swmap = new TreeMap<>();
      for (ModelSensor sen : model_sensors.values()) {
         if (sen.getTowerId() == i) {
            senmap.put(sen.getTowerSensor(),sen);
          }
       }
      if (senmap.isEmpty()) break;
      for (ModelSignal sig : model_signals.values()) {
         if (sig.getTowerId() == i) {
            sigmap.put(sig.getTowerSignal(),sig);
          }
       }
      for (ModelSwitch sw : model_switches.values()) {
         if (sw.getTowerId() == i) {
            swmap.put(sw.getTowerSwitch(),sw);
          }
       }
      ps.println("TOWER " + i);
      ps.println("   SENSORS: ");
      for (Map.Entry<Byte,ModelSensor> ent : senmap.entrySet()) {
         ps.println("      " + ent.getKey() + ":  " + ent.getValue());
       }
      if (!sigmap.isEmpty()) {
         ps.println();
         ps.println("   SIGNALS: ");
         for (Map.Entry<Byte,ModelSignal> ent : sigmap.entrySet()) {
            ps.println("      " + ent.getKey() + ":  " + ent.getValue());
          }
       }
      if (!swmap.isEmpty()) {
         ps.println();
         ps.println("   SWITCHES: ");
         for (Map.Entry<Byte,ModelSwitch> ent : swmap.entrySet()) {
            ps.println("      " + ent.getKey() + ":  " + ent.getValue());
          }
       }
      ps.println("\f");
    }
}


}       // end of class ModelBase




/* end of ModelBase.java */

