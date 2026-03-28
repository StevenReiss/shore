/********************************************************************************/
/*                                                                              */
/*              VisionFactory.java                                              */
/*                                                                              */
/*      Main facade for vison-based detection                                   */
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


package edu.brown.cs.spr.shore.vision;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.ListIterator;


public class VisionFactory implements VisionConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private VisionRecorder         vision_recorder;
private VisionLayout           vision_layout;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/


public VisionFactory()
{
   vision_recorder = new VisionRecorder(this); 
   vision_layout = new VisionLayout();
}



/********************************************************************************/
/*                                                                              */
/*      Startup methods                                                         */
/*                                                                              */
/********************************************************************************/

public void start()
{
   vision_layout.load(null);
   
   vision_recorder.start();
}



/********************************************************************************/
/*                                                                              */
/*      Point (delta) recording methods                                         */
/*                                                                              */
/********************************************************************************/

void recordDeltaPoints(List<Point2D> pts)
{
   for (ListIterator<Point2D> it = pts.listIterator(); it.hasNext(); ) {
      Point2D pt = it.next();
      Point2D pt0 = vision_layout.recordPoint(pt); 
      if (pt0 != pt) {
         it.set(pt0);
       }
    }
   
   // need logic to group pairs of points as front/back
   if (pts.size() == 2) {
      // add to train information
    }
}



}       // end of class VisionFactory




/* end of VisionFactory.java */

