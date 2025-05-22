/********************************************************************************/
/*										*/
/*		IfaceNetwork.java						*/
/*										*/
/*	Interface to network connections, towers and trains			*/
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



package edu.brown.cs.spr.shore.iface;


/**
 *      This class represents the wifi network connections.  It provides calls to
 *      send signals to change the actual physical devices on the train table.
 **/
public interface IfaceNetwork extends IfaceConstants
{

/**
 *      This method sets the switch both internally and externally to the
 *      gvien state.  It differs from IfaceSwitch.setSwitch which only sets
 *      the switch internally.
 **/
void setSwitch(IfaceSwitch sw,ShoreSwitchState set);


/**
 *      Set the signal state both internally and externally.  This differs
 *      From IfaceSignal.setSignalState which only sets the signal internally.
 **/
void setSignal(IfaceSignal sig,ShoreSignalState set);



/**
 *      Set the sensor state both internally and externally.  This differs
 *      From IfaceSensor.setSensorState which only sets the sensor internally.
 *      Note that it is not clear what setting the sensor extermally actually
 *      means, but the appropriate message is sent.
 **/
void setSensor(IfaceSensor sen,ShoreSensorState set);









/**
 *      Sends a message to stop the given engine, either with a normal
 *      stop or with an emergency stop.
 **/
void sendStopTrain(IfaceEngine train,boolean emergency);


/**
 *      Sends a message to restart the given engine.  This remembers whether
 *      the last stop was an emergency or normal stop.
 **/
void sendStartTrain(IfaceEngine train);



}	// end of interface IfaceNetwork




/* end of IfaceNetwork.java */

