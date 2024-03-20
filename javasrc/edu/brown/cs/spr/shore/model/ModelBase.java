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

class ModelBase implements ModelConstants, IfaceModel
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<String,ModelPointImpl> model_points;
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
   
   loadModel(file);
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

ModelPointImpl getPointById(String id)
{
   return model_points.get(id);
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

private boolean loadModel(File file) 
{
   Element xml = IvyXml.loadXmlFromFile(file);
   if (xml == null) return false;
   if (IvyXml.getChild(xml,"DIAGRAM") != null) {
      for (Element dxml : IvyXml.children(xml,"DIAGRAM")) {
         ModelDiagram md = new ModelDiagramImpl(dxml);
         model_diagrams.put(md.getId(),md);
       }
    }
   else {
      ModelDiagram md = new ModelDiagramImpl(xml);
      model_diagrams.put(md.getId(),md); 
    }
   
   // add all diagram information to the model
   
   return true;
}
 


}       // end of class ModelBase




/* end of ModelBase.java */

