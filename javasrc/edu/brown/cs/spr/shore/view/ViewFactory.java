/********************************************************************************/
/*										*/
/*		ViewFactory.java						*/
/*										*/
/*	Main access point for the user interface for SHORE			*/
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

import edu.brown.cs.spr.shore.iface.IfaceModel;
import edu.brown.cs.spr.shore.iface.IfaceTrains;

public class ViewFactory implements ViewConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IfaceModel	layout_model;
private IfaceTrains	train_model;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public ViewFactory(IfaceModel mdl,IfaceTrains trns)
{
   layout_model = mdl;
   train_model = trns;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

IfaceModel getLayoutModel()			{ return layout_model; }


IfaceTrains getTrainModel()			{ return train_model; }



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

public void startDisplay()
{
   JavaFxStarter starter = new JavaFxStarter();
   starter.start();
}



/********************************************************************************/
/*										*/
/*	JavaFx starter								*/
/*										*/
/********************************************************************************/

private class JavaFxStarter extends Thread {

   JavaFxStarter() {
      super("JavaFxStarter");
    }

   @Override public void run() {
      ViewDisplayFx.setupFx(ViewFactory.this);
    }
}


}	// end of class ViewFactory




/* end of ViewFactory.java */
