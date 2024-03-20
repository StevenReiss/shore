/********************************************************************************/
/*                                                                              */
/*              ModelPointImpl.java                                             */
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

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.spr.shore.model.ModelConstants.ModelPoint;

class ModelPointImpl implements ModelPoint, ModelConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ModelDiagramImpl in_diagram;
private String  point_id;
private double  point_x;
private double  point_y;
private PointType point_type;
private List<ModelPoint> to_points;
private List<ModelPoint> from_points;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ModelPointImpl(ModelDiagramImpl dgm,Element xml)
{
   in_diagram = dgm;
   point_id = IvyXml.getAttrString(xml,"ID");
   point_x = IvyXml.getAttrDouble(xml,"X",0);
   point_y = IvyXml.getAttrDouble(xml,"Y",0);
   point_type = IvyXml.getAttrEnum(xml,"TYPE",PointType.STRAIGHT);
   to_points = new ArrayList<>();
   from_points = new ArrayList<>();
}


void resolve(ModelBase mdl,Element xml)
{
   for (Element toxml : IvyXml.children(xml,"TO")) {
      String pid = IvyXml.getAttrString(toxml,"POINT");
      ModelPointImpl topt = mdl.getPointById(pid);  
      if (topt != null) {
         to_points.add(topt);
         topt.from_points.add(this);
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

ModelDiagram getDiagram()                       { return in_diagram; }

@Override public String getId()                 { return point_id; }

@Override public double getX()                  { return point_x; }

@Override public double getY()                  { return point_y; }

@Override public PointType getType()            { return point_type; }

@Override public List<ModelPoint> getFromPoints()
{
   return from_points;
}

@Override public List<ModelPoint> getToPoints()
{
   return to_points;
}







}       // end of class ModelPointImpl




/* end of ModelPointImpl.java */

