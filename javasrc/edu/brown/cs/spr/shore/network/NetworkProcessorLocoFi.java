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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
private DatagramSocket speed_socket;
private DatagramSocket rpm_socket;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

NetworkProcessorLocoFi(DatagramSocket sock,DatagramSocket speed,
      DatagramSocket rpm,IfaceTrains trains)
{
   super(sock);
   engine_model = trains;
   engine_map = new ConcurrentHashMap<>();
   reply_map = new ConcurrentHashMap<>();
   speed_socket = speed;
   rpm_socket = rpm;
   startReader(speed,new SpeedHandler());
   startReader(rpm,new RpmHandler());
}



/********************************************************************************/
/*                                                                              */
/*      Top level message requests                                              */
/*                                                                              */
/********************************************************************************/

void sendLight(IfaceEngine eng,boolean front,boolean sts) 
{
   EngineInfo ei = findEngineInfo(eng);
   if (ei == null) return;
   
   ei.sendLight(front,sts);
}


void sendMute(IfaceEngine eng,boolean mute)
{
   EngineInfo ei = findEngineInfo(eng);
   if (ei == null) return;
   
   ei.setMute(mute);
}


void sendBell(IfaceEngine eng,boolean on)
{
   EngineInfo ei = findEngineInfo(eng);
   if (ei == null) return;
   
   ei.sendBell(on);
}


void sendHorn(IfaceEngine eng)
{
   EngineInfo ei = findEngineInfo(eng);
   if (ei == null) return;
    
   ei.sendHorn(eng.isHornOn());
}


void sendThrottle(IfaceEngine eng,double v)
{
   EngineInfo ei = findEngineInfo(eng);
   if (ei == null) return;
   
   int vint = (int) v;
   ei.sendThrottle(vint);
}

void sendReverse(IfaceEngine eng,boolean rev)
{
   EngineInfo ei = findEngineInfo(eng);
   if (ei == null) return;
   
   ei.sendReverse(rev);
}


void sendStartStopEngine(IfaceEngine eng,boolean start)
{
   EngineInfo ei = findEngineInfo(eng);
   if (ei == null) return;
   
   ei.sendStartEngine(start);
}


void sendEmergencyStop(IfaceEngine eng,boolean stop)
{
   EngineInfo ei = findEngineInfo(eng);
   if (ei == null) return;
   
   ei.sendEmergencyStop(stop);
}


void sendReboot(IfaceEngine eng)
{
   EngineInfo ei = findEngineInfo(eng);
   if (ei == null) return;
   
   ei.sendReboot();
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
         ei.sendClearConsist();
         ei.sendQueryAboutMessage();
         IfaceEngine eng = findEngine(ei.getEngineId());
         engine_model.setEngineSocket(eng,sa);
         ei.sendQuerySettingsMessage();
         ei.sendQueryVersionMessage();
         ei.sendHeartbeatMessage(true);
         ei.sendQueryStateMessage();
         ei.sendSpeedReportMessage();
         ei.sendRpmReportMessage();
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
   
   byte[] reply = rh.getReply();
   if (reply == null) {
      ShoreLog.logE("NETWORK","No reply received from " + who + " " + msg[0]);
    }
   return reply;
}



