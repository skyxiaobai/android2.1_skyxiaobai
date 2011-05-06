package SMSReceive.com;

import java.util.Arrays;
import java.util.List;

import android.content.BroadcastReceiver;

import android.content.Context;

import android.content.Intent;

import android.os.Bundle;

import android.telephony.SmsMessage;
import android.util.Log;

public class SMSReceive extends BroadcastReceiver {

	public static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";

	public static final String baidu[] = {"ECOM_NOVA_TRANSMIT_Y","ECOM_NOVA_APACHE_Y","ECOM_NOVA_BVS_TTM","BAIDU_BVS_TTM_MONITOR"
		
		,"ECOM_NOVA_UI_FIND_CORE","ECOM_NOVA_PB_FLOW_RATIO","ECOM_CPRO_APACHE_P","apache配置检查","tomcat配置检查","日志","ui","fr"};

	public static boolean containsAny(String str, String searchChars) { 
		  if(str.length()!=str.replace(searchChars,"").length())  { 
		   return true; 
		  } 
		  return false;
		 }
	  public void onReceive(Context context, Intent intent) {  
		         Log.v("###############", ">>>>>>>onReceive start");  
		           
		         StringBuilder body = new StringBuilder();
		         StringBuilder number = new StringBuilder();
		         Bundle bundle = intent.getExtras();  
		         if (bundle != null) {  
		             Object[] _pdus = (Object[]) bundle.get("pdus");  
		             SmsMessage[] message = new SmsMessage[_pdus.length];  
		             for (int i = 0; i < _pdus.length; i++) {  
		                 message[i] = SmsMessage.createFromPdu((byte[]) _pdus[i]);  
		             }  
		             for (SmsMessage currentMessage : message) {  
		                 body.append(currentMessage.getDisplayMessageBody());  
		                 number.append(currentMessage.getDisplayOriginatingAddress());  
		             }  
		             String smsBody = body.toString();  
		             String smsNumber = number.toString();  
		             if (smsNumber.contains("+86")) {  
		                 smsNumber = smsNumber.substring(3);  
		             }  
		             Log.v("@@@@@@@@@@@@@@@@", "smsNumber is :"+smsNumber); 
		              
		             if (smsNumber.equals("106575009006")) {
		            	 boolean flags_filter = false; 
		            	 for (int i = 0;i<baidu.length;i++) {	
		            		 Log.v("@@@@@@@@@@@@@@@@", "baidu[i] is :"+baidu[i]);  
		            		 if (containsAny(smsBody,baidu[i])) {
		            			 flags_filter = true;
		 		                 Log.v("@@@@@@@@@@@@@@@@", "successful!!");  
		 		                break;
		            		 }
		            	 }
			             if (!flags_filter) { 
			            	 Log.v("@@@@@@@@@@@@@@@@", "abortBroadcast!!");  
			            	 this.abortBroadcast();
		        		 }
		             } 		              
		         }  
		         Log.v("###############", ">>>>>>>onReceive end");  
		     }  
		 }  

