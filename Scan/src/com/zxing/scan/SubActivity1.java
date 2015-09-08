package com.zxing.scan;

import com.zxing.scan.zxing.CaptureActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;

public class SubActivity1 extends Activity implements OnClickListener{
	public static final int REQ_THIRD = 100;
	
	private View backBtn;
	
	private View btn1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_sub1);
		
		initView();
	}
	
	private void initView() {
		backBtn = (View) findViewById(R.id.newmore_btn_back);
		btn1 = findViewById(R.id.btn1);
		backBtn.setOnClickListener(this);
		btn1.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.newmore_btn_back:
			SubActivity1.this.finish();
			break;
		case R.id.btn1:
			startCamera();
			break;
		}
	}
	
	public void startCamera() {
		Intent intent = new Intent(this, CaptureActivity.class);
		startActivityForResult(intent, REQ_THIRD);
	}
}
