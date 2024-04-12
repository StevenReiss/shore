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

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
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
}



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private VisionTest(String [] args)
{ 
   System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
}



/********************************************************************************/
/*                                                                              */
/*      Test methods                                                            */
/*                                                                              */
/********************************************************************************/

private void process0()
{
   VideoCapture capture = new VideoCapture(CAMERA_ID);
   Mat matrix = new Mat();
   capture.read(matrix);
   System.err.println("CAPTRUED FRAME");
   BufferedImage image = new BufferedImage(matrix.width(),
         matrix.height(),BufferedImage.TYPE_3BYTE_BGR);
   WritableRaster raster = image.getRaster();
   DataBufferByte db = (DataBufferByte) raster.getDataBuffer();
   byte [] data = db.getData();
   matrix.get(0,0,data);
   // want to get average brightness of image, and set camera BRIGHTNESS, CONTRAST,
   //   SATURATION, EXPOSURE accordingly
   
   String file = "/home/spr/testimage.jpg";
   Imgcodecs.imwrite(file,matrix);
}


}       // end of class VisionTest




/* end of VisionTest.java */

