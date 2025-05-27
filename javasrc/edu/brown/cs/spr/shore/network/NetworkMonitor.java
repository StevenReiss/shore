/********************************************************************************/
/*										*/
/*		NetworkMonitor.java						*/
/*										*/
/*	Monitor to handle our UDP communications				*/
/*										*/
/********************************************************************************/
/*	Copyright 2023 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2023, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/



package edu.brown.cs.spr.shore.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jmdns.JmmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;

import edu.brown.cs.spr.shore.iface.IfaceNetwork;
import edu.brown.cs.spr.shore.iface.IfaceModel;
import edu.brown.cs.spr.shore.iface.IfaceSensor;
import edu.brown.cs.spr.shore.iface.IfaceSignal;
import edu.brown.cs.spr.shore.iface.IfaceSwitch;
import edu.brown.cs.spr.shore.iface.IfaceEngine;
import edu.brown.cs.spr.shore.shore.ShoreLog;

public class NetworkMonitor implements NetworkConstants, NetworkControlMessages,
      NetworkLocoFiMessages, IfaceNetwork 
{ 



/********************************************************************************/
/*										*/
/*	Main program (for testing/standalone use)				*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   ShoreLog.setup();

   NetworkMonitor mon = new NetworkMonitor(null);
   // possibly handle args

   mon.start();
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private DatagramSocket	our_socket;
private MulticastSocket multi_socket;
private InetSocketAddress multi_group;
private IfaceModel layout_model;

private Map<SocketAddress,ControllerInfo>  controller_map;
private Map<Integer,ControllerInfo>        id_map;
private Map<SocketAddress,EngineInfo>      engine_map;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public NetworkMonitor(IfaceModel model)
{
   controller_map = new ConcurrentHashMap<>();
   id_map = new ConcurrentHashMap<>();
   engine_map = new ConcurrentHashMap<>();
   
   layout_model = model;
   
   if (our_socket != null) {
      try {
         our_socket.close();
       }
      catch (Throwable e) { }
    }
   our_socket = null;
   InetAddress useaddr = null;
   
   try {
      useaddr = InetAddress.getLocalHost();
      NetworkInterface netif= null;
      for (int i = 10; i >= 0; --i) {
         NetworkInterface ni = NetworkInterface.getByIndex(i);
         if (isWifiInterface(ni)) {
            netif = ni;
            break;
          }
       }
      useaddr = InetAddress.getLocalHost();
      if (useaddr.isLoopbackAddress()) {
         for (Enumeration<InetAddress> en = netif.getInetAddresses(); en.hasMoreElements(); ) {
            InetAddress ia1 = en.nextElement();
            if (ia1 instanceof Inet4Address) {
               useaddr = ia1;
               break;
             }
          }
       }
      
      our_socket = new DatagramSocket(UDP_PORT,useaddr);
      our_socket.setReceiveBufferSize(BUFFER_SIZE);
      our_socket.setReuseAddress(true);
      our_socket.setSendBufferSize(BUFFER_SIZE);
      our_socket.setSoTimeout(0);
      multi_group = null;               // Don't try to multicast for now
      multi_socket = null;
//    InetAddress mcastaddr = InetAddress.getByName("239.1.2.3");
//    multi_group = new InetSocketAddress(mcastaddr,6879);
//    multi_socket = new MulticastSocket(6879);
//    multi_socket.joinGroup(new InetSocketAddress(mcastaddr,0),netif);
    }
   catch (IOException e) {
      ShoreLog.logE("NETWORK","Can't create Datagram Socket",e);
      System.err.println("Problem with network connection: " + e);
      System.exit(1);
    }
   
   try {
      JmmDNS jmm = JmmDNS.Factory.getInstance();
      jmm.addServiceListener("_udp._udp.local.",new ServiceFinder("2"));
      jmm.registerServiceType("_master._udp.local");
      jmm.registerServiceType("_tower._udp.local.");
      jmm.registerServiceType("master._udp.local");
      jmm.registerServiceType("tower._udp.local.");
      jmm.registerServiceType("udp._udp.local.");
      ServiceInfo info = ServiceInfo.create("master._udp.local.","shore",
            UDP_PORT,"SHORE controller");
      jmm.registerService(info);
    }
   catch (IOException e) {
      ShoreLog.logE("NETWORK","Problem registering service",e);
    }
   
   ShoreLog.logD("NETWORK","Monitor setup  " + our_socket.getLocalAddress());
}


private boolean isWifiInterface(NetworkInterface ni)
{
   if (ni == null) return false;
   if (ni.isVirtual()) return false;
   try {
      if (ni.isLoopback()) return false;
      if (!ni.isUp()) return false;
      if (ni.isPointToPoint()) return false;
      
    }
   catch (SocketException e) {
      return false;
    }
   
   if (ni.getParent() != null) return false;
   boolean havei4 = false;
   for (Enumeration<InetAddress> en = ni.getInetAddresses(); en.hasMoreElements(); ) {
      InetAddress ia = en.nextElement();
      if (ia instanceof Inet4Address) {
         havei4 = true;
       }
    }
   if (!havei4) return false;
  
// String s1 = ni.getName().toLowerCase();
// String s2 = ni.getDisplayName().toLowerCase();
   // check name here -- but it might not be significant on a mac?
   
   
   return true;
}




/********************************************************************************/
/*										*/
/*	Start monitoring							*/
/*										*/
/********************************************************************************/

