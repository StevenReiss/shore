/********************************************************************************/
/*                                                                              */
/*              ViewPlannerFx.java                                              */
/*                                                                              */
/*      description of class                                                    */
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
import java.util.Collection;
import java.util.List;


import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceEngine;
import edu.brown.cs.spr.shore.iface.IfacePlanner;
import edu.brown.cs.spr.shore.iface.IfacePoint;
import edu.brown.cs.spr.shore.iface.IfaceSafety;
import edu.brown.cs.spr.shore.iface.IfaceSignal; 
import edu.brown.cs.spr.shore.iface.IfacePlanner.PlanAction;
import edu.brown.cs.spr.shore.iface.IfacePlanner.PlanActionType;
import edu.brown.cs.spr.shore.iface.IfacePlanner.PlanExecutable;
import edu.brown.cs.spr.shore.shore.ShoreLog;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

class ViewPlannerFx extends HBox implements ViewConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfacePlanner planner_model;
private IfaceSafety safety_model;
private List<TrainPlanner> train_plans;
private TrainPlanner train_planner;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ViewPlannerFx(ViewFactory vf)
{
   planner_model = vf.getPlannerModel();
   safety_model = vf.getSafetyModel();
   train_plans = new ArrayList<>();
   
   setSpacing(10.0);
   setFillHeight(true);
   
   train_planner = new TrainPlanner();
   train_plans.add(train_planner);
   getChildren().add(train_planner);
   
   EngineChanged cb = new EngineChanged();
   for (IfaceEngine eng : vf.getTrainModel().getAllEngines()) {
      eng.addEngineCallback(cb);
    }
}


/********************************************************************************/
/*                                                                              */
/*      Methods to return viable options for planner                            */
/*                                                                              */
/********************************************************************************/


private ObservableList<PlanAction> startOptions()
{
   Collection<PlanAction> starts = planner_model.getStartActions();
   return FXCollections.observableArrayList(starts);
}

private ObservableList<PlanAction> options(PlanAction from) 
{
   Collection<PlanAction> nexts = planner_model.getNextActions(from);
   return FXCollections.observableArrayList(nexts);
}

private ObservableList<PlanAction> noOptions()
{
   return FXCollections.observableArrayList();
}


/********************************************************************************/
/*                                                                              */
/*      The actual planner to let the user create a plan                        */
/*                                                                              */
/********************************************************************************/

private class TrainPlanner extends GridPane { 
   
   private List<ChoiceBox<PlanAction>> choice_boxes;
   private List<Spinner<Integer>> count_boxes;
   private Button start_button;
   private Button signal_button;
   private IfaceEngine for_engine;
   
   TrainPlanner() {
      int steps = planner_model.getMaxSteps();
      choice_boxes = new ArrayList<>();
      count_boxes = new ArrayList<>();
      setMinSize(200,200);
      setPrefSize(340,200);
      add(new Label("Start From:"),0,0,1,REMAINING);
      ChoiceBox<PlanAction> prv = null;
      for (int i = 1; i <= steps; ++i) {
         add(new Label("Step " + i + ":"),0,i,1,1);
         ChoiceBox<PlanAction> cb = new ChoiceBox<>();
         cb.setConverter(new TargetConverter());
         if (prv == null) {
            cb.setItems(startOptions());
          }
         else {
            PlanAction prvtgt = prv.getValue();
            if (prvtgt != null) {
               cb.setItems(options(prvtgt));
             }
          } 
         prv = cb;   
         add(cb,1,i,1,1);
         Spinner<Integer> sp = new Spinner<>(0,20,0);
         add(sp,2,i,1,1);
         choice_boxes.add(cb);
         count_boxes.add(sp);
         cb.valueProperty().addListener(new ChoiceListener(this,i-1));
         cb.itemsProperty().addListener(new ValuesListener(this,i-1));
       }
      
      HBox buttons = new HBox();
      buttons.setAlignment(Pos.CENTER);
      signal_button = new Button("Set STOP Signal");
      signal_button.setOnAction(new SignalSetter(this));
      start_button = new Button("START");
      start_button.setOnAction(new PlanStarter(this));
      buttons.getChildren().addAll(signal_button,start_button);
      add(buttons,0,steps+1,1,REMAINING);
      start_button.setDisable(true);
    }
   
   ChoiceBox<PlanAction> getChoiceBox(int idx) {
      return choice_boxes.get(idx);
    }
   
   Spinner<Integer> getCountBox(int idx) {
      return count_boxes.get(idx);
    }
   
   int getNumSteps() {
      return choice_boxes.size();
    }
   
   boolean isComplete() {
      for (int i = 0; i < choice_boxes.size(); ++i) {
         PlanAction t = choice_boxes.get(i).getValue();
         if (t == null) return false;
         switch (t.getActionType()) {
            case START :
               if (i != 0) return false;
               break;
            case LOOP :
               if (i == 0) return false;
               break;
            case END :
               if (i > 0) return true;
               if (i == 0) return false;
               break;
          }
       }
      return false;
    }
   
   PlanAction getStartAction() {
      if (!isComplete()) return null;
      return choice_boxes.get(0).getValue();
    }
   
   IfaceEngine getEngine()                      { return for_engine; }
   void enablePlan(IfaceEngine eng) {
      if (eng == null || !isComplete()) {
         eng = null;
         start_button.setDisable(true);
         // temporary: allow debug without engine
         if (isComplete()) start_button.setDisable(false);
       }
      else {
         start_button.setDisable(false); 
       }
      for_engine = eng;
    }
   
