/********************************************************************************/
/*                                                                              */
/*              NetworkProcessorLocoFi.java                                     */
/*                                                                              */
/*      Processor for LocoFi messages                                           */
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

import edu.brown.cs.spr.shore.iface.IfaceEngine;
import edu.brown.cs.spr.shore.iface.IfaceTrains;
import edu.brown.cs.spr.shore.shore.ShoreLog;

class NetworkProcessorLocoFi extends NetworkProcessor implements NetworkLocoFiMessages
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceTrains     engine_model;
private Map<SocketAddress,EngineInfo> engine_map;
private Map<SocketAddress,ReplyHandler> reply_map;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

NetworkProcessorLocoFi(DatagramSocket sock,IfaceTrains trains)
{
   super(sock);
   engine_model = trains;
   engine_map = new ConcurrentHashMap<>();
   reply_map = new ConcurrentHashMap<>();
}



/********************************************************************************/
/*                                                                              */
/*      Top level message requests                                              */
/*                                                                              */
/********************************************************************************/

void sendLight(IfaceEngine eng,boolean front) 
{
   EngineInfo ei = findEngineInfo(eng);
   if (ei == null) return;
   
   boolean sts = (front ? eng.isFrontLightOn() : eng.isRearLightOn());
   
   ei.sendLight(front,sts);
}


void sendMute(IfaceEngine eng)
{
   EngineInfo ei = findEngineInfo(eng);
   if (ei == null) return;
   
   ei.setMute(eng.isMuted());
}


void sendBell(IfaceEngine eng)
{
   EngineInfo ei = findEngineInfo(eng);
   if (ei == null) return;
   
   ei.sendBell(eng.isBellOn());
}


void sendHorn(IfaceEngine eng)
{
   EngineInfo ei = findEngineInfo(eng);
   if (ei == null) return;
    
   ei.sendHorn(eng.isHornOn());
}


void sendThrottle(IfaceEngine eng)
{
   EngineInfo ei = findEngineInfo(eng);
   if (ei == null) return;
   
   double v = eng.getThrottle();
   int vint = (int) v;
   ei.sendThrottle(vint);
}

void sendReverse(IfaceEngine eng)
{
   EngineInfo ei = findEngineInfo(eng);
   if (ei == null) return;
   
   ei.sendReverse(eng.isReverse());
}


void sendStartEngine(IfaceEngine eng)
{
   EngineInfo ei = findEngineInfo(eng);
   if (ei == null) return;
   
   boolean start = false;
   switch (eng.getEngineState()) {
      case STARTUP :
         start = true;
         break;
      case SHUTDOWN :
         start = false;
         break;
      default :
         return;
    }
   
   ei.sendStartEngine(start);
}


void sendEmergencyStop(IfaceEngine eng)
{
   EngineInfo ei = findEngineInfo(eng);
   if (ei == null) return;
   
   ei.sendEmergencyStop(eng.isEmergencyStopped());
}



/********************************************************************************/
/*                                                                              */
/*      Engines query methods                                                   */
/*                                                                              */
/********************************************************************************/

private IfaceEngine findEngine(String nameid)
{
   if (nameid == null) return null;
   
   for (IfaceEngine eng : engine_model.getAllEngines()) {
      if (nameid.equals(eng.getEngineId())) return eng;
      if (nameid.equals(eng.getEngineName())) return eng;  
    }
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Engine controller maintenance                                           */
/*                                                                              */
/********************************************************************************/

private EngineInfo findEngineInfo(IfaceEngine eng)
{
   for (EngineInfo ei : engine_map.values()) {
      if (eng.getEngineId() != null && eng.getEngineId().equals(ei.getEngineId())) {
         return ei;
       }
      if (eng.getEngineAddress() != null && 
            eng.getEngineAddress().equals(ei.getSocketAddress())) {
         return ei;
       }
    }
   return null;
}




@Override protected void handleServiceResolved(ServiceInfo si)
{
   String nm = si.getName().toLowerCase();
   if (nm.startsWith("loco") || nm.equals("consist")) {
      ShoreLog.logD("NETWORK","Found engine: "+ nm);
      SocketAddress sa = getServiceSocket(si,engine_map);
      setupEngine(sa);
    }
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
         ShoreLog.logD("NETWORK","New engine " + ei.getEngineId());
         ei.sendQueryAboutMessage();
         IfaceEngine eng = findEngine(ei.getEngineId());
         engine_model.setEngineSocket(eng,sa);
         ei.sendQueryStateMessage();
         ei.sendQueryVersionMessage();
         ei.sendHeartbeatMessage(true);
         ei.sendQuerySettingsMessage();
       }
    }
   
   return ei;
}



