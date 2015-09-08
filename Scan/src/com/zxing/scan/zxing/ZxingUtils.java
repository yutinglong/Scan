package com.zxing.scan.zxing;

import android.content.Context;
import android.telephony.TelephonyManager;

public class ZxingUtils {

	
	/**
	 * 获取手机的唯一标示IMEI号
	 * @return
	 */
	public static String getIMEI(Context mContext){
		TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE); 
		String IMEI = tm.getDeviceId();//String
		
		return IMEI;
	}
	
}
