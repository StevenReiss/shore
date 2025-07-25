/********************************************************************************/
/*                                                                              */
/*              IfaceTrains.java                                                */
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



package edu.brown.cs.spr.shore.iface;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.EventListener;

/**
 *      This ineterface represents the manager for the active
 *      trains on the table.  It lets one create and find trains,
 *      get the set of all trains, and communicate with the
 *      engines.  It also provides a callback for when information
 *      about a train changes.
 **/
public interface IfaceTrains 
{


/**
 *      Create a new train with the given name if it doesn't exist.
 *      Returns the train with that name if it does.
 **/
IfaceEngine createTrain(String name,String id);


/**
 *      Find the train with the given name.  Returns null if the
 *      train does not exist
 **/
IfaceEngine findTrain(String name);


/**
 *      Return the set of all entines that are known
 **/
Collection<IfaceEngine> getAllEngines();


/**
 *      Set up the communication with an engine by providing its
 *      UDP socket addess for communication.
 **/
IfaceEngine setEngineSocket(IfaceEngine engine,SocketAddress sa);


/**
 *      Add a callback to listen for train changes
 **/
void addTrainCallback(EngineCallback cb);


/**
 *      Remove a callback.  Has no effect if the callback has not
 *      been added or has been previously removed.
 **/
void removeTrainCallback(EngineCallback cb);


/**
 *      Define the network model -- used for initialization
 **/
void setNetworkModel(IfaceNetwork net);


/**
 *      Callback that is invoked when information about an engine
 *      or train is changed.
 **/
interface EngineCallback extends EventListener {
   
   default void engineChanged(IfaceEngine engine)        { }
   default void enginePositionChanged(IfaceEngine e)   { }
   
}


}       // end of interface IfaceTrains




/* end of IfaceTrains.java */

