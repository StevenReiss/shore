/********************************************************************************/
/*                                                                              */
/*              TrainFactory.java                                               */
/*                                                                              */
/*      Factory for creating and managing engines and trains                    */
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



package edu.brown.cs.spr.shore.train;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.swing.SwingEventListenerList;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceConnection;
import edu.brown.cs.spr.shore.iface.IfaceEngine;
import edu.brown.cs.spr.shore.iface.IfaceModel;
import edu.brown.cs.spr.shore.iface.IfaceNetwork;
import edu.brown.cs.spr.shore.iface.IfaceSensor;
import edu.brown.cs.spr.shore.iface.IfaceTrains;

public class TrainFactory implements TrainConstants, IfaceTrains 
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceNetwork    network_model;
private IfaceModel      layout_model;
private Map<String,TrainEngine> known_trains;
private Map<SocketAddress,TrainEngine> assigned_trains;
private Map<IfaceBlock,TrainEngine> train_locations;
private Map<IfaceBlock,TrainEngine> expected_trains;
private SwingEventListenerList<TrainCallback> train_listeners; 



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public TrainFactory(IfaceNetwork net,IfaceModel mdl)
{
   network_model = net;
   layout_model = mdl;
   known_trains = new LinkedHashMap<>();
   assigned_trains = new HashMap<>();
   train_locations = new HashMap<>();
   expected_trains = new HashMap<>();
   
   train_listeners = new SwingEventListenerList<>(TrainCallback.class);
   
   loadTrains();
   
   layout_model.addModelCallback(new TrainModelUpdater());
}



/********************************************************************************/
/*                                                                              */
/*      Train management                                                        */
/*                                                                              */
/********************************************************************************/

@Override
public TrainEngine createTrain(String name,String id)
{
   if (name == null) return null; 
   
   TrainEngine eng = known_trains.get(name);
   if (eng == null) eng = known_trains.get(id);
   if (eng == null) { 
      int idx = known_trains.size();
      eng = new TrainEngine(this,name,id,ENGINE_COLORS[idx]);  
      known_trains.put(name,eng); 
      known_trains.put(id,eng);
    }
   return eng;
}


@Override 
public TrainEngine setEngineSocket(IfaceEngine engine0,SocketAddress sa)
{ 
   TrainEngine engine = (TrainEngine) engine0;
   if (sa != null) {
      TrainEngine eng1 = assigned_trains.get(sa);
      if (eng1 != null) return eng1;
    }
   if (engine == null) return null;
   if (sa == null) {
      SocketAddress osa = engine.getEngineAddress();
      if (osa != null) assigned_trains.remove(osa);
    }
   engine.setEngineAddress(sa);
   if (sa != null) {
      assigned_trains.put(sa,engine);
    }
   return engine;
}


@Override public TrainEngine findTrain(String name)
{
   if (name == null) return null;
   return known_trains.get(name);
}


@Override public TrainEngine findTrain(SocketAddress sa)
{
   if (sa == null) return null;
   return assigned_trains.get(sa);
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

IfaceNetwork getNetworkModel()                  { return network_model; }

IfaceModel getLayoutModel()                     { return layout_model; }

@Override public Collection<IfaceEngine> getAllEngines()
{
   return new ArrayList<>(known_trains.values());
}


/********************************************************************************/
/*                                                                              */
/*      Model loading methods                                                   */
/*                                                                              */
/********************************************************************************/

private void loadTrains()
{
   Element xml = layout_model.getModelXml();
   
   for (Element telt : IvyXml.children(xml,"ENGINE")) {
      String name = IvyXml.getAttrString(telt,"NAME");
      String id = IvyXml.getAttrString(telt,"ID");
      createTrain(name,id);
    }
}


/********************************************************************************/
/*                                                                              */
/*      Callback methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public void addTrainCallback(TrainCallback cb)
{
   train_listeners.add(cb);
}
 
@Override public void removeTrainCallback(TrainCallback cb)
{
   train_listeners.remove(cb);
}


void fireTrainChanged(TrainEngine eng)
{
   for (TrainCallback cb : train_listeners) {
      cb.trainChanged(eng);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Handle model changes that might affect trains                           */
/*                                                                              */
/********************************************************************************/

private final class TrainModelUpdater implements IfaceModel.ModelCallback {
   
   @Override public void blockChanged(IfaceBlock blk) {
      TrainEngine eng = null;
      
      switch (blk.getBlockState()) {
         case EMPTY :
            eng = train_locations.get(blk);
            if (eng != null) eng.exitBlock(blk);
            expected_trains.remove(blk);
            break;
         case INUSE :
            eng = train_locations.get(blk); 
            if (eng != null) break;
            eng = expected_trains.remove(blk);
            if (eng == null) {
               for (IfaceConnection conn : blk.getConnections()) {
                  IfaceSensor xsen = conn.getExitSensor(blk);
                  IfaceSensor esen = conn.getEntrySensor(blk);
                  if (xsen != null && esen != null && 
                        xsen.getSensorState() == ShoreSensorState.ON &&
                        esen.getSensorState() == ShoreSensorState.ON) {
                     IfaceBlock prev = xsen.getBlock();
                     eng = train_locations.get(prev);
                     if (eng != null) {
                        train_locations.put(blk,eng);
                        eng.enterBlock(blk);
                        break;
                      }
                   }
                }
             }
            if (eng != null) {
               train_locations.put(blk,eng);
               eng.enterBlock(blk);
             }
            break;
         case PENDING :
            IfaceBlock prev = blk.getPendingFrom();
            if (prev != null) {
               eng = train_locations.get(prev);
               if (eng != null) expected_trains.put(blk,eng);
             }
            break;
         case UNKNOWN :
            break;
       }
    }
   
   @Override public void sensorChanged(IfaceSensor s) {
      // update train's location to the most forward sensor in the block
      // if at signal sensor and signal is red, stop the train (see SafetySignal)
    }
   
}

}       // end of class TrainFactory




/* end of TrainFactory.java */

