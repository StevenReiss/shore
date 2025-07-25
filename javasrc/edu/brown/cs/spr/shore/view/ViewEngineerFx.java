/********************************************************************************/
/*                                                                              */
/*              ViewEngineerFx.java                                             */
/*                                                                              */
/*      Engineer Panel to control a train                                       */
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



import java.net.URL;

import edu.brown.cs.spr.shore.iface.IfaceEngine;
import edu.brown.cs.spr.shore.iface.IfaceTrains.EngineCallback;
import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.Gauge.SkinType;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;

class ViewEngineerFx extends GridPane implements ViewConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ViewFactory     view_factory;
private IfaceEngine     for_engine;
private IconToggle      front_light;
private IconToggle      back_light;
private IconToggle      train_bell;
private FwdRevSwitch    reverse_switch;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ViewEngineerFx(ViewFactory fac,IfaceEngine engine)
{
   view_factory = fac;
   for_engine = engine;
   
   Color bkgcol = Color.GRAY;
   if (for_engine != null && for_engine.getEngineId() != null) {
      bkgcol = for_engine.getEngineColor();
    }
   bkgcol = bkgcol.desaturate().desaturate();
   String s = bkgcol.toString();
   s = s.replace("0x","");
   s = s.substring(0,6);
   s = "-fx-background-color: #" + s;
   setStyle(s);
// Background bkg = new Background(new BackgroundFill(bkgcol,null,null));
// setBackground(bkg);
// getStyleClass().add("gridpane-background");
   
   URL r = getClass().getClassLoader().getResource("engineer.css");
   getStylesheets().add(r.toExternalForm());
   
   // top row for labels
   Label hdr1 = new Label("CONTROL ID   ");
   Label hdr2 = new Label("Engine " + for_engine.getEngineName());
   add(hdr1,0,0,2,1);
   add(hdr2,2,0,3,1);
   
   // next row for speedometer, speed control, tach
   Gauge speed = getSpeedometer();
   add(speed,0,1,3,2);
   setHgrow(speed,Priority.ALWAYS);
   setVgrow(speed,Priority.ALWAYS);
   setFillHeight(speed,true);
   
   Slider accel = getAccelerator();
   add(accel,3,1,1,4);
   setVgrow(accel,Priority.ALWAYS);
   setHalignment(accel,HPos.CENTER);
   setFillHeight(accel,true);
   
   Gauge tach = getTachometer();
   add(tach,4,1,1,1);
// setVgrow(tach,Priority.ALWAYS);
   setFillHeight(tach,true);
   setHalignment(tach,HPos.CENTER);
   Label spacer1 = new Label();
   spacer1.setMinHeight(100);
   spacer1.getStyleClass().add(".clearButton");
   add(spacer1,4,2,1,1);
   
   Button horn = getHornButton();
   add(horn,4,3,1,1);
   setHalignment(horn,HPos.CENTER);
   
   front_light = getFrontLightButton();
   add(front_light,0,4,1,1);  
   back_light = getRearLightButton();
   add(back_light,1,4,1,1);
   
   train_bell = getBellButton();
   add(train_bell,4,4,1,1);
   setHalignment(train_bell,HPos.CENTER);
   
   reverse_switch = getFwdReverse();
   add(reverse_switch,0,5,3,1);
   
   Button stop = getStopButton();
   add(stop,0,6,1,1);
   
   Button pwer = getPowerButton();
   add(pwer,3,6,1,1);
   
   Label spacer2 = new Label();
   spacer2.setMinHeight(3.0);
   add(spacer2,0,7);
   
   setVgap(5.0);
   setHgap(5.0);
   
   CallbackHandler hdlr = new CallbackHandler();
   fac.getTrainModel().addTrainCallback(hdlr);
}



/********************************************************************************/
/*                                                                              */
/*      Accelerator                                                             */
/*                                                                              */
/********************************************************************************/

private Slider getAccelerator()
{
   Slider accel = new Accelerator();
   
   return accel;
}


