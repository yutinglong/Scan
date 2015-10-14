package com.zxing.scan;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.zxing.scan.module.Company;
import com.zxing.scan.net.BaseAPI;
import com.zxing.scan.net.BaseAPI.RequestListener;

// 签收包裹
public class SubActivity1 extends Activity implements OnClickListener{
	public static final int REQ_THIRD = 100;
	
	private List<Company> companyList;
	private Company currentCompany;// 快递公司
	
	private View backBtn;
	private View btn1;
	
	private EditText mOrderEditText;
	private TextView edittext_company;
	private EditText mEditText2;
	private EditText mEditText3;
	private EditText mEditText4;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_sub1);
		
		initView();
		getExpressList();
		
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
		edittext_company = (TextView) findViewById(R.id.edittext_company);
		mEditText2 = (EditText) findViewById(R.id.edittext2);
		mEditText3 = (EditText) findViewById(R.id.edittext3);
		mEditText4 = (EditText) findViewById(R.id.edittext4);
		
		edittext_company.setOnClickListener(this);
	}
	
	private void commitData() {
		String urlStr = "http://" + BaseAPI.IP_HOST + "/dxs/yjAction_add.action?";
		String mTemp = mOrderEditText.getText().toString();
		if (!TextUtils.isEmpty(mTemp)) {
			urlStr = urlStr + "waybillCode=" + mTemp;
		}
		else {
			Toast.makeText(this, "请填写全部数据", Toast.LENGTH_SHORT).show();
			return;
		}
		
		mTemp = edittext_company.getText().toString();
		if (!TextUtils.isEmpty(mTemp)) {
			urlStr = urlStr + "waybillCode=" + mTemp;
		}
		else {
			Toast.makeText(this, "请填写全部数据", Toast.LENGTH_SHORT).show();
			return;
		}
		
		mTemp = mEditText2.getText().toString();
		if (!TextUtils.isEmpty(mTemp)) {
			urlStr = urlStr + "waybillCode=" + mTemp;
		}
		else {
			Toast.makeText(this, "请填写全部数据", Toast.LENGTH_SHORT).show();
			return;
		}
		
		mTemp = mEditText3.getText().toString();
		if (!TextUtils.isEmpty(mTemp)) {
			urlStr = urlStr + "waybillCode=" + mTemp;
		}
		else {
			Toast.makeText(this, "请填写全部数据", Toast.LENGTH_SHORT).show();
			return;
		}
		
		mTemp = mEditText4.getText().toString();
		if (!TextUtils.isEmpty(mTemp)) {
			urlStr = urlStr + "waybillCode=" + mTemp;
		}
		else {
			Toast.makeText(this, "请填写全部数据", Toast.LENGTH_SHORT).show();
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
					Toast.makeText(SubActivity1.this, "操作失败，请检查网络或IP设置", Toast.LENGTH_SHORT).show();
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
			commitData();
			break;
		case R.id.edittext_company:// 快递公司
			showCompanySelectDialog();
			break;
		}
	}

	private void getExpressList() {
		companyList = new ArrayList<Company>();
		String[] items = getResources().getStringArray(R.array.strs2);
		for (int i=0; i<items.length; i++) {
			Company mCompany = new Company();
			mCompany.name = items[i];
			companyList.add(mCompany);
		}
		
		Company mCompany = companyList.get(0);
		currentCompany = mCompany;
		edittext_company.setText(mCompany.name);
	}
	
	private void showCompanySelectDialog() {
		if (companyList != null && companyList.size() > 0) {
			int length = companyList.size();
			String[] array = new String[length];
			for (int i = 0; i < length; i++) {
				Company info = companyList.get(i);
				array[i] = info.name;
			}
			AlertDialog dialog = new AlertDialog.Builder(
					SubActivity1.this).setItems(array,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (companyList.size() > which) {
								String mExpressCompany = companyList.get(which).name;
								if (!TextUtils.isEmpty(mExpressCompany)) {
									mExpressCompany = mExpressCompany.trim();
								}
								currentCompany = companyList.get(which);
								edittext_company.setText(currentCompany.name);
							}
						}
					}).create();
			dialog.show();
		}
	}
}
