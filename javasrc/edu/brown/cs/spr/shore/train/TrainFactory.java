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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceConnection;
import edu.brown.cs.spr.shore.iface.IfaceEngine;
import edu.brown.cs.spr.shore.iface.IfaceModel;
import edu.brown.cs.spr.shore.iface.IfaceNetwork;
import edu.brown.cs.spr.shore.iface.IfacePoint;
import edu.brown.cs.spr.shore.iface.IfaceSensor;
import edu.brown.cs.spr.shore.iface.IfaceTrains;
import edu.brown.cs.spr.shore.iface.IfaceEngine.EngineState;

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
private Map<IfaceBlock,TrainData> train_locations;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public TrainFactory(IfaceModel mdl)
{
   network_model = null;
   layout_model = mdl;
   known_trains = new LinkedHashMap<>();
   assigned_trains = new HashMap<>();
   train_locations = new HashMap<>();
   
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
   if (eng == null && id != null) eng = known_trains.get(id);
   if (eng == null) { 
      int idx = known_trains.size();
      eng = new TrainEngine(this,name,id,ENGINE_COLORS[idx]);  
      known_trains.put(name,eng); 
      if (id != null) known_trains.put(id,eng);
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


@Override public TrainEngine findTrain(String nameorid)
{
   if (nameorid == null) return null;
   return known_trains.get(nameorid);
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/
 
@Override public void setNetworkModel(IfaceNetwork net)                          
{
   network_model = net;
}

IfaceNetwork getNetworkModel()                  { return network_model; }

IfaceModel getLayoutModel()                     { return layout_model; }

@Override public Collection<IfaceEngine> getAllEngines()
{
   return new TreeSet<>(known_trains.values());
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



/**************************Cur******************************************************/
/*                                                                              */
/*      Handle model changes that might affect trains                           */
/*                                                                              */
/********************************************************************************/

private final class TrainModelUpdater implements IfaceModel.ModelCallback {
   
   @Override public void blockChanged(IfaceBlock blk) {
      switch (blk.getBlockState()) {
         case EMPTY :
            TrainData td = train_locations.remove(blk);
            if (td != null) td.getEngine().exitBlock(blk);
            break;
         case INUSE :
            break;
         case PENDING :
            break;
         case UNKNOWN :
            break;
       }
    }
   
   @Override public void sensorChanged(IfaceSensor s) {
      if (s.getSensorState() != ShoreSensorState.ON)  return;
      IfaceBlock blk = s.getBlock();
      TrainData td = train_locations.get(blk);
      if (td == null) {
         IfaceConnection conn = s.getConnection();
         if (conn != null) {
            IfaceBlock prev = conn.getOtherBlock(blk);
            td = train_locations.get(prev);
            if (td != null) {
               td.setBlock(blk);
               td.setCurrentPoints(s.getAtPoint(),conn.getGapPoint());
             }
          }
         if (td == null) {
            for (TrainEngine e1 : assigned_trains.values()) {
               if (e1.getEngineState() == EngineState.READY &&
                     e1.getSpeed() > 0 &&
                     e1.getEngineBlock() == null) {
                  td = new TrainData(e1);
                  td.setBlock(blk);
                  td.setCurrentPoints(s.getAtPoint(),null);
                  break;
                }
             }
          }
         else {
            if (blk == td.getBlock()) {
               IfacePoint prior = td.getEngine().getCurrentPoint();
               td.setCurrentPoints(s.getAtPoint(),prior);
             }
          }
       }
    }
   
}       //  end of inner class TrainModelUpdater



private class TrainData {

   private TrainEngine for_engine;
   private IfaceBlock active_block;
   private Set<IfacePoint> block_points;
    
   TrainData(TrainEngine eng) {
      for_engine = eng;
      active_block = null;
      block_points = new HashSet<>();
    }
   
   TrainEngine getEngine()                      { return for_engine; }
   IfaceBlock getBlock()                        { return active_block; }
   
   void setBlock(IfaceBlock blk) {
      active_block = blk;
      block_points.clear();
    }
   
   void setCurrentPoints(IfacePoint cur,IfacePoint prior) {
      if (cur.getBlock() == active_block) {
         if (block_points.contains(cur)) return;
         block_points.add(cur);
         if (prior != null) block_points.add(prior);
         for_engine.setCurrentPoints(cur,prior);
       }
    }
   
}       // end of inner class TrainData

}       // end of class TrainFactory




/* end of TrainFactory.java */

