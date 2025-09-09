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

import java.io.PrintStream;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
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
import edu.brown.cs.spr.shore.iface.IfaceSignal;
import edu.brown.cs.spr.shore.iface.IfaceSpeedZone;
import edu.brown.cs.spr.shore.iface.IfaceSwitch;
import edu.brown.cs.spr.shore.iface.IfaceTrains;
import edu.brown.cs.spr.shore.iface.IfaceEngine.EngineState;
import edu.brown.cs.spr.shore.shore.ShoreLog;

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
private int             train_index;
private Map<SocketAddress,TrainEngine> assigned_trains;
private Map<IfaceBlock,TrainData> train_locations;
private ZoneUpdater     zone_updater;
private Timer           train_timer;

private static final long EXIT_DELAY = 500;



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
   train_index = 0;
   zone_updater = null;
   train_timer = null;
   
   loadTrains();
   
   layout_model.addModelCallback(new TrainModelUpdater());
   
   setupZoneUpdater();
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
      int idx = train_index++;
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
      TrainEngine eng = createTrain(name,id);
      if (!IvyXml.getAttrBool(telt,"REARLIGHT",true)) {
         eng.setNoRearLight(); 
       }
    }
}


public void outputTrains(PrintStream ps)
{
   ps.println("TRAINS:\n");
   
   for (IfaceEngine eng : getAllEngines()) {
      ps.println("ENGINE " + eng.getEngineId());
      ps.println("    NAME:  " + eng.getEngineName());
      ps.println("    COLOR: " + eng.getEngineColor());
      ps.println();
      ps.println();
    }
   
   ps.println("\f");
}



/**************************Cur******************************************************/
/*                                                                              */
/*      Handle model changes that might affect trains                           */
/*                                                                              */
/********************************************************************************/

private final class TrainModelUpdater implements IfaceModel.ModelCallback {
   
   private Map<IfaceSensor,List<IfaceSensor>> turn_offs;
   
   TrainModelUpdater() {
      turn_offs = new HashMap<>();
    }
   
