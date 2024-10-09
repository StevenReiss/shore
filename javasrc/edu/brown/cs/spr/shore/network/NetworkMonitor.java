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
      for (int i = 10; i >= 0 ; --i) {
         NetworkInterface ni = NetworkInterface.getByIndex(i);
         if (ni == null) continue;
         if (ni.isLoopback()) continue;
         if (ni.isVirtual()) continue;
         netif = ni;
         break;
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
      InetAddress mcastaddr = InetAddress.getByName("239.1.2.3");
      multi_group = null;               // Don't try to multicast for now
//    multi_group = new InetSocketAddress(mcastaddr,6879);
      multi_socket = new MulticastSocket(6879);
      multi_socket.joinGroup(new InetSocketAddress(mcastaddr,0),netif);
    }
   catch (IOException e) {
      ShoreLog.logE("NETWORK","Can't create Datagram Socket",e);
      System.err.println("Problem with network connection: " + e);
      System.exit(1);
    }
   
   try {
      JmmDNS jmm = JmmDNS.Factory.getInstance();
//    String [] names = jmm.getNames();
//    JmDNS jm1 = JmDNS.create(useaddr);
//    jmm.addServiceListener("._udp.local.", new ServiceFinder("0"));
//    jmm.addServiceListener("_udp.local.", new ServiceFinder("1")); 
      jmm.addServiceListener("_udp._udp.local.",new ServiceFinder("2"));
//    jm1.addServiceListener("._udp.local.", new ServiceFinder("3"));
//    jm1.addServiceListener("_udp.local.", new ServiceFinder("4")); 
//    jm1.addServiceListener("_udp._udp.local.",new ServiceFinder("5"));
//    jmm.addServiceTypeListener(new ServiceFinder("T1"));
//    jm1.addServiceTypeListener(new ServiceFinder("T2"));
      jmm.registerServiceType("_master._udp.local");
      jmm.registerServiceType("_tower._udp.local.");
      jmm.registerServiceType("master._udp.local");
      jmm.registerServiceType("tower._udp.local.");
      jmm.registerServiceType("udp._udp.local.");
      
//    InetAddress [] iasd = jmm.getInetAddresses();
      
//    jmm.addServiceTypeListener(new ServiceFinder());
      ServiceInfo info = ServiceInfo.create("master._udp.local.","shore",UDP_PORT,"SHORE controller");
      jmm.registerService(info);
    }
   catch (IOException e) {
      ShoreLog.logE("NETWORK","Problem registering service",e);
    }
   
   ShoreLog.logD("NETWORK","Monitor setup  " + our_socket.getLocalAddress() +
         " " + multi_socket.getLocalAddress());
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
   ReaderThread rt1 = new ReaderThread(multi_socket,new NotificationHandler());
   rt1.start();
   
   broadcastInfo();
   
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
}
 

@Override
public void setSignal(IfaceSignal sig,ShoreSignalState set) 
{
   if (sig == null) return;
   
   int id = sig.getTowerId();
   ControllerInfo ci = id_map.get(id);
   if (ci == null) return;
   sig.setSignalState(set);
   ci.sendSignalMessage(sig.getTowerSignal(),set);
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


@Override
public void sendDefSensor(IfaceSensor sen,IfaceSwitch sw,ShoreSwitchState set)
{
   if (sen == null) return;
   int id = sen.getTowerId();
   ControllerInfo ci = id_map.get(id);
   if (ci == null) return;
   int s = 64;
   if (sw != null) {
      s = sw.getTowerSwitch() * 4 + set.ordinal();
      int idx1 = sw.getTowerRSwitch();
      if (set == ShoreSwitchState.R && idx1 >= 0) {
         s = idx1 * 4 + ShoreSwitchState.N.ordinal();
       }
    }
   ci.sendDefSensorMessage(sen.getTowerSensor(),s);
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



private void sendSetupMessages(byte controller)
{
   if (layout_model == null) return;
   
   for (IfaceSensor sen : layout_model.getSensors()) {
      if (sen.getTowerId() == controller) {
         ShoreSwitchState state = ShoreSwitchState.UNKNOWN;
         IfaceSwitch stsw = null;
         for (IfaceSwitch sw : layout_model.getSwitches()) {
            if (sw.getTowerId() == controller) {
               if (sw.getNSensor() == sen) {
                  stsw = sw;
                  state = ShoreSwitchState.N;
                  break;
                }
               else if (sw.getRSensor() == sen) {
                  stsw = null;
                  state = ShoreSwitchState.R;
                  break;
                }
             }
          }
         sendDefSensor(sen,stsw,state);
       }
    }
   
   for (IfaceSignal sig : layout_model.getSignals()) {
      if (sig.getTowerId() == controller) {
         setSignal(sig,sig.getSignalState());
       }
    }
   
   for (IfaceSwitch sw : layout_model.getSwitches()) {
      if (sw.getTowerId() == controller) {
         setSwitch(sw,sw.getSwitchState());
       }
    }
}



private void broadcastInfo()
{
   byte msg [] = { CONTROL_SYNC, MESSAGE_ALL, 0, 0 };
   sendMessage(null,msg,0,4);
}




/********************************************************************************/
/*										*/
/*	Handle Send requests							*/
/*										*/
/********************************************************************************/

public void sendMessage(SocketAddress who,byte [] msg,int off,int len)
{
   if (our_socket == null) return;
   
   if (who == null) {
      if (multi_group != null) {
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


private class NotificationHandler implements MessageHandler {

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
      ControllerInfo ci = setupController(sa);
      ControllerInfo ci1 = id_map.get(id);
      if (ci1 == null) {
         id_map.put(id,ci);
         ci.setId(id);
       }
      else if (ci != ci1) {
         ShoreLog.logE("NETWORK","Conflicing controllers for " + id);
       }
   
      switch (data[0]) {
         case CONTROL_ID :
            ci.setId(id);
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
               s.setSwitch(sst);
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
         case CONTROL_ENDSYNC :
            ci.sendEndSync();
            break;
         default :
            break;
       }
    }

}	// end of inner class Notification Handler



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
         ShoreLog.logD("NETWORK","FINISH PACKET");
       }
    }

}	// end of inner class ReaderThread



/********************************************************************************/
/*                                                                              */
/*      Controller information                                                  */
/*                                                                              */
/********************************************************************************/

private ControllerInfo setupController(ServiceInfo si)
{
   SocketAddress sa = getServiceSocket(si,controller_map);
   return setupController(sa);
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
      SocketAddress sa = new InetSocketAddress(ia,port);
      if (known != null && known.containsKey(sa)) return sa;
      if (use == null) use = ia;
      else if (use.isLoopbackAddress()) continue;
      else if (use.isAnyLocalAddress()) continue;
      else if (use instanceof Inet4Address) continue;
      else if (ia instanceof Inet4Address) use = ia;
    }
   
   if (use == null) return null;
   
   SocketAddress sa = new InetSocketAddress(use,port);
   
   return sa;
}
 