private class Accelerator extends Slider {
   
   Accelerator() {
      setMin(0);
      setMax(100);
      setValue(0);
      setShowTickMarks(true);
      setShowTickLabels(false);
      setMajorTickUnit(50);
      setMinorTickCount(5);
      setBlockIncrement(10);
      setOrientation(Orientation.VERTICAL);
      setValueChanging(true);
      String style = "-fx-fill: linear-gradient(to right,#ff0000 0,#00ff00 100)";
      setStyle(style);
      
    }
}

/********************************************************************************/
/*                                                                              */
/*      Speedometer                                                             */
/*                                                                              */
/********************************************************************************/

private Gauge getSpeedometer()
{
   Gauge gauge = new Gauge();
   gauge.setSkinType(SkinType.MODERN);
   
   return gauge;
}



/********************************************************************************/
/*                                                                              */
/*      Tachometer                                                              */
/*                                                                              */
/********************************************************************************/

private Gauge getTachometer()
{
   Gauge gauge = new Gauge();
   gauge.setSkinType(SkinType.MODERN);
   gauge.setPrefSize(100,100);
   
   return gauge;
}


/********************************************************************************/
/*                                                                              */
/*      Buttons                                                                 */
/*                                                                              */
/********************************************************************************/

private Button getHornButton()
{
   Image img = new Image("images/horn.png");
   ImageView imgv = new ImageView();
   imgv.setImage(img);
   imgv.setFitWidth(85);
   imgv.setFitHeight(60);
   imgv.setSmooth(true);
   imgv.setCache(true);
   
   Button b = new Button("",imgv);
   b.getStyleClass().add("clearButton");
   
   return b;
}



private IconToggle getBellButton()
{
   IconToggle it = new IconToggle("","bell");
   return it;
}


private IconToggle getFrontLightButton()
{
   return new FrontLightToggle();
}


private IconToggle getRearLightButton()
{
   return new RearLightToggle();
}


private Button getPowerButton()
{
   Image img = new Image("images/power.png");
   ImageView imgv = new ImageView();
   imgv.setImage(img);
   imgv.setFitWidth(100);
   imgv.setFitHeight(100);
   imgv.setSmooth(true);
   imgv.setCache(true);
   
   return new Button("",imgv);
}


private Button getStopButton()
{
   Image img = new Image("images/stopbutton.png");
   ImageView imgv = new ImageView();
   imgv.setImage(img);
   imgv.setFitWidth(40);
   imgv.setFitHeight(40);
   imgv.setSmooth(true);
   imgv.setCache(true);
   
   return new Button("",imgv);
}


