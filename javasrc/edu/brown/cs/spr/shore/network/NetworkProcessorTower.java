/********************************************************************************/
/*                                                                              */
/*              NetworkProcessorTower.java                                      */
/*                                                                              */
/*      Process messages to and from the various towers                         */
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



package edu.brown.cs.spr.shore.network;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jmdns.ServiceInfo;

import edu.brown.cs.spr.shore.iface.IfaceModel;
import edu.brown.cs.spr.shore.iface.IfaceSensor;
import edu.brown.cs.spr.shore.iface.IfaceSignal;
import edu.brown.cs.spr.shore.iface.IfaceSwitch;
import edu.brown.cs.spr.shore.shore.ShoreLog;

class NetworkProcessorTower extends NetworkProcessor implements NetworkControlMessages
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceModel layout_model;
private Map<SocketAddress,ControllerInfo>  controller_map;
private Map<Integer,ControllerInfo>        id_map;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

NetworkProcessorTower(DatagramSocket sock,IfaceModel model)
{
   super(sock);
   layout_model = model;
   controller_map = new ConcurrentHashMap<>();
   id_map = new ConcurrentHashMap<>();
}

/********************************************************************************/
/*                                                                              */
/*      Top-level message requests                                              */
/*                                                                              */
/********************************************************************************/

void setSwitch(IfaceSwitch sw,ShoreSwitchState set)  
{
   if (sw == null) return;
   int id = sw.getTowerId();
   ControllerInfo ci = id_map.get(id);
   if (ci == null) return;
   sw.setSwitch(set);
   if (set == ShoreSwitchState.R && sw.getTowerRSwitch() >= 0) {
      ci.sendSwitchMessage(sw.getTowerRSwitch(),ShoreSwitchState.N);
    }
   else {
      ci.sendSwitchMessage(sw.getTowerSwitch(),set);
    }
   sw.setSwitch(set);
}



private boolean sendSwitchStatus(IfaceSwitch sw) 
{
   if (sw == null) return false;
   int id = sw.getTowerId();
   ControllerInfo ci = id_map.get(id);
   if (ci == null) return false;
   ShoreSwitchState set = sw.getSwitchState();
   if (set == ShoreSwitchState.R && sw.getTowerRSwitch() >= 0) {
      ci.sendSwitchMessage(sw.getTowerRSwitch(),ShoreSwitchState.N);
    }
   else {
      ci.sendSwitchMessage(sw.getTowerSwitch(),set);
    }
   return true;
}


void setSignal(IfaceSignal sig,ShoreSignalState set) 
{
   if (sig == null) return;
   
   ShoreLog.logD("NETWORK","Set signal request " + sig + " " + set);
   
   int id = sig.getTowerId();
   ControllerInfo ci = id_map.get(id);
   if (ci != null) {
      ci.sendSignalMessage(sig.getTowerSignal(),set);
    }
   
   sig.setSignalState(set);
}


private boolean sendSignalStatus(IfaceSignal sig) 
{
   if (sig == null) return false;
   
   int id = sig.getTowerId();
   ControllerInfo ci = id_map.get(id);
   if (ci == null) return false;
   ShoreSignalState set = sig.getSignalState();
   ci.sendSignalMessage(sig.getTowerSignal(),set);
   return true;
}



void setSensor(IfaceSensor sen,ShoreSensorState set)  
{
   if (sen == null) return;
   
   int id = sen.getTowerId();
   ControllerInfo ci = id_map.get(id);
   if (ci == null) return;
   sen.setSensorState(set);
   ci.sendSensorMessage(sen.getTowerSensor(),set);
}


private boolean sendDefSwitch(IfaceSwitch sw) 
{
   if (sw.getTowerRSwitch() >= 0) {
      int id = sw.getTowerId();
      ControllerInfo ci = id_map.get(id);
      if (ci != null) {
         ci.sendDefSwitchMessage(sw.getTowerSwitch(),sw.getTowerRSwitch());
         return true;
       }
    }
   
   return false;
}

