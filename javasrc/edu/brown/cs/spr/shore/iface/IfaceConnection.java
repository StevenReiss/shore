/********************************************************************************/
/*                                                                              */
/*              IfaceConnection.java                                            */
/*                                                                              */
/*      Representation of a conenction between blocks                           */
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
 *      An IfaceConnection represents a gap/connection between two 
 *      blocks.  There are sensors on either side of the gap to detect
 *      When a train goes between blocks.  There are also signals to
 *      stop a train from entering a block that is otherwise occupied
 *      or reserved.  There can also be a switch and switch state which
 *      need to be set to enter the given block.
 **/

public interface IfaceConnection extends IfaceConstants
{

/**
 *      Given the current block, find the other block associated with this
 *      connection.  If the given block is not part of the connections, return
 *      null.
 **/
IfaceBlock getOtherBlock(IfaceBlock inblock);


/**
 *      Get the sensor that exits from the current block to the next using this 
 *      connection
 **/
IfaceSensor getExitSensor(IfaceBlock inblock);


/**
 *      Get the sensor for entry into the next block for this connection.
 **/
IfaceSensor getEntrySensor(IfaceBlock inblock);


/**
 *      Return the signal associated with this connection if there is one.
 **/
IfaceSignal getStopSignal(IfaceBlock inblock);


/**
 *      Get the sensor associated with the stop signal if there is one.
 **/
IfaceSensor getStopSensor(IfaceBlock inblock);


/**
 *      If there is a switch needed for the connection to take place in the
 *      exited block, return that switch.
 **/
IfaceSwitch getExitSwitch(IfaceBlock inblock);


/**
 *      If there is a switch needed for the connection to take place in the
 *      exited block, return the switch state needed for the connection to occur.
 **/
ShoreSwitchState getExitSwitchState(IfaceBlock inblock);




}       // end of interface IfaceConnection




/* end of IfaceConnection.java */

