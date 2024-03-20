/********************************************************************************/
/*                                                                              */
/*              ModelConstants.java                                             */
/*                                                                              */
/*      Constants for the SHORE model                                           */
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



package edu.brown.cs.spr.shore.model;

import java.util.Collection;
import java.util.List;

import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceSensor;
import edu.brown.cs.spr.shore.iface.IfaceSignal;
import edu.brown.cs.spr.shore.iface.IfaceSwitch;
import edu.brown.cs.spr.shore.iface.IfaceTrain;

public interface ModelConstants
{

enum PointType { TURNING, STRAIGHT, SWITCH, BLOCK, END, TURNTABLE, DIAGRAM };

interface ModelPoint {
   String getId();
   double getX();
   double getY();
   PointType getType();
   List<ModelPoint> getToPoints();
   List<ModelPoint> getFromPoints();
}


interface ModelSwitch extends IfaceSwitch {
   String getId();
   ModelPoint getPivotPoint();
   ModelPoint getNPoint();
   ModelPoint getRPoint();
   ModelSwitch getAssociatedSwitch();
}


interface ModelSensor extends IfaceSensor {
   String getId();
   ModelPoint getAtPoint();
}


interface ModelSignal extends IfaceSignal {
   String getId();
   ModelPoint getAtPoint();
   ModelPoint getToPoint();
}


interface ModelBlock extends IfaceBlock {
   String getId();
   ModelPoint getAtPoint();
}

interface ModelTrain extends IfaceTrain {
   String getId();
}


interface ModelDiagram {
   String getId();
   Collection<ModelPointImpl> getPoints();
   Collection<ModelSwitch> getSwitches();
   Collection<ModelBlock> getBlocks();
   Collection<ModelSensor> getSensors();
   Collection<ModelSignal> getSignals();
}


 
}       // end of interface ModelConstants




/* end of ModelConstants.java */

