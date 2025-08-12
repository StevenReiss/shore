/********************************************************************************/
/*                                                                              */
/*              ModelSpeedZone.java                                             */
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



package edu.brown.cs.spr.shore.model;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import edu.brown.cs.spr.shore.iface.IfaceSensor;
import edu.brown.cs.spr.shore.iface.IfaceSpeedZone;

class ModelSpeedZone implements IfaceSpeedZone, ModelConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ModelSensor     start_sensor;
private ModelSensor     end_sensor;
private double          speed_percent;
private List<ModelSensor> all_sensors;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ModelSpeedZone(ModelBase mb,ModelSensor start,ModelSensor end,double speed)
{
   start_sensor = start;
   end_sensor = end;
   speed_percent = speed;
   all_sensors = getSensors(mb);
   if (all_sensors == null) {
      mb.noteError("Not path for speed zone from " + start + " to " + end);
    }
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public double getSpeedPercent()
{
   return speed_percent;
}


@Override public IfaceSensor getEndSensor()
{
   return start_sensor;
}


@Override public IfaceSensor getStartSensor()
{
   return end_sensor;
}


@Override public List<IfaceSensor> getZoneSensors() 
{
   List<IfaceSensor> rslt = new ArrayList<>();
   rslt.addAll(all_sensors);
   return rslt;
}


/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

private List<ModelSensor> getSensors(ModelBase mb)
{
   ModelPoint pt0 = start_sensor.getAtPoint();
   ModelPoint pt1 = end_sensor.getAtPoint();
   
   List<ModelSensor> sensors = findShortestPath(mb,pt0,pt1);
   
   return sensors;
}


private List<ModelSensor> findShortestPath(ModelBase mb,ModelPoint pt0,ModelPoint pt1)
{
   Map<ModelPoint,Double> distance = new HashMap<>();
   Map<ModelPoint,ModelPoint> pred = new HashMap<>();
   PriorityQueue<PointData> queue = new PriorityQueue<>();
   Set<ModelPoint> settled = new HashSet<>();
   
   distance.put(pt0,0.0);
   queue.add(new PointData(pt0,0));
   while (!queue.isEmpty()) {
      PointData cur = queue.remove();
      ModelPoint curpt = cur.getPoint();
      if (!settled.add(curpt)) continue;
      double d1 = distance.get(curpt);
      for (ModelPoint npt : curpt.getModelConnectedTo()) {
         double d = distance(curpt,npt);
         double d2 = d + d1;
         Double dnpt = distance.get(npt);
         if (dnpt == null || dnpt > d2) {
            distance.put(npt,d2);
            pred.put(npt,curpt);
            queue.add(new PointData(npt,d2));
          }
       }
    }
   
   if (distance.get(pt1) == null) return null;
   
   List<ModelSensor> rslt = new ArrayList<>();
   for (ModelPoint ptx = pt1; ptx != null; ptx = pred.get(ptx)) {
       ModelSensor ms = mb.findSensorForPoint(ptx);
       if (ms != null) rslt.add(0,ms);
    }
   
   return rslt;
}


private double distance(ModelPoint mpt0,ModelPoint mpt1)
{
   Point2D pt0 = mpt0.getPoint2D();
   Point2D pt1 = mpt1.getPoint2D();
   return pt0.distance(pt1);
}


private class PointData implements Comparable<PointData> {
   
   private ModelPoint for_point;
   private double point_distance;
   
   PointData(ModelPoint pt,double d) {
      for_point = pt;
      point_distance = d;
    }
   
   ModelPoint getPoint()                { return for_point; }
   
   @Override public int compareTo(PointData pd) {
      return Double.compare(point_distance,pd.point_distance);
    }
}

}       // end of class ModelSpeedZone




/* end of ModelSpeedZone.java */

