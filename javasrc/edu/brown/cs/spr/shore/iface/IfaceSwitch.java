/********************************************************************************/
/*                                                                              */
/*              IfaceSwitch.java                                                */
/*                                                                              */
/*      Information and control for a railroad switch                                                  */
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


/**
 *      Represent a switch in the train layout.  Each switch is associated with a sensor
 *      for N and for R to ensure the switch is set correctly as a train approaches it.
 *      Switches
 **/
public interface IfaceSwitch extends IfaceConstants
{


/**
 *      Return the current state of the switch (N/R)
 **/
ShoreSwitchState getSwitchState(); 


/**
 *      Set the current state of the switch internally.  Does not directly change
 *      the switch externally.
 **/
void setSwitch(ShoreSwitchState n);



/**
 *      Return the name/id of the switch
 **/
String getId();


/**
 *      Return the sensor associated with the N branch of the switch.
 **/
IfaceSensor getNSensor();


/**
 *      Return the sensor associated with the R branch of the switch
 **/
IfaceSensor getRSensor();


/**
 *      Return the point where the N and R branches diverge in the layout.
 **/
IfacePoint getPivotPoint();



/**
 *      Return the id of the tower controlling the switch
 **/
byte getTowerId();


/**
 *      Return the index of the switch in the tower.
 **/
byte getTowerSwitch();


}       // end of interface IfaceSwitch




/* end of IfaceSwitch.java */

