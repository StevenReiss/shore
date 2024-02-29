/********************************************************************************/
/*                                                                              */
/*              ModelFactory.java                                               */
/*                                                                              */
/*      description of class                                                    */
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

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.spr.shore.shore.ShoreLog;


public class ModelFactory implements ModelConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private File    model_file;

private static ModelFactory the_factory;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private ModelFactory()
{ }


synchronized public ModelFactory getFactory()
{
   if (the_factory == null) {
      the_factory = new ModelFactory();
    }
   return the_factory;
}



/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

public void resetModel() 
{
   // reset everything to off
   // if file has changed, reread the model file
   // run initialization sequences to find states
   // check connectivity
   // try to locate trains
}


public void loadModel(File description)
{
   model_file = description;
   Element xml = IvyXml.loadXmlFromFile(model_file);
   
   ShoreLog.logD("Loaded model: " + IvyXml.convertXmlToString(xml));
   
   // load model from file
   // setup all switches, signals, trains, sensors
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

public Collection<ModelSwitch> getSwitches()
{
   return new ArrayList<>();
}

public Collection<ModelSignal> getSignals()
{
   return new ArrayList<>();
}

public Collection<ModelSensor> getSernsors()
{
   return new ArrayList<>();
}

public Collection<ModelTrain> getTrains()
{
   return new ArrayList<>();
}


}       // end of class ModelFactory




/* end of ModelFactory.java */

