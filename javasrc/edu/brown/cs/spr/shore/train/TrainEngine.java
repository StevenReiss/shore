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
private boolean                 has_rear_light;
private double                  engine_speed;
private double                  engine_rpm;
private double                  engine_throttle; 
private boolean                 front_light;
private boolean                 rear_light;
private EngineState             engine_state;
private Color                   engine_color;
private String                  engine_id;

private int                     start_speed;
private int                     max_speed;
private double                  max_display;
private boolean                 use_kmph;
private int                     num_steps;

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
   front_light = false;
   rear_light = false;
   has_rear_light = true;
   engine_state = EngineState.UNKNOWN; 
   engine_color = color;
   if (id != null) {
      engine_id = id.toUpperCase();
    }
   else engine_id = null;
   current_point = null;
   prior_point = null;
   start_speed = 0;
   max_speed = 1023;
   max_display = 200;
   num_steps = 8;
   use_kmph = false;
    
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

void setEngineAddress(SocketAddress sa)                  
{ 
   if (socket_address == sa) return;
   
   socket_address = sa;
   
   fireEngineChanged();
}  

@Override public boolean isEmergencyStopped()           { return is_emergency; }

@Override public boolean isBellOn()                     { return bell_on; }

@Override public boolean isHornOn()                     { return horn_on; }

@Override public boolean isReverse()                    { return reverse_on; } 

@Override public double getRpm()                        { return engine_rpm; }

@Override public boolean isFrontLightOn()               { return front_light; }

@Override public boolean isRearLightOn()                { return rear_light; }

@Override public boolean isMuted()                      { return mute_on; }

@Override public EngineState getEngineState()           { return engine_state; }

@Override public Color getEngineColor()                 { return engine_color; }

void setNoRearLight()                                   { has_rear_light = false; }

@Override public boolean hasRearLight()                 { return has_rear_light; }
 


/********************************************************************************/
/*                                                                              */
/*      Throttle/speed control access methods                                   */
/*                                                                              */
/********************************************************************************/

@Override public double getThrottle()                   { return engine_throttle; }


@Override public void setThrottle(double v) 
{
   train_factory.getNetworkModel().sendThrottle(this,v);
// 
// boolean chng = engine_throttle != v; 
// engine_throttle = v;
// 
// train_factory.getNetworkModel().sendThrottle(this);
// 
// if (chng) fireEngineChanged();
}


@Override public double getSpeed()                      { return engine_speed; }
@Override public double getThrottleMax()                { return max_speed; }
@Override public double getSpeedMax()                   { return max_display; }
@Override public boolean isSpeedKMPH()                  { return use_kmph; }      
@Override public int getThrottleSteps()                 { return num_steps; } 
@Override public double getStartSpeed()                 { return start_speed; }


@Override public void setSpeedParameters(int start,int max,int nstep,double maxdisplay,boolean kmph) 
{
   start_speed = start;
   max_speed = max;
   num_steps = nstep;
   max_display = maxdisplay;
   use_kmph = kmph;
} 



/********************************************************************************/
/*                                                                              */
/*      Action methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public void setEmergencyStop(boolean stop)
{
   // actual stop set from engine state request
   train_factory.getNetworkModel().sendEmergencyStop(this,stop); 
}


@Override public void setBell(boolean on)  
{ 
   // bell will be set when engine says its on
   train_factory.getNetworkModel().sendBell(this,on);
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
   // light setting should change when engine actually says so
   
   train_factory.getNetworkModel().sendLight(this,true,on);
}


@Override public void setRearLight(boolean on) 
{
   // light setting should change when engine actually says so
   
   train_factory.getNetworkModel().sendLight(this,false,on);
}


@Override public void setMute(boolean on)
{
   train_factory.getNetworkModel().sendMute(this,on); 
}


@Override public void setReverse(boolean on)
{
   // reverse set from engine state
   
   train_factory.getNetworkModel().sendReverse(this,on);
}


@Override public void setState(EngineState st)
{
   if (engine_state == st) return;
   
   engine_state = st;
   
   switch (st) {
      case STARTUP :
         train_factory.getNetworkModel().sendStartStopEngine(this,true); 
         break;
      case SHUTDOWN :
         train_factory.getNetworkModel().sendStartStopEngine(this,false); 
         break;
    }
}


@Override public void doReboot()
{
   train_factory.getNetworkModel().sendReboot(this); 
}

/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

@Override public void setupEngine(boolean fwdlight,boolean revlight,
      boolean bell,boolean reverse,int state,
      int speedstep,int rpmstep,int speed,boolean estop,boolean mute)
{
   front_light = fwdlight;
   rear_light = revlight;
   bell_on = bell;
   horn_on = false;
   reverse_on = reverse;
   mute_on = mute;
   switch (state) {
      case 0 :
         if (engine_state != EngineState.STARTUP) {
            engine_state = EngineState.OFF;
          }
         break;
      case 1 :
         // SHUTDOWN -> SHUTDOWN, IDLE-> IDLE
         // READY -> READY
         switch (engine_state) {
            case UNKNOWN :
            case OFF :
            case STARTUP :
            case RUNNING :
               engine_state = EngineState.READY;
               break;
            default :
               break;
          }
         break;
      case 2 :
         engine_state = EngineState.RUNNING;
         break;
    }
   
   // convert this if necessary  
   engine_speed = speed;
   if (estop) { 
      is_emergency = true;
      engine_speed = 0;
//    engine_throttle = 0;
    }
   else {
      is_emergency = false;
    }
   
   engine_throttle = speedstep;
   
   switch (engine_state) {
      case OFF :
      case IDLE :
         engine_rpm = 0; 
         break;
      case STARTUP :
      case SHUTDOWN :
         engine_rpm = MIN_RPM;
         break;
      case RUNNING :
      case READY :
         double v0 = (rpmstep - start_speed);
         v0 /= (max_speed - start_speed);
         v0 = v0 * (MAX_RPM - MIN_RPM) + MIN_RPM;
//       v0 = rpmstep;
         engine_rpm = v0;
         break;
    } 
   
   // might want to check if there is a change
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