private boolean sendDefSensor(IfaceSensor sen)
{
   if (sen == null) return false;
   if (sen.getTowerSensor() < 0) return false;
   
   int id = sen.getTowerId();
   ControllerInfo ci = id_map.get(id);
   if (ci == null) return false;
   IfaceSwitch sw = null;
   ShoreSwitchState state = ShoreSwitchState.UNKNOWN;
   for (IfaceSwitch sw1 : layout_model.getSwitches()) {
      if (sw1.getTowerId() == id) {
         if (sw1.getNSensor() == sen) {
            sw = sw1;
            state = ShoreSwitchState.N;
            break;
          }
         else if (sw1.getRSensor() == sen) {
            sw = sw1;
            state = ShoreSwitchState.R;
            break;
          }
       }
    }
   
   
   int s = 64;
   if (sw != null) {
      s = sw.getTowerSwitch() * 4 + state.ordinal();
      int idx1 = sw.getTowerRSwitch();
      if (state == ShoreSwitchState.R && idx1 >= 0) {
         s = idx1 * 4 + ShoreSwitchState.N.ordinal();
       }
    }
   ci.sendDefSensorMessage(sen.getTowerSensor(),s);
   
   return true;
}


private boolean sendDefSignal(IfaceSignal sig)
{
   if (sig == null) return false;
   int id = sig.getTowerId();
   ControllerInfo ci = id_map.get(id);
   if (ci == null) return false;
   
   ShoreSignalType sst = sig.getSignalType();
   switch (sst) {
      case ENGINE :
         sst = ShoreSignalType.RG;
         break;
      case ENGINE_ANODE :
         sst = ShoreSignalType.RG_ANODE;
         break;
    }
   
   if (sig.isUnused()) sst = ShoreSignalType.RG;
   
   ci.sendDefSignalMessage(sig.getTowerSignal(),sst.ordinal());
   
   return true;
}


private void broadcastInfo()
{
   for (ControllerInfo ci : controller_map.values()) {
      ci.sendSyncMessage();
    }
}



/********************************************************************************/
/*                                                                              */
/*      Model query methods                                                     */
/*                                                                              */
/********************************************************************************/

private IfaceSensor findSensor(int tower,int id)
{
   for (IfaceSensor ms : layout_model.getSensors()) {
      if (ms.getTowerId() == tower && ms.getTowerSensor() == id) return ms;
    }
   return null;
}


private IfaceSwitch findSwitch(int tower,int id)
{
   for (IfaceSwitch ms : layout_model.getSwitches()) {
      if (ms.getTowerId() == tower && ms.getTowerSwitch() == id) return ms;
      else if (ms.getTowerId() == tower && ms.getTowerRSwitch() == id) return ms;
    }
   return null;
}



private IfaceSignal findSignal(int tower,int id)
{
   for (IfaceSignal ms : layout_model.getSignals()) {
      if (ms.getTowerId() == tower && ms.getTowerSignal() == id) return ms;
    }
   return null;
}


/********************************************************************************/
/*                                                                              */
/*      Controller maintenance                                                  */
/*                                                                              */
/********************************************************************************/

private ControllerInfo findController(SocketAddress sa)
{
// ShoreLog.logD("NETWORK","Find controller on " + sa);
   if (sa == null) return null;
   ControllerInfo ci = controller_map.get(sa);
   if (ci == null) {
      ci = new ControllerInfo(sa);
      ControllerInfo nci = controller_map.putIfAbsent(sa,ci);
      if (nci != null) ci = nci;
      else { 
         ShoreLog.logD("NETWORK","New Controller " + sa);
         ci.sendSyncMessage();
       }
    }
   
   return ci;
}



@Override protected void handleServiceResolved(ServiceInfo si) 
{
   String nm = si.getName().toLowerCase();
   if (nm.startsWith("controller") || nm.startsWith("tower")) {
      ShoreLog.logD("NETWORK","Found controller: " + nm);
      SocketAddress sa = getServiceSocket(si,controller_map); 
      findController(sa);
    }    
}



