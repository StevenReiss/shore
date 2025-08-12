/********************************************************************************/
/*                                                                              */
/*              NetworkLocoFiMessages.java                                      */
/*                                                                              */
/*      description of interface                                                */
/*                                                                              */
/*      Written by spr                                                          */
/*                                                                              */
/********************************************************************************/



package edu.brown.cs.spr.shore.network;



interface NetworkLocoFiMessages
{


/********************************************************************************/
/*										*/
/*	LocoFI commands 							*/
/*										*/
/********************************************************************************/

byte [] LOCOFI_START_ENGINE_CMD 	 = {0x00, 0x01};
byte [] LOCOFI_STOP_ENGINE_CMD		 = {0x00, 0x00};
byte [] LOCOFI_FWD_DIR_CMD		 = {0x01, 0x00};
byte [] LOCOFI_REV_DIR_CMD		 = {0x01, 0x01};
//{0x02} is reserved for setting the speed step
//the second and third argument is the actual speed step (two bytes passed in little endian format); 
//  for older versions, only second argument is needed; default 0
byte [] LOCOFI_SET_THROTTLE_CMD          = {0x02, 0x00, 0x00 };
byte [] LOCOFI_FWD_LIGHT_OFF_CMD	 = {0x03, 0x00};
byte [] LOCOFI_FWD_LIGHT_ON_CMD 	 = {0x03, 0x01};
byte [] LOCOFI_FWD_LIGHT_BLINK_CMD	 = {0x03, 0x02};
byte [] LOCOFI_REV_LIGHT_OFF_CMD	 = {0x04, 0x00};
byte [] LOCOFI_REV_LIGHT_ON_CMD 	 = {0x04, 0x01};
byte [] LOCOFI_REV_LIGHT_BLINK_CMD	 = {0x04, 0x02};
byte [] LOCOFI_HORN_ON_CMD		 = {0x05, 0x03, 0x01};
byte [] LOCOFI_HORN_OFF_CMD		 = {0x05, 0x03, 0x00};
byte [] LOCOFI_BELL_ON_CMD		 = {0x05, 0x04, 0x01};
byte [] LOCOFI_BELL_OFF_CMD		 = {0x05, 0x04, 0x00};
byte [] LOCOFI_RPM_REPORT_CMD		 = {0x06, 0x00};
byte [] LOCOFI_SPEED_REPORT_CMD 	 = {0x07, 0x00};
byte [] LOCOFI_QUERY_LOCO_STATE_CMD	 = {0x08, 0x00};
byte [] LOCOFI_CONNECT_SSID_CMD 	 = {0x09, 0x00};
byte [] LOCOFI_DISCONNECT_SSID		 = {0x09, 0x01};
byte [] LOCOFI_REBOOT_CMD		 = {0x0A, 0x00};
byte [] LOCOFI_VERSION_CMD		 = {0x0B, 0x00};
byte [] LOCOFI_HOSTNAME_CMD		 = {0x0C, 0x00};
byte [] LOCOFI_SETTINGS_READ_CMD	 = {0x0D, 0x00};
byte [] LOCOFI_SETTINGS_WRITE_CMD	 = {0x0D, 0x01};
//Do not use the following command for running the loco, use only for configuring speed
//the second and third argument is the actual speed step (two bytes passed in little endian format); 
//   for older versions, only second argument is needed; default 0
byte [] LOCOFI_SET_SPEED_CMD		 = {0x0E, 0x00, 0x00};
byte [] LOCOFI_HEARTBEAT_ON_CMD 	 = {0x0F, 0x01};
byte [] LOCOFI_HEARTBEAT_OFF_CMD	 = {0x0F, 0x00};
byte [] LOCOFI_FACTORY_RESET_CMD	 = {0x10, 0x00};
byte [] LOCOFI_QUERY_ABOUT_LOCO_CMD	 = {0x11, 0x00};
byte [] LOCOFI_EMERGENCY_STOP_CMD	 = {0x12, 0x00};       //the second argument 00 is for stop
byte [] LOCOFI_EMERGENCY_START_CMD	 = {0x12, 0x01};       //the second argument 01 is for resume
byte [] LOCOFI_GET_CONSIST_CMD		 = {0x13, 0x00};
byte [] LOCOFI_CREATE_CONSIST_LEAD_CMD	 = {0x14, 0x00};
byte [] LOCOFI_CREATE_CONSIST_HELPER_CMD = {0x14, 0x01};
byte [] LOCOFI_DELETE_CONSIST_CMD	 = {0x15, 0x00};
byte [] LOCOFI_SPEED_TABLE_READ_CMD	 = {0x16, 0x00};
byte [] LOCOFI_SPEED_TABLE_WRITE_CMD	 = {0x16, 0x01};
byte [] LOCOFI_SPEED_TABLE_UPDATE_CMD	 = {0x16, 0x02};
byte [] LOCOFI_SPEED_TABLE_DELETE_CMD	 = {0x16, 0x03};
byte [] LOCOFI_MUTE_VOLUME_CMD		 = {0x17, 0x00};       //only applicable for sound upgradeable modules
byte [] LOCOFI_UNMUTE_VOLUME_CMD	 = {0x17, 0x01};       //only applicable for sound upgradeable modules
byte [] LOCOFI_HIGH_FREQ_OP_OFF_CMD	 = {0x18, 0x00};
byte [] LOCOFI_HIGH_FREQ_OP_ON_CMD	 = {0x18, 0x01};

//{0x7C} is reserved for messages from lead to helper only
byte [] LOCOFI_HELPER_CMD_SS		 = {0x7C, 0x00}; //stop for safety; third byte contains OFF or ON

//{0x7D} is reserved for (asynchronous) ack messages from helper to lead
byte [] LOCOFI_HELPER_ACK_CMD_ES	 = {0x7D, 0x00}; //engine state; third byte contains the state



}       // end of interface NetworkLocoFiMessages




/* end of NetworkLocoFiMessages.java */
