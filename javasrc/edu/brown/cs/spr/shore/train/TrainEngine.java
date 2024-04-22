/********************************************************************************/
/*                                                                              */
/*              TrainEngine.java                                                */
/*                                                                              */
/*      description of class                                                    */
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



package edu.brown.cs.spr.shore.train;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceEngine;

class TrainEngine implements TrainConstants, IfaceEngine
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private TrainFactory            train_factory;
private String                  engine_name;
private List<IfaceBlock>        train_blocks;
private SocketAddress           socket_address;
private boolean                 is_stopped;
private boolean                 is_emergency;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

TrainEngine(TrainFactory fac,String name)
{
   train_factory = fac;
   engine_name = name;
   train_blocks = new ArrayList<>();
   socket_address = null;
   is_stopped = true;
   is_emergency = false;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String getTrainName()                  { return engine_name; }

@Override public SocketAddress getEngineAddress()       { return socket_address; }

void setEngineAddress(SocketAddress sa)                 { socket_address = sa; }  

@Override public boolean isStopped()                    { return is_stopped; }

@Override public boolean isEmergencyStopped()
{
   return is_stopped && is_emergency;
}



/********************************************************************************/
/*                                                                              */
/*      Block tracking methods                                                  */
/*                                                                              */
/********************************************************************************/


@Override public Collection<IfaceBlock> getAllBlocks()
{
   return new ArrayList<>(train_blocks);
}

@Override public IfaceBlock getEngineBlock()
{
   if (train_blocks.isEmpty()) return null;
   return train_blocks.get(train_blocks.size()-1);
}

@Override public IfaceBlock getCabooseBlock()
{
   if (train_blocks.isEmpty()) return null;
   return train_blocks.get(0);
}


@Override public void enterBlock(IfaceBlock blk)
{
   if (getEngineBlock() == blk) return;
   train_blocks.add(blk);
}

@Override 
public void exitBlock(IfaceBlock blk)
{
   train_blocks.remove(blk);
}



/********************************************************************************/
/*                                                                              */
/*      Action methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public void stopTrain()
{
   is_stopped = true;
   is_emergency = false;
   train_factory.getNetworkModel().sendStopTrain(this,false);
}


@Override public void emergencyStopTrain()
{
   is_stopped = true;
   is_emergency = true;
   train_factory.getNetworkModel().sendStopTrain(this,true);
}


@Override public void startTrain()
{
   is_stopped = false;
   train_factory.getNetworkModel().sendStartTrain(this);
}


@Override public void setSpeed(int arg0)
{
   // method body goes here
}

@Override public void ringBell()
{
   // method body goes here
}

@Override public void blowHorn()
{
   // method body goes here
}










}       // end of class TrainEngine




/* end of TrainEngine.java */

