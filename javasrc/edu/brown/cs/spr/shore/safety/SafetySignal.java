/********************************************************************************/
/*                                                                              */
/*              SafetySignal.java                                               */
/*                                                                              */
/*      Handle automatic settings for signals                                   */
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



package edu.brown.cs.spr.shore.safety;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceConnection;
import edu.brown.cs.spr.shore.iface.IfaceSensor;
import edu.brown.cs.spr.shore.iface.IfaceSignal;
import edu.brown.cs.spr.shore.iface.IfaceSwitch;
import edu.brown.cs.spr.shore.shore.ShoreLog;
import edu.brown.cs.spr.shore.iface.IfaceEngine;

class SafetySignal implements SafetyConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private SafetyFactory safety_factory;
private Map<IfaceSignal,SignalData> active_signals;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SafetySignal(SafetyFactory sf)
{
   safety_factory = sf;
   active_signals = new HashMap<>();
}



/********************************************************************************/
/*                                                                              */
/*      Let user set signal if it is safe                                       */
/*                                                                              */
/********************************************************************************/

boolean safelySetSignal(IfaceSignal ss,ShoreSignalState state)
{
   if (state == ShoreSignalState.GREEN || state == ShoreSignalState.YELLOW) {
      IfaceBlock frm = ss.getFromBlock();
      for (IfaceConnection cc : ss.getConnections()) {
         IfaceBlock blk = cc.getOtherBlock(frm);
         switch (blk.getBlockState()) {
            case EMPTY :
            case UNKNOWN :
               break;
            case PENDING :
            case INUSE :
               ShoreLog.logD("SAFETY","Attempt to set signal " + ss + 
                     "when target block in use " + blk);
               return false;
          }
       }
    }
   
   safety_factory.getNetworkModel().setSignal(ss,state);
   return true;
}



/********************************************************************************/
/*                                                                              */
/*      Change Entry Points                                                     */
/*                                                                              */
/********************************************************************************/

void handleSensorChange(IfaceSensor s)
{
   if (s.getSensorState() != ShoreSensorState.ON) return;
   
   // check if we have to stop a train
   for (IfaceSignal sig : s.getSignals()) { 
      Collection<IfaceSensor> pset = sig.getPriorSensors();
      ShoreLog.logD("SAFETY","PRIOR SENSORS " + s + " " + pset);
      if (safety_factory.checkPriorSensors(pset)) {  
         SignalData sd = active_signals.get(sig);
         if (sd == null) {
            // ensure first time we have this signal for block
            if (sig.getSignalState() != ShoreSignalState.GREEN) {
               // get train for block
               IfaceEngine trn = null;
               ShoreLog.logD("SAFETY","Signal " + sig + 
                     " should stop train " + trn);
               SignalData si = new SignalData(sig,trn);
               active_signals.put(sig,si);
               si.stopTrain();
             }
            else {
               // add to active signals so we ignore later sets
               active_signals.put(sig,new SignalData(sig,null));
             }
          }
       }
    }
} 


void handleBlockChange(IfaceBlock b)
{
   if (b.getBlockState() != ShoreBlockState.INUSE) {
      // remove active signals from block
      for (Iterator<SignalData> it = active_signals.values().iterator(); it.hasNext(); ) {
         SignalData sd = it.next();
         if (sd.getBlock() == b) it.remove();
       }
    }
   
   updateSignals(b);
}


void handleSwitchChange(IfaceSwitch sw)
{ 
   // switch changes will result in block status changes -- we can ignore here
}


void handleSignalChange(IfaceSignal sig)
{ }



/********************************************************************************/
/*                                                                              */
/*      Update all signals                                                      */
/*                                                                              */
/********************************************************************************/

private void updateSignals(IfaceBlock blk)
{
   for (IfaceSignal sig : safety_factory.getLayoutModel().getSignals()) {
      IfaceBlock sigblk = sig.getFromBlock();
      for (IfaceConnection conn : sig.getConnections()) {
         IfaceBlock toblk = conn.getOtherBlock(sigblk);
         if (toblk == blk) {
            updateSignal(sig);
            break;
          }
       }
    }
}


private void updateSignal(IfaceSignal sig)
{
   IfaceBlock from = sig.getFromBlock();
   ShoreSignalState rslt = ShoreSignalState.GREEN;
   
   if (from != null) {
      for (IfaceConnection conn : sig.getConnections()) {
         ShoreSignalState nst = updateSignal(sig,conn);
         if (nst.ordinal() > rslt.ordinal()) rslt = nst;
       }
    }
   else {
      rslt = ShoreSignalState.OFF;
    }
  
   if (sig.getSignalState() == rslt) return;
   
   ShoreLog.logD("SAFETY","Set signal " + sig.getId() + " = " + rslt + " from " +
         sig.getSignalState());
   
   safety_factory.getNetworkModel().setSignal(sig,rslt);
}


private ShoreSignalState updateSignal(IfaceSignal sig,IfaceConnection conn)
{
         
   IfaceBlock from = sig.getFromBlock();
   IfaceSwitch sw = conn.getExitSwitch(from);
   ShoreSwitchState st = conn.getExitSwitchState(from);  
   IfaceBlock to = conn.getOtherBlock(from);
   
   ShoreLog.logD("SAFETY","Update signal " + sig.getId() + " for " + 
         from + " " + to + " " + sw + " " + st); 
         
   if (sw != null && st != null && sw.getSwitchState() != st) {
      ShoreLog.logD("SAFETY","Signal not relevant due to switch state");
      // this connection is not relevant due to switch state
      return ShoreSignalState.GREEN;                         
    } 
   
   ShoreSignalState rslt = ShoreSignalState.GREEN;
   switch (to.getBlockState()) {
      case INUSE :
         rslt = ShoreSignalState.RED;
      case EMPTY :
         break;
      case PENDING :
         if (to.getPendingFrom() != from) {
            rslt = ShoreSignalState.RED;
          }
         break;
      case UNKNOWN :
         break;
    }
   
   ShoreLog.logD("SAFETY","Next block state " + to + " " + to.getBlockState() + 
         " " + to.getPendingFrom() + " " + from + " = " + rslt);
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Data for an active siganl                                               */
/*                                                                              */
/********************************************************************************/

private class SignalData {
   
   private IfaceSignal for_signal;
   private IfaceEngine for_train;
   
   SignalData(IfaceSignal sig,IfaceEngine train) {
      for_signal = sig;
      for_train = train;
    }
   
   void stopTrain() {
      if (for_train != null) {
         for_train.stopTrain();
       }
    } 
   
   IfaceBlock getBlock() {
      return for_signal.getFromBlock();
    }
   
   
}


}       // end of class SafetySignal




/* end of SafetySignal.java */

