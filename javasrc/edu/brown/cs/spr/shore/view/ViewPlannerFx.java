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
import java.util.Set;

import javax.swing.SwingUtilities;

import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceEngine;
import edu.brown.cs.spr.shore.iface.IfacePlanner;
import edu.brown.cs.spr.shore.iface.IfacePoint;
import edu.brown.cs.spr.shore.iface.IfaceSignal;
import edu.brown.cs.spr.shore.iface.IfacePlanner.PlanTarget;
import edu.brown.cs.spr.shore.shore.ShoreLog;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
private IfaceEngine  active_train;
private List<TrainPlanner> train_plans;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ViewPlannerFx(ViewFactory vf)
{
   planner_model = vf.getPlannerModel();
   active_train = null;
   train_plans = new ArrayList<>();
   
   setSpacing(10.0);
   setFillHeight(true);
   
   for (int i = 0; i < 3; ++i) {
      TrainPlanner tp = new TrainPlanner();
      train_plans.add(tp);
      getChildren().add(tp);
    }
   
   for (IfaceEngine eng : vf.getTrainModel().getAllEngines()) {
      eng.addEngineCallback(new EngineChanged());
    }
}



private ObservableList<PlanTarget> startOptions()
{
   Collection<PlanTarget> starts = planner_model.getStartTargets();
   return FXCollections.observableArrayList(starts);
}

private ObservableList<PlanTarget> options(PlanTarget from) 
{
   Collection<PlanTarget> nexts = planner_model.getNextTargets(from);
   return FXCollections.observableArrayList(nexts);
}

private ObservableList<PlanTarget> noOptions()
{
   return FXCollections.observableArrayList();
}


private class TrainPlanner extends GridPane { 
   
   private List<ChoiceBox<PlanTarget>> choice_boxes;
   private List<Spinner<Integer>> count_boxes;
   
   TrainPlanner() {
      int steps = planner_model.getMaxSteps();
      choice_boxes = new ArrayList<>();
      count_boxes = new ArrayList<>();
      setMinSize(200,200);
      setPrefSize(340,200);
      add(new Label("Start From:"),0,0,1,1);
      ChoiceBox<PlanTarget> prv = null;
      for (int i = 1; i <= steps; ++i) {
         add(new Label("Step " + i + ":"),0,i,1,1);
         ChoiceBox<PlanTarget> cb = new ChoiceBox<>();
         cb.setConverter(new TargetConverter());
         if (prv == null) {
            cb.setItems(startOptions());
          }
         else {
            PlanTarget prvtgt = prv.getValue();
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
      // add a button that will set signal if plan is complete and plan not being used
      // add a button that will start plan if there is a valid train and plan is valid and
      //        not in use and train not involved in a plan
    }
   
   ChoiceBox<PlanTarget> getChoiceBox(int idx) {
      return choice_boxes.get(idx);
    }
   
   Spinner<Integer> getCountBox(int idx) {
      return count_boxes.get(idx);
    }
   
   int getNumSteps() {
      return choice_boxes.size();
    }
   
   boolean isComplete() {
      return false;
    }
   
   PlanTarget getStartTarget() {
      if (!isComplete()) return null;
      return choice_boxes.get(0).getValue();
    }
   
}


private final class TargetConverter extends StringConverter<PlanTarget> {
   
   @Override public PlanTarget fromString(String s) {
      return planner_model.findTarget(s);
    }
      
   @Override public String toString(PlanTarget t) {
      return t.getName();
    }
   
}       // end of inner class TargetConverter

private class ChoiceListener implements ChangeListener<PlanTarget> {   
   
   private TrainPlanner for_planner;
   private int for_step;
   
   ChoiceListener(TrainPlanner plnr,int idx) {
      for_planner = plnr;
      for_step = idx;
    }
   
   @Override public void changed(ObservableValue<? extends PlanTarget> obs,
         PlanTarget oldv,PlanTarget newv) {
      boolean donext = true;
      if (newv == null || !newv.isLoop()) {
         for_planner.getCountBox(for_step).setDisable(true);
          if (newv == null || for_step != 0) donext = false;
       }
      else {
         for_planner.getCountBox(for_step).setDisable(false);
       }
      if (donext) {
         ChoiceBox<PlanTarget> nextcb = for_planner.getChoiceBox(for_step+1);
         nextcb.setValue(null);
         nextcb.itemsProperty().set(options(newv));
       }
      else {
         for (int i = for_step+1; i < for_planner.getNumSteps(); ++i) {
            ChoiceBox<PlanTarget> cb1 = for_planner.getChoiceBox(i);
            cb1.setItems(noOptions());
            for_planner.getCountBox(i).setDisable(true);
          }
       }
    }
}


private class ValuesListener implements ChangeListener<ObservableList<PlanTarget>>,
      Runnable {
   
   private TrainPlanner for_planner;
   private int for_step;
   private PlanTarget set_value;
   
   ValuesListener(TrainPlanner plnr,int idx) {
      for_planner = plnr;
      for_step = idx;
    }
   
   @Override public void changed(
         ObservableValue<? extends ObservableList<PlanTarget>> obs,
               ObservableList<PlanTarget> oldv,ObservableList<PlanTarget> newv) {
      if (newv != null && newv.size() == 1) {
         PlanTarget pt = newv.get(0);
         set_value = pt;
         Platform.runLater(this);
       }
    }
   
   @Override public void run() {
      PlanTarget pt = set_value;
      set_value = null;
      ChoiceBox<PlanTarget> cb = for_planner.getChoiceBox(for_step);
      try {
         cb.setValue(pt);
       }
      catch (Throwable t) {
         ShoreLog.logE("VIEW","Problem setting plan value " +
               cb.getItems() + " " + pt);
       }
    }
   
}       // enf of ininer class ValuesListener



/********************************************************************************/
/*                                                                              */
/*      Monitor state of engines                                                */
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
    }
   
   @Override public void enginePositionChanged(IfaceEngine eng) {
      IfacePoint pt = eng.getCurrentPoint();
      if (pt.getType() == ShorePointType.SIGNAL) {
         for (TrainPlanner tp : train_plans) {
            PlanTarget tgt = tp.getStartTarget();
            if (tgt == null) continue;
            if (tgt.getStartSignal().getAtPoints().contains(pt)) {
               start_signal = tgt.getStartSignal();
               start_block = pt.getBlock();
             }
            if (eng.getSpeed() == 0) {
               enableEngine(eng);
             }
          }
       }
      else {
         if (start_block != null && pt.getBlock() != start_block) {
            start_block = null;
            start_signal = null;
          }
       }
    }
   
   private void enableEngine(IfaceEngine eng) { }
   
}       // end of inner class EngineChanged

}       // end of class ViewPlannerFx




/* end of ViewPlannerFx.java */

