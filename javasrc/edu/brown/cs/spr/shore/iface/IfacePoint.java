/********************************************************************************/
/*                                                                              */
/*              IfacePoint.java                                                 */
/*                                                                              */
/*      description of class                                                    */
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

import edu.brown.cs.spr.shore.iface.IfaceConstants.ShorePointType;

/**
 *      Represents a point in a diagram.  Points are defined in the layout.xml file
 *      which is loaded into SHORE.  Points can be in an arbitrary coordinate system
 *      and are scaled (and aligned) appropriately for display.  Points are genearlly
 *      associated with an object (switch/signal/sensor/gap) or indicate a curve point
 *      in the layout.  Points can be associated with a block if they represent a point
 *      within that block.  Points are also associated with a diagram.
 **/
public interface IfacePoint
{


/**
 *      return the point's X coordinate in user coordinates.
 **/
double getX();


/**
 *      Return the point's Y coordinate in user coordinates.
 **/
double getY();


/**
 *      Return the type of point.  This indicates what is associated with the point.
 **/
ShorePointType getType();


/**
 *      Return the list of points connected to (via rails) this point.
 **/
Collection<IfacePoint> getConnectedTo();


/**
 *      Return the diagram this point is in
 **/
IfaceDiagram getDiagram();


/**
 *      return the block this point is in.  Returns null if not in a block.
 **/
IfaceBlock getBlock();


}       // end of interface IfacePoint




/* end of IfacePoint.java */

