package com.zxing.scan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;

import com.zxing.scan.zxing.CaptureActivity;

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
