/********************************************************************************/
/*                                                                              */
/*              ViewDisplay.java                                                */
/*                                                                              */
/*      Main display for SHORE user interface                                   */
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

import java.util.Collection;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import edu.brown.cs.spr.shore.iface.IfaceDiagram;
import edu.brown.cs.spr.shore.iface.IfaceEngine;

class ViewDisplay extends JFrame implements ViewConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ViewFactory     view_factory;

private static final long serialVersionUID = 1;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ViewDisplay(ViewFactory fac)
{
   super("SHORE Display");
   
   view_factory = fac;
   
   EngineerPanel epanel = new EngineerPanel();
   ViewPlanner ppanel = new ViewPlanner();
   DiagramPanel dpanel = new DiagramPanel();
   
   JSplitPane p0 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, 
         dpanel,ppanel);
   JSplitPane p1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
         p0,epanel);
   
   setContentPane(p1);
   pack();
}



/********************************************************************************/
/*                                                                              */
/*      Engineer panel -- hold multiple engineers                               */
/*                                                                              */
/********************************************************************************/

private class EngineerPanel extends JPanel {
   
   private static final long serialVersionUID = 1;
   
   EngineerPanel() {
      Collection<IfaceEngine> engs = view_factory.getTrainModel().getAllEngines(); 
      JComponent comp = null;
      for (IfaceEngine eng : engs) {
         ViewEngineer ve = new ViewEngineer(eng);
         if (comp == null) comp = ve;
         else {
            comp = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                  comp,ve);
          }
       }
      if (comp == null) {
         comp = new ViewEngineer(null);
       }
     
      add(comp);
    }
   
}       // end of inner class EngineerPanel







/********************************************************************************/
/*                                                                              */
/*      Diagram panel -- hold multiple diagrams                                 */
/*                                                                              */
/********************************************************************************/

private class DiagramPanel extends JPanel {
   
   private static final long serialVersionUID = 1;
   
   DiagramPanel() {
      Collection<IfaceDiagram> dgms = view_factory.getLayoutModel().getDiagrams();
      JComponent comp = null;
      for (IfaceDiagram dgm : dgms) {
         ViewDiagram vd = new ViewDiagram(dgm); 
         if (comp == null) comp = vd;
         else {
            comp = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                  comp,vd); 
          }
       }
      add(comp);
    }
   
}       // end of inner class DiagramPanel



}       // end of class ViewDisplay




/* end of ViewDisplay.java */