   @Override public void blockChanged(IfaceBlock blk) {
      ShoreLog.logD("TRAIN","Train block changed " + blk);
      
      if (zone_updater != null) {
         zone_updater.blockChanged(blk);
       }
      
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
   
   @Override public void preSensorChanged(IfaceSensor s) {
      handlePreSensorChanged(s);
    }
   
   @Override public void sensorChanged(IfaceSensor s) {
      ShoreLog.logD("TRAIN","Handle sensor changed " + s);
      handleSensorChanged(s);
      if (zone_updater != null) {
         zone_updater.sensorChanged(s);
       }
    }
   
   private void handlePreSensorChanged(IfaceSensor s)
   {
      if (s.getSensorState() != ShoreSensorState.ON) return;
      IfaceBlock blk = s.getBlock();
      
      ShoreLog.logD("TRAIN","Check for skipped sensor " + s + " " + blk);
      
      // first check if this is a new point or not -- ignore if not
      TrainData td = train_locations.get(blk);
      if (td == null) {
         for (TrainData td1 : train_locations.values()) {
            if (td1.getNextBlock() == blk) {
               ShoreLog.logD("TRAIN","Check skipped location " + blk + " " + td1.getNextBlock());
               td = td1;
               break;
             }
          }
         if (td == null && blk.getBlockState() == ShoreBlockState.PENDING) {
            IfaceBlock bold = blk.getPendingFrom();
            ShoreLog.logD("TRAIN","SKIP check previous block " + blk + " " + bold);
            td = train_locations.get(bold);
          }
       }
      if (td == null || td.seenPoint(s.getAtPoint())) {
         ShoreLog.logD("TRAIN","Sensor not relevant to skip checking " + td);
         return;
       }
      
      // next check if this sensor is adjacent to the current sensor for the engine
      // and just return if so
      IfacePoint engat = td.getEngine().getCurrentPoint();
      if (engat == null) return;
      IfaceSensor engsensor = layout_model.findSensorForPoint(engat);
      if (engsensor.getAdjacentSensors().contains(s)) {
         ShoreLog.logD("TRAIN","Sensor is proper successor for engine " + td.getEngine());
         return;
       }
      
      // finally, get prior sensor and check for a sensor inbetween current and this
      // that is not in the prior direction
      
      IfacePoint engpr = td.getEngine().getPriorPoint();
      if (engpr.getType() == ShorePointType.GAP) {
         for (IfacePoint p1 : engpr.getConnectedTo()) {
            if (p1 == s.getAtPoint()) continue;
            if (p1 == engat) continue;
            engpr = p1;
            break;
          }
       }
      IfaceSensor psensor = layout_model.findSensorForPoint(engpr);
      if (psensor == null) {
         ShoreLog.logD("TRAIN","Can't compute skipped sensor without prior one");
         return;
       }
      
      ShoreLog.logD("TRAIN","Find SKIPPED sensors " + s + " " + engsensor + " " + psensor);
      List<IfaceSensor> skipped = findSkippedSensors(s,engsensor,psensor);
      for (IfaceSensor skip : skipped) {
         ShoreLog.logD("TRAIN","SKIPPED SENSOR " + skip);
         List<IfaceSensor> offs = turn_offs.get(s);
         if (offs == null) {
            offs = new ArrayList<>();
            turn_offs.put(s,offs);
          }
         offs.add(s);
         skip.setSensorState(ShoreSensorState.ON);
       }
   }
   
   private List<IfaceSensor> findSkippedSensors(IfaceSensor at,IfaceSensor cur,IfaceSensor prior) {
      List<IfaceSensor> rslt = new ArrayList<>();
      // for now only look for a single skipped sensor
      for (IfaceSensor s1 : cur.getAdjacentSensors()) {
         if (s1 == prior) continue;
         if (s1 == at) break;
         for (IfaceSensor s2 : s1.getAdjacentSensors()) {
            if (s2 == cur || s2 == prior) continue;
            if (s2 == at) rslt.add(s1);
          }
       }
      return rslt;
    }
   
   
   private void handleSensorChanged(IfaceSensor s) {
      // turn off any skipped sensors -- possibly only do if s is now off
      List<IfaceSensor> offs = turn_offs.remove(s);
      if (offs != null) {
         for (IfaceSensor s1 : offs) {
            s1.setSensorState(ShoreSensorState.OFF);
          }
       }
      
      if (s.getSensorState() != ShoreSensorState.ON)  return;
      IfaceBlock blk = s.getBlock();
      TrainData td = train_locations.get(blk);
      ShoreLog.logD("TRAIN","Train sensor changed " + s + " " + s.getSensorState() + " " +
            blk + " " + td);
      if (td == null) {
         IfaceConnection conn = s.getConnection();
         ShoreLog.logD("TRAIN","Use connection " + conn);
         if (conn != null) {
            IfaceBlock prev = conn.getOtherBlock(blk);
            td = train_locations.get(prev);
            if (td != null) {
               td.setBlock(blk);
               ShoreLog.logD("TRAIN","Get train from previous block " + blk +
                     " " + prev + " " + td.getEngine().getEngineName());
               train_locations.put(blk,td);
               td.setCurrentPoints(s.getAtPoint(),conn.getGapPoint());
             }
          }
         if (td == null) {
            ShoreLog.logD("TRAIN","No connection found");
            TrainEngine enew = null;
            for (TrainEngine e1 : assigned_trains.values()) {
               if (e1.getEngineState() == EngineState.RUNNING &&
   //                e1.getSpeed() > 0 &&
                     e1.getEngineBlock() == null) {
                  enew = e1;
                  ShoreLog.logD("TRAIN","Found unattached running train " + 
                        e1.getEngineId());
                  break;
                }
             }
            if (enew == null) {
               for (TrainEngine e1 : assigned_trains.values()) {
                  if (e1.getEngineState() == EngineState.RUNNING && e1.getSpeed() > 0) {
                     if (enew == null) {
                        ShoreLog.logD("TRAIN","Found attached running train " + 
                              e1.getEngineId());
                        enew = e1;
                      }
                     else {
                        ShoreLog.logD("TRAIN","Found multiple running train " + 
                              e1.getEngineId());
                        enew = null;
                        break;
                      }
                   }
                }
             }
            if (enew != null) {
               td = new TrainData(enew);
               td.setBlock(blk);
               train_locations.put(blk,td);
               td.setCurrentPoints(s.getAtPoint(),null);
               ShoreLog.logD("TRAIN","Add new train " + blk + " " + 
                     td.getEngine().getEngineName());
             }
            else {
               ShoreLog.logD("TRAIN","Can't find train for block " + blk);
             }
          }
       }
      else {
         if (blk == td.getBlock()) {
            ShoreLog.logD("TRAIN","Engine in same block");
            IfacePoint prior = td.getEngine().getCurrentPoint();
            td.setCurrentPoints(s.getAtPoint(),prior);
            if (s.getAtPoint().getType() == ShorePointType.END) {
               td.getEngine().stopTrain();
               td.getEngine().setReverse(!td.getEngine().isReverse());
             }
          }
         else {
            ShoreLog.logD("TRAIN","Unknown block " + blk + " " +
                  td.getBlock() + " " + td.getEngine().getEngineName());
          }
       }
    }
   
   @Override public void switchChanged(IfaceSwitch sw) {
      ShoreLog.logD("TRAIN","Handle switch changed " + sw);
      IfaceBlock blk = sw.getPivotPoint().getBlock();
      TrainData td = train_locations.get(blk);
      if (td != null) td.checkNextBlock();
   }
   
   @Override public void signalChanged(IfaceSignal sg) { 
      ShoreLog.logD("TRAIN","Handle signal changed " + sg);
      IfaceBlock blk = sg.getFromBlock();
      TrainData td = train_locations.get(blk);
      if (td != null && td.getBlock() == blk) td.checkNextBlock();
    }
   
}       //  end of inner class TrainModelUpdater



private class TrainData {

