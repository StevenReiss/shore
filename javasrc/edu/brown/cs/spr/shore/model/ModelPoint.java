/********************************************************************************/
/*                                                                              */
/*              ModelPoint.java                                                 */
/*                                                                              */
/*      Representation of a point on the layout                                 */
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
import java.util.Collection;
import java.util.List;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.spr.shore.iface.IfacePoint;

class ModelPoint implements ModelConstants, IfacePoint 
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ModelDiagram in_diagram;
private String  point_id;
private double  point_x;
private double  point_y;
private ShorePointType point_type;
private List<ModelPoint> conn_points;
private String ref_id;
private ModelBlock in_block;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ModelPoint(ModelDiagram dgm,Element xml)
{
   in_diagram = dgm;
   point_id = IvyXml.getAttrString(xml,"ID");
   point_x = IvyXml.getAttrDouble(xml,"X",0);
   point_y = IvyXml.getAttrDouble(xml,"Y",0);
   point_type = IvyXml.getAttrEnum(xml,"TYPE",ShorePointType.OTHER); 
   ref_id = IvyXml.getAttrString(xml,"REF");
   
   conn_points = new ArrayList<>();
}



/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

void connectTo(ModelPoint pt)
{
   conn_points.add(pt);
   pt.conn_points.add(this);
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public ModelDiagram getDiagram()      { return in_diagram; } 

String getId()                                  { return point_id; }

@Override public double getX()                  { return point_x; } 

@Override public double getY()                  { return point_y; }

Point2D getPoint2D()                            
{ 
   return new Point2D.Double(point_x,point_y);
}

void setPoint2D(double x,double y)
{
   point_x = x;
   point_y = y;
}

@Override public ShorePointType getType()               { return point_type; }   

String getRefId()                               { return ref_id; }

List<ModelPoint> getModelConnectedTo()
{
   return conn_points;
}

@Override public Collection<IfacePoint> getConnectedTo()
{
   return new ArrayList<>(conn_points); 
}



@Override public ModelBlock getBlock()                  { return in_block; }
void setBlock(ModelBlock blk)                   { in_block = blk; }



/********************************************************************************/
/*                                                                              */
/*      Output Methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String toString()
{
   return point_id + "[" + point_type + "]";
}


}       // end of class ModelPointImpl




/* end of ModelPointImpl.java */

