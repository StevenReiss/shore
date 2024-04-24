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

import edu.brown.cs.spr.shore.iface.IfaceConnection;
import edu.brown.cs.spr.shore.iface.IfaceDiagram;
import edu.brown.cs.spr.shore.iface.IfacePoint;
import edu.brown.cs.spr.shore.iface.IfaceSwitch;
import edu.brown.cs.spr.shore.iface.IfaceConstants.ShorePointType;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

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
private List<List<IfacePoint>> line_segments;  


private static final double BORDER_SPACE = 30;
private static final double MIN_WIDTH = 1200;
private static final double GAP_WIDTH = 5;
private static final double TRACK_WIDTH = 10;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ViewDiagramFx(ViewFactory fac,IfaceDiagram dgm)
{
   for_diagram = dgm;
   invert_y = false;
   double minx = Integer.MAX_VALUE;
   double maxx = 0;
   double miny = Integer.MAX_VALUE;
   double maxy = 0;
   for (IfacePoint pt : for_diagram.getPoints()) {
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
   Set<IfacePoint> done = new HashSet<>();
   for (IfacePoint pt0 : for_diagram.getPoints()) {
      if (!done.contains(pt0)) {
         List<IfacePoint> seg = computeLineSegment(pt0,done);
         if (seg != null) line_segments.add(seg);
       }
    }
}



private List<IfacePoint> computeLineSegment(IfacePoint pt,Set<IfacePoint> done)
{
   LinkedList<IfacePoint> pts = new LinkedList<>();
   
   pts.add(pt);
   done.add(pt);
   
   boolean fwd = true;
   IfacePoint endpt = null;
   IfacePoint bendpt = null;
   for (IfacePoint nextpt : pt.getConnectedTo()) {
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


private void augmentLineSegment(LinkedList<IfacePoint> pts,
      IfacePoint pt,IfacePoint prev,boolean fwd,Set<IfacePoint> done)
{
   if (done.contains(pt)) return;
   if (fwd) pts.addLast(pt);
   else pts.addFirst(pt);
   done.add(pt);
   
   IfacePoint endpt = null;
   
   for (IfacePoint nextpt : pt.getConnectedTo()) {
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
   gx.save();
   gx.clearRect(0,0,w,h);
   gx.setFill(new Color(0.8,1.0,0.8,0.5));
   gx.fillRect(0,0,w,h);
   
   predrawTurntables(gx);
   drawLines(gx);
   drawSwitches(gx);
   drawGaps(gx);
   // drawTurntables(); -- we need to know the state of the turntables
   // drawSignals();
   
   // drawTrains();
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
   
   gx.setLineWidth(TRACK_WIDTH);
   gx.setLineCap(StrokeLineCap.ROUND);
   gx.setLineJoin(StrokeLineJoin.ROUND);
   Color c = Color.BROWN.darker().darker();
   c = c.deriveColor(0,1.0,1.0,0.5);
   gx.setStroke(c);
   for (List<IfacePoint> seq : line_segments) {
      gx.beginPath();
      int ct = 0;
      for (IfacePoint pt : seq) {
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



private void drawSwitches(GraphicsContext gx)
{
   gx.save();
   gx.setTextAlign(TextAlignment.CENTER);
   gx.setTextBaseline(VPos.CENTER);
   Font ft0 = gx.getFont();
   Font ft = Font.font(ft0.getFamily(),FontWeight.BOLD,18);
   gx.setFont(ft);
   
   gx.setStroke(Color.GREEN);
   gx.setLineWidth(5);
   for (IfaceSwitch sw : for_diagram.getSwitches()) {
      IfacePoint tpt = null;
      switch (sw.getSwitchState()) {
         case UNKNOWN :
            continue;
         case N :
            tpt = sw.getNSensor().getAtPoint();
            break;
         case R :
            tpt = sw.getRSensor().getAtPoint();
            break;
       }
      if (tpt == null) continue;
      Point2D p0 = getCoords(sw.getPivotPoint());
      Point2D p1 = getCoords(tpt);
      gx.strokeLine(p0.getX(),p0.getY(),p1.getX(),p1.getY());
    }
   
   for (IfaceSwitch sw : for_diagram.getSwitches()) {
      IfacePoint pt = sw.getPivotPoint();
      String txt = sw.getId();
      Point2D cpt2 = getCoords(pt);
      Text t = new Text(txt);
      t.setFont(ft);
      Bounds b = t.getBoundsInLocal();
      gx.setFill(Color.WHITE);
      gx.fillOval(cpt2.getX()-b.getWidth()/2,cpt2.getY()-b.getHeight()/2,
           b.getWidth(),b.getHeight());
      gx.setFill(Color.BLACK);
      gx.fillText(txt,cpt2.getX(),cpt2.getY());
    }
   gx.restore();
}



private void drawGaps(GraphicsContext gx)
{  
   gx.save();
   
   gx.setStroke(Color.YELLOW);
   gx.setLineWidth(TRACK_WIDTH);
   
   double g = GAP_WIDTH/2.0;
   
   for (IfacePoint pt : for_diagram.getPoints()) {
      if (pt.getType() != ShorePointType.GAP) continue;
      if (pt.getDiagram() != for_diagram) continue;
      IfacePoint pt1 = null;
      IfacePoint pt2 = null;
      for (IfacePoint cpt : pt.getConnectedTo()) {
         if (pt1 == null) pt1 = cpt;
         else if (pt2 == null) pt2 = cpt;
       }
      if (pt2 == null) continue;
      Point2D cpt1 = getCoords(pt1);
      Point2D cpt2 = getCoords(pt2);
      Point2D cpt = getCoords(pt);
      double d1 = cpt.distance(cpt1);
      double d2 = cpt.distance(cpt2);
      double x1 = cpt.getX() + g/d1 * (cpt1.getX()-cpt.getX());
      double y1 = cpt.getY() + g/d1  * (cpt1.getY()-cpt.getY());
      gx.strokeLine(cpt.getX(),cpt.getY(),x1,y1);
      double x2 = cpt.getX() + g/d2 * (cpt2.getX()-cpt.getX());
      double y2 = cpt.getY() + g/d2 * (cpt2.getY()-cpt.getY());
      gx.strokeLine(cpt.getX(),cpt.getY(),x2,y2);
    }
   
   gx.restore();
}




private void predrawTurntables(GraphicsContext gx)
{
   gx.save();
   
   for (IfacePoint pt : for_diagram.getPoints()) {
      if (pt.getType() != ShorePointType.TURNTABLE) continue;
      Point2D cpt = getCoords(pt);
      double radius = 0;
      for (IfacePoint pt1 : pt.getConnectedTo()) {
         Point2D cpt1 = getCoords(pt1);
         double r = cpt.distance(cpt1);
         if (radius == 0 || radius > r) radius = r;
       }
      if (radius == 0) continue;
      gx.setFill(Color.LIGHTBLUE);
      gx.fillOval(cpt.getX()-radius,cpt.getY()-radius,2*radius,2*radius);
    }
   
   gx.restore();
}


      

private Point2D getCoords(IfacePoint pt)
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

