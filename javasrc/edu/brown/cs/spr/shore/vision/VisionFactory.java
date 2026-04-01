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
import java.io.File;
import java.util.List;
import java.util.ListIterator;

import org.opencv.core.Mat;

import edu.brown.cs.spr.shore.iface.IfaceModel;
import edu.brown.cs.spr.shore.iface.IfaceSensor;
import edu.brown.cs.spr.shore.iface.IfaceVision;
import edu.brown.cs.spr.shore.model.ModelBase;


public class VisionFactory implements VisionConstants, IfaceVision, 
      IfaceModel.ModelCallback 
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/
 
private VisionRecorder         vision_recorder;
private VisionLayout           vision_layout;
private ModelBase              model_base;
private File                   vision_file;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public VisionFactory(ModelBase mb)
{
   model_base = mb;
   vision_recorder = new VisionRecorder(this); 
   vision_recorder.pauseRecording();
   vision_layout = new VisionLayout();
   
   File f1 = new File(System.getProperty("user.home"));
   vision_file = new File(f1,"shorevision.xml");
}



/********************************************************************************/
/*                                                                              */
/*      Startup methods                                                         */
/*                                                                              */
/********************************************************************************/

public void start()
{
   vision_layout.load(vision_file);
   vision_recorder.start();
}


@Override public void startRecording()
{
   vision_layout.clearLayout();
   vision_recorder.resumeRecording();  
   model_base.addModelCallback(this);
}


@Override public void finishRecording() 
{
   vision_recorder.pauseRecording();
   vision_layout.save(vision_file);
   model_base.removeModelCallback(this);
}


@Override public void pauseRecording(boolean puase)
{
   if (puase) vision_recorder.pauseRecording();
   else vision_recorder.resumeRecording(); 
}


@Override public boolean isRecording() 
{
   return vision_recorder.isRecording(); 
}


@Override public boolean isPaused()
{
   return vision_recorder.isPaused(); 
}


boolean isLayoutReady()
{
   return vision_layout.isLayoutReady();
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


int getLayoutSize()
{
   return vision_layout.getSize(); 
}


void fillInLayout(Mat out)
{
   vision_layout.fillInLayout(out); 
}


/********************************************************************************/
/*                                                                                  */
/*      Handle vision processing when not recording                               */
/*                                                                                  */
/********************************************************************************/

void noteDeltaPoints(List<Point2D> pts)
{
   
   // find corresponding layout point
   // trigger sensor if there is one associated
}



/********************************************************************************/
/*                                                                              */
/*      Model callback while recording                                          */
/*                                                                              */
/********************************************************************************/

@Override public void sensorChanged(IfaceSensor sen)
{
   if (isRecording() && !isPaused()) {
      vision_layout.noteSensorChanged(sen);  
    }
}


}       // end of class VisionFactory




/* end of VisionFactory.java */

