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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.brown.cs.ivy.file.IvyLog;
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
private Collection<VisionPoint>  connected_set;
private Collection<VisionPoint>  singleton_set;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

VisionLayout()
{
   point_set = new ArrayList<>();
   connected_set = new ArrayList<>();
   singleton_set = new ArrayList<>();
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
   
   Map<VisionPoint,Double> close = findClosestPoints(given,MIN_DISTANCE_CONNECT);
   
   // see if there is an existing point close enough
   VisionPoint rslt = null;
   double mind = -1;
   for (Map.Entry<VisionPoint,Double> ent : close.entrySet()) {
      double d = ent.getValue();
      if (d <= MIN_DISTANCE_SAVE) {
         if (rslt == null || mind > d) {
            rslt = ent.getKey();
            mind = d;
          }
       }
    }
   if (rslt != null) return rslt;
   
   rslt = new VisionPoint(given.getX(),given.getY());
   IvyLog.logD("VISION","Create new VisionPoint " + given.getX() +
         " " + given.getY());
   
   for (VisionPoint conn : close.keySet()) {
      // need to restrict this set to closest connected points that aren't
      //   already connected or to points inbetween a connection
      if (singleton_set.contains(conn)) {
         singleton_set.remove(conn);
         connected_set.add(conn);
       }
      IvyLog.logD("VISION","Connect to " + conn.getX() + " " + conn.getY());
      conn.connectTo(rslt);
      rslt.connectTo(conn);
    }
   point_set.add(rslt);
   if (!close.isEmpty()) {
      connected_set.add(rslt);
    }
   else {
      singleton_set.add(rslt);
    }
   
   return rslt;
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
/*      Closest point methods                                                   */
/*                                                                              */
/********************************************************************************/

Map<VisionPoint,Double> findClosestPoints(Point2D pt,double max)
{
   Map<VisionPoint,Double> rslt = new HashMap<>();
   for (VisionPoint vp : point_set) {
      double d = vp.distance(pt);
      if (d <= max) {
         rslt.put(vp,d);
       }
    }
   
   return rslt;
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
   
   void connectTo(VisionPoint vp) {
      connect_to.add(vp);
    }
   
}       // end of inner class VisionPoint


}       // end of class VisionLayout




/* end of VisionLayout.java */

