/********************************************************************************/
/*                                                                              */
/*              ModelDiagram.java                                               */
/*                                                                              */
/*      Representation of a digram for layout purposes                          */
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;

class ModelDiagram implements ModelConstants
{

 
/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String diagram_id;
private double diagram_scale;

private Map<String,ModelPoint> diagram_points;
private Map<String,ModelSwitch> diagram_switches;
private Map<String,ModelBlock> diagram_blocks;
private Map<String,ModelSensor> diagram_sensors;
private Map<String,ModelSignal> diagram_signals;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ModelDiagram(Element xml)
{
   diagram_id = null;
   diagram_points = new HashMap<>();
   diagram_switches = new HashMap<>();
   diagram_blocks = new HashMap<>();
   diagram_sensors = new HashMap<>();
   diagram_signals = new HashMap<>();
   
   preloadDiagram(xml);
   
   if (diagram_id == null) diagram_id = "MAIN";
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getId()                                  { return diagram_id; }


Collection<ModelSensor> getSensors()
{
   return diagram_sensors.values();
}

Collection<ModelPoint> getPoints()
{ 
   return diagram_points.values();
}

Collection<ModelSwitch> getSwitches()
{
   return diagram_switches.values();
}

Collection<ModelBlock> getBlocks()
{
   return diagram_blocks.values();
}

Collection<ModelSignal> getSignals()
{
   return diagram_signals.values();
}

double getEngineSize()                          { return diagram_scale; }

ModelPoint getPointById(String id)      
{
   return diagram_points.get(id);
}


/********************************************************************************/
/*                                                                              */
/*      Load diagram from XML                                                   */
/*                                                                              */
/********************************************************************************/

private void preloadDiagram(Element xml)
{
   diagram_id = IvyXml.getAttrString(xml,"ID");
   diagram_scale = IvyXml.getAttrDouble(xml,"ENGINE",10);
   
   for (Element ptxml : IvyXml.children(xml,"POINT")) {
      ModelPoint pt = new ModelPoint(this,ptxml);  
      diagram_points.put(pt.getId(),pt);
    }
}

 
void loadDiagram(ModelBase mdl,Element xml)
{
   for (Element conxml : IvyXml.children(xml,"CONNECT")) {
      String pts = IvyXml.getAttrString(conxml,"POINTS");
      StringTokenizer tok = new StringTokenizer(pts);
      ModelPoint prev = null;
      while (tok.hasMoreTokens()) {
         String ptnm = tok.nextToken();
         ModelPoint pn = mdl.getPointById(ptnm);
         if (pn == null) {
            mdl.noteError("POINT " + ptnm + 
                  " not defined in connection list: " + pts);           
          }
         else {
            if (prev != null) prev.connectTo(pn); 
            prev = pn;
          }
       }
    }
   
   for (Element blkxml : IvyXml.children(xml,"BLOCK")) {
      ModelBlock blk = new ModelBlock(mdl,blkxml);
      if (diagram_blocks.put(blk.getId(),blk) != null) {
         mdl.noteError("Block " + blk.getId() + " defined twice");
       }
    }
   for (Element sensorxml : IvyXml.children(xml,"SENSOR")) {
      ModelSensor sensor = new ModelSensor(mdl,sensorxml);
      if (diagram_sensors.put(sensor.getId(),sensor) != null) {
         mdl.noteError("Sensor " + sensor.getId() + " defined twice");
       }
    }
   for (Element switchxml : IvyXml.children(xml,"SWITCH")) {
      ModelSwitch  sw = new ModelSwitch(mdl,switchxml);
      if (diagram_switches.put(sw.getId(),sw) != null) {
         mdl.noteError("Switch " + sw.getId() + " defined twice");
       }
    }
   for (Element signalxml : IvyXml.children(xml,"SIGNAL")) {
      ModelSignal signal = new ModelSignal(mdl,signalxml); 
      if (diagram_signals.put(signal.getId(),signal) != null) {
         mdl.noteError("Signal " + signal.getId() + " defined twice");
       }
    }
}


}       // end of class ModelDiagramImpl




/* end of ModelDiagramImpl.java */

