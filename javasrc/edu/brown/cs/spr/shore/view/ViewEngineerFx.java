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



import edu.brown.cs.spr.shore.iface.IfaceEngine;
import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.Gauge.SkinType;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;

class ViewEngineerFx extends GridPane implements ViewConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceEngine    for_engine;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ViewEngineerFx(IfaceEngine engine)
{
   for_engine = engine;
   
   Background bkg = new Background(new BackgroundFill(Color.WHEAT,null,null));
   setBackground(bkg);
   
   // top row for labels
   Label hdr1 = new Label("CONTROL ID   ");
   Label hdr2 = new Label("Engine " + for_engine.getTrainName());
   add(hdr1,0,0,2,1);
   add(hdr2,2,0,3,1);
   
   // next row for speedometer, speed control, tach
   Gauge speed = getSpeedometer();
   add(speed,0,1,3,2);
   setHgrow(speed,Priority.ALWAYS);
   setVgrow(speed,Priority.ALWAYS);
   setFillHeight(speed,true);
   
   Slider accel = new Slider();
   accel.setMin(0);
   accel.setMax(100);
   accel.setValue(0);
   accel.setShowTickMarks(true);
   accel.setShowTickLabels(false);
   accel.setMajorTickUnit(50);
   accel.setMinorTickCount(5);
   accel.setBlockIncrement(10);
   accel.setOrientation(Orientation.VERTICAL);
   accel.setValueChanging(true);
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
   add(spacer1,4,2,1,1);
   
   Button horn = getHornButton();
   add(horn,4,3,1,1);
   setHalignment(horn,HPos.CENTER);
   
   ToggleButton light0 = getFrontLightButton();
   add(light0,0,4,1,1);  
   ToggleButton light1 = getReadLightButton();
   add(light1,1,4,1,1);
   
   Button bell = getBellButton();
   add(bell,4,4,1,1);
   setHalignment(bell,HPos.CENTER);
   
   ToggleButton rev = new ToggleButton("Reverse");
   add(rev,0,5,3,1);
   
   Button stop = getStopButton();
   add(stop,0,6,1,1);
   
   Button pwer = getPowerButton();
   add(pwer,3,6,1,1);
   
   Label power = new Label("On/Off");
   add(power,4,6,1,1);
   
   Label spacer2 = new Label();
   spacer2.setMinHeight(3.0);
   add(spacer2,0,7);
   
   setVgap(5.0);
   setHgap(5.0);
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
   
   return new Button("",imgv);
}



private Button getBellButton()
{
   Image img = new Image("images/bell.png");
   ImageView imgv = new ImageView();
   imgv.setImage(img);
   imgv.setFitWidth(40);
   imgv.setFitHeight(40);
   imgv.setSmooth(true);
   imgv.setCache(true);
   
   return new Button("",imgv);
}


private ToggleButton getFrontLightButton()
{
   Image img = new Image("images/light.png");
   ImageView imgv = new ImageView();
   imgv.setImage(img);
   imgv.setFitWidth(40);
   imgv.setFitHeight(40);
   imgv.setSmooth(true);
   imgv.setCache(true);
   
   return new ToggleButton("Front",imgv);
}




private ToggleButton getReadLightButton()
{
   Image img = new Image("images/light.png");
   ImageView imgv = new ImageView();
   imgv.setImage(img);
   imgv.setFitWidth(40);
   imgv.setFitHeight(40);
   imgv.setSmooth(true);
   imgv.setCache(true);
   
   return new ToggleButton("Rear",imgv);
}


private Button getPowerButton()
{
   Image img = new Image("images/power.png");
   ImageView imgv = new ImageView();
   imgv.setImage(img);
   imgv.setFitWidth(60);
   imgv.setFitHeight(60);
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






}       // end of class ViewEngineerFx




/* end of ViewEngineerFx.java */

