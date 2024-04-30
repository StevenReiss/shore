/********************************************************************************/
/*                                                                              */
/*              IfaceSensor.java                                                */
/*                                                                              */
/*      Information about sensors                                               */
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

/**
 *      Represents a sensor embedded in the track.  Sensors are used by the system 
 *      to detect train locations, implement safety constraints, and plan what should
 *      happen.  A sensor can be associated with a switch N or R direction (to force the
 *      switch to go that way and avoid derailment).  They can also be associated with
 *      a connection when they represent two sides of a gap between blocks.  They can
 *      also be associated with a signal in which case they represent where a train should
 *      be stopped if the signal is red.  These are all setup from the layout.xml file.
 **/
public interface IfaceSensor extends IfaceConstants
{


/**
 *      Return switch if on N path to switch; null otherwise
 **/
IfaceSwitch getSwitchN();


/**
 *      Return switch if on R path to switch; null otherwise
 **/
IfaceSwitch getSwitchR();


/**
 *      Return the connection if sensor is on either side of a gap, null otherwise
 **/
IfaceConnection getConnection();


/**
 *      Return the block this sensor is in.
 **/
IfaceBlock getBlock();

/**
 *      Return the current state (ON/OFF) of the sensor
 **/
ShoreSensorState getSensorState();


/**
 *      Set the state of the sensor
 **/
void setSensorState(ShoreSensorState state);


/**
 *      Get the signals associated with the sensor.  This can be more than one if the
 *      sensor is used for stopping trains from both directions
 **/
Collection<IfaceSignal> getSignals();


/**
 *      Get the location point for this sensor.
 **/
IfacePoint getAtPoint();


/**
 *      Get the id of the tower connected to this sensor.  
 **/
byte getTowerId();

/**
 *      Get the index number of this sensor in the tower
 **/
byte getTowerSensor();

}       // end of interface IfaceSensor




/* end of IfaceSensor.java */

