/********************************************************************************/
/*                                                                              */
/*              ModelDiagramImpl.java                                           */
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

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.spr.shore.model.ModelConstants.ModelDiagram;

class ModelDiagramImpl implements ModelConstants, ModelDiagram
{

 
/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String diagram_id;

private Map<String,ModelPointImpl> diagram_points;
private Map<String,ModelSwitch> diagram_switches;
private Map<String,ModelBlock> diagram_blocks;
private Map<String,ModelSensor> diagram_sensors;
private Map<String,ModelSignal> diagram_signals;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ModelDiagramImpl(Element xml)
{
   diagram_id = null;
   diagram_points = new HashMap<>();
   diagram_switches = new HashMap<>();
   diagram_blocks = new HashMap<>();
   diagram_sensors = new HashMap<>();
   diagram_signals = new HashMap<>();
   
   loadDiagram(xml);
   
   if (diagram_id == null) diagram_id = "MAIN";
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String getId()                 { return diagram_id; }


@Override public Collection<ModelSensor> getSensors()
{
   return diagram_sensors.values();
}

@Override public Collection<ModelPointImpl> getPoints()
{ 
   return diagram_points.values();
}

@Override public Collection<ModelSwitch> getSwitches()
{
   return diagram_switches.values();
}

@Override public Collection<ModelBlock> getBlocks()
{
   return diagram_blocks.values();
}

@Override public Collection<ModelSignal> getSignals()
{
   return diagram_signals.values();
}


ModelPointImpl getPointById(String id)      
{
   return diagram_points.get(id);
}


/********************************************************************************/
/*                                                                              */
/*      Load diagram from XML                                                   */
/*                                                                              */
/********************************************************************************/

private void loadDiagram(Element xml)
{
   diagram_id = IvyXml.getAttrString(xml,"ID");
   
   for (Element ptxml : IvyXml.children(xml,"POINT")) {
      ModelPointImpl pt = new ModelPointImpl(this,ptxml); 
      diagram_points.put(pt.getId(),pt);
    }
   for (Element blkxml : IvyXml.children(xml,"BLOCK")) {
      // ModelBlock blk = new ModelBlockImpl(this,blkxml);
      // diagram_blocks.put(blk.getId(),blk);
    }
   for (Element sensorxml : IvyXml.children(xml,"SENSOR")) {
      // ModelSensor  sensor = new ModelSensorImpl(this,sensorxml);
      // diagram_sensors.put(sensor.getId(),sensor);
    }
   for (Element switchxml : IvyXml.children(xml,"SWITCH")) {
      // ModelSwitch  switch = new ModelSwitchImpl(this,switchxml);
      // diagram_switches.put(switch.getId(),switch);
    }
   for (Element signalxml : IvyXml.children(xml,"SIGNAL")) {
      // ModelSignal Signal = new ModelSignalImpl(this,signalxml);
      // diagram_signals.put(signal.getId(),signal);
    }
}


private void resolveDiagram(ModelBase mdl,Element xml)
{
   for (Element ptxml : IvyXml.children(xml,"POINT")) {
      String pid = IvyXml.getAttrString(ptxml,"ID");
      ModelPointImpl pt = diagram_points.get(pid);
      if (pt == null) continue;
      // THIS SHOULD BE DONE BY THE MODEL, NOT THE DIAGRAM
      pt.resolve(mdl,ptxml); 
    }
}


}       // end of class ModelDiagramImpl




/* end of ModelDiagramImpl.java */

