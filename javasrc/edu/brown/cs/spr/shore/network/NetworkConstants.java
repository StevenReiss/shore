/********************************************************************************/
/*										*/
/*		NetworkConstants.java						*/
/*										*/
/*	Constants for using UDP over interet for control and modeling		*/
/*										*/
/********************************************************************************/
/*	Copyright 2023 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2023, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/



package edu.brown.cs.spr.shore.network;

import java.net.DatagramPacket;

import edu.brown.cs.spr.shore.iface.IfaceConstants;

public interface NetworkConstants extends IfaceConstants
{


/********************************************************************************/
/*										*/
/*	Network ports								*/
/*										*/
/********************************************************************************/

int	UDP_PORT = 2390;
int     ALT_PORT = 8266;
int	CONTROLER_PORT = 2390;

int	BUFFER_SIZE = 40;

long    STATUS_DELAY = 1000;            // delay between status messages
int     FINAL_DELAY = 10;               // Multiplier for final delay (of STATUS_DELAY)
long    HEARTBEAT_TIME = 70000;         // heartbeat check (should be 30000 after updates)

long    REPLY_DELAY = 1000;
long    LOCOFI_STATUS_DELAY = 250;
int     LOCOFI_FINAL_DELAY = 1;


/********************************************************************************/
/*										*/
/*	Callbacks								*/
/*										*/
/********************************************************************************/

interface MessageHandler {

   void handleMessage(DatagramPacket msg);

}	// end of inner interface MessageHandler



}	// end of interface NetworkConstants




/* end of NetworkConstants.java */