/********************************************************************************/
/*                                                                              */
/*      Process incoming message                                                */
/*                                                                              */
/********************************************************************************/

@Override protected void handleMessage(DatagramPacket msg)
{
   String msgtxt = decodeMessage(msg.getData(),msg.getOffset(),msg.getLength());
   ShoreLog.logD("NETWORK","Received from " + msg.getAddress() + " " +
         msg.getPort() + " " + msg.getLength() + " " + msg.getOffset() + ": " +
         msgtxt);
   
   byte [] data = msg.getData();
   int id = data[1];                         // controller id
   int which = data[2];                      // switch, signal, sensor id
   int value = data[3];                      // switch, signal, sensor value
   
   SocketAddress sa = msg.getSocketAddress();
   ControllerInfo ci = findController(sa);
   ControllerInfo ci1 = id_map.get(id);
   if (ci1 == null && data[0] != CONTROL_ID) {
      ShoreLog.logD("NETWORK","Message without heartbeat " + id);
      //    id_map.put(id,ci);
      //    ShoreLog.logD("NETWORK","Assign id to controller " + id);
      //    ci.setId(id);
    }
   else if (ci != ci1 && ci1 != null) {
      ShoreLog.logE("NETWORK","Conflicing controllers for " + id);
    }
   
   switch (data[0]) {
      case CONTROL_ID :
         ci.noteConnection(id,which);
         break;
      case CONTROL_SENSOR :
         if (layout_model != null) {
            IfaceSensor s = findSensor(id,which);
            if (s == null) {
               ShoreLog.logD("NETWORK","Sensor not found " + id + " " + which);
               break;
             }
            ShoreSensorState sst = getState(value,ShoreSensorState.UNKNOWN); 
            s.setSensorState(sst);
          }
         break;
      case CONTROL_SWITCH :
         if (layout_model != null) {
            IfaceSwitch s = findSwitch(id,which);
            if (s == null) break;
            ShoreSwitchState sst = getState(value,ShoreSwitchState.UNKNOWN);
            if (sst != ShoreSwitchState.UNKNOWN &&
                  s.getSwitchState() == ShoreSwitchState.UNKNOWN) {
               if (s.getTowerRSwitch() == which) sst = ShoreSwitchState.R;
               s.setSwitch(sst);
             }
          }
         break;
      case CONTROL_SIGNAL :
         if (layout_model != null) {
            IfaceSignal s = findSignal(id,which);
            if (s == null) break;
            ShoreSignalState sst = getState(value,ShoreSignalState.OFF); 
            s.setSignalState(sst);
          }
         break;
      default :
         break;
    }
}


/********************************************************************************/
/*                                                                              */
/*     Handle periodic status updates                                           */
/*                                                                              */
/********************************************************************************/

@Override protected Thread getStatusUpdater() 
{
   return new TowerStatusUpdater();
}


private final class TowerStatusUpdater extends Thread {
   
   TowerStatusUpdater() {
      super("ShoreTowerStatusUpdater");
      setDaemon(true);
    }
   
   @Override public void run() {
      for ( ; ; ) {
         try {
            broadcastInfo();
            for (IfaceSensor sen : layout_model.getSensors()) {
               if (sendDefSensor(sen)) delay();
             }
            for (IfaceSignal sig : layout_model.getSignals()) {
               if (sendDefSignal(sig)) {
                  delay();
                  sendSignalStatus(sig);
                  delay();
                }
             }
            for (IfaceSwitch sw : layout_model.getSwitches()) {
               if (sendDefSwitch(sw)) {
                  delay();
                }
               if (sendSwitchStatus(sw)) {
                  delay();
                }
             }
          }
         catch (Throwable t) {
            ShoreLog.logE("NETWORK","Problem doing status updates",t);
          }
         broadcastInfo();
         finalDelay();
       }
    }
   
