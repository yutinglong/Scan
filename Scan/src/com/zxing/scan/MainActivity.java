package com.zxing.scan;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.zxing.scan.zxing.CaptureActivity;

public class MainActivity extends Activity implements OnClickListener {
	private static final String weidianURL = "http://weidian.com/item.html?itemID=230780308&wfr=wx&from=singlemessage&isappinstalled=0";
	public static final int REQ_THIRD = 100;

	private Button btn1, btn2, btn3;
	private ImageView setting_img;

	protected Timer timer;
	private boolean isExit;
	protected MyTimerTask task;
	
	private int mPageStatus = 0;// 0

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);

		initView();
	}

	private void initView() {
		btn1 = (Button) findViewById(R.id.btn1);
		btn2 = (Button) findViewById(R.id.btn2);
		btn3 = (Button) findViewById(R.id.btn3);
		setting_img = (ImageView) findViewById(R.id.setting_img);
		btn1.setOnClickListener(this);
		btn2.setOnClickListener(this);
		btn3.setOnClickListener(this);
		setting_img.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn1:// 签收
//			try {
//				BaseAPI.requestByGet("http://"
//						+ BaseAPI.IP_HOST
//						+ "/dxs/yjAction_add.action?waybillCode=DXS111111118&expressCompanyName=中通&senderUserTelephone=13010456119&senderUserName=李四&type1=便利封(1KG)&senderArea=北京&money=10.0&type=1&state=0&agentCode=daishou20150903&courierUserName=KDY001&recipientUserName=张三&recipientUserTelephone=13717674044&remark=一些备注1");
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
			
			mPageStatus = 0;
			startCamera();
			break;
		case R.id.btn2:// 寄件
			mPageStatus = 1;
			startCamera();
			break;
		case R.id.btn3:// 微店
			openURL(MainActivity.this, weidianURL);
			break;
		case R.id.setting_img:
			Intent intent = new Intent(this, SettingActivity.class);
			startActivity(intent);
			break;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (isExit) {
				if (timer != null) {
					timer.cancel();
				}
				finish();
			} else {
				isExit = true;
				if (task != null) {
					task.cancel();
				}
				Toast.makeText(this, "再按一次退出应用", Toast.LENGTH_SHORT).show();
				task = new MyTimerTask();
				timer = new Timer();
				timer.schedule(task, 3000);
			}
			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	class MyTimerTask extends TimerTask {
		@Override
		public void run() {
			isExit = false;
		}
	}

	public static void openURL(Context context, String url) {
		try {
			Intent intent = new Intent("android.intent.action.VIEW",
					Uri.parse(url));
			context.startActivity(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQ_THIRD:
			switch (resultCode) {
			case RESULT_OK:
				Bundle b = data.getExtras();
				String mOrderId = b.getString("resultString");
				Log.d("YTL", "mOrderId = " + mOrderId);
				if(mPageStatus == 0){// 签收包裹
					Intent mIntent = new Intent(MainActivity.this, SubActivity1.class);
					mIntent.putExtra("resultString", mOrderId);
					startActivity(mIntent);
				}
				else if (mPageStatus == 1) {
					Intent mIntent = new Intent(MainActivity.this, SubActivity2.class);
					mIntent.putExtra("resultString", mOrderId);
					startActivity(mIntent);
				}
				break;
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	public void startCamera() {
		Intent intent = new Intent(this, CaptureActivity.class);
		startActivityForResult(intent, REQ_THIRD);
	}
}
