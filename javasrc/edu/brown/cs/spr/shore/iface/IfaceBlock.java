/********************************************************************************/
/*                                                                              */
/*              IfaceBlock.java                                                 */
/*                                                                              */
/*      Representation of a track block for SHORE                               */
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

/**
 *      Represents a track block.  This is a connected section of the track that
 *      should only contain one train at a time.
 **/ 

public interface IfaceBlock extends IfaceConstants
{


/**
 *      return the state of the block
 **/
ShoreBlockState getBlockState();
 

/**
 *      Set the state of the block
 **/
void setBlockState(ShoreBlockState state);


/**
 *      If the state is PENDING, get the block it is pending from,
 *      return null if not pending or block not known.
 **/
IfaceBlock getPendingFrom();

/**
 *      Set the pending from block.  The current state of the
 *      block must be EMPTY.  Sets the state to PENDING.
 *      Returns true if the state was set.
 **/

boolean setPendingFrom(IfaceBlock blk);


/**
 *      If this is non-null, then the block given is in PENDING state
 *      and the train that will go into that block will have to enter
 *      this block as well.  Hence this block can't be set pending otherwise.
 *      Note that this block can be in other states while this is set.  This
 *      might not be the best way of preventing deadlock, but should work.
 **/ 
IfaceBlock getNextPending();



/**
 *      Return the set of connections associated with this block
 **/

Collection<IfaceConnection> getConnections();


/**
 *      Return the name/id of this block
 **/
String getId();


/**
 *      Get the point in the block to display the block id if that
 *      is desired as part of the user interface.  Also identifies
 *      a point known to be in the block.
 **/
IfacePoint getAtPoint();

 


}       // end of interface IfaceBlock




/* end of IfaceBlock.java */

