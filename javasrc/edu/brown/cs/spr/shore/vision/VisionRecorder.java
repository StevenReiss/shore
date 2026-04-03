/********************************************************************************/
/*                                                                              */
/*              VisionRecorder.java                                             */
/*                                                                              */
/*      Continuous Vision Processing                                            */
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
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import edu.brown.cs.ivy.file.IvyLog;

class VisionRecorder implements VisionConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private VisionFactory   vision_base;
private boolean         is_paused;
private boolean         is_recording;
private Mat             prior_image;
private VideoCapture    train_cam;
private CameraThread    camera_thread;
private int             prior_size;
private Mat             total_image;

private AtomicInteger   image_count = new AtomicInteger(0);



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

VisionRecorder(VisionFactory vb)
{
   System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
   vision_base = vb;
   is_paused = false;
   is_recording = false;
   prior_image = null;
   train_cam = new VideoCapture(CAMERA_ID);
   camera_thread = null;
   prior_size = 0;
   total_image = null;
   
}


/********************************************************************************/
/*                                                                              */
/*      Running method                                                          */
/*                                                                              */
/********************************************************************************/

void start()
{ 
   try {
      Mat matrix = new Mat();
      train_cam.read(matrix);
      try {
         Thread.sleep(1000);
       }
      catch (InterruptedException e) { }
    }
   catch (Throwable t) {
      IvyLog.logE("VISION","Initial camera read failed",t);
    }
   
   if (camera_thread == null) {
      camera_thread = new CameraThread();
      camera_thread.start();
    }
}


synchronized void startRecording()
{
   is_recording = true;
   resumeRecording();
}
 


synchronized void finishRecording()
{
   is_recording = false;
   is_paused = true;
   notifyAll();
}


synchronized void pauseRecording()
{
   if (is_recording) is_paused = true;
}



synchronized void resumeRecording()
{
   if (is_recording) {
      prior_image = null;
      is_paused = false;
      notifyAll();
    }
}


private synchronized void waitForRunning()
{
   while (!vision_base.isLayoutReady() && is_paused) { 
      try {
         wait(60000);
       }
      catch (InterruptedException e) { }
    }
}


boolean isRecording()
{
   return is_recording;
}


boolean isPaused()
{
   return is_paused;
}



/********************************************************************************/
/*                                                                              */
/*      Vision processing                                                       */
/*                                                                              */
/********************************************************************************/

/**
 *      process camera image passed in as argument.  
 *      Returns a matrix to be released (or null if there is none)
 **/

