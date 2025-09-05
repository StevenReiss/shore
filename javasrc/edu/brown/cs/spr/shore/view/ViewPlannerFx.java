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
import edu.brown.cs.spr.shore.iface.IfacePlanner.PlanCallback;
import edu.brown.cs.spr.shore.iface.IfacePlanner.PlanExecutable;
import edu.brown.cs.spr.shore.shore.ShoreLog;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;
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

private static int      STEP_START = -1;
private static int      STEP_DONE = 0;



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
      ChoiceBox<PlanAction> prv = null;
      
      signal_button = new Button("Set STOP Signal");
      signal_button.setOnAction(new SignalSetter(this));
      signal_button.setDisable(true);
//    signal_button.setMinWidth(120);
      start_button = new Button("START");
      start_button.setOnAction(new PlanStarter(this));
      start_button.setDisable(true);
//    start_button.setMinWidth(120);
      
      Label title = new Label("Define a Plan");
      title.setAlignment(Pos.CENTER);
      title.setTextAlignment(TextAlignment.CENTER);
      title.setTextFill(Color.RED);
      title.setFont(Font.font(20));
      add(title,1,0,REMAINING,1);
      ObservableList<PlanAction> starts = startOptions();
      for (int i = 0; i <= steps; ++i) {
         String lbl = (i == 0 ? "Start From:" : "Step " + i + ":");
         add(new Label(lbl),0,i+1,1,1);
         ChoiceBox<PlanAction> cb = new ChoiceBox<>();
         cb.setMinWidth(120);
         cb.setConverter(new TargetConverter());
         if (prv == null) {
            cb.setItems(starts);
          }
         else {
            PlanAction prvtgt = prv.getValue();
            if (prvtgt != null) {
               cb.setItems(options(prvtgt));
             }
          } 
         prv = cb;   
         add(cb,1,i+1,1,1);
         Spinner<Integer> sp = new Spinner<>(0,20,0);
         sp.setPrefWidth(60);
         add(sp,2,i+1,1,1);
         choice_boxes.add(cb);
         count_boxes.add(sp);
         cb.valueProperty().addListener(new ChoiceListener(this,i));
         cb.itemsProperty().addListener(new ValuesListener(this,i));
       }
      
      choice_boxes.get(0).setValue(starts.get(0));
      
      HBox buttons = new HBox();
      buttons.setSpacing(10.0);
      buttons.setAlignment(Pos.CENTER);
      buttons.getChildren().addAll(signal_button,start_button);
      add(buttons,0,steps+1+1,REMAINING,1);
      setMargin(buttons,new Insets(10));
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
         signal_button.setDisable(true);
         if (isComplete()) {
            signal_button.setDisable(false);
          }
       }
      else {
         signal_button.setDisable(true);
         start_button.setDisable(false); 
       }
      for_engine = eng;
   }
   
   PlanExecutable createPlan(IfaceEngine eng) {
      if (!isComplete()) return null;
      PlanExecutable plan = planner_model.createPlan(eng);
      for (int i = 0; i < choice_boxes.size(); ++i) {
         PlanAction pa = choice_boxes.get(i).getValue();
         if (pa == null) {
   //       ShoreLog.logD("VIEW","Action box " + i + " not defined");
            continue;
          }
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
      if (t == null) return "            ";
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
          if (newv == null || newv.getActionType() == PlanActionType.END) donext = false;
       }
      else {
         for_planner.getCountBox(for_step).setDisable(false);
       }
      if (donext) {
         if (for_step+1 < for_planner.getNumSteps()) {
            ChoiceBox<PlanAction> nextcb = for_planner.getChoiceBox(for_step+1);
            nextcb.setValue(null);
            nextcb.itemsProperty().set(options(newv));
          }
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
      if (eng == null) return;
      PlanExecutable pe = for_planner.createPlan(eng);
      ShoreLog.logD("VIEW","Create plan " + pe);
      if (pe == null) return;
      PlanAction act = for_planner.getStartAction();
      if (act == null) return;
      IfaceSignal sig = act.getSignal();
      if (sig == null) return;
      
      PlanViewer pv = new PlanViewer(pe);
      getChildren().add(pv);
      
      safety_model.setSignal(sig,ShoreSignalState.GREEN);
      // create a display for the executing plan
      // add a callback for this display to track state
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
   private IfaceEngine check_engine;
   
   EngineChanged() {
      start_signal = null;
      check_engine = null;
    }
   
   @Override public void engineChanged(IfaceEngine eng) {
      if (eng.getSpeed() == 0 && start_signal != null && start_block != null &&
            eng == check_engine) {
         // might want to check if engine is at stop point
         enableEngine(eng);
         check_engine = null;
       }
    }
   
   @Override public void enginePositionChanged(IfaceEngine eng) {
      IfacePoint pt = eng.getCurrentPoint();
      if (pt == null) return;
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
         else {
            check_engine = eng;
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



/********************************************************************************/
/*                                                                              */
/*      PlanViewer -- class to show a plan in action                            */
/*                                                                              */
/********************************************************************************/

private final class PlanViewer extends VBox implements PlanCallback {
   
   private PlanExecutable plan_executable;
   private ListView<PlanViewStep> list_view;
   private Button abort_button;
   private Button pause_button; 
   private Button close_button;
   private int active_step;
   
   PlanViewer(PlanExecutable pe) {
      plan_executable = pe;
      active_step = 0;
      
      setupDisplay();
      
      ShoreLog.logD("VIEW","Set active step (inital) to " + list_view.getItems().get(0));
            
      pe.addPlanCallback(this);
    }
   
   boolean isActive(PlanViewStep step) {
      int idx = list_view.getItems().indexOf(step);
      ShoreLog.logD("VIEW","Check if step " + step + " is active " +
            idx + " " + active_step);
      
      if (idx == active_step) return true;
      
      return false;
    }

   boolean isComplete(PlanViewStep step) {
      int idx = list_view.getItems().indexOf(step);
      ShoreLog.logD("VIEW","Check if step " + step + " is complete " + 
            idx + " " + active_step);
      
      if (idx < active_step) return true;
      
      return false;
    }            
   
   private void setupDisplay() {
      pause_button = new Button("PAUSE");
      pause_button.setOnAction(new ViewerPause(this));
      abort_button = new Button("ABORT");
      abort_button.setOnAction(new ViewerAbort(this));
      close_button = new Button("CLOSE");
      close_button.setOnAction(new ViewerClose(this));
      
      Label title = new Label("Plan for " + plan_executable.getEngine().getEngineName());
      title.setAlignment(Pos.CENTER);
      title.setTextAlignment(TextAlignment.CENTER);
      title.setTextFill(Color.RED);
      title.setFont(Font.font(20));
      
      ObservableList<PlanViewStep> steps = getSteps();
      list_view = new ListView<>(steps);
      list_view.setCellFactory(new ViewerCellFactory(this));
      
      HBox buttons = new HBox();
      buttons.setSpacing(10.0);
      buttons.setAlignment(Pos.CENTER);
      buttons.getChildren().addAll(abort_button,pause_button,close_button);
      
      getChildren().addAll(title,list_view,buttons);
    }
   
   private ObservableList<PlanViewStep> getSteps() {
      List<PlanViewStep> rslt = new ArrayList<>();
      PlanAction prev = null;
      for (int i = 0; i < plan_executable.getNumberOfSteps(); ++i) {
         PlanAction act = plan_executable.getStepAction(i);
         int actct = plan_executable.getStepCount(i);
         PlanActionType typ = act.getActionType();
         String txt = act.getName();                             
         if (prev != null) {
            String nm = prev.getName() + " to " + txt;
            PlanViewStep pvs = new PlanViewStep(nm,prev,STEP_DONE);
            rslt.add(pvs);
          }
         if (typ != PlanActionType.LOOP) {
            PlanViewStep pvs = new PlanViewStep(txt,prev,STEP_START);
            rslt.add(pvs);
          }
         else {
            for (int j = 0; j <= actct; ++j) {
               String nm = act.getName();
               if (j != actct) nm += " #" + (j+1);
               int idx = STEP_START;
               if (j > 0 && j != actct) idx = j;
               else if (j > 0) idx = STEP_DONE;
               PlanViewStep pvs = new PlanViewStep(nm,act,idx);
               rslt.add(pvs);
             }
          }
         prev = act;
       }
      
      PlanViewStep fini = new PlanViewStep("Finished plan",prev,STEP_START);
      rslt.add(fini);
      
      ShoreLog.logD("VIEW","Plan steps: ");
      for (int i = 0; i < rslt.size(); ++i) {
         ShoreLog.logD("VIEW","\t" + i + ": " + rslt.get(i));
       }
      
      return FXCollections.observableArrayList(rslt);
    }
   
   private boolean checkActive(PlanAction act,int ct) {
      PlanViewStep cur = list_view.getItems().get(active_step);
      if (cur.isActive(act,ct)) return false;
      
      int idx = 0;
      for (PlanViewStep pvs : list_view.getItems()) {
         if (pvs.isActive(act,ct)) {
            ShoreLog.logD("VIEW","Set active step to " + pvs);
            active_step = idx;
            list_view.scrollTo(pvs);
            return true;
          }
         ++idx;
       }
      
      return false;
    }
   
   void abortPlan() {
      ShoreLog.logD("VIEW","Abort plan");
      plan_executable.abort();
    }
   
   void pausePlan() {
      ShoreLog.logD("VIEW","Pause plan");
    }
   
   @Override public void planStarted(PlanExecutable p) { }
   
   @Override public void planStepStarted(PlanExecutable p,PlanAction act) {
      ShoreLog.logD("VIEW","Plan step started " + act);
      if (checkActive(act,STEP_START)) {
         list_view.refresh();
       }
    }
   
   @Override public void planStepCompleted(PlanExecutable p,PlanAction act,int ct) {
      ShoreLog.logD("VIEW","Plan step completed " + act + " " + ct);
      if (checkActive(act,ct)) {
         list_view.refresh();
       }
    }
   
   @Override public void planCompleted(PlanExecutable p,boolean abort) {
      plan_executable.removePlanCallback(this);
      setVisible(false);
    }
   
   @Override public void planPaused(PlanExecutable p,boolean paused) {
      if (paused) pause_button.setText("Resume");
      else pause_button.setText("Pause");
    }
   
}       // end of inner class PlanViewer


/********************************************************************************/
/*                                                                              */
/*      Displayed step in a plan                                                */
/*                                                                              */
/********************************************************************************/

private final class PlanViewStep {
   
   private String step_name;
   private PlanAction step_action;
   private int step_count;
   
   PlanViewStep(String name,PlanAction act,int ct) {
      step_name = name;
      step_action = act;
      step_count = ct;
    }
   
   String getName()                                     { return step_name; }
   
   boolean isActive(PlanAction act,int ct) {
      if (act != step_action) return false;
      if (ct != step_count) return false;
      return true;
    }
   
   @Override public String toString() { 
      return step_name + ": " + step_action + ":" + step_count;
    }
   
}       // end of inner class PlanViewStep



/********************************************************************************/
/*                                                                              */
/*      Classes for displaying the step in a listview                           */
/*                                                                              */
/********************************************************************************/

private class ViewerCellFactory 
      implements Callback<ListView<PlanViewStep>,ListCell<PlanViewStep>>
{
   private PlanViewer plan_viewer;
   
   ViewerCellFactory(PlanViewer pv) {
      plan_viewer = pv;
    }
   
   @Override public ListCell<PlanViewStep> call(ListView<PlanViewStep> param) {
      return new ViewerListCell(plan_viewer);
    }
   
}       // end of inner class ViewerCellFactory



private final class ViewerListCell extends ListCell<PlanViewStep> {
   
   private PlanViewer plan_viewer;
   
   ViewerListCell(PlanViewer pv) {
      plan_viewer = pv;
    }
   
   @Override protected void updateItem(PlanViewStep item,boolean empty) {
      super.updateItem(item,empty);
      ShoreLog.logD("VIEW","Update item " + item + " " + empty);
      
      if (empty || item == null) {
         setText(null);
         setGraphic(null);
       }
      else {
         Text tn = new Text(item.getName());
         if (plan_viewer.isActive(item)) {
            Font fn = tn.getFont();
            fn = Font.font(fn.getFamily(),FontWeight.BOLD,fn.getSize());
            tn.setFont(fn);
            tn.setStrikethrough(false);
          }
         else if (plan_viewer.isComplete(item)) {
            tn.setStrikethrough(true);
          }
         else {
            tn.setStrikethrough(false);
          }
         setText(null);
         setGraphic(tn);
       }
    }
   
}       // end of inner class ViewerListCell



/********************************************************************************/
/*                                                                              */
/*      Viewer actions                                                          */
/*                                                                              */
/********************************************************************************/

private class ViewerPause implements EventHandler<ActionEvent> {
   
   private PlanViewer for_viewer;
   
   ViewerPause(PlanViewer pv) {
      for_viewer = pv;
    }
   
   @Override public void handle(ActionEvent evt) {
      for_viewer.pausePlan();
    }
   
}       // end of inner class ViewerPause


private class ViewerAbort implements EventHandler<ActionEvent> {

   private PlanViewer for_viewer;

   ViewerAbort(PlanViewer pv) {
      for_viewer = pv;
    }
   
   @Override public void handle(ActionEvent evt) {
      for_viewer.abortPlan();
    }

}       // end of inner class ViewerAbort


private class ViewerClose implements EventHandler<ActionEvent> {

   private PlanViewer for_viewer;
   
   ViewerClose(PlanViewer pv) {
      for_viewer = pv;
    }
   
   @Override public void handle(ActionEvent evt) {
      for_viewer.setVisible(false);
    }
   
}       // end of inner class ViewerClose




}       // end of class ViewPlannerFx




/* end of ViewPlannerFx.java */

