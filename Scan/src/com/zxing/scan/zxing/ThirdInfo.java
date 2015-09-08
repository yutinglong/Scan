package com.zxing.scan.zxing;

public class ThirdInfo {
	private String third_app_id;
	private String owner_app_id;
	private String third_user_id;
	
	public ThirdInfo(String third_app_id, String owner_app_id,
			 String third_user_id) {
		super();
		this.third_app_id = third_app_id;
		this.owner_app_id = owner_app_id;
		this.third_user_id = third_user_id;
	}

	public String getThird_app_id() {
		return third_app_id;
	}

	public void setThird_app_id(String third_app_id) {
		this.third_app_id = third_app_id;
	}

	public String getOwner_app_id() {
		return owner_app_id;
	}

	public void setOwner_app_id(String owner_app_id) {
		this.owner_app_id = owner_app_id;
	}

	public String getThird_user_id() {
		return third_user_id;
	}

	public void setThird_user_id(String third_user_id) {
		this.third_user_id = third_user_id;
	}
	
}
