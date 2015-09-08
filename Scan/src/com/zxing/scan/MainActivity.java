package com.zxing.scan;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener{
	private static final String weidianURL = "http://weidian.com/item.html?itemID=230780308&wfr=wx&from=singlemessage&isappinstalled=0";
	
	private Button btn1, btn2, btn3;
	
	protected Timer timer;
	private boolean isExit;
	protected MyTimerTask task;
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
		btn1.setOnClickListener(this);
		btn2.setOnClickListener(this);
		btn3.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn1:// 签收
			startActivity(new Intent(MainActivity.this, SubActivity1.class));
			break;
		case R.id.btn2:// 寄件
			startActivity(new Intent(MainActivity.this, SubActivity2.class));
			break;
		case R.id.btn3:// 微店
			openURL(MainActivity.this, weidianURL);
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
	            Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(url));
	            context.startActivity(intent);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
}