/********************************************************************************/
/*                                                                              */
/*      Process incoming messages                                               */
/*                                                                              */
/********************************************************************************/

private synchronized byte [] sendReplyMessage(SocketAddress who,byte[] msg,int off,int len)
{
   ReplyHandler rh = new ReplyHandler();
   reply_map.put(who,rh);
   sendMessage(who,msg,off,len);
   return rh.getReply();
}



protected void handleMessage(DatagramPacket msg)
{
   String msgtxt = decodeMessage(msg.getData(),msg.getOffset(),msg.getLength());
   ShoreLog.logD("NETWORK","Received from " + msg.getAddress() + " " +
         msg.getPort() + " " + msg.getLength() + " " + msg.getOffset() + ": " +
         msgtxt);
   
   SocketAddress sa = msg.getSocketAddress();
   ReplyHandler rh = reply_map.remove(sa);
   if (rh != null) {
      byte [] rslt = new byte[msg.getLength()];
      System.arraycopy(msg.getData(),msg.getOffset(),rslt,0,msg.getLength());
      rh.handleReply(rslt);
    }
   else {
      ShoreLog.logD("NETWORK","Unsolicited message from engine");
      // if we get here, we should spawn a thread to handle the request
    }
}


/********************************************************************************/
/*                                                                              */
/*      Handle periodic status updates                                         */
/*                                                                              */
/********************************************************************************/

@Override protected Thread getStatusUpdater()
{
   return new LocoFiStatusUpdater();
}


private final class LocoFiStatusUpdater extends Thread {

   LocoFiStatusUpdater() {
      super("ShoreLocoFiStatusUpdater");
      setDaemon(true);
    }
   
   @Override public void run() {
      for ( ; ; ) {
         for (EngineInfo ei : engine_map.values()) {
            if (ei.sendQueryStateMessage()) {
               delay();
             }
          }
         finalDelay();
       }
    }
   
   private void delay() {
//    checkHeartbeat();
      try {
         Thread.sleep(LOCOFI_STATUS_DELAY);  
       }
      catch (InterruptedException e) { }
    }
   
