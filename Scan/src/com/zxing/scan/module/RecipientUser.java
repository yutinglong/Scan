package com.zxing.scan.module;


import org.json.simple.JSONObject;

import com.zxing.scan.JSonUtils;

public class RecipientUser {
	public String recipientUserName;
	public String recipientUserTelephone;
	public String date;
	public String num;
	
	
	public static RecipientUser builder(JSONObject object){
		RecipientUser model = new RecipientUser();
		model.recipientUserName = JSonUtils.getString(object, "recipientUserName");
		model.recipientUserTelephone = JSonUtils.getString(object, "recipientUserTelephone");
		model.date = JSonUtils.getString(object, "date");
		model.num = JSonUtils.getString(object, "num");
		return model;
	}
}
