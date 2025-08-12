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
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

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
import edu.brown.cs.spr.shore.iface.IfaceTrains;
import edu.brown.cs.spr.shore.iface.IfaceEngine;
import edu.brown.cs.spr.shore.shore.ShoreLog;

public class NetworkMonitor implements NetworkConstants, NetworkControlMessages,
      NetworkLocoFiMessages, IfaceNetwork 
{ 


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private DatagramSocket	our_socket;
private DatagramSocket  alt_socket;
private DatagramSocket  speed_socket;
private DatagramSocket  rpm_socket;

private NetworkProcessorTower tower_processor;
private NetworkProcessorLocoFi locofi_processor;

private static boolean dummy_loco = false;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public NetworkMonitor(IfaceModel model,IfaceTrains trains)
{
   if (our_socket != null) {
      try {
         our_socket.close();
       }
      catch (Throwable e) { }
    }
   our_socket = null;
   if (alt_socket != null) {
      try {
         alt_socket.close();
       }
      catch (Throwable e) { }
    }
   alt_socket = null;
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
      
//    alt_socket = new DatagramSocket(ALT_PORT,useaddr);
      alt_socket = new DatagramSocket(0,useaddr);
      alt_socket.setReceiveBufferSize(BUFFER_SIZE);
      alt_socket.setReuseAddress(true);
      alt_socket.setSendBufferSize(BUFFER_SIZE);
      alt_socket.setSoTimeout(0);
      
      speed_socket = new DatagramSocket(0,useaddr);
      speed_socket.setReceiveBufferSize(BUFFER_SIZE);
      speed_socket.setReuseAddress(true);
      speed_socket.setSendBufferSize(BUFFER_SIZE);
      speed_socket.setSoTimeout(0);
      
      rpm_socket = new DatagramSocket(0,useaddr);
      rpm_socket.setReceiveBufferSize(BUFFER_SIZE);
      rpm_socket.setReuseAddress(true);
      rpm_socket.setSendBufferSize(BUFFER_SIZE);
      rpm_socket.setSoTimeout(0);
    }
   catch (IOException e) {
      ShoreLog.logE("NETWORK","Can't create Datagram Socket",e);
      System.err.println("Problem with network connection: " + e);
      System.exit(1);
    }
   
   ShoreLog.logD("NETWORK","Listening for datagrams on " + useaddr + " " +
         our_socket.getLocalPort() + " " + alt_socket.getLocalPort() + " " +
         speed_socket.getLocalPort());
   
   tower_processor = new NetworkProcessorTower(our_socket,model);
   locofi_processor = new NetworkProcessorLocoFi(alt_socket,speed_socket,
         rpm_socket,trains); 
   
   try {
      JmmDNS jmm = JmmDNS.Factory.getInstance(); 
//    jmm.addServiceTypeListener(new ServiceFinder("TYPE"));
      jmm.addServiceListener("_udp._udp.local.",new ServiceFinder("UDP"));
      jmm.addServiceListener("_loco._udp.local.",new ServiceFinder("LOCO"));
      jmm.registerServiceType("_master._udp.local");
      jmm.registerServiceType("_tower._udp.local.");
      jmm.registerServiceType("master._udp.local");
      jmm.registerServiceType("tower._udp.local.");
      jmm.registerServiceType("_loco._udp.local");
      jmm.registerServiceType("_engineer._udp.local");
      jmm.registerServiceType("_consist._udp.local");
      jmm.registerServiceType("loco._udp.local");
      jmm.registerServiceType("engineer._udp.local");
      jmm.registerServiceType("consist._udp.local.");
      jmm.registerServiceType("udp._udp.local.");
      ServiceInfo info = ServiceInfo.create("master._udp.local.","shore",
            UDP_PORT,"SHORE controller");
      ServiceInfo info1 = ServiceInfo.create("engineer._udp.local.","engineer",
            ALT_PORT,"SHORE engineer");
      jmm.registerService(info);
      jmm.registerService(info1);
      if (dummy_loco) {
         ServiceInfo info2 = ServiceInfo.create("loco._udp.local.","engineer",
               ALT_PORT,"SHORE dummy loco");
         jmm.registerService(info2);
       }
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
  
   String s1 = ni.getName().toLowerCase();
   String s2 = ni.getDisplayName().toLowerCase();
   if (s2.startsWith("wlo") || s1.startsWith("wlo")) return true;               // linux
   
   return true;
}




/********************************************************************************/
/*										*/
/*	Start monitoring							*/
/*										*/
/********************************************************************************/

public void start()
{
   tower_processor.start();
   locofi_processor.start();
   
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
   tower_processor.setSwitch(sw,set);
}


@Override
public void setSignal(IfaceSignal sig,ShoreSignalState set) 
{
   tower_processor.setSignal(sig,set);
}


@Override
public void setSensor(IfaceSensor sen,ShoreSensorState set)  
{
   tower_processor.setSensor(sen,set);
}


/********************************************************************************/
/*                                                                              */
/*      Locomotive (engine/train) messages                                      */
/*                                                                              */
/********************************************************************************/

@Override 
public void sendEmergencyStop(IfaceEngine tr,boolean stop)  
{
   locofi_processor.sendEmergencyStop(tr,stop); 
}


@Override public void sendLight(IfaceEngine eng,boolean front,boolean on)  
{
   locofi_processor.sendLight(eng,front,on);
}


@Override public void sendBell(IfaceEngine eng,boolean on) 
{
   locofi_processor.sendBell(eng,on);
} 


@Override public void sendHorn(IfaceEngine eng) 
{
   locofi_processor.sendHorn(eng); 
}


@Override public void sendMute(IfaceEngine eng,boolean mute) 
{
   locofi_processor.sendMute(eng,mute); 
}


@Override public void sendThrottle(IfaceEngine eng,double v) 
{
   locofi_processor.sendThrottle(eng,v); 
}


@Override public void sendReverse(IfaceEngine eng,boolean rev) 
{
   locofi_processor.sendReverse(eng,rev);
}


@Override public void sendStartStopEngine(IfaceEngine eng,boolean start)
{
   locofi_processor.sendStartStopEngine(eng,start);   
}


@Override public void sendReboot(IfaceEngine eng)
{
   locofi_processor.sendReboot(eng); 
}


/********************************************************************************/
/*										*/
/*	Handle Send requests							*/
/*										*/
/********************************************************************************/

// public synchronized void sendMessage(SocketAddress who,byte [] msg,int off,int len)
// {
// if (our_socket == null) return;
// 
// if (who == null) {
//    for (ControllerInfo ci : controller_map.values()) {
//       sendMessage(ci.getSocketAddress(),msg,off,len);
//     }
//    return;
//  }
// 
// String msgtxt = decodeMessage(msg,off,len);
// ShoreLog.logD("NETWORK","Send " + msgtxt + " >> " + who);
// 
// DatagramPacket packet = new DatagramPacket(msg,off,len,who);
// try {
//    our_socket.send(packet);
//  }
// catch (Throwable e) {
//    ShoreLog.logE("Problem sending packet " + who,e);
//  }
// }
// 
// 



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
    }

   @Override public void serviceRemoved(ServiceEvent event) {
      ShoreLog.logI("NETWORK","Service removed: " + finder_id + event.getInfo().getName());
    }

   @Override public void serviceResolved(ServiceEvent event) {
      ServiceInfo si = event.getInfo();
      try {
         tower_processor.handleServiceResolved(si);
         locofi_processor.handleServiceResolved(si);
       }
      catch (Throwable t) {
         ShoreLog.logE("NETWORK","Problem resolving service",t);
       }
      ShoreLog.logI("NETWORK","Service resolved: " + finder_id + "> " + event.getInfo() + " " +
            si.getName());
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