   private TrainEngine for_engine;
   private IfaceBlock active_block;
   private IfaceBlock prior_block;
   private Set<IfacePoint> block_points;
   private IfaceBlock next_block;
   private IfaceSignal exit_signal;
    
   TrainData(TrainEngine eng) {
      for_engine = eng;
      active_block = null;
      block_points = new HashSet<>();
      next_block = null;
      exit_signal = null;
      prior_block = null;
    }
   
   TrainEngine getEngine()                      { return for_engine; }
   IfaceBlock getBlock()                        { return active_block; }
   IfaceBlock getNextBlock()                    { return next_block; }
   
   void setBlock(IfaceBlock blk) {
      ShoreLog.logD("TRAIN","Associate train " + for_engine.getEngineId() + 
            " with block " + blk + " from " + active_block);
      prior_block = active_block;
      active_block = blk;
      block_points.clear();
      next_block = null;
      exit_signal = null;
    }
   
   boolean seenPoint(IfacePoint cur) {
      if (cur.getBlock() != active_block) return true;
      if (block_points.contains(cur)) return true;
      
      return false;
    }
   
   void setCurrentPoints(IfacePoint cur,IfacePoint prior) {
      if (cur.getBlock() == active_block) {
         if (block_points.contains(cur)) return;
         block_points.add(cur);
         if (prior != null) block_points.add(prior);
         ShoreLog.logD("TRAIN","Set train points " + for_engine.getEngineId() + " " +
               cur + " <- " + prior);
         for_engine.setCurrentPoints(cur,prior);
         checkNextBlock();
       }
      else {
         ShoreLog.logE("TRAIN","Currrent point not in active block " + cur + " " +
            active_block + " " + prior_block);
       }
    }
   
   void checkNextBlock() {
      if (active_block == null) return;
      IfacePoint cur = for_engine.getCurrentPoint();
      IfacePoint prior = for_engine.getPriorPoint();
      if (prior == null) return;
      IfaceBlock next = layout_model.findNextBlock(prior,cur);
      
      boolean slow = false;
      boolean stop = false;
      if (next == prior_block) {
         ShoreLog.logD("TRAIN","Need to get proper block");
         next = active_block;
       }
      if (next != next_block) {
         ShoreLog.logD("TRAIN","Check next block " + prior + " " + cur + " " + next + " " +
               active_block + " " + next_block + " " + prior_block + " " + exit_signal);
         next_block = next;
         for (IfaceConnection c : active_block.getConnections()) {
            if (c.getOtherBlock(active_block) == next) {
               exit_signal = c.getStopSignal(active_block);
               break;
             }
          }
         if (next_block != null) {
            switch (next_block.getBlockState()) {
               case EMPTY :
                  break;
               case INUSE :
                  if (train_locations.get(next_block) == this) break;
                  slow = true;
                  break;
               case UNKNOWN :
                  break;
               case PENDING :
                  IfaceBlock pendon = next_block.getPendingFrom();
                  ShoreLog.logD("TRAIN","Check next block pending " + 
                        active_block + " " + next_block + " " + pendon);
                  // handle case of already in new block
                  if (pendon == active_block || active_block == next_block) break;
                  slow = true;
                  break;
             }
          }
       }
      
      ShoreLog.logD("TRAIN","Check exit signal " + exit_signal);
      if (exit_signal != null && exit_signal.getSignalState() == ShoreSignalState.RED) {
         for (IfacePoint pt : exit_signal.getAtPoints()) {
            if (block_points.contains(pt)) stop = true;
          }
         slow = true;
       }
      
      ShoreLog.logD("TRAIN","Train signal action " + active_block + " " +
            next + " " + exit_signal + " " + slow + " " + stop);
      
      if (stop) {
         // slow train in case it hasn't been before
         for_engine.slowTrain(ShoreSlowReason.SIGNAL,SLOW_THROTTLE); 
         for_engine.stopTrain();
       } 
      else if (slow) {
         // Might want a different reason than in Safety Signal
         for_engine.slowTrain(ShoreSlowReason.SIGNAL,SLOW_THROTTLE); 
       }
      else {
         if (for_engine.hasSavedThrottle(ShoreSlowReason.SIGNAL)) {
            for_engine.resumeTrain(ShoreSlowReason.SIGNAL);
          }
       }
    }
   
   
}       // end of inner class TrainData



/********************************************************************************/
/*                                                                              */
/*      Updater for speed zone monitoring                                       */
/*                                                                              */
/********************************************************************************/

private void setupZoneUpdater()
{
   int maxsensor = 0;
   int ctr = 0;
   for (IfaceSpeedZone sz : layout_model.getSpeedZones()) {
      int si = sz.getZoneSensors().size();
      maxsensor = Math.max(si,maxsensor);
      ++ctr;
    }
   
   if (ctr != 0) {
      ShoreLog.logD("TRAIN","Zone updater setup");
      zone_updater = new ZoneUpdater(maxsensor);
      if (maxsensor != 0) train_timer = new Timer("Train timer");
    }
}


private final class ZoneUpdater implements IfaceModel.ModelCallback {
   
