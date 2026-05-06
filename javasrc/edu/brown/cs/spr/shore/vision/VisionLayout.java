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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.w3c.dom.Element;

import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
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

private Collection<VisionPoint> point_set;
private Collection<VisionPoint> connected_set;
private Collection<VisionPoint> singleton_set;
private boolean                  layout_ready;
private IfaceSensor              last_sensor;

private static AtomicInteger id_counter = new AtomicInteger(0);



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
   layout_ready = false;
   last_sensor = null;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

int getSize()
{
   return connected_set.size();
}


VisionPoint getLayoutPoint(Point2D given,boolean coord)
{
   VisionPoint vp = null;
   if (given instanceof VisionPoint) {
      vp = (VisionPoint) given;
    }
   else {
      double d0 = MAX_DISTANCE_FIND;
      if (coord) d0 = MAX_DISTANCE_FIND_POINT; 
      Map<VisionPoint,Double> close = findClosestPoints(given,d0); 
      double mind = -1;
      for (Map.Entry<VisionPoint,Double> ent : close.entrySet()) {
         double d = ent.getValue();
         VisionPoint xp = ent.getKey();
         boolean use = (vp == null || mind > d);
         if (vp != null && coord) {
            if (vp.getCorrespondingPoint() == null && xp.getCorrespondingPoint() != null) {
               use = true;
             }
            if (vp.getCorrespondingPoint() != null && xp.getCorrespondingPoint() == null) {
               use = false;
             }
          }
         if (use) {
            vp = xp;
            mind = d;
          }
       }
    }
   
   return vp;
}


IfacePoint getShorePoint(Point2D given)
{
   VisionPoint vp = getLayoutPoint(given,true);
   
   return vp.getCorrespondingPoint();
}



void fillInLayout(Mat out)
{
   double [] c0 = new double [] { 255,255,0 };
   double [] c1 = new double [] { 255,0,255 };
   double [] c2 = new double [] { 0,255,255 };
   
   for (VisionPoint vp : connected_set) {
      int xv = (int) vp.getX();
      int yv = (int) vp.getY();
      double [] c = c0;
      if (vp.getCorrespondingPoint() != null) c = c2;
      for (int i = -2; i <= 2; ++i) {
         for (int j = -2; j <= 2; ++j) {
            out.put(yv+i,xv+j,c);
          }
       }
      Point pa = new Point(xv,yv);
      for (VisionPoint xvp : vp.connectedTo()) {
         Point pb = new Point(xvp.getX(),xvp.getY());
         Imgproc.line(out,pa,pb,
               new Scalar(255,255,255));
       }
    }
   for (VisionPoint vp : singleton_set) {
      int xv = (int) vp.getX();
      int yv = (int) vp.getY();
      double [] c = c1;
      if (vp.getCorrespondingPoint() != null) c = c2;
      for (int i = -1; i <= 1; ++i) {
         for (int j = -1; j <= 1; ++j) {
            out.put(yv+i,xv+j,c);
          }
       }
    }
}


boolean isLayoutReady()
{
   return layout_ready;
}



