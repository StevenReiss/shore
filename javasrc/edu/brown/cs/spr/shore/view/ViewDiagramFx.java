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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceDiagram;
import edu.brown.cs.spr.shore.iface.IfacePoint;
import edu.brown.cs.spr.shore.iface.IfaceSensor;
import edu.brown.cs.spr.shore.iface.IfaceSignal;
import edu.brown.cs.spr.shore.iface.IfaceSwitch;
import edu.brown.cs.spr.shore.iface.IfaceModel.ModelCallback;
import edu.brown.cs.spr.shore.iface.IfaceTrains.EngineCallback;
import edu.brown.cs.spr.shore.shore.ShoreLog;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

class ViewDiagramFx extends Pane implements ViewConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ViewFactory view_factory;
private IfaceDiagram for_diagram;
private Rectangle2D display_bounds;
private boolean invert_y;
private double scale_value;
private List<List<IfacePoint>> line_segments;
private Map<IfaceSwitch,SwitchDrawData> switch_map;
private Map<IfaceSignal,SignalDrawData> signal_map;
private Map<IfaceSensor,SensorDrawData> sensor_map;
private Map<IfaceBlock,BlockDrawData> block_map;


private static final double BORDER_SPACE = 30;
private static final double MIN_WIDTH = 1200;
private static final double GAP_WIDTH = 8;
private static final double TRACK_WIDTH = 10;
private static final double SWITCH_ARROW_WIDTH = 6;
private static final double SWITCH_ARROW_LENGTH = 40;
private static final double SIGNAL_DISTANCE = 20;
private static final double SIGNAL_LIGHT = 8;           
private static final double SIGNAL_GAP = 2;
private static final double SENSOR_RADIUS = 3;

private static final Color BACKGROUND_COLOR = new Color(0.8,1.0,0.8,0.5);
private static final Color TRACK_COLOR;
private static final Color GAP_COLOR = Color.YELLOW;
private static final Color SWITCH_ARROW_COLOR = Color.GREEN;
private static final Color SWITCH_BACKGROUD_COLOR = Color.WHITE;
private static final Color SWITCH_LABEL_COLOR = Color.BLACK;
private static final Color SIGNAL_BACKGROUND = Color.WHITE;
private static final Color SIGNAL_OFF = Color.LIGHTGRAY;
private static final Color SIGNAL_STROKE = Color.BLACK;
private static final Color SENSOR_UNKNOWN_COLOR = Color.LIGHTGRAY;
private static final Color SENSOR_OFF_COLOR = Color.LIGHTGREEN;
private static final Color SENSOR_ON_COLOR = Color.RED;
private static final Color BLOCK_BACKGROUD_COLOR = Color.BLACK;
private static final Color BLOCK_BACKGROUND_UNKNOWN_COLOR = Color.DARKGRAY;
private static final Color BLOCK_BACKGROUD_INUSE_COLOR = Color.RED;
private static final Color BLOCK_BACKGROUD_PENDING_COLOR = Color.YELLOW;
private static final Color BLOCK_LABEL_COLOR = Color.WHITE;
private static final Color BLOCK_LABEL_INUSE_COLOR = Color.WHITE;
private static final Color BLOCK_LABEL_PENDING_COLOR = Color.BLACK;

private static final Font SWITCH_FONT;
private static final Font BLOCK_FONT;