   private void checkHeartbeat() {
      long now = System.currentTimeMillis();
      for (ControllerInfo ci : controller_map.values()) {
         ci.checkHeartbeat(now);
       }
    }
   
   private void delay() {
      checkHeartbeat();
      try {
         Thread.sleep(STATUS_DELAY); 
       }
      catch (InterruptedException e) { }
    }
   
   private void finalDelay() {
      for (int i = 0; i < FINAL_DELAY; ++i) {
         delay();
       }
    }
   
}       // end of innter class TowerStatusUpdater



/********************************************************************************/
/*                                                                              */
/*      Conrtroller for a single tower                                          */
/*                                                                              */
/********************************************************************************/

private class ControllerInfo {
   
   private byte controller_id;
   private SocketAddress net_address;
   private long last_heartbeat;
   
   ControllerInfo(SocketAddress net) {
      net_address = net;
      controller_id = -1;
      last_heartbeat = 0;
    }
   
   synchronized void noteConnection(int id,int first) {
      if (id != controller_id) {
         setId(id);
         id_map.put(id,this);
         ShoreLog.logD("NETWORK","Assign id to controller " + id + " " + first);
         first = 1;
       }
      else if (id_map.get(id) != this) {
         id_map.put(id,this);
         first = 1;
       }
      
      last_heartbeat = System.currentTimeMillis();
      if (first == 1) {
         ShoreLog.logD("NETWORK","Set up new controller " + id);
         setToUnknown();
         sendSyncMessage();
       }
    }
   
   private void setToUnknown() {
      for (IfaceSensor sen : layout_model.getSensors()) {
         if (sen.getTowerId() == controller_id) {
            sen.setSensorState(ShoreSensorState.UNKNOWN);
          }
         // possibly reset switches and signals and blocks as well
       }
    }
   
   synchronized boolean checkHeartbeat(long now) {
      if (last_heartbeat == 0 || controller_id < 0) {
         return false;
       }
      if (now - last_heartbeat > HEARTBEAT_TIME) { 
         int val = (int) controller_id;
         id_map.remove(val);
         setToUnknown();
         controller_id = -1;
         
         return false;
       }
      return true;
    }
   
   void setId(int id)                                   { controller_id = (byte) id; }
   
   void sendSyncMessage() {
      byte [] msg = { CONTROL_SYNC, MESSAGE_ALL, 0, 0 };
      sendMessage(net_address,msg,0,4);
    }
   
   void sendSwitchMessage(byte sid,IfaceSwitch.ShoreSwitchState state) {
      byte [] msg = { CONTROL_SETSWTICH, controller_id, sid,(byte) state.ordinal()};
      sendMessage(net_address,msg,0,4);
    }
   
   void sendSignalMessage(byte sid,IfaceSignal.ShoreSignalState state) {
      byte [] msg = { CONTROL_SETSIGNAL, controller_id, sid,(byte) state.ordinal()};
      sendMessage(net_address,msg,0,4);
    }
   
   void sendSensorMessage(byte sid,IfaceSensor.ShoreSensorState state) {
      byte [] msg = { CONTROL_SETSENSOR, controller_id, sid,(byte) state.ordinal()}; 
      sendMessage(net_address,msg,0,4);
    }
   
   void sendDefSensorMessage(byte sid,int value) {
      byte [] msg = { CONTROL_DEFSENSOR, controller_id,sid,(byte) value };
      sendMessage(net_address,msg,0,4);
    }
   
   void sendDefSignalMessage(byte sid,int value) {
      byte [] msg = { CONTROL_DEFSIGNAL, controller_id, sid, (byte) value };
      sendMessage(net_address,msg,0,4);
    }
   
   void sendDefSwitchMessage(byte sid,byte rsid) {
      byte[] msg = { CONTROL_DEFSWITCH, controller_id, sid, rsid };
      sendMessage(net_address,msg,0,4);
    }
    
}       // end of inner class ControllerInfo


}       // end of class NetworkProcessorTower




/* end of NetworkProcessorTower.java */

