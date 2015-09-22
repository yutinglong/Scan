package com.zxing.scan;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;

import com.zxing.scan.net.BaseAPI;

public class SettingActivity extends Activity implements OnClickListener{
	public static final int REQ_THIRD = 100;
	
	private View backBtn;
	
	private View btn1;
	
	private EditText edittext_ip;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_setting);
		
		initView();
		
	}
	
	private void initView() {
		backBtn = (View) findViewById(R.id.newmore_btn_back);
		btn1 = findViewById(R.id.btn1);
		backBtn.setOnClickListener(this);
		btn1.setOnClickListener(this);
		
		edittext_ip = (EditText) findViewById(R.id.edittext_ip);
		
		SharedPreferences share=getSharedPreferences("setting", Context.MODE_PRIVATE);
		String str = share.getString("IPStr", "192.168.1.106:8080");
		
		edittext_ip.setText(str);
	}
	
	private void commitData() {
		
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.newmore_btn_back:
			SettingActivity.this.finish();
			break;
		case R.id.btn1:
			String IPStr = edittext_ip.getText().toString();
			if (!TextUtils.isEmpty(IPStr)) {
				BaseAPI.IP_HOST = IPStr;
			}
			SharedPreferences sharedPreferences = getSharedPreferences("setting", Context.MODE_PRIVATE); //私有数据
			Editor editor = sharedPreferences.edit();//获取编辑器
			editor.putString("IPStr", BaseAPI.IP_HOST);
			editor.commit();//提交修改
			this.finish();
			break;
		}
	}
}
