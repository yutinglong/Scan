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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.zxing.scan.module.Company;
import com.zxing.scan.module.PackageM;
import com.zxing.scan.module.PlaceM;
import com.zxing.scan.net.BaseAPI;
import com.zxing.scan.net.BaseAPI.RequestListener;

// 用户寄件
public class SubActivity2 extends Activity implements OnClickListener{
	private View backBtn;
	private EditText mEditTextOrderID;
	private TextView edittext_company;
	private EditText edittext_photo;
	private EditText edittext_name;
	private TextView edittext_package;
	private TextView edittext_place;
	private EditText edittext_money;
	private CheckBox checkbox_money;
	
	private List<Company> companyList;
	private List<PackageM> packageList;
	private List<PlaceM> placeMList;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_sub2);
		
		initView();
		
		Intent mIntent = getIntent();
		String mOrderId = mIntent.getStringExtra("resultString");
		
		mEditTextOrderID.setText(mOrderId);
		
		getExpressList();
		getPackageList();
		getPlaceList();
	}
	
	private void initView() {
		backBtn = (View) findViewById(R.id.newmore_btn_back);
		backBtn.setOnClickListener(this);
		
		mEditTextOrderID = (EditText) findViewById(R.id.edittext_orderid);
		edittext_company = (TextView) findViewById(R.id.edittext_company);
		edittext_photo = (EditText) findViewById(R.id.edittext_photo);
		edittext_name = (EditText) findViewById(R.id.edittext_name);
		edittext_package = (TextView) findViewById(R.id.edittext_package);
		edittext_place = (TextView) findViewById(R.id.edittext_place);
		edittext_money = (EditText) findViewById(R.id.edittext_money);
		
		checkbox_money = (CheckBox) findViewById(R.id.checkbox_money);
		
		edittext_company.setOnClickListener(this);
		edittext_package.setOnClickListener(this);
		edittext_place.setOnClickListener(this);
		findViewById(R.id.btn1).setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.newmore_btn_back:
			SubActivity2.this.finish();
			break;
		case R.id.edittext_company:// 快递公司
			showCompanySelectDialog();
			break;
		case R.id.edittext_package:// 包裹
			showPackageSelectDialog();
			break;
		case R.id.edittext_place:// 地点
			showPlaceSelectDialog();
			break;
		case R.id.btn1:
			commitData();
			break;
		}
	}
	private void commitData() {
		String urlStr = "http://" + BaseAPI.IP_HOST + "/dxs/yjAction_add.action?";
		String mTemp = mEditTextOrderID.getText().toString();
		if (!TextUtils.isEmpty(mTemp)) {
			urlStr = urlStr + "waybillCode=" + mTemp;
		}
		else {
			Toast.makeText(this, "请填写数据", Toast.LENGTH_SHORT).show();
			return;
		}
		
		mTemp = edittext_company.getText().toString();
		if (!TextUtils.isEmpty(mTemp)) {
			urlStr = urlStr + "&expressCompanyName=" + mTemp;
		}
		else {
			Toast.makeText(this, "请填写数据", Toast.LENGTH_SHORT).show();
			return;
		}
		
		mTemp = edittext_photo.getText().toString();
		if (!TextUtils.isEmpty(mTemp)) {
			urlStr = urlStr + "&senderUserTelephone=" + mTemp;
		}
		else {
			Toast.makeText(this, "请填写数据", Toast.LENGTH_SHORT).show();
			return;
		}
		
		mTemp = edittext_name.getText().toString();
		if (!TextUtils.isEmpty(mTemp)) {
			urlStr = urlStr + "&senderUserName=" + mTemp;
		}
		else {
			Toast.makeText(this, "请填写数据", Toast.LENGTH_SHORT).show();
			return;
		}
		
		mTemp = edittext_package.getText().toString();
		if (!TextUtils.isEmpty(mTemp)) {
			urlStr = urlStr + "&type1=" + mTemp;
		}
		else {
			Toast.makeText(this, "请填写数据", Toast.LENGTH_SHORT).show();
			return;
		}
		
		mTemp = edittext_place.getText().toString();
		if (!TextUtils.isEmpty(mTemp)) {
			urlStr = urlStr + "&senderArea=" + mTemp;
		}
		else {
			Toast.makeText(this, "请填写数据", Toast.LENGTH_SHORT).show();
			return;
		}
		
		mTemp = edittext_money.getText().toString();
		if (!TextUtils.isEmpty(mTemp)) {
			urlStr = urlStr + "&money=" + mTemp;
		}
		else {
			Toast.makeText(this, "请填写数据", Toast.LENGTH_SHORT).show();
			return;
		}
		
		if (checkbox_money.isChecked()) {
			urlStr = urlStr + "&type=1";
		}
		else {
			urlStr = urlStr + "&type=0";
		}
		urlStr = urlStr + "&state=0";
		
		Log.d("YTL", "urlStr = " + urlStr);
		try {
			BaseAPI.requestByGet(urlStr, new RequestListener(){
				@Override
				public void onSuccess(String result) {
					if (!TextUtils.isEmpty(result) && "1".equals(result)) {
						// 成功
						Toast.makeText(SubActivity2.this, "新建成功", Toast.LENGTH_SHORT).show();
						SubActivity2.this.finish();
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
	private void showPlaceSelectDialog() {
		if (placeMList != null && placeMList.size() > 0) {
			int length = placeMList.size();
			String[] array = new String[length];
			for (int i = 0; i < length; i++) {
				PlaceM info = placeMList.get(i);
				array[i] = info.name;
			}
			AlertDialog dialog = new AlertDialog.Builder(
					SubActivity2.this).setItems(array,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (placeMList.size() > which) {
								String mExpressCompany = placeMList.get(which).name;
								if (!TextUtils.isEmpty(mExpressCompany)) {
									mExpressCompany = mExpressCompany.trim();
								}
								edittext_place.setText(mExpressCompany);
							}
						}
					}).create();
			dialog.show();
		}
	}
	private void showPackageSelectDialog() {
		if (packageList != null && packageList.size() > 0) {
			int length = packageList.size();
			String[] array = new String[length];
			for (int i = 0; i < length; i++) {
				PackageM info = packageList.get(i);
				array[i] = info.name;
			}
			AlertDialog dialog = new AlertDialog.Builder(
					SubActivity2.this).setItems(array,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (packageList.size() > which) {
								String mExpressCompany = packageList.get(which).name;
								if (!TextUtils.isEmpty(mExpressCompany)) {
									mExpressCompany = mExpressCompany.trim();
								}
								edittext_package.setText(mExpressCompany);
							}
						}
					}).create();
			dialog.show();
		}
		
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
					SubActivity2.this).setItems(array,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (companyList.size() > which) {
								String mExpressCompany = companyList.get(which).name;
								if (!TextUtils.isEmpty(mExpressCompany)) {
									mExpressCompany = mExpressCompany.trim();
								}
								edittext_company.setText(mExpressCompany);
							}
						}
					}).create();
			dialog.show();
		}
	}
	
	private void getPlaceList() {
		placeMList = new ArrayList<PlaceM>();
		String[] items = getResources().getStringArray(R.array.strs3);
		for (int i=0; i<items.length; i++) {
			PlaceM mPlaceM = new PlaceM();
			mPlaceM.name = items[i];
			placeMList.add(mPlaceM);
		}
		
		PlaceM mPlaceM = placeMList.get(0);
		edittext_place.setText(mPlaceM.name);
	}
	
	private void getPackageList() {
		packageList = new ArrayList<PackageM>();
		String[] items = getResources().getStringArray(R.array.strs1);
		for (int i=0; i<items.length; i++) {
			PackageM mPackageM = new PackageM();
			mPackageM.name = items[i];
			packageList.add(mPackageM);
		}
		
		PackageM mPackageM = packageList.get(0);
		edittext_package.setText(mPackageM.name);
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
		edittext_company.setText(mCompany.name);
	}
}
