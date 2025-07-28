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



import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.Timer;

import edu.brown.cs.spr.shore.iface.IfaceEngine;
import edu.brown.cs.spr.shore.iface.IfaceEngine.EngineState;

import eu.hansolo.medusa.Gauge;
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
private IconToggle      rear_light;
private IconToggle      train_bell;
private FwdRevSwitch    reverse_switch;
private IconToggle      mute_button;
private Gauge           speed_gauge;
private Gauge           tach_gauge;
private Slider          throttle_slider;
private PowerButton     power_button;
private IconToggle      emergency_stop;



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
   Label hdr1 = new Label("ID: " + for_engine.getEngineId() + "   ");
   Label hdr2 = new Label("Engine " + for_engine.getEngineName());
   add(hdr1,0,0,2,1);
   add(hdr2,2,0,3,1);
   
   // next row for speedometer, speed control, tach
   speed_gauge = getSpeedometer();
   add(speed_gauge,0,1,3,2);
   setHgrow(speed_gauge,Priority.ALWAYS);
   setVgrow(speed_gauge,Priority.ALWAYS);
   setFillHeight(speed_gauge,true);
   
   throttle_slider = getThrottle();
   add(throttle_slider,3,1,1,4);
   setVgrow(throttle_slider,Priority.ALWAYS);
   setHalignment(throttle_slider,HPos.CENTER);
   setFillHeight(throttle_slider,true);
   
   tach_gauge = getTachometer();
   add(tach_gauge,4,1,1,1);
// setVgrow(tach,Priority.ALWAYS);
   setFillHeight(tach_gauge,true);
   setHalignment(tach_gauge,HPos.CENTER);
   Label spacer1 = new Label();
   spacer1.setMinHeight(100);
   spacer1.getStyleClass().add(".clearButton");
   add(spacer1,4,2,1,1);
   
   Button horn = getHornButton();
   add(horn,4,3,1,1);
   setHalignment(horn,HPos.CENTER);
   
   front_light = getFrontLightButton();
   add(front_light,0,4,1,1);  
   rear_light = getRearLightButton();
   add(rear_light,1,4,1,1);
   
   train_bell = getBellButton();
   add(train_bell,4,4,1,1);
   setHalignment(train_bell,HPos.CENTER);
   
   reverse_switch = getFwdReverse();
   add(reverse_switch,0,5,3,1);
   
   emergency_stop = getStopButton();
   add(emergency_stop,0,6,1,1);
   
   power_button = getPowerButton();
   add(power_button,3,6,1,1);
   
   mute_button = getMuteButton();
   add(mute_button,4,6,1,1);
   
   Label spacer2 = new Label();
   spacer2.setMinHeight(3.0);
   add(spacer2,0,7);
   
   setVgap(5.0);
   setHgap(5.0);
   
   CallbackHandler hdlr = new CallbackHandler();
   for_engine.addEngineCallback(hdlr);
}



/********************************************************************************/
/*                                                                              */
/*      Accelerator                                                             */
/*                                                                              */
/********************************************************************************/

private Slider getThrottle()
{
// return new Throttle();
   
   Slider s = new Slider();
   s.setMin(0);
   s.setMax(100);
   s.setValue(0);
   s.setShowTickMarks(true);
   s.setShowTickLabels(false);
   s.setMajorTickUnit(50);
   s.setMinorTickCount(5);
   s.setBlockIncrement(10);
   s.setOrientation(Orientation.VERTICAL);
   s.setValueChanging(true);
// String style = "-fx-fill: linear-gradient(to right,#ff0000 0,#00ff00 100)";
// s.setStyle(style);
// s.getStyleClass().add("throttle");
   
   return s;
}


private class Throttle extends Slider {
   
   Throttle() {
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
      getStyleClass().add("throttle");
    }
   
}       // end of inner class Accelerator



/********************************************************************************/
/*                                                                              */
/*      Speedometer                                                             */
/*                                                                              */
/********************************************************************************/

private Gauge getSpeedometer()
{
   Gauge g = new Gauge();
   g.setSkinType(Gauge.SkinType.MODERN);
   return g;
}


/********************************************************************************/
/*                                                                              */
/*      Tachometer                                                              */
/*                                                                              */
/********************************************************************************/

private Gauge getTachometer()
{
   Gauge g = new Gauge();
   // can't subclass because of resource loading
   g.setSkinType(Gauge.SkinType.MODERN);
   g.setPrefSize(100,100);
   
   return g;
}



/********************************************************************************/
/*                                                                              */
/*      Buttons                                                                 */
/*                                                                              */
/********************************************************************************/

private Button getHornButton()
{
   return new Horn();
}


private class Horn extends Button implements EventHandler<ActionEvent>, ActionListener {
   
   Horn() {
      Image img = new Image("images/horn.png");
      ImageView imgv = new ImageView();
      imgv.setImage(img);
      imgv.setFitWidth(85);
      imgv.setFitHeight(60);
      imgv.setSmooth(true);
      imgv.setCache(true);
      setGraphic(imgv);
      getStyleClass().add("clearButton");
      setOnAction(this);
    }
   
   @Override public void handle(ActionEvent evt) {
      if (for_engine.isHornOn()) return;
      Timer t = new Timer(HORN_TIME,this);
      t.setRepeats(false);
      t.start();
      for_engine.setHorn(true);
    }
   
   @Override public void actionPerformed(java.awt.event.ActionEvent evt) {
      for_engine.setHorn(false);
    }
    
}       // end of inner class Horn




/********************************************************************************/
/*                                                                              */
/*      Power buttons                                                           */
/*                                                                              */
/********************************************************************************/

private PowerButton getPowerButton()
{
   return new PowerButton();
}