private FwdRevSwitch getFwdReverse()
{
   FwdRevSwitch rslt = new FwdRevSwitch();
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Icon Toggle                                                             */
/*                                                                              */
/********************************************************************************/

private class IconToggle extends ToggleButton implements EventHandler<ActionEvent> {
   
   private ImageView off_view;
   private ImageView on_view;

   IconToggle(String label,String name) {
      super(label);
      String nm1 = "images/" + name + ".png";
      String nm2 = "images/" + name + "_on.png";
      Image img1 = new Image(nm1);
      off_view = new ImageView();
      off_view.setImage(img1);
      off_view.setFitWidth(40);
      off_view.setFitHeight(40);
      off_view.setSmooth(true);
      off_view.setCache(true);
      Image img2 = new Image(nm2);
      on_view = new ImageView();
      on_view.setImage(img2);
      on_view.setFitWidth(40);
      on_view.setFitHeight(40);
      on_view.setSmooth(true);
      on_view.setCache(true);
      setOnAction(this);
      handle(null);
      getStyleClass().add("clearButton");
    }
   
   @Override public void handle(ActionEvent evt) {
      if (isSelected()) {
         setGraphic(off_view);
       }
      else {
         setGraphic(on_view);
       }
    }
   
}       // end of inner class IconToggle


private class FrontLightToggle extends IconToggle implements EventHandler<ActionEvent> {
   
   FrontLightToggle() {
      super("Front","light");
    }
   
   @Override public void handle(ActionEvent evt) {
      if (for_engine != null) {
         for_engine.setFwdLight(isSelected());
       }
      super.handle(evt);
    }
   
}       // end of inner class FrontLightToggle



private class RearLightToggle extends IconToggle implements EventHandler<ActionEvent> {
   
   RearLightToggle() {
      super("Front","light");
    }
   
   @Override public void handle(ActionEvent evt) {
      if (for_engine != null) {
         for_engine.setRevLight(isSelected());
       }
      super.handle(evt);
    }
   
}       // end of inner class FrontLightToggle



/********************************************************************************/
/*                                                                              */
/*      Forward-reverse toggle                                                  */
/*                                                                              */
/********************************************************************************/

private class FwdRevSwitch extends HBox implements ChangeListener<Boolean> {

   private Label fwdrev_label;
   private Button fwdrev_button;
   
   private static final String ON_LABEL = "REV";
   private static final String OFF_LABEL = "FWD";
   private SimpleBooleanProperty switched_on;
   
   FwdRevSwitch() {
      fwdrev_label = new Label();
      fwdrev_button = new Button();
      switched_on = new SimpleBooleanProperty(false);
      init();
      switched_on.addListener(this);
    }
   
   void setSwtich(boolean on) {
      switched_on.set(on);
    }
   
   private void init() {
      fwdrev_label.setText(OFF_LABEL);
      getChildren().addAll(fwdrev_label, fwdrev_button);	
      fwdrev_button.setOnAction(new ActionHandler());
      fwdrev_label.setOnMouseClicked(new MouseHandler());
      setStyle(); 
      bindProperties();
      setToggle();
    }
   
   private void setStyle() {
      //Default Width
      setWidth(80);
      fwdrev_label.setAlignment(Pos.CENTER);
      setStyle("-fx-background-color: gray; -fx-text-fill:black; -fx-background-radius: 4;");
      setAlignment(Pos.CENTER_LEFT);
    }
   
   private void bindProperties() {
      fwdrev_label.prefWidthProperty().bind(widthProperty().divide(2));
      fwdrev_label.prefHeightProperty().bind(heightProperty());
      fwdrev_button.prefWidthProperty().bind(widthProperty().divide(2));
      fwdrev_button.prefHeightProperty().bind(heightProperty());
    }
   
   @Override public void changed(ObservableValue<? extends Boolean> obs,Boolean oldval,Boolean newval) {
      setToggle();
    } 
   
   private void setToggle() {
      if (switched_on.get()) {
         fwdrev_label.setText(ON_LABEL);
         fwdrev_label.setStyle("-fx-background-color: yellow;");
         fwdrev_label.toFront();
       }
      else {
         fwdrev_label.setText(OFF_LABEL);
         fwdrev_label.setStyle("-fx-background-color: lightgreen;");
         fwdrev_button.toFront();
       }
    }
   
   private void toggle() {
      switched_on.set(!switched_on.get());
    }
   
   private final class ActionHandler implements EventHandler<ActionEvent> {
      @Override public void handle(ActionEvent evt) {
         toggle();
       }
    }
   
   private final class MouseHandler implements EventHandler<MouseEvent> {
      @Override public void handle(MouseEvent evt) {
         toggle();
       }
    }
   
}       // end of inner class FwdRevSwitch



/********************************************************************************/
/*                                                                              */
/*      Train update handler                                                    */
/*                                                                              */
/********************************************************************************/

private final class CallbackHandler implements EngineCallback {

   @Override public void engineChanged(IfaceEngine e) {
      if (e != for_engine) return;
      front_light.setSelected(e.isFwdLightOn());
      back_light.setSelected(e.isRevLightOn());
      train_bell.setSelected(e.isBellOn());
      reverse_switch.setSwtich(e.isReverse());
    }
}


}       // end of class ViewEngineerFx




/* end of ViewEngineerFx.java */