private Mat handleNextFrame(Mat matrix)
{
   if (prior_image == null) {
      prior_image = matrix;
      return null;
    }
   
   Mat delta = new Mat();
   Core.absdiff(prior_image,matrix,delta);
   
   Mat gray = new Mat();
   Imgproc.cvtColor(delta,gray,Imgproc.COLOR_BGR2GRAY);
   Mat thresh = new Mat();
   Imgproc.threshold(gray,thresh,MIN_THRESOLD,255,
         Imgproc.THRESH_BINARY); 
   
   Mat lbls = new Mat();
   Mat stats = new Mat();
   Mat cent = new Mat();
   Mat hier = new Mat();
   
   int c0 = Imgproc.connectedComponentsWithStats(thresh,lbls,stats,cent);
   
   List<MatOfPoint> contours = new ArrayList<>();
   Imgproc.findContours(thresh,contours,hier,
         Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_SIMPLE);
   
   IvyLog.logD("VISION","Components with stats returned " +
         c0 + " " + lbls.size() + " " + stats.size() + " " + cent.size() + " " +
         contours.size() + " " + hier.size());
   int ctr = 0;
   List<Point2D> usepoints = new ArrayList<>();
   for (int i = 1; i < stats.height(); ++i) {
      double sz = stats.get(i,4)[0];
      if (sz >= 2) {
         IvyLog.logD("VISION","Found block of size " + sz);
       }
      if (sz >= MIN_SIZE) {
         ++ctr;
         if (sz < MAX_SIZE) {
            double xpos = cent.get(i,0)[0];
            double ypos = cent.get(i,1)[0];
            usepoints.add(new Point2D.Double(xpos,ypos));
            IvyLog.logD("VISION","USE DELTA " + " " +
                  xpos + " " + ypos + " " +
                  stats.get(i,2)[0] + " " + stats.get(i,3)[0] + " " +
                  stats.get(i,4)[0]);
          }
         else if (sz < IGNORE_SIZE) { 
            int cont = -1;
            for (int j = 0; j < contours.size(); ++j) {
               MatOfPoint mp = contours.get(j);
               double [] coord = mp.get(0,0);
               int x0 = (int) coord[0];
               int y0 = (int) coord[1];
               double [] lbl = lbls.get(y0,x0);
               if (lbl[0] == i) {
                  IvyLog.logD("VISION","Found contour " + i + " " + lbl[0] + " " + j + 
                        " " + x0 + " " + y0);
                  cont = j;
                  break;
                }
             }
            if (cont >= 0) {
               MatOfPoint mp = contours.get(cont);
               List<Point2D> cpts = getContourRectangle(mp);
               if (cpts != null) {
                  usepoints.addAll(cpts);
                  continue;
                }
             }
            
            double xpos = cent.get(i,0)[0];
            double ypos = cent.get(i,1)[0];
            IvyLog.logD("VISION","Check for rectangle " + xpos + " " + ypos);
            // loo through contours to find one containing (xpos,ypos)
            usepoints.add(new Point2D.Double(xpos,ypos));
          }
       }
    }
   
   Mat rslt = null;
   if (ctr > 0) {
      if (!usepoints.isEmpty()) {
         if (is_recording) {
            vision_base.recordDeltaPoints(usepoints); 
            int ct = image_count.incrementAndGet();
            if (ct % 25 == 0) {
//             IvyLog.logD("Images saved as " + ct);
//             saveMatrix(matrix,"/vol/spr/image" + ct + ".jpg");
//             saveMatrix(delta,"/vol/spr/delta" + ct + ".jpg");
//             saveMatrix(thresh,"/vol/spr/thresh" + ct + ".jpg");
             }
            rslt = prior_image;
            
            if (total_image == null) {
               total_image = thresh.clone();
             }
            else {
               Core.bitwise_or(total_image,thresh,total_image);
             }
            int sz = vision_base.getLayoutSize();
            if (sz > 0 && sz % 100 == 0 && sz != prior_size) {
               prior_size = sz;
               Mat out = Mat.ones(matrix.size(),matrix.type());
               vision_base.fillInLayout(out); 
               saveMatrix(out,"/vol/spr/layout" + ct + ".jpg"); 
               saveMatrix(total_image,"/vol/spr/total" + ct + ".jpg");
             }
          }
         else {
            vision_base.noteDeltaPoints(usepoints); 
          }
       }
      prior_image = matrix;
    }
   else {
      rslt = matrix;
    }
   
   gray.release();
   thresh.release();
   lbls.release();
   stats.release();
   cent.release();
   hier.release();
   for (MatOfPoint mp : contours) mp.release();
   
   return rslt;
}


