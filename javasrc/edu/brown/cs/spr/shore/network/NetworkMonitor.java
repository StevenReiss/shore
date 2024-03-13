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

import edu.brown.cs.spr.shore.iface.IfaceSwitch;
import edu.brown.cs.spr.shore.model.ModelConstants.ModelSwitch;
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

   ShoreLog.logD("NETWORK","Monitor setup " + can_broadcast + " " + our_socket);
}



/********************************************************************************/
/*										*/
/*	Start monitoring							*/
/*										*/
/********************************************************************************/

void start()
{
   ReaderThread rt = new ReaderThread();
   rt.start();
}



/********************************************************************************/
/*                                                                              */
/*      Top-level message requests                                              */
/*                                                                              */
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
   catch (IOException e) {
      ShoreLog.logE("Problem sending packet " + who + " " + port,e);
    }
}

/********************************************************************************/
/*										*/
/*	Handle incoming messages						*/
/*										*/
/********************************************************************************/

private void handleMessage(DatagramPacket msg)
{
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




/********************************************************************************/
/*										*/
/*	Server thread								*/
/*										*/
/********************************************************************************/

private class ReaderThread extends Thread {

   ReaderThread() {
      super("UDP_READER_" + our_socket.getLocalPort());
    }

   @Override public void run() {
      byte [] buf = new byte[BUFFER_SIZE];
      DatagramPacket packet = new DatagramPacket(buf,buf.length);
      while (our_socket != null) {
	 try {
	    our_socket.receive(packet);
	    handleMessage(packet);
	  }
	 catch (SocketTimeoutException e) { }
	 catch (IOException e) {
	    ShoreLog.logE("Problem reading UDP",e);
	    // possibly recreate our_socket or set to null
	  }
       }
    }
}



}	// end of class NetworkMonitor




/* end of NetworkMonitor.java */