void noteSensorChanged(IfaceSensor sen)
{
   if (sen.getSensorState() == ShoreSensorState.ON) {
      last_sensor = sen;
      IvyLog.logD("VISION","Record last sensor " + sen);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

void clearLayout()
{
   point_set = new ArrayList<>();
   connected_set = new ArrayList<>();
   singleton_set = new ArrayList<>();
   layout_ready = false;
}


VisionPoint recordPoint(Point2D point)
{
   return addLayoutPoint(point);
}


void noteSensor(Point2D pt0,IfaceSensor sen,ShoreSensorState st)
{
   VisionPoint vp = addLayoutPoint(pt0);
   IfacePoint pt = sen.getAtPoint();
   if (st == ShoreSensorState.ON) {
      vp.setCorrespondingPoint(pt);
      vp.setSensor(sen);
    }
}


private VisionPoint addLayoutPoint(Point2D given)
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
      VisionPoint vp0 = ent.getKey();
      if (d <= MIN_DISTANCE_SAME || vp0.getCorrespondingPoint() != null) { 
         if (rslt == null || mind > d) {
            rslt = ent.getKey();
            mind = d;
          }
       }
    }
   
   boolean reuse = false;
   if (rslt != null) {
      rslt.reusePoint();
      reuse = true;
      IvyLog.logD("VISION","Reuse existing point " + rslt.getX() + " " +
            rslt.getY() + " " + last_sensor);
    }
   else {
      rslt = new VisionPoint(given.getX(),given.getY());
      IvyLog.logD("VISION","Create new VisionPoint " + given.getX() +
            " " + given.getY());
    }
   
   if (last_sensor != null) {
      IvyLog.logD("VISION","Note sensor " + last_sensor + " at " +
            rslt.getX() + " " + rslt.getY());
      rslt.setCorrespondingPoint(last_sensor.getAtPoint());
      rslt.setSensor(last_sensor);
      last_sensor = null;
    }
   
   if (!reuse) {
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
    }
   
   return rslt;
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

void load(File f)
{
   Element data = IvyXml.loadXmlFromFile(f);
   if (data == null) {
      clearLayout();
      Map<String,VisionPoint> pts = new HashMap<>();
      Element ptxml = IvyXml.getChild(data,"POINTS");
      for (Element pe : IvyXml.children(ptxml,"POINT")) {
         VisionPoint vp = new VisionPoint(pe);
         pts.put(vp.getId(),vp);
       }
      for (Element pe : IvyXml.children(ptxml,"POINT")) {
         String id = IvyXml.getAttrString(pe,"ID");
         String conn = IvyXml.getAttrString(pe,"CONNECT");
         if (conn != null) {
            VisionPoint vp0 = pts.get(id);
            for (StringTokenizer tok = new StringTokenizer(conn); tok.hasMoreTokens(); ) {
               String cid = tok.nextToken();
               VisionPoint cpt = pts.get(cid);
               if (cpt != null && cpt != vp0) {
                  vp0.connectTo(cpt);
                  cpt.connectTo(vp0);
                }
             }
          }
       }
      for (VisionPoint vp : pts.values()) {
         point_set.add(vp);
         if (!vp.connectedTo().isEmpty()) {
            connected_set.add(vp);
          }
       }
      last_sensor = null;
      layout_ready = true;
    }
}


void save(File f)
{
   // save contents in file so it can be reloaded
   layout_ready = true;
   
   try (IvyXmlWriter xw = new IvyXmlWriter(f)) {
      xw.begin("LAYOUT");
      xw.begin("POINTS");
      for (VisionPoint vp : point_set) {
         if (vp.getCorrespondingPoint() == null &&
               vp.getSensor() == null &&
               vp.connectedTo().isEmpty()) {
            // ignore irrelevant points
            continue;
          }
         vp.outputXml(xw);
       }
      xw.end("POINTS");
      xw.end("LAYOUT");
    }
   catch (IOException e) {
      IvyLog.logE("VISION","Save layout failed",e);
    }
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
   private int point_count;
   private String point_id;
   
   private static final long serialVersionUID = 1;
   
   
   VisionPoint(double x,double y) {
      super(x,y);
      connect_to = new ArrayList<>();
      correspond_to = null;
      use_sensor = null;
      point_count = 1;
      point_id = "POINT_" + id_counter.incrementAndGet();
    }
   
   VisionPoint(Element xml) {
      super(IvyXml.getAttrDouble(xml,"X"),IvyXml.getAttrDouble(xml,"Y"));
      point_count = IvyXml.getAttrInt(xml,"COUNT");
      point_id = IvyXml.getAttrString(xml,"ID");
      connect_to = new ArrayList<>();
      String cpt = IvyXml.getAttrString(xml,"CORRESPOND");
      correspond_to = null;
      if (cpt != null) {
         // find point for model with id cpt
       }
      String sid = IvyXml.getAttrString(xml,"SENSOR");
      if (sid != null) {
         // find sensor for model with id sid
       }
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
   
   int getPointCount() {
      return point_count;
    }
   
   void setCorrespondingPoint(IfacePoint pt) {
      correspond_to = pt;
    }
   
   void setSensor(IfaceSensor sen) {
      use_sensor = sen;
    }
   
   void connectTo(VisionPoint vp) {
      connect_to.add(vp);
    }
   
   String getId() {
      return point_id;
    }
   
   void reusePoint() {
      point_count++;
    }
   
   void outputXml(IvyXmlWriter xw) {
      xw.begin("POINT");
      xw.field("ID",point_id);
      xw.field("COUNT",point_count);
      xw.field("X",getX());
      xw.field("Y",getY());
      if (correspond_to != null) {
         xw.field("CORRESPOND",correspond_to.getId());
       }
      if (use_sensor != null) {
         xw.field("SENSOR",use_sensor.getId());
       }
      StringBuffer buf = new StringBuffer();
      for (VisionPoint vp : connect_to) {
         if (!buf.isEmpty()) buf.append(" ");
         buf.append(vp.point_id);
       }
      if (!buf.isEmpty()) {
         xw.field("CONNECT",buf.toString());
       }
      xw.end("POINT");
    }
   
}       // end of inner class VisionPoint


}       // end of class VisionLayout




/* end of VisionLayout.java */