public void start()
{
   ReaderThread rt = new ReaderThread(our_socket,new NotificationHandler());
   rt.start();
   if (multi_socket != null) {
      ReaderThread rt1 = new ReaderThread(multi_socket,new NotificationHandler());
      rt1.start();
    }
   
   broadcastInfo();
   
   StatusUpdater upd = new StatusUpdater();
   upd.start();
   
   ShoreLog.logD("NETWORK","MONITOR STARTED");   
}



/********************************************************************************/
/*										*/
/*	Top-level message requests						*/
/*										*/
/********************************************************************************/

@Override 
public void setSwitch(IfaceSwitch sw,ShoreSwitchState set)  
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
 

@Override
public void setSignal(IfaceSignal sig,ShoreSignalState set) 
{
   if (sig == null) return;
   
   int id = sig.getTowerId();
   ControllerInfo ci = id_map.get(id);
   if (ci != null) {
      ci.sendSignalMessage(sig.getTowerSignal(),set);
    }
   else {
      sig.setSignalState(set);
    }
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



@Override
public void setSensor(IfaceSensor sig,ShoreSensorState set)  
{
   if (sig == null) return;
   
   int id = sig.getTowerId();
   ControllerInfo ci = id_map.get(id);
   if (ci == null) return;
   sig.setSensorState(set);
   ci.sendSensorMessage(sig.getTowerSensor(),set);
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
   
   ci.sendDefSignalMessage(sig.getTowerSignal(),sst.ordinal());
   
   return true;
}


@Override 
public void sendStopTrain(IfaceEngine tr,boolean emergency) 
{
   SocketAddress sa = tr.getEngineAddress();
   if (sa == null) return;
   EngineInfo ei = engine_map.get(sa);
   if (ei == null) return;
   
   if (emergency) {
      ei.sendEmergencyStop();
    }
   else {
      ei.sendStopEngine();
    }
}


public void sendStartTrain(IfaceEngine tr)
{
   SocketAddress sa = tr.getEngineAddress();
   if (sa == null) return;
   EngineInfo ei = engine_map.get(sa);
   if (ei == null) return;
   
   boolean emergency = tr.isEmergencyStopped();
   
   if (emergency) {
      ei.sendEmergencyStart();
    }
   else {
      ei.sendStartEngine();
    }
}



private void broadcastInfo()
{
   byte [] msg = { CONTROL_SYNC, MESSAGE_ALL, 0, 0 };
   sendMessage(null,msg,0,4);
}


/********************************************************************************/
/*										*/
/*	Handle Send requests							*/
/*										*/
/********************************************************************************/

public synchronized void sendMessage(SocketAddress who,byte [] msg,int off,int len)
{
   if (our_socket == null) return;
   
   if (who == null) {
      if (multi_group != null && multi_socket != null) {
         try {
            DatagramPacket dp = new DatagramPacket(msg,len,multi_group);
            multi_socket.send(dp);
            ShoreLog.logD("NETWORK","MULTI SEND " + len);
            return;
          }
         catch (IOException e) {
            ShoreLog.logE("NETWORK","Problem with multicast: ",e);
          }
       }
      for (ControllerInfo ci : controller_map.values()) {
         sendMessage(ci.getSocketAddress(),msg,off,len);
       }
      return;
    }
   
   String msgtxt = decodeMessage(msg,off,len);
   ShoreLog.logD("NETWORK","Send " + msgtxt + " >> " + who);
   
   DatagramPacket packet = new DatagramPacket(msg,off,len,who);
   try {
      our_socket.send(packet);
    }
   catch (Throwable e) {
      ShoreLog.logE("Problem sending packet " + who,e);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Model finding methods                                                   */
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
/*										*/
/*	Handle incoming messages						*/
/*										*/
/********************************************************************************/

@SuppressWarnings("unchecked")
private <T extends Enum<T>> T getState(int v,T dflt)
{
   for (Object x : dflt.getClass().getEnumConstants()) {
     Enum<?> e = (Enum<?>) x;
     if (e.ordinal() == v) {
        return (T) e;
      }
    }
   
   return dflt;
}

static String decodeMessage(byte [] msg,int off,int len)
{
   StringBuffer buf = new StringBuffer();
   for (int i = 0; i <len; ++i) {
      int v = msg[off+i];
      String vs = Integer.toHexString(v);
      if (vs.length() == 1) vs = "0" + vs;
      buf.append(vs);
      buf.append(" ");
    }
   return buf.toString();
}


private final class NotificationHandler implements MessageHandler {

   @Override public void handleMessage(DatagramPacket msg) {
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
               if (s == null) break;
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

}	// end of inner class Notification Handler


/********************************************************************************/
/*                                                                              */
/*      Handle continuous status sending                                        */
/*                                                                              */
/********************************************************************************/

private final class StatusUpdater extends Thread {
   
   StatusUpdater() {
      super("ShoreStatusUpdater");
      setDaemon(true);
    }
   
   @Override public void run() {
      for ( ; ; ) {
         try {
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
               if (sendSwitchStatus(sw)) {
                  delay();
                }
             }
          }
         catch (Throwable t) {
            ShoreLog.logE("NETWORK","Problem doing status updates",t);
          }
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
   
}

/********************************************************************************/
/*										*/
/*	Server thread								*/
/*										*/
/********************************************************************************/

private class ReaderThread extends Thread {

   private DatagramSocket reader_socket;
   private MessageHandler msg_handler;

   ReaderThread(DatagramSocket s,MessageHandler h) {
      super("UDP_READER_" + s.getLocalPort());
      reader_socket = s;
      msg_handler = h;
    }

   @Override public void run() {
      byte [] buf = new byte[BUFFER_SIZE];
      DatagramPacket packet = new DatagramPacket(buf,buf.length);
      while (reader_socket != null) {
         try {
            reader_socket.receive(packet);
            msg_handler.handleMessage(packet);
          }
         catch (SocketTimeoutException e) { }
         catch (IOException e) {
            ShoreLog.logE("Problem reading UDP",e);
            // possibly recreate our_socket or set to null
          }
   //    ShoreLog.logD("NETWORK","FINISH PACKET");
       }
    }

}	// end of inner class ReaderThread



/********************************************************************************/
/*                                                                              */
/*      Controller information                                                  */
/*                                                                              */
/********************************************************************************/

private ControllerInfo findController(ServiceInfo si)
{
   SocketAddress sa = getServiceSocket(si,controller_map);
   return findController(sa);
}



private SocketAddress getServiceSocket(ServiceInfo si,Map<?,?> known)
{
   InetAddress [] possibles = si.getInet4Addresses();
   if (possibles == null || possibles.length == 0) {
      possibles = si.getInetAddresses();
    }
   int port = si.getPort();
   
   InetAddress use = null;
   for (InetAddress ia : possibles) {
      ShoreLog.logD("NETWORK","Check address " + ia);
      SocketAddress sa = new InetSocketAddress(ia,port);
      if (known != null && known.containsKey(sa)) return sa;
      if (use == null) use = ia;
      else if (use.isLoopbackAddress()) continue;
      else if (use.isAnyLocalAddress()) continue;
      else if (use instanceof Inet4Address) continue;
      else if (ia instanceof Inet4Address) use = ia;
    }
   
   ShoreLog.logD("NETWORK","Get address " + use + " " + port);
   
   if (use == null) return null;
   
   SocketAddress sa = new InetSocketAddress(use,port);
   
   return sa;
}
 


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



private EngineInfo setupEngine(ServiceInfo si)
{
   SocketAddress sa = getServiceSocket(si,engine_map);
   return setupEngine(sa);
}
 

private EngineInfo setupEngine(SocketAddress sa)
{
   if (sa == null) return null;
   EngineInfo ei = engine_map.get(sa);
   if (ei == null) {
      ei = new EngineInfo(sa);
      EngineInfo nei = engine_map.putIfAbsent(sa,ei);
      if (nei != null) ei = nei;
      else {
         ei.sendQueryAboutMessage();
         ei.sendQueryStateMessage();
       }
    }
   
   return ei;
}


private String getMacAddress(SocketAddress sa)
{
   // this code doesn't work. Need to use arp <address> which is not reliable'
// if (sa instanceof InetSocketAddress) {
//    InetSocketAddress inet = (InetSocketAddress) sa;
//    InetAddress iadd = inet.getAddress();
//    try {
//       NetworkInterface ni = NetworkInterface.getByInetAddress(iadd);
//       if (ni == null) return null;
//       byte [] mac = ni.getHardwareAddress();
//       StringBuffer buf = new StringBuffer();
//       for (int i = 0; i < mac.length; ++i) {
//          if (i > 0) buf.append("-");
//          int v = mac[i] & 0xff;
//          String s = Integer.toString(v,16);
//          if (s.length() == 1) buf.append("0");
//          buf.append(s);
//        }
//       return buf.toString();
//     }
//    catch (Throwable e) {
//       ShoreLog.logD("NETWORK","Problem getting mac access: " + e);
//     }
//  }
   
   return null;
}

private class ControllerInfo {
   
   private byte controller_id;
   private SocketAddress net_address;
   private long last_heartbeat;
   
   ControllerInfo(SocketAddress net) {
      net_address = net;
      controller_id = -1;
      last_heartbeat = 0;
      String mac = getMacAddress(net);
      if (mac != null) {
         ShoreLog.logD("NETWORK","Controller mac address: " + mac);
       }
    }
   
   SocketAddress getSocketAddress()                     { return net_address; }
   
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
         for (IfaceSensor sen : layout_model.getSensors()) {
            if (sen.getTowerId() == id) {
               sen.setSensorState(ShoreSensorState.UNKNOWN);
             }
            // possibly reset switches and signals as well
          }
         sendSyncMessage();
       }
    }
   
   synchronized boolean checkHeartbeat(long now) {
      if (last_heartbeat == 0 || controller_id < 0) {
         return false;
       }
      if (now - last_heartbeat > HEARTBEAT_TIME) { 
         int val = (int) controller_id;
         id_map.remove(val);
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
   
}       // end of inner class ControllerInfo



private class EngineInfo {

   private SocketAddress net_address;
   
   EngineInfo(SocketAddress net) {
      net_address = net;
      String mac = getMacAddress(net);
      if (mac != null) {
         ShoreLog.logD("NETWORK","Found engine with mac address " + mac);
       }
    }
   
// SocketAddress getSocketAddress()                     { return net_address; }
   
   void sendQueryStateMessage() {
      byte [] msg = LOCOFI_QUERY_LOCO_STATE_CMD;
      sendMessage(net_address,msg,0,msg.length);
    }
   
   void sendQueryAboutMessage() {
      byte [] msg = LOCOFI_QUERY_ABOUT_LOCO_CMD;
      sendMessage(net_address,msg,0,msg.length);
    }
   
   void sendEmergencyStop() {
      byte [] msg = LOCOFI_EMERGENCY_STOP_CMD;
      sendMessage(net_address,msg,0,msg.length);
    }
   
   void sendEmergencyStart() {
      byte [] msg = LOCOFI_EMERGENCY_START_CMD;
      sendMessage(net_address,msg,0,msg.length);
    }
   
   void sendStopEngine() {
      byte [] msg = LOCOFI_STOP_ENGINE_CMD;
      sendMessage(net_address,msg,0,msg.length);
    }
   
   void sendStartEngine() {
      byte [] msg = LOCOFI_START_ENGINE_CMD;
      sendMessage(net_address,msg,0,msg.length);
    }
   
   
}       // end of inner class EngineInfo




/********************************************************************************/
/*										*/
/*	Handle mDNS service discovery						*/
/*										*/
/********************************************************************************/

public class ServiceFinder implements ServiceListener, ServiceTypeListener {
   
   private String finder_id;
   
   ServiceFinder(String id) {
      finder_id = id;
    }

   @Override public void serviceAdded(ServiceEvent event) {
      ShoreLog.logI("NETWORK","Service added: " + finder_id + "> " + event.getInfo().getName());
//    broadcastInfo();
    }

   @Override public void serviceRemoved(ServiceEvent event) {
      ShoreLog.logI("NETWORK","Service removed: " + finder_id + event.getInfo().getName());
    }

   @Override public void serviceResolved(ServiceEvent event) {
      ShoreLog.logI("NETWORK","Service resolved: " + finder_id + "> " + event.getInfo());
      ServiceInfo si = event.getInfo();
      String nm = si.getName();
      if (nm.startsWith("controller") || nm.startsWith("tower")) {
         ShoreLog.logD("NETWORK","Found controller: " + finder_id + "> " + nm);
         findController(si);
       }    
      else if (nm.equals("loco") || nm.equals("consist")) {
         setupEngine(si);
       }
      else if (nm.equals("engineer")) {
         // handle engineer
       }
      else {
         ShoreLog.logI("Unknown service entry " + nm);
       }
    }
   
   @Override public void serviceTypeAdded(ServiceEvent event) {
      ShoreLog.logI("NETWORK","Service type added: " + event);
    }
   
   @Override public void subTypeForServiceTypeAdded(ServiceEvent event) {
      ShoreLog.logI("NETWORK","Service added: " + event);
    }
   
}	// end of inner class ServiceFinder











}	// end of class NetworkMonitor



/* end of NetworkMonitor.java */