   private Map<IfaceEngine,SensorBuffer> train_sensors;
   private int max_size;
   
   ZoneUpdater(int max) {
      max_size = max;
      train_sensors = new HashMap<>();
    }
   
   @Override public void blockChanged(IfaceBlock blk) {
      SensorBuffer buffer = getBuffer(blk);
      if (buffer != null) {
         buffer.noteBlock(blk);
       }
      else {
         ShoreLog.logD("TRAIN","Block " + blk + " not relevant for zones");
       }
    }
   
   @Override public void sensorChanged(IfaceSensor s) {
      SensorBuffer buffer = getBuffer(s.getBlock());
      ShoreLog.logD("TRAIN","Zone sensor change " + s + " " +
            (buffer != null));
      if (buffer != null) {
         buffer.noteSensor(s);
       }
    }
   
   private SensorBuffer getBuffer(IfaceBlock blk) {
      TrainData td = train_locations.get(blk);
      if (td == null) return null;
      IfaceEngine eng = td.getEngine();
      ShoreLog.logD("TRAIN","Find buffer for block " + eng + " " + blk);
      SensorBuffer buffer = train_sensors.get(eng);
      if (buffer == null) {
         buffer = new SensorBuffer(eng,max_size);
         train_sensors.put(eng,buffer);
         ShoreLog.logD("TRAIN","Create sensor buffer for " + eng.getEngineId());
       }
      return buffer;
    }
   
}       // end of inner class ZoneUpdater




private final class SensorBuffer {
   
   private IfaceEngine for_engine;
   private IfaceSensor [] last_sensors;
   private int head_index;
   private int tail_index;
   private int buffer_size;
   private int buffer_length;
   private IfaceSpeedZone sensor_zone;
   private IfaceSpeedZone block_zone;
   private boolean is_done;
   private ZoneCheckTask end_task;
   
   SensorBuffer(IfaceEngine eng,int max) {
      for_engine = eng;
      last_sensors = new IfaceSensor[max+1];
      head_index = 0;
      tail_index = 0;
      buffer_size = 0;
      buffer_length = last_sensors.length;
      sensor_zone = null;
      block_zone = null;
      is_done = false;
      end_task = null;
    }
   
