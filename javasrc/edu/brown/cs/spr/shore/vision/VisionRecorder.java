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
private Mat             prior_image;
private VideoCapture    train_cam;
private CameraThread    camera_thread;

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
   prior_image = null;
   train_cam = new VideoCapture(CAMERA_ID);
   camera_thread = null;
}


/********************************************************************************/
/*                                                                              */
/*      Running method                                                          */
/*                                                                              */
/********************************************************************************/

void start()
{ 
   Mat matrix = new Mat();
   train_cam.read(matrix);
   try {
      Thread.sleep(1000);
    }
   catch (InterruptedException e) { }
   
   saveMatrix(matrix,"/vol/spr/image0.jpg");
   
   if (camera_thread == null) {
      camera_thread = new CameraThread();
      camera_thread.start();
    }
   else {
      resume();
    }
}



synchronized void pause()
{
   is_paused = true;
}


synchronized void resume()
{
   prior_image = null;
   is_paused = false;
   notifyAll();
}


private synchronized void waitForRunning()
{
   while (is_paused) {
      try {
         wait(5000);
       }
      catch (InterruptedException e) { }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Vision processing                                                       */
/*                                                                              */
/********************************************************************************/

void handleNextFrame()
{
   Mat matrix = new Mat();
   train_cam.read(matrix);
  
   if (prior_image == null) {
      prior_image = matrix;
      return;
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
   
   int c0 = Imgproc.connectedComponentsWithStats(thresh,lbls,stats,cent);
   
   IvyLog.logD("VISION","Components with stats returned " +
         c0 + " " + lbls.size() + " " + stats.size() + " " + cent.size());
   int ctr = 0;
   List<Point2D> usepoints = new ArrayList<>();
   for (int i = 1; i < stats.height(); ++i) {
      double sz = stats.get(i,4)[0];
      if (sz >= 2) {
         IvyLog.logD("VISION","Found block of size " + sz);
       }
      if (sz >= MIN_SIZE) {
         ++ctr;
         if (sz < MIN_SIZE) {
            double xpos = cent.get(i,0)[0];
            double ypos = cent.get(i,1)[0];
            usepoints.add(new Point2D.Double(xpos,ypos));
            IvyLog.logD("VISION","USE DELTA " + " " +
                  xpos + " " + ypos + " " +
                  stats.get(i,2)[0] + " " + stats.get(i,3)[0] + " " +
                  stats.get(i,4)[0]);
          }
       }
    }
   if (ctr == 0) return;
   
   if (!usepoints.isEmpty()) {
      int ct = image_count.incrementAndGet();
      vision_base.recordDeltaPoints(usepoints); 
      
      IvyLog.logD("Images saved as " + ct);
      saveMatrix(matrix,"/vol/spr/image" + ct + ".jpg");
      saveMatrix(delta,"/vol/spr/delta" + ct + ".jpg");
      saveMatrix(thresh,"/vol/spr/thresh" + ct + ".jpg");  
    }
   
   prior_image = matrix;
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
         last_read = System.currentTimeMillis();
       
         handleNextFrame();
       }
    }
   
}       // end of inner class CameraThread



}       // end of class VisionRecorder




/* end of VisionRecorder.java */

