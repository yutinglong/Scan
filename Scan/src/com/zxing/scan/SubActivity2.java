package com.zxing.scan;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;

public class SubActivity2 extends Activity implements OnClickListener{
	private View backBtn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_sub2);
		
		initView();
	}
	
	private void initView() {
		backBtn = (View) findViewById(R.id.newmore_btn_back);
		backBtn.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.newmore_btn_back:
			SubActivity2.this.finish();
			break;
		}
	}
}
