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
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;

class ViewEngineerFx extends AnchorPane implements ViewConstants
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
   
   TextArea ta = new TextArea("Dummy Panel for Engine " + for_engine.getTrainName());
   AnchorPane.setTopAnchor(ta,0.0);
   AnchorPane.setBottomAnchor(ta,0.0);
   AnchorPane.setLeftAnchor(ta,0.0);
   AnchorPane.setRightAnchor(ta,0.0);
   getChildren().add(ta);
   setPrefSize(400,500);
}


}       // end of class ViewEngineerFx




/* end of ViewEngineerFx.java */

