/********************************************************************************/
/*                                                                              */
/*              ViewDiagramFx.java                                              */
/*                                                                              */
/*      JavaFx implementation of an interactive diagram                         */
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.brown.cs.spr.shore.iface.IfaceDiagram;
import edu.brown.cs.spr.shore.iface.IfaceDiagram.DiagramPoint;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

class ViewDiagramFx extends AnchorPane implements ViewConstants
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

private static final double BORDER_SPACE = 30;
private static final double MIN_WIDTH = 1200;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ViewDiagramFx(IfaceDiagram dgm)
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
   display_bounds = new Rectangle2D(minx,miny,maxx-minx,maxy-miny);
   double ratio = display_bounds.getWidth() / display_bounds.getHeight();
   double minw = MIN_WIDTH;
   double minh = MIN_WIDTH / ratio;
   
   setMinSize(minw,minh);
   setPrefSize(minw,minh);
   
   setupLineSegments();
   
   ResizableCanvas canvas = new ResizableCanvas();
   AnchorPane.setTopAnchor(canvas,0.0);
   AnchorPane.setBottomAnchor(canvas,0.0);
   AnchorPane.setLeftAnchor(canvas,0.0);
   AnchorPane.setRightAnchor(canvas,0.0);
   getChildren().add(canvas);
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
   DiagramPoint bendpt = null;
   for (DiagramPoint nextpt : pt.getConnectedTo()) {
      if (nextpt.getDiagram() != pt.getDiagram()) continue;
      if (done.contains(nextpt)) {
         if (endpt == null) endpt = nextpt;
         else if (bendpt == null) bendpt = nextpt;
         continue;
       }
      augmentLineSegment(pts,nextpt,pt,fwd,done);
      if (!fwd) {
         endpt = null;
         break;
       }
      fwd = false;
    }
   
   if (endpt != null) pts.addFirst(endpt);
   if (fwd && bendpt != null) pts.addLast(bendpt);
   
   if (pts.size() <= 1) return null;
   
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
/*      Diagram drawing methods                                                 */
/*                                                                              */
/********************************************************************************/

private void drawDiagram(Canvas v)
{
   double w = v.getWidth();
   double h = v.getHeight();
   setupScaling(w,h);

   GraphicsContext gx = v.getGraphicsContext2D();
   gx.clearRect(0,0,w,h);
   
   drawLines(gx);
}




private void setupScaling(double w,double h)
{
   double xpix = w - 2 * BORDER_SPACE;
   double xval = display_bounds.getWidth() / xpix;
   double ypix = h - 2 * BORDER_SPACE;
   double yval = display_bounds.getHeight() / ypix;
   scale_value = Math.max(xval,yval);
}


private void drawLines(GraphicsContext gx)
{
   gx.save();
   
   gx.setLineWidth(10);
   gx.setLineCap(StrokeLineCap.ROUND);
   gx.setLineJoin(StrokeLineJoin.ROUND);
   for (List<DiagramPoint> seq : line_segments) {
      gx.beginPath();
      int ct = 0;
      for (DiagramPoint pt : seq) {
         Point2D pt0 = getCoords(pt);
         if (ct++ == 0) {
            gx.moveTo(pt0.getX(),pt0.getY());
          }
         else {
            gx.lineTo(pt0.getX(),pt0.getY());
          }
       }
      gx.stroke();
    }
   
   gx.restore();
}



private Point2D getCoords(DiagramPoint pt)
{
   double x = pt.getX();
   double y = pt.getY();
   x = x - display_bounds.getMinX();
   if (invert_y) {
      double y0 = display_bounds.getMinY();
      y = (display_bounds.getHeight() + y0) - y + y0;
    }
   else y = y - display_bounds.getMinY();
   
   x = BORDER_SPACE + (x) / scale_value;
   y = BORDER_SPACE + y / scale_value;
   Point2D rslt = new Point2D(x,y);
   return rslt;
}




/********************************************************************************/
/*                                                                              */
/*      Drawing canvas (for resizing)                                           */
/*                                                                              */
/********************************************************************************/

private class ResizableCanvas extends Canvas {
   
   ResizableCanvas() {
      super();
      
      double ratio = display_bounds.getWidth() / display_bounds.getHeight();
      double minw = MIN_WIDTH;
      double minh = MIN_WIDTH / ratio;
      
      setMinSize(minw,minh);
      setPrefSize(minw,minh);
    }
   
   private void paint() {
      GraphicsContext gx = getGraphicsContext2D();
      gx.clearRect(0,0, getWidth(),getHeight());
      drawDiagram(this);
    }
   
   @Override public boolean isResizable()               { return true; }
   
   @Override public void resize(double w,double h) {
      super.setWidth(w);
      super.setHeight(h);
      paint();
    }
   
}       // end of inner class ResizableCanvas



}       // end of class ViewDiagramFx




/* end of ViewDiagramFx.java */

