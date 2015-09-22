package com.zxing.scan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import com.zxing.scan.net.BaseAPI;
import com.zxing.scan.net.BaseAPI.RequestListener;
import com.zxing.scan.zxing.CaptureActivity;

// 签收包裹
public class SubActivity1 extends Activity implements OnClickListener{
	public static final int REQ_THIRD = 100;
	
	private View backBtn;
	
	private View btn1;
	
	private EditText mOrderEditText;
	private EditText mEditText1;
	private EditText mEditText2;
	private EditText mEditText3;
	private EditText mEditText4;	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_sub1);
		
		initView();
		
		Intent mIntent = getIntent();
		String mOrderId = mIntent.getStringExtra("resultString");
		Log.d("YTL", "mOrderId11111 = " + mOrderId);
		
		mOrderEditText.setText(mOrderId);
	}
	
	private void initView() {
		backBtn = (View) findViewById(R.id.newmore_btn_back);
		btn1 = findViewById(R.id.btn1);
		backBtn.setOnClickListener(this);
		btn1.setOnClickListener(this);
		
		mOrderEditText = (EditText) findViewById(R.id.order_id);
		mEditText1 = (EditText) findViewById(R.id.edittext1);
		mEditText2 = (EditText) findViewById(R.id.edittext2);
		mEditText3 = (EditText) findViewById(R.id.edittext3);
		mEditText4 = (EditText) findViewById(R.id.edittext4);
	}
	
	private void commitData() {
		String urlStr = "http://" + BaseAPI.IP_HOST + "/dxs/yjAction_add.action?";
		String mTemp = mOrderEditText.getText().toString();
		if (!TextUtils.isEmpty(mTemp)) {
			urlStr = urlStr + "waybillCode=" + mTemp;
		}
		else {
			Toast.makeText(this, "请填写数据", Toast.LENGTH_SHORT).show();
			return;
		}
		
		mTemp = mEditText1.getText().toString();
		if (!TextUtils.isEmpty(mTemp)) {
			urlStr = urlStr + "waybillCode=" + mTemp;
		}
		else {
			Toast.makeText(this, "请填写数据", Toast.LENGTH_SHORT).show();
			return;
		}
		
		mTemp = mEditText2.getText().toString();
		if (!TextUtils.isEmpty(mTemp)) {
			urlStr = urlStr + "waybillCode=" + mTemp;
		}
		else {
			Toast.makeText(this, "请填写数据", Toast.LENGTH_SHORT).show();
			return;
		}
		
		mTemp = mEditText3.getText().toString();
		if (!TextUtils.isEmpty(mTemp)) {
			urlStr = urlStr + "waybillCode=" + mTemp;
		}
		else {
			Toast.makeText(this, "请填写数据", Toast.LENGTH_SHORT).show();
			return;
		}
		
		mTemp = mEditText4.getText().toString();
		if (!TextUtils.isEmpty(mTemp)) {
			urlStr = urlStr + "waybillCode=" + mTemp;
		}
		else {
			Toast.makeText(this, "请填写数据", Toast.LENGTH_SHORT).show();
			return;
		}
		
		
		Log.d("YTL", "urlStr = " + urlStr);
		try {
			BaseAPI.requestByGet(urlStr, new RequestListener(){
				@Override
				public void onSuccess(String result) {
					if (!TextUtils.isEmpty(result) && "1".equals(result)) {
						// 成功
						Toast.makeText(SubActivity1.this, "签收成功", Toast.LENGTH_SHORT).show();
						SubActivity1.this.finish();
					}
				}

				@Override
				public void onFailure() {
					
				}
				
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.newmore_btn_back:
			SubActivity1.this.finish();
			break;
		case R.id.btn1:
			break;
		}
	}
}
