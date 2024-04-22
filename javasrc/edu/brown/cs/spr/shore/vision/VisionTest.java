/********************************************************************************/
/*                                                                              */
/*              VisionTest.java                                                 */
/*                                                                              */
/*      Test program for experimenting with opencv                              */
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

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

public class VisionTest implements VisionConstants
{



/********************************************************************************/
/*                                                                              */
/*      Main program                                                            */
/*                                                                              */
/********************************************************************************/

public static void main(String [] args)
{
   VisionTest vt = new VisionTest(args);
   vt.process0();
   
   try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
      System.err.println("INPUT WHEN READY");
      String wait = reader.readLine();
      System.err.println("READ: " + wait);
    }
   catch (IOException e) { }
   
   vt.process1();
   
   vt.process2();
}



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private VideoCapture train_cam;
private Mat     background_image;
private Mat     delta_image;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private VisionTest(String [] args)
{ 
   System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
   background_image = null;
   delta_image = null;
   train_cam = new VideoCapture(CAMERA_ID);
}



/********************************************************************************/
/*                                                                              */
/*      Test methods                                                            */
/*                                                                              */
/********************************************************************************/

private void process0()
{
   // run guvcview to set up camera and get camera id
   
// setProperty(capture,Videoio.CAP_PROP_BRIGHTNESS,16.0);
// setProperty(capture,Videoio.CAP_PROP_SATURATION, 100.0);
// double v3 = capture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
   Mat matrix = new Mat();
   train_cam.read(matrix);
   try {
      Thread.sleep(10000);
    }
   catch (InterruptedException e) { }
   train_cam.read(matrix);
   
   System.err.println("CAPTRUED FRAME " + matrix.width() + "x" + matrix.height());
   
   saveMatrix(matrix,"/home/spr/backgroundimage.jpg");
   
   background_image = matrix;
}


private void process1()
{
   Mat next = new Mat();  
   train_cam.read(next);
   System.err.println("CAPTRUED FRAME " + next.width() + "x" + next.height()); 
   
   saveMatrix(next,"/home/spr/trainimage.jpg");
   
   delta_image = new Mat();
   Core.absdiff(background_image,next,delta_image);
   
   double threshold = 30;
   int [] data = new int[3];
   for (int j = 0; j < delta_image.rows(); ++j) {
      for (int i = 0; i < delta_image.cols(); ++i) {
         delta_image.get(j,i,data);
         double dist = data[0] * data[0] + data[1] * data[1] + data[2] * data[2];
         dist = Math.sqrt(dist);
         if (dist > threshold) {
            data[0] = data[1] = data[2] = 255;
            delta_image.put(0,0,data);
          }
       }
    }
   
   saveMatrix(delta_image,"/home/spr/deltaimage.jpg");
}


private void process2()
{
   Mat gray = new Mat();
   Imgproc.cvtColor(delta_image,gray,Imgproc.COLOR_BGR2GRAY);
   Mat thresh = new Mat();
   Imgproc.threshold(gray,thresh,50,255,Imgproc.THRESH_BINARY);
   
   List<MatOfPoint> contours = new ArrayList<>();
   Imgproc.findContours(thresh,contours,null,
         Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);
   
   Mat conn = new Mat();
   Imgproc.connectedComponents(thresh,conn);
}



private void saveMatrix(Mat matrix,String file)
{
   BufferedImage image = new BufferedImage(matrix.width(),
         matrix.height(),BufferedImage.TYPE_3BYTE_BGR);
   WritableRaster raster = image.getRaster();
   DataBufferByte db = (DataBufferByte) raster.getDataBuffer();
   byte [] data = db.getData();
   matrix.get(0,0,data);
   Imgcodecs.imwrite(file,matrix); 
}



}       // end of class VisionTest




/* end of VisionTest.java */

