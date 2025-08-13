/********************************************************************************/
/*                                                                              */
/*              IfaceModel.java                                                 */
/*                                                                              */
/*      Model interface for SHORE                                               */
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

import java.util.Collection;
import java.util.EventListener;
import java.util.Set;

import org.w3c.dom.Element;

/**
 *      Representation of the complete layout model.  This includes
 *      all the diagrams, sensors, switches, blocks, signals, and
 *      connections.  This class provides some utility methods and
 *      the ability to get callbacks when items change.  The class
 *      is set up from a layout.xml file.
 **/
public interface IfaceModel
{

/**
 *      Return the set of all switches in the model
 **/
Collection<IfaceSwitch> getSwitches();


/**
 *      Return the set of all block connections in the model
 **/
Collection<IfaceConnection> getConnections(); 


/**
 *      Return the set of all signals in the model
 **/
Collection<IfaceSignal> getSignals();


/**
 *      Return the set of all sensors in the model
 **/
Collection<IfaceSensor> getSensors();


/**
 *      Return the set of all blocks in the model
 **/
Collection<IfaceBlock> getBlocks();


/**
 *      Return the set of all diagrams in the model
 **/
Collection<IfaceDiagram> getDiagrams();

/**
 *      Return the seet of all speed zones
 **/
Collection<IfaceSpeedZone> getSpeedZones();


/**
 *      Determine if a path from prev through pt goes to the
 *      point tgt without leaving the current block
 **/
boolean goesTo(IfacePoint prev,IfacePoint pt,IfacePoint tgt);


/**
 *      Find the next block on a patch from prev through pt.  This
 *      takes into account the current state of any switches along
 *      the way.
 **/
IfaceBlock findNextBlock(IfacePoint prev,IfacePoint at);


/**
 *      Return the XML used to load the model.  This is provided
 *      so that the XML can include additional information for 
 *      other modules (e.g. loops for planning, engine names).
 **/
Element getModelXml();



/**
 *      Add a callback for the model
 **/
void addModelCallback(ModelCallback cb);


/**
 *      Remove a callback for the model
 **/
void removeModelCallback(ModelCallback cb);


/**
 *      Find prior points in a block given prior and current
 **/
Set<IfacePoint> findPriorPoints(IfacePoint current,IfacePoint entry);


/**
 *      Find successor points in a block from given point.  This
 *      users the result of findPriorPoints and can follow switch
 *      settings if desired.
 **/
Set<IfacePoint> findSuccessorPoints(IfacePoint current,
      Set<IfacePoint> prior,boolean useswitch);


/**
 *      Find successor points given current and entry point
 **/
Set<IfacePoint> findSuccessorPoints(IfacePoint current,IfacePoint entry,
      boolean useswitch);

/**
 *      Call to note setup error
 **/
void noteError(String msg);



/**
 *      Callback interface for any changes to model componetns
 **/
interface ModelCallback extends EventListener {
   default void sensorChanged(IfaceSensor sensor)       { }
   default void switchChanged(IfaceSwitch sw)           { }
   default void signalChanged(IfaceSignal sig)          { }
   default void blockChanged(IfaceBlock blk)            { }
}



}       // end of interface IfaceModel




/* end of IfaceModel.java */

