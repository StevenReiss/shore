/********************************************************************************/
/*                                                                              */
/*              IfaceTrain.java                                                 */
/*                                                                              */
/*      Information about a train in SHORE                                      */
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

import java.net.SocketAddress;
import java.util.Collection;

/**
 *      representation of an engine, possibly pulling a train.  This tracks the
 *      current location of thr train and provides hooks for controlling the
 *      behavior of the train.
 **/
public interface IfaceEngine
{

IfaceBlock getEngineBlock();
IfaceBlock getCabooseBlock();
Collection<IfaceBlock> getAllBlocks();
void enterBlock(IfaceBlock blk);
void exitBlock(IfaceBlock blk);

boolean isStopped();
boolean isEmergencyStopped();
void stopTrain();
void emergencyStopTrain();
void startTrain();

String getTrainName();
SocketAddress getEngineAddress();

void setSpeed(int speed);
void blowHorn();
void ringBell();



}       // end of interface IfaceTrain




/* end of IfaceTrain.java */

