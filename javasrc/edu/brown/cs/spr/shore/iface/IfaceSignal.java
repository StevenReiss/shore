/********************************************************************************/
/*                                                                              */
/*              IfaceSignal.java                                                */
/*                                                                              */
/*      Representation of a signal for SHORE                                    */
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



package edu.brown.cs.spr.shore.iface;

import java.util.Collection;
import java.util.List;

/**
 *      Represent a train control signal.  Each signal can be in one of 3 state (Red/Green/Yellow), 
 *      although a signal might only support Red/Green (based on signal type).  Yellow in this case
 *      is indicated by both red and green on.  Signals are used to control travel from one block
 *      to another (and thus have a direction).  
 **/
public interface IfaceSignal extends IfaceConstants
{

/**
 *      Return the name of the signal
 **/
String getId();


/**
 *      Return the type of signal
 **/  
ShoreSignalType getSignalType();


/**
 *      Set the current state of the signal interally.  This does not set
 *      the physical signal.
 **/
void setSignalState(ShoreSignalState state);


/**
 *      Return the current state of the signal
 **/
ShoreSignalState getSignalState();


/**
 *      Return the block for the signal.  This is the block the train is
 *      coming from and the block where the signal stopping point is located.
 **/
IfaceBlock getFromBlock();


/**
 *      Return the possible connections associated with this signal (to blocks).
 *      Multiple connections are possible because a switch can occur after the
 *      signal but before the end of th block.
 **/
Collection<IfaceConnection> getConnections();



/**
 *      Get the sensor indicating the stopping position for this signal
 **/
List<IfaceSensor> getStopSensors();


/**
 *      Return the set of prior sensors that might be triggered in the signal
 *      block by a train heading toward this sensor.  This might only be the
 *      immediate predecessors.
 **/
Collection<IfaceSensor> getPriorSensors();


/**
 *      Return the point at thich the signal/stop sensor is located
 **/
List<IfacePoint> getAtPoints();


/**
 *      Return the subsequent point for the sensor (provides forward direction).
 */
IfacePoint getNextPoint();


/**
 *      Return the tower id controlling this signal
 **/
byte getTowerId();

/**
 *      Return the index of this signal in its tower
 **/
byte getTowerSignal();



}       // end of interface IfaceSignal




/* end of IfaceSignal.java */

