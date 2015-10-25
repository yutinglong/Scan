package com.zxing.scan;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.zxing.scan.net.BaseAPI;
import com.zxing.scan.zxing.CaptureActivity;

public class MainActivity extends Activity implements OnClickListener {
	private static final String weidianURL = "http://weidian.com/item.html?itemID=230780308&wfr=wx&from=singlemessage&isappinstalled=0";
	public static final int REQ_THIRD = 100;

	private Button btn1, btn2, btn3, btn4, fast_btn1;
	private ImageView setting_img;
	private EditText edittext_password;

	protected Timer timer;
	private boolean isExit;
	protected MyTimerTask task;
	
	private int mPageStatus = 0;// 0

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		
		SharedPreferences sharedPreferences = getSharedPreferences("setting", Context.MODE_PRIVATE); //私有数据
		String IPStr = sharedPreferences.getString("IPStr", BaseAPI.IP_HOST);
		if (!TextUtils.isEmpty(IPStr)) {
			BaseAPI.IP_HOST = IPStr;
		}

		initView();
	}

	private void initView() {
		btn1 = (Button) findViewById(R.id.btn1);
		btn2 = (Button) findViewById(R.id.btn2);
		btn3 = (Button) findViewById(R.id.btn3);
		btn4 = (Button) findViewById(R.id.btn4);
		setting_img = (ImageView) findViewById(R.id.setting_img);
		btn1.setOnClickListener(this);
		btn2.setOnClickListener(this);
		btn3.setOnClickListener(this);
		btn4.setOnClickListener(this);
		setting_img.setOnClickListener(this);
		
		edittext_password = (EditText) findViewById(R.id.edittext_password);
		fast_btn1 = (Button) findViewById(R.id.fast_btn1);
		fast_btn1.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn1:// 签收
			mPageStatus = 0;
			startCamera();
			break;
		case R.id.btn2:// 代收货
			Intent intent = new Intent(this, SubActivity3.class);
			startActivity(intent);
			break;
		case R.id.btn3:// 寄快件
			mPageStatus = 1;
			startCamera();
			break;
		case R.id.btn4:// 微店
			openURL(MainActivity.this, weidianURL);
			break;
		case R.id.setting_img:
			Intent intentL = new Intent(this, SettingActivity.class);
			startActivity(intentL);
			break;
		case R.id.fast_btn1:// 快速取货
			fastGet();
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

	/**
	 * 快速取货
	 */
	private void fastGet() {
		String mTemp = edittext_password.getText().toString();
		if (TextUtils.isEmpty(mTemp)) {
			Toast.makeText(this, "请输入取货码", Toast.LENGTH_SHORT).show();
			return;
		}
		
		Intent intentL = new Intent(MainActivity.this, UserGetData.class);
		intentL.putExtra("identifyingCode", mTemp);
		startActivity(intentL);
		edittext_password.setText("");
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