private ControllerInfo setupController(SocketAddress sa)
{
   ShoreLog.logD("NETWORK","Setup controller " + sa);
   if (sa == null) return null;
   ControllerInfo ci = controller_map.get(sa);
   if (ci == null) {
      ShoreLog.logD("NETWORK","New Controller " + sa);
      ci = new ControllerInfo(sa);
      ControllerInfo nci = controller_map.putIfAbsent(sa,ci);
      if (nci != null) ci = nci;
      else ci.sendSyncMessage();
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
   
   ControllerInfo(SocketAddress net) {
      net_address = net;
      controller_id = -1;
      String mac = getMacAddress(net);
      if (mac != null) {
         ShoreLog.logD("NETWORK","Controller mac address: " + mac);
       }
    }
   
   SocketAddress getSocketAddress()                     { return net_address; }
   
   void setId(int id)                                   { controller_id = (byte) id; }
   
   void sendSyncMessage() {
      byte msg [] = { CONTROL_SYNC, MESSAGE_ALL, 0, 0 };
      sendMessage(net_address,msg,0,4);
    }
   
   void sendEndSync() {
      byte msg [] = { CONTROL_HEARTBEAT, controller_id, 1, 0 };
      sendMessage(net_address,msg,0,4);
      sendSyncMessage();
      sendSetupMessages(controller_id);
    }
   
   void sendSwitchMessage(byte sid,IfaceSwitch.ShoreSwitchState state) {
      byte msg [] = { CONTROL_SETSWTICH, controller_id, sid,(byte) state.ordinal()};
      sendMessage(net_address,msg,0,4);
    }
   
   void sendSignalMessage(byte sid,IfaceSignal.ShoreSignalState state) {
      byte msg [] = { CONTROL_SETSIGNAL, controller_id, sid,(byte) state.ordinal()};
      sendMessage(net_address,msg,0,4);
    }
   
   
   void sendSensorMessage(byte sid,IfaceSensor.ShoreSensorState state) {
      byte msg [] = { CONTROL_SETSENSOR, controller_id, sid,(byte) state.ordinal()}; 
      sendMessage(net_address,msg,0,4);
   }
   
   void sendDefSensorMessage(byte sid,int value) {
      byte msg [] = { CONTROL_DEFSENSOR, controller_id,sid,(byte) value };
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
      byte msg [] = LOCOFI_QUERY_LOCO_STATE_CMD;
      sendMessage(net_address,msg,0,msg.length);
    }
   
   void sendQueryAboutMessage() {
      byte msg [] = LOCOFI_QUERY_ABOUT_LOCO_CMD;
      sendMessage(net_address,msg,0,msg.length);
    }
   
   void sendEmergencyStop() {
      byte msg [] = LOCOFI_EMERGENCY_STOP_CMD;
      sendMessage(net_address,msg,0,msg.length);
    }
   
   void sendEmergencyStart() {
      byte msg [] = LOCOFI_EMERGENCY_START_CMD;
      sendMessage(net_address,msg,0,msg.length);
    }
   
   void sendStopEngine() {
      byte msg [] = LOCOFI_STOP_ENGINE_CMD;
      sendMessage(net_address,msg,0,msg.length);
    }
   
   void sendStartEngine() {
      byte msg [] = LOCOFI_START_ENGINE_CMD;
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
         setupController(si);
       }    
      else if (nm.equals("loco") || nm.equals("consist")) {
         setupEngine(si);
       }
      else if (nm.equals("engineer")) {
         
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

