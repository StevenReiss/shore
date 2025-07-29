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
import java.util.List;

import edu.brown.cs.ivy.swing.SwingEventListenerList;
import edu.brown.cs.spr.shore.iface.IfaceBlock;
import edu.brown.cs.spr.shore.iface.IfaceEngine;
import edu.brown.cs.spr.shore.iface.IfacePoint;
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
private SocketAddress           socket_address;
private boolean                 is_emergency;
private boolean                 bell_on;
private boolean                 horn_on;
private boolean                 reverse_on;
private boolean                 mute_on;
private double                  engine_speed;
private double                  engine_rpm;
private double                  engine_throttle; 
private boolean                 fromt_light;
private boolean                 rear_light;
private EngineState             engine_state;
private Color                   engine_color;
private String                  engine_id;

private List<IfaceBlock>        train_blocks;
private IfacePoint              current_point;
private IfacePoint              prior_point;

private SwingEventListenerList<EngineCallback> engine_listeners;



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
   current_point = null;
   prior_point = null;
   socket_address = null;
   is_emergency = false;
   bell_on = false;
   horn_on = false;
   reverse_on = false;
   mute_on = false;
   engine_speed = 0;
   engine_rpm = 0;
   engine_throttle = 0;
   fromt_light = false;
   rear_light = false;
   engine_state = EngineState.IDLE;
   engine_color = color;
   if (id != null) {
      engine_id = id.toUpperCase();
    }
   else engine_id = null;
   current_point = null;
   prior_point = null;
   
   engine_listeners = new SwingEventListenerList<>(EngineCallback.class);
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String getEngineName()                  { return engine_name; }

@Override public String getEngineId()                    { return engine_id; } 
 
@Override public SocketAddress getEngineAddress()        { return socket_address; }

void setEngineAddress(SocketAddress sa)                  { socket_address = sa; }  

@Override public boolean isEmergencyStopped()           { return is_emergency; }

@Override public boolean isBellOn()                     { return bell_on; }

@Override public boolean isHornOn()                     { return horn_on; }

@Override public boolean isReverse()                    { return reverse_on; } 

@Override public double getSpeed()                      { return engine_speed; }

@Override public double getRpm()                        { return engine_rpm; }

@Override public double getThrottle()                   { return engine_throttle; }

@Override public boolean isFrontLightOn()               { return fromt_light; }

@Override public boolean isRearLightOn()                { return rear_light; }

@Override public boolean isMuted()                      { return mute_on; }

@Override public EngineState getEngineState()           { return engine_state; }

@Override public Color getEngineColor()                 { return engine_color; }



/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

@Override public void setupEngine(boolean fwdlight,boolean revlight,
      boolean bell,boolean reverse,int state,
      int speedstep,int rpmstep,int speed,boolean estop,boolean mute)
{
   fromt_light = fwdlight;
   rear_light = revlight;
   bell_on = bell;
   horn_on = false;
   reverse_on = reverse;
   mute_on = mute;
   if (state == 0) engine_state = EngineState.IDLE;
   engine_speed = speed;
   if (estop) { 
      is_emergency = true;
      engine_state = EngineState.IDLE;
      engine_throttle = 0;
    }
   
   fireEngineChanged();
}
  

/********************************************************************************/
/*                                                                              */
/*      Block tracking methods                                                  */
/*                                                                              */
/********************************************************************************/

@Override public IfaceBlock getEngineBlock()
{
   if (current_point == null) return null;
   return current_point.getBlock();
}


@Override public IfacePoint getCurrentPoint() 
{
   return current_point; 
}
 

@Override public IfacePoint getPriorPoint()
{
   return prior_point;
}


void enterBlock(IfaceBlock blk) 
{
   if (blk == null) return;
   if (getEngineBlock() == blk) return;
   train_blocks.add(blk);
}


public void exitBlock(IfaceBlock blk)
{
   train_blocks.remove(blk);
}

void setCurrentPoints(IfacePoint cur,IfacePoint prior)
{
   current_point = cur;
   prior_point = prior;
   enterBlock(cur.getBlock());
   fireEnginePositionChanged();
}





/********************************************************************************/
/*                                                                              */
/*      Action methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public void setEmergencyStop(boolean stop)
{
   boolean chng = is_emergency != stop;
   
   is_emergency = stop;
   
   train_factory.getNetworkModel().sendEmergencyStop(this);
   
   if (chng) fireEngineChanged();
}


@Override public void setThrottle(double v) 
{
   boolean chng = engine_throttle != v;
   
   engine_throttle = v;
   train_factory.getNetworkModel().sendThrottle(this);
   
   if (chng) fireEngineChanged();
}


@Override public void setBell(boolean on)  
{
   boolean chng = bell_on != on;
   
   bell_on = on;
   train_factory.getNetworkModel().sendBell(this);
   
   if (chng) fireEngineChanged();
}

@Override public void setHorn(boolean on)
{
   boolean chng = horn_on != on;
   
   horn_on = on;
   train_factory.getNetworkModel().sendHorn(this);
   
   if (chng) fireEngineChanged();
}


@Override public void setFrontLight(boolean on)
{
   boolean chng = fromt_light != on;
   
   fromt_light = on;
   train_factory.getNetworkModel().sendLight(this,true);
   
   if (chng) fireEngineChanged();
}


@Override public void setRearLight(boolean on) 
{
   boolean chng = rear_light != on;
   
   rear_light = on; 
   train_factory.getNetworkModel().sendLight(this,false);
   
   if (chng) fireEngineChanged();
}


@Override public void setMute(boolean on)
{
   boolean chng = mute_on != on;
   
   mute_on = on;
   train_factory.getNetworkModel().sendMute(this);
   
   if (chng) fireEngineChanged();
}


@Override public void setReverse(boolean on)
{
   boolean chng = reverse_on != on;
   
   reverse_on = on;
   train_factory.getNetworkModel().sendReverse(this);
   
   if (chng) fireEngineChanged();
}


@Override public void setState(EngineState st)
{
   boolean chng = engine_state != st;
   
   engine_state = st;
   switch (st) {
      case STARTUP :
      case SHUTDOWN :
         train_factory.getNetworkModel().sendStartEngine(this);
         break;
    }
   
   if (chng) fireEngineChanged();
}


/********************************************************************************/
/*                                                                              */
/*      Callback Methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public void addEngineCallback(EngineCallback cb)
{
   engine_listeners.add(cb);
}

@Override public void removeEngineCallback(EngineCallback cb)
{
   engine_listeners.remove(cb);
}


private void fireEngineChanged()
{
   for (EngineCallback cb : engine_listeners) {
      cb.engineChanged(this);
    }
}

private void fireEnginePositionChanged()
{
   for (EngineCallback cb : engine_listeners) {
      cb.enginePositionChanged(this);
    }
}


/********************************************************************************/
/*                                                                              */
/*      Comparizon methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override public int compareTo(IfaceEngine e) 
{
   return getEngineName().compareTo(e.getEngineName());
}



}       // end of class TrainEngine




/* end of TrainEngine.java */

