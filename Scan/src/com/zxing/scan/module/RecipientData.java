package com.zxing.scan.module;


import org.json.simple.JSONObject;

import com.zxing.scan.JSonUtils;

public class RecipientData {
	public String waybillCode;
	public String expressCompanyName;
	public String recipientUserName;
	public String date2;
	public String remark;

	public static RecipientData builder(JSONObject object) {
		RecipientData model = new RecipientData();
		model.waybillCode = JSonUtils.getString(object, "waybillCode");
		model.expressCompanyName = JSonUtils.getString(object, "expressCompanyName");
		model.recipientUserName = JSonUtils.getString(object, "recipientUserName");
		model.date2 = JSonUtils.getString(object, "date2");
		model.remark = JSonUtils.getString(object, "remark");
		return model;
	}
}