private List<Point2D> getContourRectangle(MatOfPoint mp)
{
   if (mp.height() == 0) return null;
   
   double [] coord = mp.get(0,0);
   double minx = coord[0];
   double maxx = coord[0];
   double miny = coord[1];
   double maxy = coord[1];
   Point2D [] points = new Point2D[4];
  
   for (int i = 0; i < mp.height(); ++i) {
      coord = mp.get(i,0);
      double x = coord[0];
      double y = coord[1];
      IvyLog.logD("VISION","Rectangle point " + x + " " + y);
      if (x <= minx) {
         minx = x;
         double y0 = y;
         if (points[0] == null) points[0] = new Point2D.Double();
         else y0 = Math.max(y,points[0].getY());
         points[0].setLocation(x,y0);
       }
      if (x >= maxx) {
         maxx = x;
         double y0 = y;
         if (points[2] == null) points[2] = new Point2D.Double();
         else y0 = Math.min(y,points[2].getY());
         points[2].setLocation(x,y0);
       }
      if (y <= miny) {
         miny = y;
         double x0 = x;
         if (points[1] == null) points[1] = new Point2D.Double();
         else x0 = Math.min(x,points[1].getX());
         points[1].setLocation(x0,y);
       }
      if (y >= maxy) {
         maxy = y;
         double x0 = x;
         if (points[3] == null) points[3] = new Point2D.Double();
         else x0 = Math.max(x,points[3].getX());
         points[3].setLocation(x0,y);
       }
    }
   
   IvyLog.logD("VISION","Rectangle points " +
         minx + " " + maxx + " " + miny + " " + maxy + " " +
         points[0] + " " + points[1] + " " + points[2] + " " + points[3]);
   
   Point2D p0 = between(points[0],points[1]);
   Point2D p1 = between(points[2],points[3]);
   double d0 = p0.distance(p1);
   
   Point2D p3 = between(points[1],points[2]);
   Point2D p4 = between(points[3],points[0]);
   double d1 = p3.distance(p4);
   
   if (d0 > d1) {
      Point2D p0a = new Point2D.Double((p0.getX() * 3 + p1.getX())/4.0,
            (p0.getY() * 3 + p1.getY())/4.0);
      Point2D p1a = new Point2D.Double((p0.getX() + p1.getX()*3)/4.0,
            (p0.getY() + p1.getY()*3)/4.0);
      return List.of(p0a,p1a);
    }
   else {
      Point2D p3a = new Point2D.Double((p3.getX() * 3 + p4.getX())/4.0,
            (p3.getY() * 3 + p4.getY())/4.0);
      Point2D p4a = new Point2D.Double((p3.getX() + p4.getX()*3)/4.0,
            (p3.getY() + p4.getY()*3)/4.0);
      return List.of(p3a,p4a);
    }
}


private Point2D between(Point2D p0,Point2D p1)
{
   return new Point2D.Double((p0.getX() + p1.getX())/2,
         (p0.getY() + p1.getY())/2);
}



/********************************************************************************/
/*                                                                              */
/*      Utility methods                                                         */
/*                                                                              */
/********************************************************************************/

private void saveMatrix(Mat matrix,String file)
{
   if (matrix.width() == 0 || matrix.height() == 0) return;
   
   BufferedImage image = new BufferedImage(matrix.width(),
         matrix.height(),BufferedImage.TYPE_3BYTE_BGR);
   WritableRaster raster = image.getRaster();
   DataBufferByte db = (DataBufferByte) raster.getDataBuffer();
   byte [] data = db.getData();
   matrix.get(0,0,data);
   Imgcodecs.imwrite(file,matrix); 
}


/********************************************************************************/
/*                                                                              */
/*      Camera thread                                                           */
/*                                                                              */
/********************************************************************************/

private final class CameraThread extends Thread {
   
   private long last_read;
   
   CameraThread() {
      super("ShoreCameraThread");
      last_read = 0;
    }
   
   @Override public void run() {
      for ( ; ; ) {
         waitForRunning();
         long now = System.currentTimeMillis();
         if (last_read != 0) {
            IvyLog.logD("VISION","Delta time " + (now-last_read));
            // possibly wait here to fixed interval
          }
         if (now - last_read > 500) {
            if (prior_image != null) {
               prior_image.release();
               prior_image = null;
             }
          }
         last_read = System.currentTimeMillis();
         try {
            Mat matrix = new Mat();
            train_cam.read(matrix);
            Mat release = handleNextFrame(matrix);
            if (release != null) release.release();
            long endnow = System.currentTimeMillis();
            IvyLog.logD("VISION","Processing time " + (endnow - last_read));
          }
         catch (Throwable t) {
            IvyLog.logE("VISION","Problem handling vision frame",t);
          }
       }
    }
   
}       // end of inner class CameraThread



}       // end of class VisionRecorder




/* end of VisionRecorder.java */

