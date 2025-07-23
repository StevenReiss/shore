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
import javafx.scene.paint.Color;

class TrainEngine implements TrainConstants, IfaceEngine, Comparable<IfaceEngine>
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
private boolean                 bell_on;
private boolean                 reverse_on;
private double                  engine_speed;
private double                  engine_rpm;
private double                  engine_throttle; 
private boolean                 fwd_light;
private boolean                 rev_light;
private EngineState             engine_state;
private Color                   engine_color;
private String                  engine_id;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

TrainEngine(TrainFactory fac,String name,String id,Color color)
{
   train_factory = fac;
   engine_name = name;
   train_blocks = new ArrayList<>();
   socket_address = null;
   is_stopped = true;
   is_emergency = false;
   bell_on = false;
   reverse_on = false;
   engine_speed = 0;
   engine_rpm = 0;
   engine_throttle = 0;
   fwd_light = false;
   rev_light = false;
   engine_state = EngineState.IDLE;
   engine_color = color;
   engine_id = id;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String getTrainName()                  { return engine_name; }

@Override public String getEngineId()                    { return engine_id; } 

@Override public SocketAddress getEngineAddress()       { return socket_address; }

void setEngineAddress(SocketAddress sa)                 { socket_address = sa; }  

@Override public boolean isStopped()                    { return is_stopped; }

@Override public boolean isEmergencyStopped()
{
   return is_stopped && is_emergency;
}

@Override public boolean isBellOn()                     { return bell_on; }

@Override public boolean isReverse()                    { return reverse_on; } 

@Override public double getSpeed()                      { return engine_speed; }

@Override public double getRpm()                        { return engine_rpm; }

@Override public double getThrottle()                   { return engine_throttle; }

@Override public boolean isFwdLightOn()                 { return fwd_light; }

@Override public boolean isRevLightOn()                 { return rev_light; }

@Override public EngineState getEngineState()           { return engine_state; }

@Override public Color getEngineColor()                 { return engine_color; }

  

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
   // set engine_throttle
   // method body goes here
}

@Override public void toggleBell() 
{
   // set bell_on
   // method body goes here
}

@Override public void blowHorn()
{
   // method body goes here
}

@Override public void setFwdLight(boolean on)
{
   fwd_light = on;
   // send message
}


@Override public void setRevLight(boolean on)
{
   rev_light = on; 
   // send message
}


/********************************************************************************/
/*                                                                              */
/*      Comparizon methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override public int compareTo(IfaceEngine e) 
{
   return getTrainName().compareTo(e.getTrainName());
}



}       // end of class TrainEngine




/* end of TrainEngine.java */

