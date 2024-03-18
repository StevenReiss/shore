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
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import javax.jmdns.JmDNS;
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
private boolean can_broadcast;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NetworkMonitor()
{
   our_socket = null;
   can_broadcast = false;

   try {
      our_socket = new DatagramSocket(UDP_PORT);
      our_socket.setReceiveBufferSize(BUFFER_SIZE);
      our_socket.setReuseAddress(true);
      our_socket.setSendBufferSize(BUFFER_SIZE);
      our_socket.setSoTimeout(0);
    }
   catch (IOException e) {
      ShoreLog.logE("NETWORK","Can't create Datagram Socket",e);
      System.err.println("Problem with network connection: " + e);
      System.exit(1);
    }

   try {
      our_socket.setBroadcast(true);
      can_broadcast = our_socket.getBroadcast();
    }
   catch (SocketException e) { }

   try {
      JmmDNS jmm = JmmDNS.Factory.getInstance();
      String [] names = jmm.getNames();
      jmm.addServiceListener("_controller._udp.local.", new ServiceFinder());
      jmm.addServiceListener("_controller._udp.", new ServiceFinder()); 
      jmm.addServiceListener("controller._udp.local.", new ServiceFinder()); 
      jmm.addServiceListener("udp._udp.local.", new ServiceFinder()); 
      jmm.addServiceTypeListener(new ServiceFinder());
      jmm.registerServiceType("_master._udp.local.");
      jmm.registerServiceType("_controller_udp.local.");
      ServiceInfo info = ServiceInfo.create("_master._udp.local","shore",UDP_PORT,"SHORE controller");
      jmm.registerService(info);
      ServiceInfo [] allinfo = jmm.list("_controller._udp.local.");
      ServiceInfo [] allinfo1 = jmm.list("_controller._udp.");
      ServiceInfo [] allinfo2 = jmm.list("_udp.local.");
      ServiceInfo [] allinfo3 = jmm.getServiceInfos("udp.local.","controller");
      ServiceInfo [] allinfo4 = jmm.getServiceInfos("_udp.local.","controller");
      ServiceInfo [] allinfo5 = jmm.getServiceInfos("udp.local.","_controller");
      ServiceInfo [] allinfo6 = jmm.getServiceInfos("_udp.local.","_controller");
      
      ShoreLog.logD("NETWORK","SERVICES " + allinfo.length); 
    }
   catch (IOException e) {
      ShoreLog.logE("NETWORK","Problem registering service",e);
    }
   
   broadcastInfo();

   ShoreLog.logD("NETWORK","Monitor setup " + can_broadcast + " " + our_socket);
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
}



/********************************************************************************/
/*										*/
/*	Top-level message requests						*/
/*										*/
/********************************************************************************/

public void sendSetSwitch(IfaceSwitch sw,IfaceSwitch.SwitchSetting set)
{
   if (sw == null) return;
   byte id = sw.getControllerId();
   byte idx = sw.getControllerSwitch();
   // get InetAddress/port for the controller
   byte msg [] = { CONTROL_SETSWTICH, id, idx, (byte) set.ordinal() };
   // sendMessage(who,port,msg,0,4);

}


private void broadcastInfo()
{
   byte msg [] = { CONTROL_INFO, MESSAGE_ALL, 0, 0 };
   sendMessage(null,UDP_PORT,msg,0,4);
}

/********************************************************************************/
/*										*/
/*	Handle Send requests							*/
/*										*/
/********************************************************************************/

public void sendMessage(InetAddress who,int port,byte [] msg,int off,int len)
{
   if (our_socket == null) return;

   DatagramPacket packet = new DatagramPacket(msg,off,len,
	 who,port);
   try {
      our_socket.send(packet);
    }
   catch (Throwable e) {
      ShoreLog.logE("Problem sending packet " + who + " " + port,e);
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

      int which = data[1];
      int id = data[2];
      int value = data[3];

      switch (data[0]) {
	 case CONTROL_ID :
	    // note which is at address/port
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
       }
    }

}	// end of inner class ReaderThread



/********************************************************************************/
/*										*/
/*	Handle mDNS service discovery						*/
/*										*/
/********************************************************************************/

public class ServiceFinder implements ServiceListener, ServiceTypeListener {

   @Override public void serviceAdded(ServiceEvent event) {
      ShoreLog.logI("NETWORK","Service added: " + event.getInfo());
    }

   @Override public void serviceRemoved(ServiceEvent event) {
      ShoreLog.logI("NETWORK","Service removed: " + event.getInfo());
    }

   @Override public void serviceResolved(ServiceEvent event) {
      ShoreLog.logI("NETWORK","Service resolved: " + event.getInfo());
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