private final class PowerButton extends Button implements EventHandler<ActionEvent>, ActionListener {

   private ImageView idle_view;
   private ImageView startup_view;
   private ImageView ready_view;
   private ImageView shutdown_view;
   
   PowerButton() {
      idle_view = loadImage("off");
      startup_view = loadImage("up");
      ready_view = loadImage("on");
      shutdown_view = loadImage("down");
      noteState(for_engine.getEngineState());
    }
   
   private ImageView loadImage(String sfx) {
      String res = "images/power_" + sfx + ".png";
      Image img = new Image(res);
      ImageView imgv = new ImageView();
      imgv.setImage(img);
      imgv.setFitWidth(100);
      imgv.setFitHeight(100);
      imgv.setSmooth(true);
      imgv.setCache(true);
      return imgv;
    }
   
   void noteState(EngineState st) {
      ImageView iv = null;
      switch (st) {
         case IDLE :
            iv = idle_view;
            break;
         case STARTUP :
            iv = startup_view;
            break;
         case READY :
            iv = ready_view;
            break;
         case SHUTDOWN :
            iv = shutdown_view;
            break;
       }
      setGraphic(iv);
    }

   
   @Override public void handle(ActionEvent evt) {
      EngineState next = null;
      int time = 0;
      switch (for_engine.getEngineState()) {
         case IDLE :
            next = EngineState.STARTUP;
            time = STARTUP_TIME;
            break;
         case READY :
            next = EngineState.SHUTDOWN;
            time = SHUTDOWN_TIME;
            break;
         default :
            return;
       }
      Timer t = new Timer(time,this);
      t.setRepeats(false);
      t.start();
      for_engine.setState(next);
    }
   
   @Override public void actionPerformed(java.awt.event.ActionEvent evt) {
      EngineState next = null;
      switch (for_engine.getEngineState()) {
         case STARTUP :
            next = EngineState.READY;
            break;
         case SHUTDOWN :
            next = EngineState.IDLE;
            break;
         default :
            return;
       }
      for_engine.setState(next);
    }
   
}       // end of inner class PowerButton




/********************************************************************************/
/*                                                                              */
/*      Toggles                                                                 */
/*                                                                              */
/********************************************************************************/

private IconToggle getBellButton()
{
   return new BellToggle();
}


private IconToggle getFrontLightButton()
{
   return new FrontLightToggle();
}


private IconToggle getRearLightButton()
{
   return new RearLightToggle();
}


private IconToggle getMuteButton()
{
   return new MuteToggle();
}

private IconToggle getStopButton()
{
   return new StopToggle();
}



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



private class BellToggle extends IconToggle implements EventHandler<ActionEvent> {
   
   BellToggle() {
      super("","bell");
    }
   
   @Override public void handle(ActionEvent evt) {
      if (for_engine != null) {
         for_engine.setBell(isSelected());
       }
      super.handle(evt);
    }
   
}       // end of inner class FrontLightToggle




private class FrontLightToggle extends IconToggle implements EventHandler<ActionEvent> {
   
   FrontLightToggle() {
      super("Front","light");
    }
   
   @Override public void handle(ActionEvent evt) {
      if (for_engine != null) {
         for_engine.setFrontLight(isSelected());
       }
      super.handle(evt);
    }
   
}       // end of inner class FrontLightToggle



private class RearLightToggle extends IconToggle implements EventHandler<ActionEvent> {
   
   RearLightToggle() {
      super("Rear","light");
    }
   
   @Override public void handle(ActionEvent evt) {
      if (for_engine != null) {
         for_engine.setRearLight(isSelected());
       }
      super.handle(evt);
    }
   
}       // end of inner class RearLightToggle


private class MuteToggle extends IconToggle implements EventHandler<ActionEvent> {
   
   MuteToggle() {
      super("","mute");
    }
   
   @Override public void handle(ActionEvent evt) {
      if (for_engine != null) {
         for_engine.setMute(isSelected());
       }
      super.handle(evt);
    }

}       // end of inner class MuteToggle



private class StopToggle extends IconToggle implements EventHandler<ActionEvent> {

   StopToggle() {
      super("","stopbutton");
    }
   
   @Override public void handle(ActionEvent evt) {
      if (for_engine != null) {
         for_engine.setEmergencyStop(isSelected());
       }
      super.handle(evt);
    }
   
}       // end of inner class StopToggle



/********************************************************************************/
/*                                                                              */
/*      Forward-reverse toggle                                                  */
/*                                                                              */
/********************************************************************************/

private FwdRevSwitch getFwdReverse()
{
   FwdRevSwitch rslt = new FwdRevSwitch();
   
   return rslt;
}


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
      for_engine.setReverse(newval); 
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

private final class CallbackHandler implements IfaceEngine.EngineCallback {

   @Override public void engineChanged(IfaceEngine e) {
      if (e != for_engine) return;
      front_light.setSelected(e.isFrontLightOn());
      rear_light.setSelected(e.isRearLightOn());
      train_bell.setSelected(e.isBellOn());
      reverse_switch.setSwtich(e.isReverse());
      mute_button.setSelected(e.isMuted());
      speed_gauge.setValue(e.getSpeed());
      tach_gauge.setValue(e.getRpm());
      power_button.noteState(e.getEngineState());
      emergency_stop.setSelected(e.isEmergencyStopped());
      
      if (e.getEngineState() != EngineState.READY) {
         throttle_slider.setValue(0);
         throttle_slider.setDisable(true);
       }
      else {
         throttle_slider.setDisable(false);
         throttle_slider.setValue(e.getThrottle());
       }
    }
   
}       // end of inner class CallbackHandler


}       // end of class ViewEngineerFx




/* end of ViewEngineerFx.java */