static {
   Color c = Color.BROWN.darker().darker();
   TRACK_COLOR = c.deriveColor(0,1.0,1.0,0.5);
   Font ft0 = Font.getDefault();
   SWITCH_FONT = Font.font(ft0.getFamily(),FontWeight.BOLD,18);
   BLOCK_FONT = SWITCH_FONT;
}

      


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ViewDiagramFx(ViewFactory fac,IfaceDiagram dgm)
{
   view_factory = fac;
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
   
   switch_map = new HashMap<>();
   signal_map = new HashMap<>();
   sensor_map = new HashMap<>();
   block_map = new HashMap<>();
   
   CallbackHandler hdlr = new CallbackHandler();
   fac.getLayoutModel().addModelCallback(hdlr);
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

private void drawDiagram()
{
   double w = getWidth();
   double h = getHeight();
   setupScaling(w,h);
   
   getChildren().clear();
   
   BackgroundFill fill = new BackgroundFill(BACKGROUND_COLOR,
         CornerRadii.EMPTY,Insets.EMPTY);
   Background bkg = new Background(fill);
   setBackground(bkg);
   
   predrawTurntables();
   drawTracks();
   drawGaps();
   
   drawSwitches();
   drawSignals();
   drawSensors();
   drawBlocks();
   
   // drawTurntables(); -- we need to know the state of the turntables
   
   // drawTrains();
}



/********************************************************************************/
/*                                                                              */
/*      Track drwaing                                                           */
/*                                                                              */
/********************************************************************************/

private void predrawTurntables()
{
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
      Circle c = new Circle(cpt.getX(),cpt.getY(),radius,
            Color.LIGHTBLUE);
      getChildren().add(c);
    }
}



private void drawTracks()
{
   Path path = new Path();
   path.setStrokeWidth(TRACK_WIDTH);
   path.setStrokeLineCap(StrokeLineCap.ROUND);
   path.setStrokeLineJoin(StrokeLineJoin.ROUND);
   path.setStroke(TRACK_COLOR);
   
   for (List<IfacePoint> seg : line_segments) {
      int ct = 0;
      for (IfacePoint pt : seg) {
         Point2D pt0 = getCoords(pt);
         PathElement e = null;
         if (ct++ == 0) {
            e = new MoveTo(pt0.getX(),pt0.getY());
          }
         else {
            e = new LineTo(pt0.getX(),pt0.getY());
          }
         path.getElements().add(e);
       }
    }
   
   getChildren().add(path);
}


