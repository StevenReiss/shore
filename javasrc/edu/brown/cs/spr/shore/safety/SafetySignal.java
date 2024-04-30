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
import edu.brown.cs.spr.shore.iface.IfaceEngine;
import edu.brown.cs.spr.shore.iface.IfaceBlock.BlockState;

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
/*      Change Entry Points                                                     */
/*                                                                              */
/********************************************************************************/

void handleSensorChange(IfaceSensor s)
{
   for (IfaceSignal sig : s.getSignals()) { 
      Collection<IfaceSensor> pset = sig.getPriorSensors();
      if (safety_factory.checkPriorSensors(pset)) {  
         if (s.getSensorState() == ShoreSensorState.ON && sig != null) {
            SignalData sd = active_signals.get(sig);
            if (sd == null) {
               if (sig.getSignalState() != ShoreSignalState.GREEN) {
                  // get train for block
                  SignalData si = new SignalData(sig,null);
                  active_signals.put(sig,si);
                  si.stopTrain();
                }
               else {
                  active_signals.put(sig,new SignalData(sig,null));
                  sig.setSignalState(ShoreSignalState.RED);
                }
             }
          }
       }
    }
} 


void handleBlockChange(IfaceBlock b)
{
   if (b.getBlockState() == BlockState.EMPTY) {
      for (Iterator<SignalData> it = active_signals.values().iterator(); it.hasNext(); ) {
         SignalData sd = it.next();
         if (sd.getBlock() == b) it.remove();
       }
    }
   
   // might want to track all signals associated with block 
   updateSignals();
}


void handleSwitchChange(IfaceSwitch sw)
{ 
   // might want to track all signals associated withs switch
   updateSignals();
}


void handleSignalChange(IfaceSignal sig)
{
   
}



/********************************************************************************/
/*                                                                              */
/*      Update all signals                                                      */
/*                                                                              */
/********************************************************************************/

private void updateSignals()
{
   for (IfaceSignal sig : safety_factory.getLayoutModel().getSignals()) {
      updateSignal(sig);
    }
}


private void updateSignal(IfaceSignal sig)
{
   IfaceBlock from = sig.getFromBlock();
   ShoreSignalState rslt = ShoreSignalState.GREEN;
   
   for (IfaceConnection conn : safety_factory.getLayoutModel().getConnections()) {
      if (conn.getStopSignal(from) == sig) {
         ShoreSignalState nst = updateSignal(sig,conn);
         if (nst.ordinal() > rslt.ordinal()) rslt = nst;
       }   
    }
   
   sig.setSignalState(rslt);
   safety_factory.getNetworkModel().sendSetSignal(sig,rslt);
}


private ShoreSignalState updateSignal(IfaceSignal sig,IfaceConnection conn)
{
   IfaceBlock from = sig.getFromBlock();
   IfaceSwitch sw = conn.getExitSwitch(from);
   ShoreSwitchState st = conn.getExitSwitchState(from);  
   if (sw != null && st != null && sw.getSwitchState() != st) {
      // this connection is not relevant due to switch state
      return ShoreSignalState.GREEN;                         
    } 
   IfaceBlock to = conn.getOtherBlock(from);
   switch (to.getBlockState()) {
      case INUSE :
         return ShoreSignalState.RED;
      case EMPTY :
         break;
      case PENDING :
         if (to.getPendingFrom() != from) return ShoreSignalState.RED;
         break;
      case UNKNOWN :
         break;
    }
   
   return ShoreSignalState.GREEN;
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

