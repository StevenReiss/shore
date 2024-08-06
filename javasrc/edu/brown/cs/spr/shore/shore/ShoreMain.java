/********************************************************************************/
/*                                                                              */
/*              ShoreMain.java                                                  */
/*                                                                              */
/*      Main program for Smart HO Railroad Environment                          */
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



package edu.brown.cs.spr.shore.shore;

import java.io.File;

import edu.brown.cs.spr.shore.model.ModelBase;
import edu.brown.cs.spr.shore.network.NetworkMonitor;
import edu.brown.cs.spr.shore.safety.SafetyFactory;
import edu.brown.cs.spr.shore.train.TrainFactory;
import edu.brown.cs.spr.shore.view.ViewFactory;

public class ShoreMain implements ShoreConstants
{



/********************************************************************************/
/*                                                                              */
/*      Main program                                                            */
/*                                                                              */
/********************************************************************************/

public static void main(String [] args)
{
   ShoreMain sm = new ShoreMain(args);
   sm.process();
}

/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private NetworkMonitor  network_monitor;
private ModelBase       model_base;
private TrainFactory    train_base;
private SafetyFactory   safety_base;
private ViewFactory     view_base;

private File            model_file;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private ShoreMain(String [] args)
{
   ShoreLog.setup();
   
   model_base = null;
   network_monitor = null;
   model_file = null;
   train_base = null;
   safety_base = null;
   
   scanArgs(args);
}


/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

private void process()
{
   model_base = new ModelBase(model_file); 
   network_monitor = new NetworkMonitor(model_base);
   train_base = new TrainFactory(network_monitor,model_base);
   safety_base = new SafetyFactory(network_monitor,model_base,train_base); 
   view_base = new ViewFactory(model_base,train_base);
  
   // set up vision module here
   // set up user interface module here
   // set up control module here
   ShoreLog.logD("SHORE","ALL MODULES SET UP: " + safety_base);
   
   network_monitor.start(); 
   view_base.startDisplay(); 
}



/********************************************************************************/
/*                                                                              */
/*      Argument scanning methods                                               */
/*                                                                              */
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      String arg = args[i];
      if (arg.startsWith("-")) {
         if (arg.startsWith("-m")) {                            // -model <file>
            if (i+1 < args.length && model_file == null) {
               model_file = new File(args[++i]);
             }
            else badArgs();
          }
         else badArgs();
       }
      else if (model_file == null) {
         model_file = new File(arg);
       }
    }
   
   if (model_file == null || !model_file.canRead()) badArgs();
}


private void badArgs()
{
   System.err.println("SHORE -m <modelfile>");
   System.exit(1);
}



}       // end of class ShoreMain




/* end of ShoreMain.java */