   private void finalDelay() {
      for (int i = 0; i < LOCOFI_FINAL_DELAY; ++i) {
         delay();
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Information for each connected engine                                   */
/*                                                                              */
/********************************************************************************/

private class EngineInfo {
   
   private SocketAddress net_address;
   private String engine_id;
   
   EngineInfo(SocketAddress net) {
      net_address = net;
      engine_id = null;
    }
   
   String getEngineId()                                 { return engine_id; }
   
   SocketAddress getSocketAddress()                     { return net_address; }
   
   boolean sendQueryStateMessage() {
      byte [] msg = LOCOFI_QUERY_LOCO_STATE_CMD;
      byte [] data = sendReplyMessage(net_address,msg,0,msg.length);
      if (data == null) {
         // engine is dead?
         return false;
       }
      IfaceEngine eng = findEngine(engine_id);
      if (eng == null) {
         return false;
       }
      
      boolean front = data[0] != 0;
      boolean back = data[1] != 0;
      boolean bell = data[2] != 0;
      // eng.setBell(bell);
      boolean rev = data[3] != 0;
      int sts = data[4];
      int speedstep = data[5] & 0xff + data[6]*16;
      int rpmstep = data[6] & 0xff + data[7]*16;
      int speed = data[10] & 0xff + data[9]*16;
      boolean estop = data[11] != 0;
      boolean mute = data[12] == 0;
      eng.setupEngine(front,back,bell,rev,sts,
            speedstep,rpmstep,speed,estop,mute);  
      return true;
    }
   
   boolean sendQueryAboutMessage() {
      byte [] msg = LOCOFI_QUERY_ABOUT_LOCO_CMD;
      byte [] data = sendReplyMessage(net_address,msg,0,msg.length);
      if (data == null) {
         return false;
       }
      String id = new String(data,0,7);
      id = id.replace("\0","");
      id = id.toUpperCase();
      engine_id = id;
      return true;
    }
   
   boolean sendQueryVersionMessage() {
      byte [] msg = LOCOFI_VERSION_CMD;
      byte [] data = sendReplyMessage(net_address,msg,0,msg.length);
      return data != null;
    }
   
   boolean sendQuerySettingsMessage() {
      byte [] msg = LOCOFI_SETTINGS_READ_CMD;
      byte [] data = sendReplyMessage(net_address,msg,0,msg.length);
      return data != null;
    }
   
   boolean sendHeartbeatMessage(boolean on) {
      byte [] msg = (on ? LOCOFI_HEARTBEAT_ON_CMD : LOCOFI_HEARTBEAT_OFF_CMD);
      byte [] ack = sendReplyMessage(net_address,msg,0,msg.length);
      return ack != null;
    }
   
   boolean sendEmergencyStop(boolean stop) {
      byte [] msg = (stop ? LOCOFI_EMERGENCY_STOP_CMD : LOCOFI_EMERGENCY_START_CMD);
      byte [] ack = sendReplyMessage(net_address,msg,0,msg.length);
      return ack != null;
    }
   
   boolean sendLight(boolean front,boolean on) {
      byte [] msg;
      if (front && !on) msg = LOCOFI_FWD_LIGHT_OFF_CMD;
      else if (front && on) msg = LOCOFI_FWD_LIGHT_ON_CMD;
      else if (!front && !on) msg = LOCOFI_REV_LIGHT_OFF_CMD;
      else if (!front && on) msg = LOCOFI_REV_LIGHT_ON_CMD;
      else return false;
      byte [] ack = sendReplyMessage(net_address,msg,0,msg.length);
      return ack != null;
    }
   
   boolean sendBell(boolean on) {
      byte [] msg = (on ? LOCOFI_BELL_ON_CMD : LOCOFI_BELL_OFF_CMD);
      byte [] ack = sendReplyMessage(net_address,msg,0,msg.length);
      return ack != null;
    }
   
   boolean sendHorn(boolean on) {
      byte [] msg = (on ? LOCOFI_HORN_ON_CMD : LOCOFI_HORN_OFF_CMD);
      byte [] ack = sendReplyMessage(net_address,msg,0,msg.length);
      return ack != null;
    }
   
   boolean sendThrottle(int v) {
      byte [] msg = new byte[3];
      msg[0] = LOCOFI_SET_SPEED_CMD[0];
      msg[1] = (byte) (v & 0xff);
      msg[2] = (byte) ((v & 0xff00) >> 8);
      byte [] ack = sendReplyMessage(net_address,msg,0,msg.length);
      return ack != null;
    }
   
   boolean sendReverse(boolean rev) {
      byte [] msg = (rev ? LOCOFI_REV_DIR_CMD : LOCOFI_FWD_DIR_CMD);
      byte [] ack = sendReplyMessage(net_address,msg,0,msg.length);
      return ack != null;
    }
   
   boolean sendStartEngine(boolean start) {
      byte [] msg = (start ? LOCOFI_START_ENGINE_CMD : LOCOFI_STOP_ENGINE_CMD);
      byte [] ack = sendReplyMessage(net_address,msg,0,msg.length);
      return ack != null;
    }
   
   boolean setMute(boolean on) {
      byte [] msg = (on ? LOCOFI_MUTE_VOLUME_CMD : LOCOFI_UNMUTE_VOLUME_CMD);
      byte [] ack = sendReplyMessage(net_address,msg,0,msg.length);
      return ack != null;
    }
   
}       // end of inner class EngineInfo



/********************************************************************************/
/*                                                                              */
/*      General reply handler                                                   */
/*                                                                              */
/********************************************************************************/

private final class ReplyHandler {
   
   private byte [] reply_data;
   
   ReplyHandler() {
      reply_data = null;
    }
   
   synchronized void handleReply(byte [] data) {
      reply_data = data;
      notifyAll();
    }
   
   byte [] getReply() {
      // only wait for reply data time
      if (reply_data == null) {
         synchronized (this) {
            try {
               wait(REPLY_DELAY);
             }
            catch (InterruptedException e) { }
          }
       }
      return reply_data;
    }
   
}



}       // end of class NetworkProcessorLocoFi




/* end of NetworkProcessorLocoFi.java */