   void noteSensor(IfaceSensor s) {
      if (block_zone != null) return;
      ShoreLog.logD("TRAIN","Check speed sensor " + for_engine + " " + 
            sensor_zone + " " + s + " " +
            s.getSensorState());
      
      if (sensor_zone == null) {
         if (s.getSensorState() == ShoreSensorState.ON) {
            for (IfaceSpeedZone sz : layout_model.getSpeedZones()) {
               if (sz.getStartSensor() == null) continue;
               ShoreLog.logD("TRAIN","Check zone sensor " + s + " " + sz.getStartSensor());
               if (s == sz.getStartSensor() || 
                     (!sz.isEndSensor(s) && sz.isZoneSensor(s))) {
                  ShoreLog.logD("TRAIN","Found start sensor for speed zone");
                  boolean fnd = contains(sz.getStartSensor());
                  for (IfaceSensor sen1 : sz.getEndSensors()) { 
                     if (contains(sen1)) fnd = true;
                   }
                  if (!fnd) {
                     sensor_zone = sz;
                     clear();
                     ShoreLog.logD("TRAIN","Slow train for speed zone " + sz);
                     for_engine.slowTrain(ShoreSlowReason.SPEED_ZONE,
                           sz.getSpeedPercent());
                     break;
                   }
                }
             }
            add(s);
          }
       }
      else {
         if (s.getSensorState() == ShoreSensorState.ON) {
            if (sensor_zone.isZoneSensor(s)) {
               add(s);
               synchronized (this) {
                  is_done = false;
                  if (end_task != null) {
                     end_task.cancel();
                     end_task = null;
                   }
                }
               ShoreLog.logD("TRAIN","Add sensor " + s);
             }
            else {
               checkIfDone();
             }
          }
         else if (sensor_zone.isEndSensor(s) && s.getSensorState() == ShoreSensorState.OFF) {
            checkIfDone();
          }
       }
    }
   
   
   private void checkIfDone() 
   {
      for (IfaceSensor ss : sensor_zone.getZoneSensors()) {
         if (ss.getSensorState() == ShoreSensorState.ON) {
            is_done = false;
            ShoreLog.logD("TRAIN","Still have a sensor on in zone " + ss);
            return;
          }
       }
      // all sensors off, end sensor just went off
      synchronized (this) {
         ShoreLog.logD("TRAIN","Start exit check for speed zone");
         is_done = true;
         ZoneCheckTask task = new ZoneCheckTask(this);
         train_timer.schedule(task,EXIT_DELAY);
       }
   }
   
   private void add(IfaceSensor s) {
      if (contains(s)) return;
      if (buffer_size == buffer_length) {
         head_index = (head_index + 1) % buffer_length;
       }
      else {
         ++buffer_size;
       }
      last_sensors[tail_index] = s;
      tail_index = (tail_index + 1) % buffer_length;
    }
   
   private void clear() {
      head_index = 0;
      tail_index = 0;
      buffer_size = 0;
    }
   
   private boolean contains(IfaceSensor s) {
      for (int i = 0; i < buffer_size; ++i) {
         int idx = (head_index + i) % buffer_length;
         if (last_sensors[idx] == s) return true;
       }
      return false;
    }
   
   synchronized void checkDone() {
      if (!is_done) return;
      ShoreLog.logD("TRAIN","Resume speed after zone " + for_engine.getEngineId());
      for_engine.resumeTrain(ShoreSlowReason.SPEED_ZONE);
      sensor_zone = null;
      block_zone = null;
      clear();
    }
   
   void noteBlock(IfaceBlock blk) {
      ShoreLog.logD("TRAIN","Check speed zone block " + blk + " " + block_zone + " " + sensor_zone);
      if (sensor_zone != null) return;
      IfaceSpeedZone inzone = null;
      for (IfaceSpeedZone sz : layout_model.getSpeedZones()) {
         Collection<IfaceBlock> blks = sz.getBlocks();
         if (blks != null && blks.contains(blk)) {
            inzone = sz;
            break;
          }
       }
      if (block_zone == null) {
         if (blk.getBlockState() == ShoreBlockState.INUSE) {
            block_zone = inzone;
            if (block_zone != null) {
               ShoreLog.logD("TRAIN","Slow train for block speed zone " + blk + " " +
                     block_zone);
               for_engine.slowTrain(ShoreSlowReason.SPEED_ZONE,
                     block_zone.getSpeedPercent());
             }
          }
       }
      else {
         if (blk.getBlockState() == ShoreBlockState.EMPTY) {
            if (block_zone.getBlocks() != null) {
               for (IfaceBlock zblk : block_zone.getBlocks()) {
                  if (zblk.getBlockState() == ShoreBlockState.INUSE) {
                     ShoreLog.logD("TRAIN","Block still in use for speed zone");
                     return;
                   }
                }
             }
            is_done = true;
            checkDone();
          }
         else if (blk.getBlockState() == ShoreBlockState.INUSE && inzone != null) {
            if (!block_zone.getBlocks().contains(blk)) {
               is_done = true;
               checkDone();
               ShoreLog.logD("TRAIN","Switch speed zones on new block " + blk + " " + inzone);
               block_zone = inzone;
               for_engine.slowTrain(ShoreSlowReason.SPEED_ZONE,
                     block_zone.getSpeedPercent());
             }
          }
       }
    }
   
}       // end of inner class SensorBuffer


private class ZoneCheckTask extends TimerTask {
   
   private SensorBuffer for_buffer;
   
   ZoneCheckTask(SensorBuffer buf) {
      for_buffer = buf;
    }
   
   @Override public void run() {
      for_buffer.checkDone();
    }
   
}       // end of inner class ZoneCheckTask

}       // end of class TrainFactory




/* end of TrainFactory.java */

