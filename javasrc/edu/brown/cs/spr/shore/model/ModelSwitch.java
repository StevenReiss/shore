/********************************************************************************/
/*                                                                              */
/*              ModelSwitch.java                                                */
/*                                                                              */
/*      Representation of a switch                                              */
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

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.spr.shore.iface.IfaceSensor;
import edu.brown.cs.spr.shore.iface.IfaceSwitch;
import edu.brown.cs.spr.shore.shore.ShoreLog;

class ModelSwitch implements IfaceSwitch, ModelConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ModelBase for_model;
private String switch_id;
private ShoreSwitchState switch_state;
private byte tower_id;
private byte tower_index;
private byte tower_rindex;
private ModelPoint pivot_point;
private ModelPoint n_point;
private ModelPoint r_point;
private ModelPoint entry_point;
private ModelSensor n_sensor;
private ModelSensor r_sensor;
private String associated_name;
private ModelSwitch associated_switch;
private boolean is_flipped;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ModelSwitch(ModelBase model,Element xml)
{
   for_model = model;
   switch_id = IvyXml.getAttrString(xml,"ID");
   pivot_point = model.getPointById(IvyXml.getAttrString(xml,"POINT"));
   n_point = model.getPointById(IvyXml.getAttrString(xml,"N"));
   r_point = model.getPointById(IvyXml.getAttrString(xml,"R"));
   tower_id = (byte) IvyXml.getAttrInt(xml,"TOWER");
   tower_index = (byte) IvyXml.getAttrInt(xml,"INDEX");
   tower_rindex = (byte) IvyXml.getAttrInt(xml,"RINDEX",-1);
   associated_name = IvyXml.getAttrString(xml,"ASSOCIATE");
   is_flipped = IvyXml.getAttrBool(xml,"FLIPPED");      // if wires are flipped
   switch_state = ShoreSwitchState.UNKNOWN;
   associated_switch = null;
   n_sensor = null;
   r_sensor = null;
   
   if (n_point == null) {
      model.noteError("Switch n-point not found for " + switch_id);
    }
   if (r_point == null) {
      model.noteError("Switch r-point not found for " + switch_id);
    }
   if (pivot_point == null) {
      model.noteError("Switch pivot point not found for " + switch_id);
    }
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String getId()                         { return switch_id; } 

@Override public ModelPoint getPivotPoint()     { return pivot_point; }  

ModelPoint getNPoint()                          { return n_point; }

ModelPoint getRPoint()                          { return r_point; }

ModelPoint getEntryPoint()                      { return entry_point; }

ModelSwitch getAssociatedSwitch()
{
   if (associated_name == null) return null;
   if (associated_switch != null) return associated_switch;
   associated_switch = for_model.getSwitchById(associated_name); 
   return associated_switch;
}


@Override public IfaceSensor getNSensor()       { return n_sensor; }

@Override public IfaceSensor getRSensor()       { return r_sensor; }

@Override public ShoreSwitchState getSwitchState()   { return switch_state; }

@Override public void setSwitch(ShoreSwitchState st)
{
   if (switch_state == st) return;
   
   ShoreLog.logD("MODEL","Set switch state " + switch_id + "=" + st);
   
   switch_state = st;
   for_model.fireSwitchChanged(this); 
   
   ModelSwitch sw = getAssociatedSwitch();
   if (sw != null) {
      sw.setSwitch(st);
    }
}


@Override public byte getTowerId()              { return tower_id; }

@Override public byte getTowerSwitch()          { return tower_index; }
@Override public byte getTowerRSwitch()         { return tower_rindex; } 




/********************************************************************************/
/*                                                                              */
/*      Normalization methods                                                   */
/*                                                                              */
/********************************************************************************/

void normalizeSwitch(ModelBase mdl)
{
   findNRPoints(mdl);
   
   n_sensor = findSensor(mdl,n_point);
   r_sensor = findSensor(mdl,r_point);
   if (n_sensor != null) n_sensor.assignSwitch(this,ShoreSwitchState.N);
   if (r_sensor != null) r_sensor.assignSwitch(this,ShoreSwitchState.R); 
   
// entry_block = pivot_point.getBlock();
// n_block = n_point.getBlock();
// r_block = r_point.getBlock();
}



void findNRPoints(ModelBase mdl)
{
   ModelPoint pvt = getPivotPoint();
   if (pvt == null) {
      mdl.noteError("Switch " + getId() + " has no associated pivot point");
      return;
    }
   ModelPoint [] next = new ModelPoint[3];
   int ctr = 0;
   for (ModelPoint pt1 : pvt.getModelConnectedTo()) { 
      if (ctr >= next.length) {
         mdl.noteError("Switch " + getId() + " pivot " + pvt.getId() + 
               " has too many connections");
         return;
       }
      next[ctr++] = pt1;
    }
   if (ctr != 3) {
      mdl.noteError("Switch " + getId() + " pivot " + pvt.getId() +
            " has too few connections");
      return;
    }
   Point2D.Double [] vecs = new Point2D.Double[3];
   for (int i = 0; i < 3; ++i) {
      vecs[i] = normalize(next[i].getX()-pvt.getX(),next[i].getY()-pvt.getY());
    }
   double dp01 = dotProduct(vecs[0],vecs[1]);
   double dp02 = dotProduct(vecs[0],vecs[2]);
   double dp12 = dotProduct(vecs[1],vecs[2]);
// ShoreLog.logD("DOT PRODUCTS " + getId() + " " + pvt.getId() + " " +
//       next[0].getId() + " " + next[1].getId() + " " + next[2].getId() + " " +
//       dp01 + " " + dp02 + " " + dp12);
   
   ModelPoint np = null;
   ModelPoint rp = null;
   if (dp01 > 0) {
      // 0 and 1 are n/r
      entry_point = next[2];
      if (dp02 < dp12) {
         np = next[0];
         rp = next[1];
       }
      else {
         np = next[1];
         rp = next[0];
       }
    }
   else if (dp02 > 0) {
      // 0 and 2 are n/r
      entry_point = next[1];
      if (dp01 < dp12) {
         rp = next[2];
         np = next[0];
       }
      else {
         rp = next[0];
         np = next[2];
       }
    }
   else if (dp12 > 0) {
      // 1and 2 are n/r
      entry_point = next[0];
      if (dp01 < dp02) {
         np = next[1];
         rp = next[2];
       }
      else {
         np = next[2];
         rp = next[1];
       }
    }
   else {
      mdl.noteError("Switch " + getId() + " seems to be connected badly");
      return;
    }
   
   if (n_point !=  null && n_point != np) {
      mdl.noteError("Switch " + getId() + " has inconsistent end points " +
            n_point.getId() + " and " + np.getId());
    }
   if (r_point !=  null && r_point != rp) {
      mdl.noteError("Switch " + getId() + " has inconsistent end points " +
            r_point.getId() + " and " + rp.getId());
    }
   
   n_point = np;
   r_point = rp;
}



private Point2D.Double normalize(double x,double y)
{
   double tot = Math.sqrt(x*x + y*y);
   if (tot > 0) {
      x = (x/tot);
      y = (y/tot);
    }
   
   return new Point2D.Double(x,y);
}

private double dotProduct(Point2D p0,Point2D p1)
{
   return p0.getX() * p1.getX() + p0.getY() * p1.getY();
}


private ModelSensor findSensor(ModelBase mdl,ModelPoint pt)
{
   if (pt == null) return null;
   
   ModelPoint p = findSensorPoint(mdl,pivot_point,pt);
   return mdl.findSensorForPoint(p); 
}

private ModelPoint findSensorPoint(ModelBase mdl,ModelPoint frm,ModelPoint to)
{
   if (to.getType() == ShorePointType.SENSOR) return to;
   if (to.getType() == ShorePointType.SIGNAL) {
      ModelSensor sen = mdl.findSensorForPoint(to);
      if (sen != null) return to;
    }
   
   ModelPoint next = null;
   for (ModelPoint npt : to.getModelConnectedTo()) {
      if (npt.equals(frm)) continue;
      if (next == null) next = npt;
      else return null;
    }
   if (next == null) return null;
   return findSensorPoint(mdl,to,next);
}



/********************************************************************************/
/*                                                                              */
/*      Output Methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String toString()
{
   String flip = "";
   if (is_flipped) flip = "*";
   return "SWITCH[" + switch_id + flip + "]";
}



}       // end of class ModelSwitch




/* end of ModelSwitch.java */

