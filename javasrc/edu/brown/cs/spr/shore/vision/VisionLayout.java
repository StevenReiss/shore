/********************************************************************************/
/*                                                                              */
/*              VisionLayout.java                                               */
/*                                                                              */
/*      Hold layout based on vision                                             */
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


package edu.brown.cs.spr.shore.vision;

import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.brown.cs.spr.shore.iface.IfacePoint;
import edu.brown.cs.spr.shore.iface.IfaceSensor;
import edu.brown.cs.spr.shore.iface.IfaceConstants.ShoreSensorState;

class VisionLayout implements VisionConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Collection<VisionPoint>  point_set;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

VisionLayout()
{
   point_set = new ArrayList<>();
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

VisionPoint findLayoutPoint(Point2D given)
{
   if (given instanceof VisionPoint) {
      return (VisionPoint) given;
    }
   
   VisionPoint pt = new VisionPoint(given.getX(),given.getY());
   point_set.add(pt);
   return pt;
}


IfacePoint getShorePoint(Point2D given)
{
   VisionPoint vp = findLayoutPoint(given);
   
   return vp.getCorrespondingPoint();
}



/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

VisionPoint recordPoint(Point2D point)
{
   return findLayoutPoint(point);
}


void noteSensor(Point2D pt0,IfaceSensor sen,ShoreSensorState st)
{
   VisionPoint vp = findLayoutPoint(pt0);
   IfacePoint pt = sen.getAtPoint();
   if (st == ShoreSensorState.ON) {
      vp.setCorresondingPoint(pt);
      vp.setSensor(sen);
    }
}


/********************************************************************************/
/*                                                                              */
/*      I/O methods                                                             */
/*                                                                              */
/********************************************************************************/

boolean load(File f)
{
   return false;
}


void save(File f)
{
   // save contents in file so it can be reloaded
}



/********************************************************************************/
/*                                                                              */
/*      Vision point                                                            */
/*                                                                              */
/********************************************************************************/

static class VisionPoint extends Point2D.Double {
   
   private List<VisionPoint> connect_to;
   private IfacePoint correspond_to;
   private IfaceSensor use_sensor;
   
   private static final long serialVersionUID = 1;
   
   
   VisionPoint(double x,double y) {
      super(x,y);
      connect_to = new ArrayList<>();
      correspond_to = null;
      use_sensor = null;
    }
   
   List<VisionPoint> connectedTo() {
      return connect_to;
    }

   IfacePoint getCorrespondingPoint() {
      return correspond_to;
    }
   
   IfaceSensor getSensor() {
      return use_sensor;
    }
   
   void setCorresondingPoint(IfacePoint pt) {
      correspond_to = pt;
    }
   
   void setSensor(IfaceSensor sen) {
      use_sensor = sen;
    }
   
}       // end of inner class VisionPoint


}       // end of class VisionLayout




/* end of VisionLayout.java */

