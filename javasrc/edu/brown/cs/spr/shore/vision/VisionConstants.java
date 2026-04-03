/********************************************************************************/
/*                                                                              */
/*              VisionConstants.java                                            */
/*                                                                              */
/*      Constants for using computer vision to detect trainsrr55                    */
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



public interface VisionConstants
{

int CAMERA_ID = 0;

double TRAIN_INITIAL_TOLERANCE = 0.5;
double TRAIN_LIST_TOLERANCE = 0.1;

double DELTA_THRESHOLD = 50;
double MIN_THRESOLD = 50; 
double MIN_SIZE = 10;
double MAX_SIZE = 100;
double IGNORE_SIZE = 200;

double MIN_DISTANCE_SAME = 5;
double MIN_DISTANCE_CONNECT = 10;
double MAX_DISTANCE_FIND = 10;
double MAX_DISTANCE_FIND_POINT = 15;



}       // end of interface VisionConstants




/* end of VisionConstants.java */

