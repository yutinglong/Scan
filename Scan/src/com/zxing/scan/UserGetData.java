package com.zxing.scan;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.zxing.scan.module.RecipientData;
import com.zxing.scan.net.BaseAPI;
import com.zxing.scan.net.BaseAPI.RequestListener;

// 用户取件
public class UserGetData extends Activity implements OnClickListener{
	private View backBtn;
	private TextView user_name;
	private TextView user_phone;
	private TextView user_tip;
	private LinearLayout content_layout;
	private EditText edittext_pwd;
	
	private TextView user_pwd_forget;
	private List<RecipientData> mDataList = new ArrayList<RecipientData>();
	
	private String identifyingCode;// 取货码
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_get_data);
		
		
//		intentL.putExtra("identifyingCode", mTemp);
		
		identifyingCode = getIntent().getStringExtra("identifyingCode");
		
		initView();
		getData();
	}
	
	private void initView() {
		backBtn = (View) findViewById(R.id.newmore_btn_back);
		backBtn.setOnClickListener(this);
		
		findViewById(R.id.btn1).setOnClickListener(this);
		
		user_name = (TextView) findViewById(R.id.user_name);
		user_phone = (TextView) findViewById(R.id.user_phone);
		user_tip = (TextView) findViewById(R.id.user_tip);
		content_layout = (LinearLayout) findViewById(R.id.content_layout);
		edittext_pwd = (EditText) findViewById(R.id.edittext_pwd);
		user_pwd_forget = (TextView) findViewById(R.id.user_pwd_forget);
		user_pwd_forget.setOnClickListener(this);
	}
	
	/**
	 * 获取某个用户的所有包裹
	 */
	private void getData() {
		String urlStr = "http://" + BaseAPI.IP_HOST
				+ "/dxs/yjAction_getPackageList.action?";
		if (identifyingCode != null && !identifyingCode.equals("")) {
			urlStr = urlStr + "identifyingCode=" + identifyingCode;
		}
		else if (SubActivity3.currentRecipientUser != null && !TextUtils.isEmpty(SubActivity3.currentRecipientUser.recipientUserTelephone)) {
			urlStr = urlStr + "recipientUserTelephone=" + SubActivity3.currentRecipientUser.recipientUserTelephone;
		}
		else {
			Toast.makeText(UserGetData.this, "获取失败，请确认数据是否正确", Toast.LENGTH_SHORT).show();
			this.finish();
			return;
		}
		
		try {
			BaseAPI.requestByGet(urlStr, new RequestListener() {
				@Override
				public void onSuccess(String result) {
					// 成功
					parseStr(result);
					initContentLayout();
				}

				@Override
				public void onFailure() {
					Toast.makeText(UserGetData.this, "操作失败，请检查网络或IP设置", Toast.LENGTH_SHORT).show();
					UserGetData.this.finish();
				}

			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		
//		parseStr(tempStr);
//		initContentLayout();
	}
	
	private void initContentLayout() {
		if (mDataList == null || mDataList.size() == 0) {
			Toast.makeText(UserGetData.this, "取货码错误", Toast.LENGTH_SHORT).show();
			UserGetData.this.finish();
			return;
		}
		
		if (identifyingCode != null && !"".equals(identifyingCode)) {
			edittext_pwd.setVisibility(View.GONE);
			user_pwd_forget.setVisibility(View.GONE);
		}
		
		for (RecipientData mRecipientData : mDataList) {
			View itemLayout = View.inflate(this, R.layout.content_layout_item, null);
			TextView name = (TextView) itemLayout.findViewById(R.id.name);
			TextView order_id = (TextView) itemLayout.findViewById(R.id.order_id);
			TextView time = (TextView) itemLayout.findViewById(R.id.time);
			TextView remark = (TextView) itemLayout.findViewById(R.id.remark);
			
			name.setText(mRecipientData.expressCompanyName);
			order_id.setText("运单号:" + mRecipientData.waybillCode);
			time.setText("签收时间:" + mRecipientData.date2);
			remark.setText("备注:" + mRecipientData.remark);
			
			content_layout.addView(itemLayout);
		}
		
		RecipientData mRecipientData  = mDataList.get(0);
		user_name.setText(mRecipientData.recipientUserName);
		
		if (SubActivity3.currentRecipientUser != null) {
			user_phone.setText(SubActivity3.currentRecipientUser.recipientUserTelephone);
		}
		
		user_tip.setText("共"+mDataList.size()+"个包裹");
	}
	
	private void parseStr(String response) {
		JSONArray mJSONArray = (JSONArray) JSONValue.parse(response);

		int count = mJSONArray.size();
		for (int i = 0; i < count; i++) {
			JSONObject twoObject = (JSONObject) mJSONArray.get(i);
			RecipientData mRecipientData = RecipientData.builder(twoObject);
			mDataList.add(mRecipientData);
		}
	}
	
//	private String tempStr = "[{\"waybillCode\":\"DXS111111120\",\"expressCompanyName\":\"中通\",\"recipientUserName\":\"张三\",\"date2\":\"2015-09-03 14:01:31\",\"remark\":\"一些备注\"},{\"waybillCode\":\"DXS111111117\",\"expressCompanyName\":\"顺丰\",\"recipientUserName\":\"张三\",\"date2\":\"2015-09-03 13:55:33\",\"remark\":\"\"}]";

	
	private void getPackage() {
		String urlStr = "http://" + BaseAPI.IP_HOST
				+ "/dxs/yjAction_getPackage.action?";
		
		if (identifyingCode != null && !identifyingCode.equals("")) {
			urlStr = urlStr + "&identifyingCode=" + identifyingCode;
		}
		else {
			if (SubActivity3.currentRecipientUser != null && !TextUtils.isEmpty(SubActivity3.currentRecipientUser.recipientUserTelephone)) {
				urlStr = urlStr + "recipientUserTelephone=" + SubActivity3.currentRecipientUser.recipientUserTelephone;
			}
			else {
				Toast.makeText(this, "请填写数据", Toast.LENGTH_SHORT).show();
				return;
			}
			
			String identifyingCode = edittext_pwd.getText().toString();
			if (!TextUtils.isEmpty(identifyingCode)) {
				urlStr = urlStr + "&identifyingCode=" + identifyingCode;
			}
			else {
				Toast.makeText(this, "请填写数据", Toast.LENGTH_SHORT).show();
				return;
			}
		}
		
		try {
			BaseAPI.requestByGet(urlStr, new RequestListener() {
				@Override
				public void onSuccess(String result) {
					if ("1".equals(result)) {
						// 成功
						Toast.makeText(UserGetData.this, "取货成功,请讲包裹交给用户!", Toast.LENGTH_LONG).show();
						UserGetData.this.finish();
					}
					else {
						Toast.makeText(UserGetData.this, "取货失败，请确认取货密码是否正确!", Toast.LENGTH_SHORT).show();
					}
				}

				@Override
				public void onFailure() {
					Toast.makeText(UserGetData.this, "操作失败，请检查网络或IP设置", Toast.LENGTH_SHORT).show();
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
			UserGetData.this.finish();
			break;
		case R.id.btn1:
			getPackage();
			break;
		case R.id.user_pwd_forget:
			Toast.makeText(this, "忘记密码，功能待开发", Toast.LENGTH_LONG).show();
			break;
		}
	}
}
