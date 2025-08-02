/********************************************************************************/
/*                                                                              */
/*              NetworkProcessor.java                                           */
/*                                                                              */
/*      Superclass for tower and locofi processors                              */
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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.Map;

import javax.jmdns.ServiceInfo;

import edu.brown.cs.spr.shore.shore.ShoreLog;

abstract class NetworkProcessor implements NetworkConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

protected DatagramSocket        our_socket;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected NetworkProcessor(DatagramSocket sock)
{
   our_socket = sock;
}


/********************************************************************************/
/*                                                                              */
/*      Start methods                                                           */
/*                                                                              */
/********************************************************************************/

void start()
{
   ReaderThread rt = new ReaderThread(our_socket);
   rt.start();
   Thread upd = getStatusUpdater();
   if (upd != null) upd.start();
}


protected abstract void handleMessage(DatagramPacket msg);
protected abstract Thread getStatusUpdater();
protected abstract void handleServiceResolved(ServiceInfo si);


/********************************************************************************/
/*                                                                              */
/*      Messaging                                                               */
/*                                                                              */
/********************************************************************************/

protected synchronized void sendMessage(SocketAddress who,byte [] msg,int off,int len)
{
   if (our_socket == null) return;
   
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


@SuppressWarnings("unchecked")
protected <T extends Enum<T>> T getState(int v,T dflt)
{
   for (Object x : dflt.getClass().getEnumConstants()) {
      Enum<?> e = (Enum<?>) x;
      if (e.ordinal() == v) {
         return (T) e;
       }
    }
   
   return dflt;
}


protected static String decodeMessage(byte [] msg,int off,int len)
{
   StringBuffer buf = new StringBuffer();
   for (int i = 0; i <len; ++i) {
      int v = msg[off+i] & 0xff;
      String vs = Integer.toHexString(v);
      if (vs.length() == 1) vs = "0" + vs;
      buf.append(vs);
      buf.append(" ");
    }
   return buf.toString();
}



protected SocketAddress getServiceSocket(ServiceInfo si,Map<?,?> known)
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


/********************************************************************************/
/*                                                                              */
/*      Reader Thread                                                           */
/*                                                                              */
/********************************************************************************/

private class ReaderThread extends Thread {

   private DatagramSocket reader_socket;
   
   ReaderThread(DatagramSocket s) {
      super("UDP_READER_" + s.getLocalPort());
      reader_socket = s;
    }
   
   @Override public void run() {
      byte [] buf = new byte[BUFFER_SIZE];
      DatagramPacket packet = new DatagramPacket(buf,buf.length);
      while (reader_socket != null) {
         try {
            reader_socket.receive(packet);
            handleMessage(packet);
          }
         catch (SocketTimeoutException e) { }
         catch (IOException e) {
            ShoreLog.logE("Problem reading UDP",e);
            // possibly recreate our_socket or set to null
          }
         catch (Throwable t) {
            ShoreLog.logE("Problem processing message",t);
          }
         //    ShoreLog.logD("NETWORK","FINISH PACKET");
       }
    }

}	// end of inner class ReaderThread



/********************************************************************************/
/*                                                                              */
/*      Status updater -- sync our status with back end                         */
/*                                                                              */
/********************************************************************************/

protected abstract class StatusUpdater extends Thread {
   
   StatusUpdater() {
      super("ShoreStatusUpdater");
      setDaemon(true);
    }
   
}       // end of inner class StatusUpdater

}       // end of class NetworkProcessor




/* end of NetworkProcessor.java */

