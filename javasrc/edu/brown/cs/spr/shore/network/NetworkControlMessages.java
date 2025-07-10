/********************************************************************************/
/*                                                                              */
/*              NetworkControlMessages.java                                     */
/*                                                                              */
/*      description of interface                                                */
/*                                                                              */
/*      Written by spr                                                          */
/*                                                                              */
/********************************************************************************/



package edu.brown.cs.spr.shore.network;



interface NetworkControlMessages
{

/********************************************************************************/
/*                                                                              */
/*      Commands to the controller                                              */
/*                                                                              */
/********************************************************************************/

byte CONTROL_HEARTBEAT  = (byte) 0x0f;          // turn on/off heartbead
byte CONTROL_REBOOT     = (byte) 0x0a;          // restart
byte CONTROL_QUERY      = (byte) 0x40;          // ask for id
byte CONTROL_RESET      = (byte) 0x41;          // reset sensors, etc.
byte CONTROL_SYNC       = (byte) 0x42;          // ask for settings of sensors, switches
byte CONTROL_SETSWTICH  = (byte) 0x43;          // set switch to state
byte CONTROL_SETSIGNAL  = (byte) 0x44;          // set signal to state
byte CONTROL_DEFSENSOR  = (byte) 0x45;          // assoc sensor with switch state
byte CONTROL_SETSENSOR  = (byte) 0x46;          // note sensor state
byte CONTROL_DEFSIGNAL  = (byte) 0x47;          // set type of signal
byte CONTROL_DEFSWITCH  = (byte) 0x48;          // assoc r-index with switch



/********************************************************************************/
/*                                                                              */
/*      Commands from the controller                                            */
/*                                                                              */
/********************************************************************************/

byte CONTROL_ID         = (byte) 0x50;          // this is our ID
byte CONTROL_SENSOR     = (byte) 0x51;          // sensor set to value
byte CONTROL_SWITCH     = (byte) 0x52;          // switch set to value
byte CONTROL_SIGNAL     = (byte) 0x53;          // signal set to value
// byte CONTROL_ENDSYNC    = (byte) 0x54;



/********************************************************************************/
/*                                                                              */
/*      Message definitions                                                     */
/*                                                                              */
/********************************************************************************/

byte MESSAGE_ALL        = 0x10;

byte MESSAGE_OFF        = 0x0;
byte MESSAGE_ON         = 0x1;
byte MESSAGE_UNKNOWN    = 0x2;
byte MESSAGE_N          = 0x0;
byte MESSAGE_R          = 0x1;
byte MESSAGE_RED        = 0x1;
byte MESSAGE_GREEN      = 0x2;
byte MESSAGE_YELLOW     = 0x3;

byte MESSAGE_SIG_RG     = 0x0;
byte MESSAGE_SIG_RGY    = 0x1;
byte MESSAGE_SIG_RG_ANODE = 0x2;
byte MESSAGE_SIG_RGY_ANODE = 0x3;

byte MESSAGE_SENSOR     = 0x1;
byte MESSAGE_SWITCH     = 0x2;
byte MESSAGE_SIGNAL     = 0x3;
      

//
//      Standard messages consist of 4 bytes
//
//  The first byte is the command (as above)
//  The second byte is the controller id (0-15).  if MESSAGE_ALL, then all controllers
//  The third byte is the id (switch #, sensor #, signal #) if relevant
//      if it is MESSAGE_ALL it refers to all switches/sensors/signals
//  The fourth byte is the value [ on/off. N/R/Unknown, Red/Green/Yellow, 
//              switch# * 2 + (N/R) ]
//


//
//      Heartbeat messages are simple CONTROL_ID with controller id and nothing more
//




}       // end of interface NetworkControlMessages




/* end of NetworkControlMessages.java */