@Override public void handleMessage(DatagramPacket msg)
{
   String msgtxt = decodeMessage(msg.getData(),msg.getOffset(),msg.getLength());
   ShoreLog.logD("NETWORK","Received from " + msg.getAddress() + " " +
         msg.getPort() + " " + msg.getLength() + " " + msg.getOffset() + ": " +
         msgtxt);
   
   if (msg.getPort() != ALT_PORT) {
      return;
    }
   
   SocketAddress sa = msg.getSocketAddress();
   ReplyHandler rh = reply_map.remove(sa);
   if (rh != null) {
      byte [] rslt = new byte[msg.getLength()];
      System.arraycopy(msg.getData(),msg.getOffset(),rslt,0,msg.getLength());
      if (!rh.handleReply(rslt)) {
         ShoreLog.logE("NETWORK","Late message from engine");
       }
    }
   else {
      ShoreLog.logE("NETWORK","Unsolicited message from engine");
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

   private Map<EngineInfo,Integer> bad_count;
   
   LocoFiStatusUpdater() {
      super("ShoreLocoFiStatusUpdater");
      setDaemon(true);
      bad_count = new HashMap<>();
    }
   
   @Override public void run() {
      for ( ; ; ) {
         List<EngineInfo> todel = null;
         for (EngineInfo ei : engine_map.values()) {
            ei.sendHeartbeatMessage(true);
            if (ei.sendQueryStateMessage()) {
               bad_count.remove(ei);
               delay();
             }
            else {
               // check if engine is dead
               Integer iv = bad_count.get(ei);
               if (iv == null) iv = 0;
               bad_count.put(ei,iv+1);
               if (iv > MAX_NO_STATE_REPLY) { 
                  ShoreLog.logE("NETWORK","Engine " + ei.getEngineId() + " timed out");
                  IfaceEngine eng = findEngine(ei.getEngineId());
                  if (eng != null) {
                     engine_model.setEngineSocket(eng,null);
                   }
                  if (todel == null) todel = new ArrayList<>();
                  todel.add(ei);
                }
             }
          }
         if (todel != null) {
            for (EngineInfo ei : todel) {
               ShoreLog.logD("NETWORK","Engine " + ei.getEngineId() + " not responsive");
               engine_map.remove(ei.getSocketAddress());
               IfaceEngine eng = findEngine(ei.getEngineId());
               if (eng != null) {
                  engine_model.setEngineSocket(eng,null);
                  eng.setNotPresent();
                } 
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
/*      General reply handler                                                   */
/*                                                                              */
/********************************************************************************/

private final class ReplyHandler {

   private byte [] reply_data;
   private boolean is_done;
   
   ReplyHandler() {
      reply_data = null;
      is_done = false;
    }
   
   synchronized boolean handleReply(byte [] data) {
      if (is_done) return false;
      reply_data = data;
      is_done = true;
      notifyAll();
      return true;
    }
   
   byte [] getReply() {
      // only wait for reply data time
      if (reply_data == null) {
         synchronized (this) {
            try {
               wait(REPLY_DELAY);
             }
            catch (InterruptedException e) { }
            is_done = true;
          }
       }
      return reply_data;
    }

}



private final class SpeedHandler implements MessageHandler {

   @Override public void handleMessage(DatagramPacket msg) {
      String msgtxt = decodeMessage(msg.getData(),msg.getOffset(),msg.getLength());
      ShoreLog.logD("NETWORK","Received SPEED from " + msg.getAddress() + " " +
            msg.getPort() + " " + msg.getLength() + " " + msg.getOffset() + ": " +
            msgtxt);
      
      if (msg.getPort() != ALT_PORT) {
         return;
       }
    }
   
}       // end of inner class SpeedHandler



private final class RpmHandler implements MessageHandler {
   
   @Override public void handleMessage(DatagramPacket msg) {
      String msgtxt = decodeMessage(msg.getData(),msg.getOffset(),msg.getLength());
      ShoreLog.logD("NETWORK","Received FROM from " + msg.getAddress() + " " +
            msg.getPort() + " " + msg.getLength() + " " + msg.getOffset() + ": " +
            msgtxt);
      
      if (msg.getPort() != ALT_PORT) {
         return;
       }
    }
   
}       // end of inner class RpmHandler



/********************************************************************************/
/*                                                                              */
/*      Information for each connected engine                                   */
/*                                                                              */
/********************************************************************************/

private class EngineInfo {
   
   private SocketAddress net_address;
   private String engine_id;
   private int engine_status;
   
   EngineInfo(SocketAddress net) {
      net_address = net;
      engine_id = null;
      engine_status = -1;
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
      if (data.length < 13) {
         ShoreLog.logD("NETWORK","BAD STATUS MESSAGE");
         return true;
       }
      
      boolean front = data[0] != 0;
      boolean back = data[1] != 0;
      boolean bell = data[2] != 0;
      // eng.setBell(bell);
      boolean rev = data[3] != 0;
      int sts = data[4];
      int speedstep = getShort(data,5);
      int rpmstep = getShort(data,7);
      int speed = getShort(data,9);
      boolean estop = data[11] != 0;
      boolean mute = data[12] == 0;
      ShoreLog.logD("NETWORK","Engine speed " + speedstep + " " + rpmstep + " " + speed);
      eng.setupEngine(front,back,bell,rev,sts,
            speedstep,rpmstep,speed,estop,mute);  
      
      if (engine_status != sts) {
         engine_status = sts;
         if (sts == 2) {
            sendSpeedReportMessage();
            sendRpmReportMessage();
          }
       }
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
      if (data == null) {
         return false;
       }
      IfaceEngine eng = findEngine(engine_id);
      if (eng == null) {
         return false;
       }
      ShoreLog.logD("NETWORK: Engine " + engine_id + " SETTINGS:");
      ShoreLog.logD("NETWORK","\tAuto Reverse Lights: " + (data[0] != 0));
      ShoreLog.logD("NETWORK","\tReverse Lights: " + (data[1] != 0));
      ShoreLog.logD("NETWORK","\tReverse Direction: " + (data[2] != 0));
      int ntn = getShort(data,3);
      ntn = (1 << ntn) * 8;
      ShoreLog.logD("NETWORK","\tNum Throttle Notches: " + ntn);
      int startstep = getShort(data,5);
      ShoreLog.logD("NETWORK","\tStart Speed Step: " + startstep);
      ShoreLog.logD("NETWORK","\tStart Delay: " + getShort(data,7));
      int maxstep = getShort(data,9);
      ShoreLog.logD("NETWORK","\tMax Speed Step: " + maxstep);
      double maxscale = getFloat(data,11);
      ShoreLog.logD("NETWORK","\tMax Scale Speed: " + maxscale);
      int maxdisp = getShort(data,15);
      ShoreLog.logD("NETWORK","\tMax Speed Display: " + maxdisp);
      boolean kmph = (data[17] != 0);
      ShoreLog.logD("NETWORK","\tSpeed Units (KMPH): " + kmph);
      int mtm = data[18];
      mtm = (1 << mtm) * 50;
      ShoreLog.logD("NETWORK","\tMomentum: " + mtm);
      int coast = data[19];
      coast = (1 << coast) * 100;
      ShoreLog.logD("NETWORK","\tCoast: " + coast);
      ShoreLog.logD("NETWORK","\tAuto Stop on Lost Heartbeat: " + (data[20] != 0));
      ShoreLog.logD("NETWORK","\tLock for Direct Connection: " + (data[21] != 0));
      ShoreLog.logD("NETWORK","\tScale: " + getFloat(data,22));
      ShoreLog.logD("NETWORK","\tSpeed Table: " + data[26]);
      ShoreLog.logD("NETWORK","\tScale Wheel Diameter: " + getFloat(data,27));
      ShoreLog.logD("NETWORK","\tVolume: " + data[31]);
      ShoreLog.logD("NETWORK","\tHigh Frequency PWM: " + (data[32] != 0));
      if (data.length > 33) {
         ShoreLog.logD("NETWORK","\tNumber Cylinders: " + data[33]);
         ShoreLog.logD("NETWORK","\tGear Ratio: " + getFloat(data,34));
       }
      
      eng.setSpeedParameters(startstep,maxstep,ntn,maxdisp,kmph);
      
      return true;
    }
   
   boolean sendClearConsist() {
      byte [] msg = LOCOFI_CLEAR_CONSIST_CMD;
      byte [] ack = sendReplyMessage(net_address,msg,0,msg.length);
      return ack != null;
    }
   
   boolean sendSpeedReportMessage() {
      if (speed_socket != null) {
//       byte [] msg = LOCOFI_SPEED_REPORT_CMD;
//       byte [] ack = sendReplyMessage(net_address,msg,0,msg.length);
//       return ack != null;
//       sendMessage(speed_socket,net_address,msg,0,msg.length); 
       }
      return true;
    }
   
   boolean sendRpmReportMessage() {
      if (rpm_socket != null) {
//       byte [] msg = LOCOFI_RPM_REPORT_CMD;
//       byte [] ack = sendReplyMessage(net_address,msg,0,msg.length);
//       return ack != null;
//       sendMessage(rpm_socket,net_address,msg,0,msg.length);
       }
      return true;
    }
   
   boolean sendHeartbeatMessage(boolean on) {
      byte [] msg = (on ? LOCOFI_HEARTBEAT_ON_CMD : LOCOFI_HEARTBEAT_OFF_CMD);
      byte [] ack = sendReplyMessage(net_address,msg,0,msg.length);
      return ack != null;
    }
   
   boolean sendEmergencyStop(boolean stop) {
      byte [] msg = (stop ? LOCOFI_EMERGENCY_STOP_CMD : LOCOFI_EMERGENCY_START_CMD);
      sendMessage(net_address,msg,0,msg.length);
      return true;
   // byte [] ack = sendReplyMessage(net_address,msg,0,msg.length);
   // return ack != null;
    }
   
   
   boolean sendReboot()
   {
      byte [] msg = LOCOFI_REBOOT_CMD;
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
   // byte [] ack = sendReplyMessage(net_address,msg,0,msg.length);
   // return ack != null;
      sendMessage(net_address,msg,0,msg.length);
      return true;
   }
   
   boolean sendHorn(boolean on) {
      byte [] msg = (on ? LOCOFI_HORN_ON_CMD : LOCOFI_HORN_OFF_CMD);
   // byte [] ack = sendReplyMessage(net_address,msg,0,msg.length);
   // return ack != null;
      sendMessage(net_address,msg,0,msg.length);
      return true;
   }
   
   boolean sendThrottle(int v) {
      byte [] msg = new byte[3];
      msg[0] = LOCOFI_SET_THROTTLE_CMD[0];  
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
   
   private int getShort(byte [] data,int offset) {
      int d1 = data[offset] & 0xff;
      int d2 = (data[offset+1] & 0xff) << 8;
      return d1+d2;
    }
   
   private double getFloat(byte [] data,int offset) {
      ByteBuffer buf = ByteBuffer.wrap(data);
      buf.order(ByteOrder.LITTLE_ENDIAN);
      double v = buf.getFloat(offset);
      return v;
    }
   
}       // end of inner class EngineInfo




}       // end of class NetworkProcessorLocoFi




/* end of NetworkProcessorLocoFi.java */

