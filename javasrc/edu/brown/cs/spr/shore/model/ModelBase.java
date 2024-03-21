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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceModel;
import edu.brown.cs.spr.shore.iface.IfaceSensor;
import edu.brown.cs.spr.shore.iface.IfaceSignal;
import edu.brown.cs.spr.shore.iface.IfaceSwitch;
import edu.brown.cs.spr.shore.iface.IfaceTrain;
import edu.brown.cs.spr.shore.shore.ShoreException;

class ModelBase implements ModelConstants, IfaceModel
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<String,ModelPoint> model_points;
private Map<String,ModelSwitch> model_switches;
private Map<String,ModelBlock> model_blocks;
private Map<String,ModelSensor> model_sensors;
private Map<String,ModelSignal> model_signals;
private Map<String,ModelDiagram> model_diagrams;
private Map<String,ModelTrain> model_trains;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ModelBase(File file)
{
   model_points = new HashMap<>();
   model_switches = new HashMap<>();
   model_blocks = new HashMap<>();
   model_sensors = new HashMap<>();
   model_signals = new HashMap<>();
   model_diagrams = new HashMap<>();
   model_trains = new HashMap<>();
   
   try {
      loadModel(file);
    }
   catch (ShoreException e) {
      System.err.println("SHORE: Problem loading model: " + e.toString());
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

@Override public Collection<IfaceSignal> getSignals()
{
   return new ArrayList<>(model_signals.values());
}

@Override public Collection<IfaceSwitch> getSwitches()
{
   return new ArrayList<>(model_switches.values());
}

@Override public Collection<IfaceTrain> getTrains()
{
   return new ArrayList<>(model_trains.values());
}
 
@Override public Collection<IfaceBlock> getBlocks() 
{
   return new ArrayList<>(model_blocks.values());
}



@Override public IfaceSensor findSensor(int tower,int id)
{
   for (ModelSensor ms : model_sensors.values()) {
      if (ms.getTowerId() == tower && ms.getTowerSensor() == id) return ms;
    }
   return null;
}

@Override public IfaceSignal findSignal(int tower,int id)
{
   for (ModelSignal ms : model_signals.values()) {
      if (ms.getTowerId() == tower && ms.getTowerSignal() == id) return ms;
    }
   return null;
}

@Override public IfaceSwitch findSwitch(int tower,int id)
{
   for (ModelSwitch ms : model_switches.values()) {
      if (ms.getTowerId() == tower && ms.getTowerSwitch() == id) return ms;
    }
   return null;
}

ModelDiagram findDiagram(String id)
{
   return model_diagrams.get(id);
}


void addTrain(ModelTrain mt)
{
   model_trains.put(mt.getId(),mt);
}


void removeTrain(ModelTrain mt)
{
   model_trains.remove(mt.getId()); 
}

ModelPoint getPointById(String id)
{
   return model_points.get(id);
}

ModelSwitch getSwitchById(String id)
{
   return model_switches.get(id);
}


/********************************************************************************/
/*                                                                              */
/*      Callback methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public void addModelCallback(ModelCallback arg0)
{
   // method body goes here
}

@Override public void removeModelCallback(ModelCallback arg0)
{
   // method body goes here
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
   
   for (ModelDiagram md : xmlmap.keySet()) {
      for (ModelPoint mp : md.getPoints()) {
         model_points.put(mp.getId(),mp);
       }
    }
   
   for (Map.Entry<ModelDiagram,Element> ent : xmlmap.entrySet()) {
      ModelDiagram md = ent.getKey(); 
      Element dxml = ent.getValue();
      md.loadDiagram(this,dxml); 
      loadDiagramData(md);
    }
   
   normalizeModel();
   
   checkModel();
}
 


private void loadDiagramData(ModelDiagram md)
{
   for (ModelSwitch ms : md.getSwitches()) {
      model_switches.put(ms.getId(),ms);  
    }
   for (ModelBlock mb : md.getBlocks()) { 
      model_blocks.put(mb.getId(),mb);
    }
   for (ModelSensor ms : md.getSensors()) {
      model_sensors.put(ms.getId(),ms);
    }
   for (ModelSignal ms : md.getSignals()) {
      model_signals.put(ms.getId(),ms);
    }
}


private void normalizeModel() throws ShoreException
{
   // now we need to normalize the model
   //    Find all points in a block
   //    find entry/exit sensors for a block
   //    associate blocks with switches, signals, sensors
   //    assiciate sensors with switches
}


private void checkModel() throws ShoreException 
{
   // finally we need to verify the model and print error messages if 
   //   there is any problem (and terminate?)
}

}       // end of class ModelBase




/* end of ModelBase.java */

