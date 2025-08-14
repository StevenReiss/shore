/********************************************************************************/
/*										*/
/*		ViewDisplayFx.java						*/
/*										*/
/*	JavaFx implementation of SHORE user interface				*/
/*										*/
/********************************************************************************/
/*	Copyright 2023 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2023, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/



package edu.brown.cs.spr.shore.view;

import java.util.Collection;
import java.util.List;

import edu.brown.cs.spr.shore.iface.IfaceDiagram;
import edu.brown.cs.spr.shore.iface.IfaceEngine;
import edu.brown.cs.spr.shore.iface.IfaceTrains;
import edu.brown.cs.spr.shore.shore.ShoreLog;
import javafx.application.Application;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;


public class ViewDisplayFx extends Application implements ViewConstants
{



/********************************************************************************/
/*										*/
/*	Startup method								*/
/*										*/
/********************************************************************************/

static void setupFx(ViewFactory fac)
{
   view_factory = fac;
   try {
      launch();
    }
   catch (Throwable t) {
      ShoreLog.logE("VIEW","Problem creating fx",t);
    }
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static ViewFactory	view_factory;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public ViewDisplayFx()
{
   ShoreLog.logD("VIEW","CONSTRUCT FX");
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

@Override public void init()
{
   ShoreLog.logD("VIEW","INIT FX");
}

@Override public void stop()
{
   ShoreLog.logD("VIEW","STOP FX");
   System.exit(0);
}


@Override public void start(Stage stage)
{
   ShoreLog.logD("VIEW","START FX");
   try {
      EngineerPanel epanel = new EngineerPanel();
      LayoutPanel lpanel = new LayoutPanel();
      SplitPane overall = new FullSplitPane(lpanel,epanel);
      overall.setOrientation(Orientation.HORIZONTAL);
      Scene scn = new Scene(overall);
// lpanel.prefHeightProperty().bind(scn.heightProperty());
// epanel.prefHeightProperty().bind(overall.heightProperty());
// overall.prefHeightProperty().bind(scn.heightProperty());
      stage.setScene(scn);
      stage.show();
    }
   catch (Throwable t) {
      t.printStackTrace();
    }
   ShoreLog.logD("VIEW","START FX COMPLETE");
}



/********************************************************************************/
/*										*/
/*	Engineer panel -- hold multiple engineers				*/
/*										*/
/********************************************************************************/

private class EngineerPanel extends Pane {

   EngineerPanel() {
      Collection<IfaceEngine> engs = view_factory.getTrainModel().getAllEngines();
      Node [] engarr = new Node[engs.size()];
      int i = 0;
      for (IfaceEngine eng : engs) {
         ViewEngineerFx n = new ViewEngineerFx(view_factory,eng);   
         SplitPane.setResizableWithParent(n,true);
         engarr[i++] = n;
       }
      if (engarr.length > 1) {
         FullSplitPane spl = new FullSplitPane(engarr);
         spl.setOrientation(Orientation.VERTICAL);
         spl.setDefaultDividers();
         getChildren().add(spl);
       }
      else if (engarr.length == 1) {
         getChildren().add(engarr[0]);
       }
    }

}	// end of inner class EngineerPanel



/********************************************************************************/
/*										*/
/*	Layout Panel								*/
/*										*/
/********************************************************************************/

private class LayoutPanel extends BorderPane {

   LayoutPanel() {
      Collection<IfaceDiagram> dgms = view_factory.getLayoutModel().getDiagrams();
      IfaceTrains trains = view_factory.getTrainModel();
      Node [] pnls = new Node[dgms.size()+1];
      int i = 0;
      double totx = 0;
      double toty = 0;
      for (IfaceDiagram dgm : dgms) {
         ViewDiagramFx vd = new ViewDiagramFx(view_factory,dgm,trains);
         SplitPane.setResizableWithParent(vd,true);
         totx = Math.max(totx,vd.getPrefWidth());
         toty += vd.getPrefHeight();
         pnls[i++] = vd;
       }
      ViewPlannerFx planner = new ViewPlannerFx(view_factory); 
      SplitPane.setResizableWithParent(planner,true);
      totx = Math.max(totx,planner.getPrefWidth());
      toty += planner.getPrefHeight();
      pnls[i++] = planner;
      FullSplitPane spl = new FullSplitPane(pnls);
      ShoreLog.logD("Set split pane prefsize " + totx + " " + toty);
      spl.setPrefSize(totx,toty+30);
      spl.setOrientation(Orientation.VERTICAL);
      spl.setDefaultDividers();
      setTop(spl);
    }

}	// end of inner class LayoutPanel




/********************************************************************************/
/*										*/
/*	Split pane that uses full size						*/
/*										*/
/********************************************************************************/

private class FullSplitPane extends SplitPane {

   FullSplitPane(Node... nodes) {
      super(nodes);
    }

   void setDefaultDividers() {
      boolean usex = getOrientation() == Orientation.HORIZONTAL;
      double tot = 0;
      List<Node> nodes = getItems();
      for (Node n : nodes) {
	 double v = 100;
	 if (n instanceof Region) {
	    Region r = (Region) n;
	    v = (usex ? r.getPrefWidth() : r.getPrefHeight());
	  }
	 tot += v;
       }
      double [] pos = new double[nodes.size()];
      int i = 0;
      int x = 0;
      for (Node n : nodes) {
	 double v = 100;
	 if (n instanceof Region) {
	    Region r = (Region) n;
	    v = (usex ? r.getPrefWidth() : r.getPrefHeight());
	  }
	 x += v;
	 pos[i++] = x / tot;
       }

      setDividerPositions(pos);
    }


   @Override protected void layoutChildren() {
      Parent p = getParent();
      Scene s = getScene();
      double w0 = getWidth();
      double h0 = getHeight();
      double w = 0;
      double h = 0;
      if (p != null && p instanceof Pane) {
         Pane ppp = (Pane) p;
         w = ppp.getWidth();
         h = ppp.getHeight();
       }
      else if (p == null) {
         w = s.getWidth();
         h = s.getHeight();
         if (w < w0 || h < h0) {
           return;
          }
       }
      else {
         System.err.println("CHECK HERE");
       }
      if (w > 1.0) {
         if (w != w0 || h != h0) {
            resize(w,h);
          }
       }
   
      super.layoutChildren();
    }
}




}	// end of class ViewDisplayFx




/* end of ViewDisplayFx.java */

