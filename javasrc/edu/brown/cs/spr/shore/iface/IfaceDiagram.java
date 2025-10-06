/********************************************************************************/
/*                                                                              */
/*              IfaceDiagram.java                                               */
/*                                                                              */
/*      Representation of a diagram to display                                  */
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
 *      Represents a diagram showing a portion (or all) of the train layout.  A layout
 *      consists of multiple diagrams, each of which is displayed separately.
 **/

public interface IfaceDiagram extends IfaceConstants
{

/**
 *      Returns the name/id of this diagram.
 **/
String getId();


/**
 *      Returns the set of all points in this diagram
 **/
Collection<IfacePoint> getPoints();


/**
 *      Returns the set of all sensors in this diagram
 **/
Collection<IfaceSensor> getSensors();


/**
 *      Returns the set of all signals in this diagram
 **/
Collection<IfaceSignal> getSignals();


/**
 *      Returns the set of all switches in this diagram
 **/
Collection<IfaceSwitch> getSwitches();


/**
 *      Returns the set of all track blocks in this diagram
 **/
Collection<IfaceBlock> getBlocks();



/**
 *      Indicates that low y is at the bottom if true, top if false.  
 **/
boolean invertDisplay();






}       // end of interface IfaceDiagram




/* end of IfaceDiagram.java */

