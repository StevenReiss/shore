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

import edu.brown.cs.spr.shore.iface.IfaceSwitch;
import edu.brown.cs.spr.shore.shore.ShoreLog;

public class NetworkMonitor implements NetworkConstants, NetworkControlMessages,
      NetworkLocoFiMessages
{



/********************************************************************************/
/*										*/
/*	Main program (for testing/standalone use)				*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   ShoreLog.setup();

   NetworkMonitor mon = new NetworkMonitor();
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

private Map<SocketAddress,ControllerInfo>  controller_map;
private Map<Integer,ControllerInfo>        id_map;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NetworkMonitor()
{
   controller_map = new ConcurrentHashMap<>();
   id_map = new ConcurrentHashMap<>();
   
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
      multi_group = new InetSocketAddress(mcastaddr,6879);
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

void start()
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

public void sendSetSwitch(IfaceSwitch sw,IfaceSwitch.SwitchSetting set)
{
   if (sw == null) return;
// byte id = sw.getControllerId();
// byte idx = sw.getControllerSwitch();
   // get InetAddress/port for the controller
// byte msg [] = { CONTROL_SETSWTICH, id, idx, (byte) set.ordinal() };
   // sendMessage(who,port,msg,0,4);
   // should be done by the Controler, not here

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
      try {
         DatagramPacket dp = new DatagramPacket(msg,len,multi_group);
         multi_socket.send(dp);
         ShoreLog.logD("NETWORK","MULTI SEND");
       }
      catch (IOException e) {
         ShoreLog.logE("NETWORK","Problem with multicast: ",e);
       }
      for (ControllerInfo ci : controller_map.values()) {
         sendMessage(ci.getSocketAddress(),msg,off,len);
       }
      return;
    }

   DatagramPacket packet = new DatagramPacket(msg,off,len,who);
   try {
      our_socket.send(packet);
    }
   catch (Throwable e) {
      ShoreLog.logE("Problem sending packet " + who,e);
    }
}



/********************************************************************************/
/*										*/
/*	Handle incoming messages						*/
/*										*/
/********************************************************************************/

private class NotificationHandler implements MessageHandler {

   @Override public void handleMessage(DatagramPacket msg) {
      StringBuffer buf = new StringBuffer();
      byte [] data = msg.getData();
      for (int i = 0; i < msg.getLength(); ++i) {
         int v = data[i + msg.getOffset()];
         String vs = Integer.toHexString(v);
         if (vs.length() == 1) vs = "0" + vs;
         buf.append(vs);
         buf.append(" ");
       }
   
      ShoreLog.logD("NETWORK","Received from " + msg.getAddress() + " " +
            msg.getPort() + " " + msg.getLength() + " " + msg.getOffset() + ": " +
            buf.toString());
   
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
            // find sensor in model for this controller/sensor #
            // set model sensor state
            break;
         case CONTROL_SWITCH :
            // find switch in model for this controller/switch #
            // set switch state
            break;
         case CONTROL_SIGNAL :
            // find signal in model for this controller/signal #
            // set signal state
            break;
         case CONTROL_ENDSYNC :
            ci.endSync();
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
   InetAddress [] possibles = si.getInet4Addresses();
   if (possibles == null || possibles.length == 0) {
      possibles = si.getInetAddresses();
    }
   int port = si.getPort();
   
   InetAddress use = null;
   for (InetAddress ia : possibles) {
      SocketAddress sa = new InetSocketAddress(ia,port);
      ControllerInfo ci = controller_map.get(sa);
      if (ci != null) return ci;
      if (use == null) use = ia;
      else if (use.isLoopbackAddress()) continue;
      else if (use.isAnyLocalAddress()) continue;
      else if (use instanceof Inet4Address) continue;
      else if (ia instanceof Inet4Address) use = ia;
    }
   
   if (use == null) return null;
   
   SocketAddress sa = new InetSocketAddress(use,port);
   return setupController(sa);
}
 


private ControllerInfo setupController(SocketAddress sa)
{
   ControllerInfo ci = controller_map.get(sa);
   if (ci == null) {
      ci = new ControllerInfo(sa);
      ControllerInfo nci = controller_map.putIfAbsent(sa,ci);
      if (nci != null) ci = null;
      else ci.sendSyncMessage();
    }
   
   return ci;
}



private class ControllerInfo {
   
   private byte controller_id;
   private SocketAddress net_address;
   
   ControllerInfo(SocketAddress net) {
      net_address = net;
      controller_id = -1;
    }
   
   SocketAddress getSocketAddress()                     { return net_address; }
   
   void setId(int id)                                   { controller_id = (byte) id; }
   
   void sendSyncMessage() {
      byte msg [] = { CONTROL_SYNC, MESSAGE_ALL, 0, 0 };
      sendMessage(net_address,msg,0,4);
    }
   
   void endSync() {
      byte msg [] = { CONTROL_HEARTBEAT, controller_id, 1, 0 };
      sendMessage(net_address,msg,0,4);
      // send DEFSENSOR messages
      // send SETSIGNAL and SETSWITCH messages if appropriate
    }

   
}       // end of inner class ControllerInfo



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
      ShoreLog.logI("NETWORK","Service added: " + finder_id + "> " + event.getInfo());
      broadcastInfo();
    }

   @Override public void serviceRemoved(ServiceEvent event) {
      ShoreLog.logI("NETWORK","Service removed: " + finder_id + "> " + event.getInfo());
    }

   @Override public void serviceResolved(ServiceEvent event) {
      ShoreLog.logI("NETWORK","Service resolved: " + event.getInfo());
      ServiceInfo si = event.getInfo();
      String nm = si.getName();
      if (nm.startsWith("controller") || nm.startsWith("tower")) {
         ShoreLog.logD("NETWORK","Found controller: " + finder_id + "> " + si);
         setupController(si);
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

