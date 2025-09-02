/********************************************************************************/
/*                                                                              */
/*              IfaceConstants.java                                             */
/*                                                                              */
/*      Global constants and enumerations for SHORE                               */
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



package edu.brown.cs.spr.shore.iface;



public interface IfaceConstants
{


/**
 *      Characterization of a point
 **/
enum ShorePointType { 
   TURNING,                     // track inflection point
   BLOCK,                       // location for block label
   SENSOR,                      // location of a sensor
   SWITCH,                      // pivot point of a switch
   SIGNAL,                      // location of a signal stopping point
   GAP,                         // gap between two blocks
   END,                         // dead end 
   TURNTABLE,                   // middle of a turntable
   DIAGRAM,                     // reference to another diagram
   LABEL,                       // label location (not part of track)
   X_CROSSING,                  // middle of an X crossing
   OTHER,                       // point on track, no other use  
};


/**
 *      State of an embedded sensor
 **/
enum ShoreSensorState {
   OFF,                         // off -- no train at this location
   ON,                          // on-- train at this location
   UNKNOWN                      // unknown -- initial value
};


/**
 *      State of a signal
 **/
enum ShoreSignalState { 
   OFF,                         // all lights off (initial state)
   GREEN,                       // green light on
   YELLOW,                      // yellow (or red and green) on
   RED                          // red on
};


/**
 *      Type of a signal
 **/
enum ShoreSignalType { 
   RG,                          // red and green lights only; signal for blocking
   RGY,                         // reg, green and yellow lights; signal for blocking
   RG_ANODE,
   RGY_ANODE,
   ENGINE,                       // red/green but there to wait to identify an engine,
   ENGINE_ANODE,                
};


/**
 *      State of a switch
 **/
enum ShoreSwitchState { 
   N,                           // N-state (straight)
   R,                           // R-state (curve)
   UNKNOWN                      // state is not known (initial)
};


/**
 *      Range of a sensor
 ***/
enum ShoreSensorRange {
   NORMAL,                      // 1000 - 2500
   HIGH,                        // 3000 - 3950
   LOW,                         // 200 - 500
   R3,
   R4,
   R5,
   R6,
   IGNORE,                      // ignore the sesnor
}


/**
 *      State of track block
 **/
enum ShoreBlockState { 
   EMPTY,                       // block is empty -- no trains present
   INUSE,                       // block has a train actively inside it
   PENDING,                     // block is reserved for a train coming from another block
   UNKNOWN                      // state is unknown (initial)
};


/**
 *      Reasons for slowing train
 **/
enum ShoreSlowReason {
   DEFAULT,                     // original speed from throttle setting
   SPEED_ZONE,                  // slow for speed zone
   SIGNAL,                      // slow for a signal ahead
   STOP,                        // stop for a signal
   ESTOP,                       // emergency stop was set before a stop
}



}       // end of interface IfaceConstants




/* end of IfaceConstants.java */

