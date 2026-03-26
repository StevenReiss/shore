/********************************************************************************/
/*                                                                              */
/*              VisionTrain.java                                                */
/*                                                                              */
/*      Vision information about a train                                        */
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

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.opencv.core.Point;

import edu.brown.cs.spr.shore.iface.IfaceEngine;

class VisionTrain implements VisionConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceEngine     for_engine;
private LinkedList<Point2D>     point_list;
private List<Point2D>     initial_points;

private static final double INITIAL_TOLERANCE = 0.5;
private static final double LIST_TOLERANCE = 0.1;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

VisionTrain()
{
   for_engine = null;
   point_list = new LinkedList<>();
   initial_points = new ArrayList<>(); 
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

IfaceEngine getEngine()                 { return for_engine; }
void setEngine(IfaceEngine e)           { for_engine = e; }

Point2D getFrontPoint()
{
   if (initial_points != null) return null;
   
   return point_list.getFirst();
}


Point2D getRearPoint()
{
   if (initial_points != null) return null;
   
   return point_list.getLast();
}


/********************************************************************************/
/*                                                                              */
/*      Update methods                                                          */
/*                                                                              */
/********************************************************************************/

void updateTrain(Point p0,Point p1)
{
   Point2D p0d = (p0 == null ? null : new Point2D.Double(p0.x,p0.y));
   Point2D p1d = (p1 == null ? null : new Point2D.Double(p1.x,p1.y));
   updateTrain(p0d,p1d);
}



void updateTrain(Point2D p0,Point2D p1)
{
   if (p0 == null && p1 == null) return;
   
   if (initial_points != null) {
      if (initial_points.isEmpty()) {
         // first time -- set initial points
         if (p0 != null && p1 != null) {
            initial_points.add(p0);
            initial_points.add(p1);
            point_list.add(p0);
            point_list.add(p1);
          }
       }
      if (p0 != null &&
            isBetween(initial_points.get(0),initial_points.get(1),
                  p0,INITIAL_TOLERANCE)) {
         Collections.reverse(point_list);
         initial_points = null;
       }
      else if (p1 != null &&
            isBetween(initial_points.get(0),initial_points.get(1),
                  p1,INITIAL_TOLERANCE)) {
         initial_points = null;
       }
      else if (p0 != null && p1 != null) {
         // reset initial points
         initial_points.clear();
         initial_points.add(p0);
         initial_points.add(p1);
         point_list.clear();
         point_list.add(p0);
         point_list.add(p1);
       }
    }
   
   if (point_list.size() >= 2 && initial_points == null) {
      // normal case where we have direction
      if (p0 != null && p1 != null) {
         double d0 = p0.distance(point_list.getFirst());
         double d1 = p0.distance(point_list.getLast());
         if (d0 < d1) {
            Point2D px = p0;
            p0 = p1;
            p1 = px;
          }
       }
      else if (p0 == null && p1 != null) {
         p0 = p1;
         p1 = null;
       }
      
      if (p0 != null) {
         point_list.addFirst(p0);
       }
      if (p1 != null) {
         point_list.removeLast();
         point_list.addLast(p1);
       }
      cleanPoints();
    }
}



/********************************************************************************/
/*                                                                              */
/*      Check if new point is between the intitial points                       */
/*                                                                              */
/********************************************************************************/

private boolean isBetween(Point2D start, Point2D end, Point2D p,double tolerance) {
   // Use the built-in Java method to get the distance from the point to the line segment
   double distance = Line2D.ptSegDist(start.getX(), start.getY(),
         end.getX(), end.getY(),
         p.getX(), p.getY());
   
   // The point is considered "on" the approximate line if the distance is within the tolerance
   if (distance <= tolerance) {
      // Additionally, verify the point is within the bounding box of the segment's endpoints.
      // This prevents points far outside the segment but on the infinite line from returning true.
      double minX = Math.min(start.getX(), end.getX());
      double maxX = Math.max(start.getX(), end.getX());
      double minY = Math.min(start.getY(), end.getY());
      double maxY = Math.max(start.getY(), end.getY());
      
      if (p.getX() >= minX - tolerance && p.getX() <= maxX + tolerance &&
            p.getY() >= minY - tolerance && p.getY() <= maxY + tolerance) {
         return true;
       }
    }
   return false;
}


/********************************************************************************/
/*                                                                              */
/*      Clean up the point set                                                  */
/*                                                                              */
/********************************************************************************/

private void cleanPoints()
{
   Point2D prev = null;
   for (ListIterator<Point2D> li = point_list.listIterator(); li.hasNext(); ) {
      Point2D cur = li.next();
      if (prev != null && li.hasNext()) {
         Point2D next = point_list.get(li.nextIndex());
         if (isBetween(prev,next,cur,LIST_TOLERANCE)) {
            li.remove();
          }
       }
      prev = cur;
    }
}



}       // end of class VisionTrain




/* end of VisionTrain.java */