   PlanExecutable createPlan() {
      if (!isComplete()) return null;
      PlanExecutable plan = planner_model.createPlan();
      for (int i = 0; i < choice_boxes.size(); ++i) {
         PlanAction pa = choice_boxes.get(i).getValue();
         int ct = count_boxes.get(i).getValue();
         plan.addStep(pa,ct);
       }
      return plan;
    }
   
}


private final class TargetConverter extends StringConverter<PlanAction> {
   
   @Override public PlanAction fromString(String s) {
      return planner_model.findAction(s);
    }
      
   @Override public String toString(PlanAction t) {
      return t.getName();
    }
   
}       // end of inner class TargetConverter

private class ChoiceListener implements ChangeListener<PlanAction> {   
   
   private TrainPlanner for_planner;
   private int for_step;
   
   ChoiceListener(TrainPlanner plnr,int idx) {
      for_planner = plnr;
      for_step = idx;
    }
   
   @Override public void changed(ObservableValue<? extends PlanAction> obs,
         PlanAction oldv,PlanAction newv) {
      boolean donext = true;
      if (newv == null || newv.getActionType() != PlanActionType.LOOP) {
         for_planner.getCountBox(for_step).setDisable(true);
          if (newv == null || for_step != 0) donext = false;
       }
      else {
         for_planner.getCountBox(for_step).setDisable(false);
       }
      if (donext) {
         ChoiceBox<PlanAction> nextcb = for_planner.getChoiceBox(for_step+1);
         nextcb.setValue(null);
         nextcb.itemsProperty().set(options(newv));
       }
      else {
         for (int i = for_step+1; i < for_planner.getNumSteps(); ++i) {
            ChoiceBox<PlanAction> cb1 = for_planner.getChoiceBox(i);
            cb1.setItems(noOptions());
            for_planner.getCountBox(i).setDisable(true);
          }
       }
      for_planner.enablePlan(null);
    }
   
}       // end of inner class ChoiceListener


private class ValuesListener implements ChangeListener<ObservableList<PlanAction>>,
      Runnable {
   
   private TrainPlanner for_planner;
   private int for_step;
   private PlanAction set_value;
   
   ValuesListener(TrainPlanner plnr,int idx) {
      for_planner = plnr;
      for_step = idx;
    }
   
   @Override public void changed(
         ObservableValue<? extends ObservableList<PlanAction>> obs,
               ObservableList<PlanAction> oldv,ObservableList<PlanAction> newv) {
      if (newv != null && newv.size() == 1) {
         PlanAction pt = newv.get(0);
         set_value = pt;
         Platform.runLater(this);
       }
    }
   
   @Override public void run() {
      PlanAction pt = set_value;
      set_value = null;
      ChoiceBox<PlanAction> cb = for_planner.getChoiceBox(for_step);
      try {
         cb.setValue(pt);
       }
      catch (Throwable t) {
         ShoreLog.logE("VIEW","Problem setting plan value " +
               cb.getItems() + " " + pt);
       }
    }
   
}       // enf of ininer class ValuesListener


private class PlanStarter implements EventHandler<ActionEvent> {
   
   private TrainPlanner for_planner;
   
   PlanStarter(TrainPlanner plnr) {
      for_planner = plnr;
    }
   
   @Override public void handle(ActionEvent evt) {
      IfaceEngine eng = for_planner.getEngine();
//    if (eng == null) return;
      PlanExecutable pe = for_planner.createPlan();
      if (pe == null) return;
      // create a display for the executing plan
      // add a callback for this display to track state
      ShoreLog.logD("VIEW","Create plan " + pe);
      pe.execute(eng);
    }
   
}       // end of inner class PlanStarter



private class SignalSetter implements EventHandler<ActionEvent> {

   private TrainPlanner for_planner;
   
   SignalSetter(TrainPlanner plnr) {
      for_planner = plnr;
    }
   
   @Override public void handle(ActionEvent evt) {
      PlanAction act = for_planner.getStartAction();
      if (act == null) return;
      IfaceSignal sig = act.getSignal();
      if (sig == null) return;
      safety_model.setSignal(sig,ShoreSignalState.RED);
    }
   
}       // end of inner class SignalSetter



/********************************************************************************/
/*                                                                              */
/*      Monitor state of engines to see if one is ready for plan                */
/*                                                                              */
/********************************************************************************/

private final class EngineChanged implements IfaceEngine.EngineCallback {
   
   private IfaceSignal start_signal;
   private IfaceBlock  start_block;
   
   EngineChanged() {
      start_signal = null;
    }
   
   @Override public void engineChanged(IfaceEngine eng) {
      if (eng.getSpeed() == 0 && start_signal != null && start_block != null) {
         enableEngine(eng);
       }
      else {
         enableEngine(null);
       }
    }
   
   @Override public void enginePositionChanged(IfaceEngine eng) {
      IfacePoint pt = eng.getCurrentPoint();
      if (pt.getType() == ShorePointType.SIGNAL) {
         PlanAction tgt = train_planner.getStartAction();
         if (tgt == null || tgt.getSignal() == null) return;
         if (tgt.getSignal().getAtPoints().contains(pt)) {
            start_signal = tgt.getSignal();
            start_block = pt.getBlock();
          }
         if (eng.getSpeed() == 0) {
            enableEngine(eng);
          }
       }
      else {
         if (start_block != null && pt.getBlock() != start_block) {
            start_block = null;
            start_signal = null;
          }
       }
    }
   
   private void enableEngine(IfaceEngine eng) {
      if (!train_planner.isComplete()) return;
      train_planner.enablePlan(eng);
    }
   
}       // end of inner class EngineChanged

}       // end of class ViewPlannerFx




/* end of ViewPlannerFx.java */