private void drawGaps()
{  
   
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
      double x2 = cpt.getX() + g/d2 * (cpt2.getX()-cpt.getX());
      double y2 = cpt.getY() + g/d2 * (cpt2.getY()-cpt.getY());
      Line line = new Line(x1,y1,x2,y2);
      line.setStroke(GAP_COLOR);
      line.setStrokeWidth(TRACK_WIDTH);
      line.setStrokeLineCap(StrokeLineCap.BUTT);
      getChildren().add(line);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Switch information                                                      */
/*                                                                              */
/********************************************************************************/

private void drawSwitches()
{
   switch_map.clear();
   
   for (IfaceSwitch sw : for_diagram.getSwitches()) {
      IfacePoint pt = sw.getPivotPoint();
      Point2D cpt2 = getCoords(pt);
      Line arrow = new Line(cpt2.getX(),cpt2.getY(),cpt2.getX(),cpt2.getY());
      arrow.setStroke(SWITCH_ARROW_COLOR);
      arrow.setStrokeWidth(SWITCH_ARROW_WIDTH);
      arrow.setVisible(false);
      getChildren().add(arrow);
      
      String txt = sw.getId();
      Text t = new Text(txt);
      t.setFont(SWITCH_FONT);
      t.setFill(SWITCH_LABEL_COLOR);  
      t.setStroke(SWITCH_LABEL_COLOR);
      Bounds b = t.getBoundsInLocal();
      double w0 = b.getWidth();
      double w = w0 + 8;
      double h = b.getHeight();
      t.setTextAlignment(TextAlignment.CENTER);
      t.setTextOrigin(VPos.CENTER);
      t.setX(cpt2.getX() - w0/2);
      t.setY(cpt2.getY());
      
      Shape bkg = new Ellipse(cpt2.getX(),cpt2.getY(),w/2,h/2);
      w -= 2;
      bkg = new Rectangle(cpt2.getX()-w/2,cpt2.getY()-h/2,w,h); 
      
      bkg.setFill(SWITCH_BACKGROUD_COLOR);
      
      getChildren().add(bkg);
      getChildren().add(t);
      
      SwitchDrawData sdd = new SwitchDrawData(sw,arrow,bkg,t);
      sdd.doSetArrow();
      switch_map.put(sw,sdd);
    }
}




private class SwitchDrawData {
   
   private Line switch_arrow;
   private IfaceSwitch for_switch;
   
   SwitchDrawData(IfaceSwitch sw,Line arrow,Node bkg,Node lbl) {
      for_switch = sw;
      switch_arrow = arrow;
      SwitchHandler hdlr = new SwitchHandler(for_switch);
      bkg.setOnMouseClicked(hdlr);
      bkg.setOnMousePressed(hdlr);
      lbl.setOnMouseClicked(hdlr);
      lbl.setOnMousePressed(hdlr);
    }
   
   void setArrow() {
      if (Platform.isFxApplicationThread()) {
         doSetArrow();
       }
      else {
         Platform.runLater(() -> doSetArrow());
       }
    }
   
   void doSetArrow() {
      IfacePoint tpt = null;
      switch (for_switch.getSwitchState()) {
         case UNKNOWN :
            break;
         case N :
            tpt = for_switch.getNSensor().getAtPoint();
            break;
         case R :
            tpt = for_switch.getRSensor().getAtPoint();
            break;
       }
      if (tpt == null) {
         switch_arrow.setVisible(false);
       }
      else {
         Point2D p0 = getCoords(for_switch.getPivotPoint());
         Point2D p1 = getCoords(tpt);
         double d = p0.distance(p1);
         double x0 = p0.getX() + (p1.getX()-p0.getX())/d*SWITCH_ARROW_LENGTH;
         double y0 = p0.getY() + (p1.getY()-p0.getY())/d*SWITCH_ARROW_LENGTH;
         switch_arrow.endXProperty().set(x0);
         switch_arrow.endYProperty().set(y0);
         switch_arrow.setVisible(true);
       }
    }
}



private class SwitchHandler implements EventHandler<MouseEvent> {
  
   private IfaceSwitch for_switch;
   
   SwitchHandler(IfaceSwitch sw) {
      for_switch = sw;
    }
   
   @Override public void handle(MouseEvent evt) { 
      ShoreSwitchState ss = for_switch.getSwitchState();
      if (evt.getEventType() == MouseEvent.MOUSE_CLICKED) {
         ShoreSwitchState nss = ShoreSwitchState.UNKNOWN;
         switch (ss) {
            case UNKNOWN :
            case R :
               nss = ShoreSwitchState.N;
               break;
            case N :
               nss = ShoreSwitchState.R;
               break;
          }
         if (nss != ShoreSwitchState.UNKNOWN) { 
            view_factory.getSafetyModel().setSwitch(for_switch,nss);  
          }
       }
    }
   
}       // end of inner class SwitchHandler



/********************************************************************************/
/*                                                                              */
/*      Signal management                                                       */
/*                                                                              */
/********************************************************************************/

private void drawSignals()
{
   signal_map.clear();
   
   for (IfaceSignal sig : for_diagram.getSignals()) {
      if (sig.getAtPoints().isEmpty()) continue;
      IfacePoint pt = sig.getAtPoints().get(0);
      IfacePoint npt = sig.getNextPoint();
      Point2D cpt = getCoords(pt);
      Point2D cnpt = getCoords(npt);
      double d = cpt.distance(cnpt);
      double dx = (cnpt.getY() - cpt.getY())/d;
      double dy = -(cnpt.getX() - cpt.getX())/d;
      double mx = cpt.getX() + SIGNAL_DISTANCE * dx;
      double my = cpt.getY() + SIGNAL_DISTANCE * dy;
      
      int nlight = 2;
      switch (sig.getSignalType()) {
         case ENGINE :
         case RG :
         case ENGINE_ANODE :
         case RG_ANODE :
            nlight = 2;
            break;
         case RGY :
         case RGY_ANODE :
            nlight = 3;
            break;
       }
      
      double ht = nlight * SIGNAL_LIGHT + (nlight+2) * SIGNAL_GAP;
      double wd = SIGNAL_LIGHT + 2 * SIGNAL_GAP;
      double y0 = my - ht/2;
      
      Line l0 = new Line(cpt.getX(),cpt.getY(),mx+wd/2,y0+ht/2);
      getChildren().add(l0);
      
      Rectangle box = new Rectangle(mx,y0,wd,ht);
      box.setFill(SIGNAL_BACKGROUND);
      getChildren().add(box);
      
      Shape [] light = new Shape[nlight];
      for (int i = 0; i < nlight; ++i) {
         y0 = y0 + SIGNAL_GAP;
         Circle c = new Circle(mx + SIGNAL_GAP + SIGNAL_LIGHT/2,
               y0 + SIGNAL_LIGHT/2,
               SIGNAL_LIGHT/2);
         c.setFill(SIGNAL_OFF);
         c.setStroke(SIGNAL_STROKE);
         light[i] = c;
         y0 += SIGNAL_LIGHT;
         getChildren().add(c);
       }
     
      SignalDrawData sdd = new SignalDrawData(sig,box,light);
      signal_map.put(sig,sdd);
    }
}


private void setAllSignals(ShoreSignalState state)
{
   ShoreLog.logD("VIEW","Set all signals " + state);
   
   for (IfaceSignal sig : for_diagram.getSignals()) {
      if (sig.isUnused()) continue;
      view_factory.getSafetyModel().setSignal(sig,state);
    }
}



private class SignalDrawData {
   
   private IfaceSignal for_signal;
   private Shape [] signal_lights;
   
   SignalDrawData(IfaceSignal sig,Shape bkg,Shape [] lights) {
      for_signal = sig;
      signal_lights = lights;
      SignalHandler hdlr = new SignalHandler(for_signal);
      bkg.setOnMouseClicked(hdlr);
      bkg.setOnMousePressed(hdlr);
      for (Shape s : lights) {
         s.setOnMouseClicked(hdlr);
         s.setOnMousePressed(hdlr);
       }
      doSetSignal();
    }
   
   void setSignal() {
      if (Platform.isFxApplicationThread()) {
         doSetSignal();
       }
      else {
         Platform.runLater(() -> doSetSignal());
       }
    }
   
   void doSetSignal() {
      ShoreSignalState st = for_signal.getSignalState();
      for (Shape s : signal_lights) {
         s.setFill(SIGNAL_OFF);
       }
      switch (st) {
         case OFF :
            break;
         case GREEN :
            signal_lights[signal_lights.length-1].setFill(Color.GREEN);
            break;
         case RED :
            signal_lights[0].setFill(Color.RED);
            break;
         case YELLOW :
            if (signal_lights.length == 2) {
               signal_lights[0].setFill(Color.RED);
               signal_lights[1].setFill(Color.GREEN);
             }
            else {
               signal_lights[1].setFill(Color.YELLOW);
             }
            break;
       }
    }
   
}       // end of inner class SignalDrawData




private class SignalHandler implements EventHandler<MouseEvent> {
   
   private IfaceSignal for_signal;
   
   SignalHandler(IfaceSignal sig) {
      for_signal = sig;
    }
   
   @Override public void handle(MouseEvent evt) {
      if (evt.getEventType() == MouseEvent.MOUSE_CLICKED) {
         if (evt.isControlDown()) {
            if (evt.isShiftDown()) { 
               setAllSignals(ShoreSignalState.RED);
               return;
             }
            else if (evt.isMetaDown() || evt.isAltDown()) { 
               setAllSignals(ShoreSignalState.GREEN);
               return;
             }
          }
         ShoreSignalState st = for_signal.getSignalState();
         ShoreSignalType typ = for_signal.getSignalType();
         ShoreSignalState next = ShoreSignalState.RED;
         switch (typ) {
            case ENGINE :
            case RG :
            case RG_ANODE :
            case ENGINE_ANODE :
               next = (st == ShoreSignalState.RED ? ShoreSignalState.GREEN : ShoreSignalState.RED);
               break;
            case RGY :
            case RGY_ANODE :
               switch (st) {
                  case RED :
                     next = ShoreSignalState.GREEN;
                     break;
                  case YELLOW :
                     next = ShoreSignalState.RED;
                     break;
                  case GREEN :
                  case OFF :
                     next = ShoreSignalState.GREEN;
                }
               break; 
          }
         view_factory.getSafetyModel().setSignal(for_signal,next);
       }
    }
   
}       // end of inner class SignalHandler





/********************************************************************************/
/*                                                                              */
/*      Sensor drawing                                                          */
/*                                                                              */
/********************************************************************************/

private void drawSensors()
{
   sensor_map.clear();
   
   for (IfaceSensor sen : for_diagram.getSensors()) {
      IfacePoint pt = sen.getAtPoint();
      if (pt == null) continue;
      Point2D cpt = getCoords(pt);
      Circle c = new Circle(cpt.getX(),cpt.getY(),SENSOR_RADIUS,
            SENSOR_OFF_COLOR);
      getChildren().add(c);
      SensorDrawData sdd = new SensorDrawData(sen,c);
      sensor_map.put(sen,sdd);
    }
}


private class SensorDrawData {
  
   private IfaceSensor for_sensor;
   private Shape sensor_node;
   
   SensorDrawData(IfaceSensor s,Shape sh) {
      for_sensor = s;
      sensor_node = sh;
      SensorHandler hdlr = new SensorHandler(for_sensor);
      sh.setOnMouseClicked(hdlr);
      sh.setOnMousePressed(hdlr);
      doSetSensor();
    }
   
   void setSensor() {
      if (Platform.isFxApplicationThread()) {
         doSetSensor();
       }
      else {
         Platform.runLater(() -> doSetSensor());
       }
    }
   
   void doSetSensor() {
      ShoreSensorState st = for_sensor.getSensorState();
      Color fill = SENSOR_UNKNOWN_COLOR;
      switch (st) {
         case ON :
            fill = SENSOR_ON_COLOR;
            break;
         case OFF :
            fill = SENSOR_OFF_COLOR;
            break;
       }
      sensor_node.setFill(fill);
    }
   
}       // end of inner class SensorDrawData



private class SensorHandler implements EventHandler<MouseEvent> {
   
   private IfaceSensor for_sensor;
   
   SensorHandler(IfaceSensor sen) {
      for_sensor = sen;
    }
   
   @Override public void handle(MouseEvent evt) {
      if (evt.getEventType() == MouseEvent.MOUSE_CLICKED) {
         ShoreSensorState st = for_sensor.getSensorState();
         ShoreSensorState next = st;
         switch (st) {
            case OFF :
            case UNKNOWN :
               next = ShoreSensorState.ON;
               break;
            case ON :
               next = ShoreSensorState.OFF;
               break;
          }
         view_factory.getSafetyModel().setSensor(for_sensor,next);
       }
    }
   
}       // end of inner class SensorHandler




/********************************************************************************/
/*                                                                              */
/*      Handle blocks                                                           */
/*                                                                              */
/********************************************************************************/

private void drawBlocks()
{
   block_map.clear();
   
   for (IfaceBlock blk : for_diagram.getBlocks()) {
      IfacePoint pt = blk.getAtPoint();
      Point2D cpt = getCoords(pt);
      String txt = blk.getId();
      Text t = new Text(txt);
      t.setFont(BLOCK_FONT);
      t.setFill(BLOCK_LABEL_COLOR);
      t.setStroke(BLOCK_LABEL_COLOR);
      Bounds b = t.getBoundsInLocal();
      double w0 = b.getWidth();
      double w = w0 + 10;
      double h = b.getHeight();
      t.setTextAlignment(TextAlignment.CENTER);
      t.setTextOrigin(VPos.CENTER);
      t.setX(cpt.getX() - w0/2);
      t.setY(cpt.getY());
      
      Ellipse bkg = new Ellipse(cpt.getX(),cpt.getY(),w/2,h/2);
      bkg.setFill(BLOCK_BACKGROUD_COLOR);
      
      getChildren().add(bkg);
      getChildren().add(t);
      
      BlockDrawData bdd = new BlockDrawData(blk,bkg,t);
      block_map.put(blk,bdd);
    }
}



private class BlockDrawData {

   private IfaceBlock for_block;
   private Shape label_shape;
   private Shape background_shape;
   
   BlockDrawData(IfaceBlock blk,Shape bkg,Shape lbl) {
      for_block = blk;
      background_shape = bkg;
      label_shape = lbl;
      setBlock();
    }
   
   void setBlock() {
      if (Platform.isFxApplicationThread()) {
         doSetBlock();
       }
      else {
         Platform.runLater(() -> doSetBlock());
       }
    }
   
   private void doSetBlock() {
      ShoreBlockState st = for_block.getBlockState();
      Color lcolor = BLOCK_LABEL_COLOR;
      Color bcolor = BLOCK_BACKGROUD_COLOR;
   
      switch (st) {
         default :
         case EMPTY :
            break;
         case UNKNOWN :
            bcolor = BLOCK_BACKGROUND_UNKNOWN_COLOR; 
            break; 
         case INUSE :
            lcolor = BLOCK_LABEL_INUSE_COLOR;
            bcolor = BLOCK_BACKGROUD_INUSE_COLOR;
            break;
         case PENDING :
            lcolor = BLOCK_LABEL_PENDING_COLOR;
            bcolor = BLOCK_BACKGROUD_PENDING_COLOR;
            break;
       }
      label_shape.setFill(lcolor);
      label_shape.setStroke(lcolor);
      background_shape.setFill(bcolor);
      BlockHandler hdlr = new BlockHandler(for_block);
      label_shape.setOnMouseClicked(hdlr);
      background_shape.setOnMouseClicked(hdlr);
    }
   
}       // end of inner class BlockDrawData


private void setAllBlocks(ShoreBlockState state)
{
   for (IfaceBlock blk : for_diagram.getBlocks()) {
      blk.setBlockState(state);
    }
}


private class BlockHandler implements EventHandler<MouseEvent> {

   private IfaceBlock for_block;
   
   BlockHandler(IfaceBlock blk) {
      for_block = blk;
    }
   
   @Override public void handle(MouseEvent evt) {
      if (evt.getEventType() == MouseEvent.MOUSE_CLICKED) {
         if (evt.isControlDown()) {
            setAllBlocks(ShoreBlockState.EMPTY); 
            return;
          }
         ShoreBlockState st = for_block.getBlockState();
         ShoreBlockState next = ShoreBlockState.UNKNOWN;
         switch (st) {
            case UNKNOWN :
               next = ShoreBlockState.EMPTY;
               break;
            case EMPTY :
               next = ShoreBlockState.PENDING;
               break;
            case PENDING :
               next = ShoreBlockState.INUSE;
               break;
            case INUSE :
               next = ShoreBlockState.EMPTY;
               break;
          }
         for_block.setBlockState(next);
       }
    }
}       // end of inner class BlockHandler



/********************************************************************************/
/*                                                                              */
/*      Handle point scaling based on size                                      */
/*                                                                              */
/********************************************************************************/

private void setupScaling(double w,double h)
{
   double xpix = w - 2 * BORDER_SPACE;
   double xval = display_bounds.getWidth() / xpix;
   double ypix = h - 2 * BORDER_SPACE;
   double yval = display_bounds.getHeight() / ypix;
   scale_value = Math.max(xval,yval);
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
/*      Handle resizing                                                         */
/*                                                                              */
/********************************************************************************/

@Override public boolean isResizable()               { return true; }

@Override public void resize(double w,double h) {
      super.setWidth(w);
      super.setHeight(h);
      drawDiagram();
}
   


/********************************************************************************/
/*                                                                              */
/*     Handle model and train updates                                           */
/*                                                                              */
/********************************************************************************/

private class CallbackHandler implements ModelCallback, EngineCallback {
   
   CallbackHandler() { }
   
   @Override public void switchChanged(IfaceSwitch sw) {
      SwitchDrawData dd = switch_map.get(sw);
      if (dd == null) return;
      dd.setArrow();
    } 
   
   @Override public void signalChanged(IfaceSignal sig) {
      SignalDrawData dd = signal_map.get(sig);
      if (dd == null) return;
      dd.setSignal();
    }
   
   @Override public void sensorChanged(IfaceSensor sen) {
      SensorDrawData dd = sensor_map.get(sen);
      if (dd == null) return;
      dd.setSensor();
    }
   
   @Override public void blockChanged(IfaceBlock blk) {
      BlockDrawData dd = block_map.get(blk);
      if (dd == null) return;
      dd.setBlock();
    } 
  
}       // end of inner class CallbackHandler



}       // end of class ViewDiagramFx




/* end of ViewDiagramFx.java */

