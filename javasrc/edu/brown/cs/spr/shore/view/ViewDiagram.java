/********************************************************************************/
/*                                                                              */
/*              ViewDiagram.java                                                */
/*                                                                              */
/*      Panel for a diagram                                                     */
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



package edu.brown.cs.spr.shore.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JTextArea;

import edu.brown.cs.spr.shore.iface.IfaceDiagram;
import edu.brown.cs.spr.shore.iface.IfaceDiagram.DiagramPoint;

class ViewDiagram extends JPanel implements ViewConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceDiagram for_diagram;
private Rectangle2D display_bounds;
private boolean invert_y;
private double scale_value;
private List<List<DiagramPoint>> line_segments;

private static final int BORDER_SPACE = 30;
private static final int MIN_WIDTH = 1200;

private static final long serialVersionUID = 1;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ViewDiagram(IfaceDiagram dgm) 
{
   for_diagram = dgm;
   invert_y = false;
   double minx = Integer.MAX_VALUE;
   double maxx = 0;
   double miny = Integer.MAX_VALUE;
   double maxy = 0;
   for (DiagramPoint pt : for_diagram.getPoints()) {
      minx = Math.min(minx,pt.getX());
      maxx = Math.max(maxx,pt.getX());
      miny = Math.min(miny,pt.getY());
      maxy = Math.max(maxy,pt.getY());
    }
   display_bounds = new Rectangle2D.Double(minx,miny,maxx-minx,maxy-miny);
   double ratio = (maxx-minx)/(maxy-miny);
   
   Dimension d = new Dimension(MIN_WIDTH,(int)(MIN_WIDTH/ratio));
         
   setMinimumSize(d);
   setPreferredSize(d);
   
   setupLineSegments();
}



/********************************************************************************/
/*                                                                              */
/*      Setup methods (one-time)                                                */
/*                                                                              */
/********************************************************************************/

private void setupLineSegments()
{
   line_segments = new ArrayList<>();
   Set<DiagramPoint> done = new HashSet<>();
   for (DiagramPoint pt0 : for_diagram.getPoints()) {
      if (!done.contains(pt0)) {
         List<DiagramPoint> seg = computeLineSegment(pt0,done);
         if (seg != null) line_segments.add(seg);
       }
    }
}



private List<DiagramPoint> computeLineSegment(DiagramPoint pt,Set<DiagramPoint> done)
{
   LinkedList<DiagramPoint> pts = new LinkedList<>();
   
   pts.add(pt);
   done.add(pt);
   
   boolean fwd = true;
   DiagramPoint endpt = null;
   for (DiagramPoint nextpt : pt.getConnectedTo()) {
      if (nextpt.getDiagram() != pt.getDiagram()) continue;
      if (done.contains(nextpt)) {
         if (endpt == null) endpt = nextpt;
         continue;
       }
      augmentLineSegment(pts,nextpt,pt,fwd,done);
      if (!fwd) {
         endpt = null;
         break;
       }
      fwd = false;
    }
   
   if (pts.size() <= 1) return null;
   if (endpt != null && !fwd) pts.addFirst(pt);
   
   return pts;
}


private void augmentLineSegment(LinkedList<DiagramPoint> pts,
      DiagramPoint pt,DiagramPoint prev,boolean fwd,Set<DiagramPoint> done)
{
   if (done.contains(pt)) return;
   if (fwd) pts.addLast(pt);
   else pts.addFirst(pt);
   done.add(pt);
   
   DiagramPoint endpt = null;
   
   for (DiagramPoint nextpt : pt.getConnectedTo()) {
      if (nextpt == prev) continue;
      if (done.contains(nextpt)) {
         if (endpt == null) endpt = nextpt;
         continue;
       }
      if (nextpt.getDiagram() != pt.getDiagram()) continue;
      augmentLineSegment(pts,nextpt,pt,fwd,done);
      return;
    }
   
   if (endpt != null) {
      if (fwd) pts.addLast(endpt);
      else pts.addFirst(endpt);
    }
}




/********************************************************************************/
/*                                                                              */
/*      Paint methods                                                           */
/*                                                                              */
/********************************************************************************/

@Override public void paintComponent(Graphics g0)
{
   Graphics2D g2 = (Graphics2D) g0;
   
   g2.setBackground(Color.YELLOW);
   
   setupScaling();
   
   Stroke rail = new BasicStroke(10f,BasicStroke.CAP_ROUND,
         BasicStroke.JOIN_ROUND);
   g2.setStroke(rail);
   g2.setColor(Color.GRAY);
   
   for (List<DiagramPoint> seg : line_segments) {
      Path2D.Double path = null;
      for (DiagramPoint pt : seg) {
         Point2D pt0 = getCoords(pt);
         if (path == null) {
            path = new Path2D.Double();
            path.moveTo(pt0.getX(),pt0.getY());
          }
         else {
            path.lineTo(pt0.getX(),pt0.getY());
          }
       }
      g2.draw(path);
    }
 
  
   // next for each point, draw what should be there (gap,signal,switch,sensor,block,...)
   
}



private void setupScaling()
{
   Dimension sz = getSize();
   double xpix = sz.getWidth() - 2 * BORDER_SPACE;
   double xval = display_bounds.getWidth() / xpix;
   double ypix = sz.getHeight() - 2 * BORDER_SPACE;
   double yval = display_bounds.getHeight() / ypix;
   scale_value = Math.min(xval,yval);
}


private Point2D getCoords(DiagramPoint pt)
{
   double x = pt.getX();
   double y = pt.getY();
   x = x - display_bounds.getX();
   if (invert_y) {
      double y0 = display_bounds.getY();
      y = (display_bounds.getHeight() + y0) - y + y0;
    }
   else y = y - display_bounds.getY();
   
   x = BORDER_SPACE + (x) / scale_value;
   y = BORDER_SPACE + y / scale_value;
   Point2D.Double rslt = new Point2D.Double(x,y);
   return rslt;
}


}       // end of class ViewDiagram




/* end of ViewDiagram.java */

