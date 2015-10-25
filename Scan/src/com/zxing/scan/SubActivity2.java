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
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
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
	private List<PlaceM> placeShunfengList;
	
	private List<PlaceM> placeOtherMList;
	
	private boolean isShunfeng = false;// 是否是顺风快递
	
	private Company currentCompany;// 快递公司
	private PackageM currentPackageM;// 包裹重量
	private PlaceM currentPlaceM;// 地点
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_sub2);
		
		initView();
		
		Intent mIntent = getIntent();
		String mOrderId = mIntent.getStringExtra("resultString");
		
		mEditTextOrderID.setText(mOrderId);
		
		getShunfengPlaceList();
		getOtherPlaceList();
		getExpressList();
		getPackageList();
		
		// 先算一遍价格
		getMoney();
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
		
		checkbox_money.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton paramCompoundButton,
					boolean paramBoolean) {
				if (checkbox_money.isChecked()) {
					edittext_money.setText("0");
					edittext_money.setEnabled(false);
				}
				else {
					int tempResult = getMoney();
					edittext_money.setText(tempResult+"");
					edittext_money.setEnabled(true);
				}
			}
		});
		
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
		String urlStr = "http://" + BaseAPI.IP_HOST + "/dxs/yjAction_daijiAdd.action?";
		String mTemp = mEditTextOrderID.getText().toString();
		if (!TextUtils.isEmpty(mTemp)) {
			urlStr = urlStr + "waybillCode=" + mTemp;
		}
		else {
			Toast.makeText(this, "填写数据不完整", Toast.LENGTH_SHORT).show();
			return;
		}
		
		mTemp = edittext_company.getText().toString();
		if (!TextUtils.isEmpty(mTemp)) {
			urlStr = urlStr + "&expressCompanyName=" + mTemp;
		}
		else {
			Toast.makeText(this, "填写数据不完整", Toast.LENGTH_SHORT).show();
			return;
		}
		
		mTemp = edittext_photo.getText().toString();
		if (!TextUtils.isEmpty(mTemp)) {
			urlStr = urlStr + "&senderUserTelephone=" + mTemp;
		}
		else {
			Toast.makeText(this, "填写数据不完整", Toast.LENGTH_SHORT).show();
			return;
		}
		
		mTemp = edittext_name.getText().toString();
		if (!TextUtils.isEmpty(mTemp)) {
			urlStr = urlStr + "&senderUserName=" + mTemp;
		}
		else {
			Toast.makeText(this, "填写数据不完整", Toast.LENGTH_SHORT).show();
			return;
		}
		
		mTemp = edittext_package.getText().toString();
		if (!TextUtils.isEmpty(mTemp)) {
			urlStr = urlStr + "&type1=" + mTemp;
		}
		else {
			Toast.makeText(this, "填写数据不完整", Toast.LENGTH_SHORT).show();
			return;
		}
		
		mTemp = edittext_place.getText().toString();
		if (!TextUtils.isEmpty(mTemp)) {
			urlStr = urlStr + "&senderArea=" + mTemp;
		}
		else {
			Toast.makeText(this, "填写数据不完整", Toast.LENGTH_SHORT).show();
			return;
		}
		
		mTemp = edittext_money.getText().toString();
		if (!TextUtils.isEmpty(mTemp)) {
			urlStr = urlStr + "&money=" + mTemp;
		}
		else {
			Toast.makeText(this, "填写数据不完整", Toast.LENGTH_SHORT).show();
			return;
		}
		
		if (checkbox_money.isChecked()) {
			urlStr = urlStr + "&type=0";
		}
		else {
			urlStr = urlStr + "&type=1";
		}
		urlStr = urlStr + "&state=1";
		
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
					Toast.makeText(SubActivity2.this, "新建失败，请检查网络或IP设置", Toast.LENGTH_SHORT).show();
				}
				
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private void showPlaceSelectDialog() {
		if (isShunfeng) {
			if (placeShunfengList != null && placeShunfengList.size() > 0) {
				int length = placeShunfengList.size();
				String[] array = new String[length];
				for (int i = 0; i < length; i++) {
					PlaceM info = placeShunfengList.get(i);
					array[i] = info.name;
				}
				AlertDialog dialog = new AlertDialog.Builder(
						SubActivity2.this).setItems(array,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								if (placeShunfengList.size() > which) {
									String mExpressCompany = placeShunfengList.get(which).name;
									if (!TextUtils.isEmpty(mExpressCompany)) {
										mExpressCompany = mExpressCompany.trim();
									}
									edittext_place.setText(mExpressCompany);
									
									currentPlaceM = placeShunfengList.get(which);
									
									getMoney();
								}
							}
						}).create();
				dialog.show();
			}
		}
		else {
			if (placeOtherMList != null && placeOtherMList.size() > 0) {
				int length = placeOtherMList.size();
				String[] array = new String[length];
				for (int i = 0; i < length; i++) {
					PlaceM info = placeOtherMList.get(i);
					array[i] = info.name;
				}
				AlertDialog dialog = new AlertDialog.Builder(
						SubActivity2.this).setItems(array,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								if (placeOtherMList.size() > which) {
									String mExpressCompany = placeOtherMList.get(which).name;
									if (!TextUtils.isEmpty(mExpressCompany)) {
										mExpressCompany = mExpressCompany.trim();
									}
									edittext_place.setText(mExpressCompany);
									currentPlaceM = placeOtherMList.get(which);
									getMoney();
								}
							}
						}).create();
				dialog.show();
			}
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
								
								currentPackageM = packageList.get(which);
								
								getMoney();
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
								currentCompany = companyList.get(which);
								setExpressText(mExpressCompany);
								
								getMoney();
							}
						}
					}).create();
			dialog.show();
		}
	}
	
	private void getShunfengPlaceList() {
		placeShunfengList = new ArrayList<PlaceM>();
		String[] items = getResources().getStringArray(R.array.strs3);
		for (int i=0; i<items.length; i++) {
			PlaceM mPlaceM = new PlaceM();
			mPlaceM.name = items[i];
			placeShunfengList.add(mPlaceM);
		}
		
//		PlaceM mPlaceM = placeShunfengList.get(0);
//		edittext_place.setText(mPlaceM.name);
	}
	
	private void getOtherPlaceList() {
		placeOtherMList = new ArrayList<PlaceM>();
		String[] items = getResources().getStringArray(R.array.strs4);
		for (int i=0; i<items.length; i++) {
			PlaceM mPlaceM = new PlaceM();
			mPlaceM.name = items[i];
			placeOtherMList.add(mPlaceM);
		}
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
		
		currentPackageM = mPackageM;
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
		setExpressText(mCompany.name);
	}
	
	private void setExpressText(String expressName) {
		edittext_company.setText(expressName);
		if (expressName != null && expressName.equals(companyList.get(0).name)) {
			// 顺风快递
			isShunfeng = true;
		}
		else {
			isShunfeng = false;
		}
		
		refreshPlase();
	}
	
	private void refreshPlase() {
		if (isShunfeng) {
			PlaceM mPlaceM = placeShunfengList.get(0);
			currentPlaceM = mPlaceM;
			edittext_place.setText(mPlaceM.name);
		}
		else {
			PlaceM mPlaceM = placeOtherMList.get(0);
			currentPlaceM = mPlaceM;
			edittext_place.setText(mPlaceM.name);
		}
	}
	
	private int getMoney() {
		int result = 0;
//		private Company currentCompany;// 快递公司
//		private PackageM currentPackageM;// 包裹重量
//		private PlaceM currentPlaceM;// 地点
		if(isShunfeng){// 顺丰
			String[] shunFengItems = getResources().getStringArray(R.array.strs3);
			String[] packageItems = getResources().getStringArray(R.array.strs1);
			if (currentPlaceM.name.equals(shunFengItems[0])) {
				// 河北、天津、北京
				if (currentPackageM.name.equals(packageItems[0])) {
					// 便利封(1KG)
					result = 15;
				}
				else if (currentPackageM.name.equals(packageItems[1])) {
					// 便利袋(1KG)
					result = 15;
				}
				else if (currentPackageM.name.equals(packageItems[2])) {
					// 1号便利箱(2KG)
					result = 18;
				}
				else if (currentPackageM.name.equals(packageItems[3])) {
					// 2号便利箱(3KG)
					result = 21;
				}
				else if (currentPackageM.name.equals(packageItems[4])) {
					// 3号便利箱(5KG)
					result = 27;
				}
				else if (currentPackageM.name.equals(packageItems[5])) {
					// 标准件
					result = 0;
				}
				else if (currentPackageM.name.equals(packageItems[6])) {
					// 其他
					result = 0;
				}
			}
			else if (currentPlaceM.name.equals(shunFengItems[1])) {
				// 新疆、西藏
				if (currentPackageM.name.equals(packageItems[0])) {
					// 便利封(1KG)
					result = 26;
				}
				else if (currentPackageM.name.equals(packageItems[1])) {
					// 便利袋(1KG)
					result = 26;
				}
				else if (currentPackageM.name.equals(packageItems[2])) {
					// 1号便利箱(2KG)
					result = 48;
				}
				else if (currentPackageM.name.equals(packageItems[3])) {
					// 2号便利箱(3KG)
					result = 70;
				}
				else if (currentPackageM.name.equals(packageItems[4])) {
					// 3号便利箱(5KG)
					result = 112;
				}
				else if (currentPackageM.name.equals(packageItems[5])) {
					// 标准件
					result = 0;
				}
				else if (currentPackageM.name.equals(packageItems[6])) {
					// 其他
					result = 0;
				}
			}
			else if (currentPlaceM.name.equals(shunFengItems[2])) {
				// 其他
				if (currentPackageM.name.equals(packageItems[0])) {
					// 便利封(1KG)
					result = 23;
				}
				else if (currentPackageM.name.equals(packageItems[1])) {
					// 便利袋(1KG)
					result = 23;
				}
				else if (currentPackageM.name.equals(packageItems[2])) {
					// 1号便利箱(2KG)
					result = 37;
				}
				else if (currentPackageM.name.equals(packageItems[3])) {
					// 2号便利箱(3KG)
					result = 51;
				}
				else if (currentPackageM.name.equals(packageItems[4])) {
					// 3号便利箱(5KG)
					result = 79;
				}
				else if (currentPackageM.name.equals(packageItems[5])) {
					// 标准件
					result = 0;
				}
				else if (currentPackageM.name.equals(packageItems[6])) {
					// 其他
					result = 0;
				}
			}
		}
		else {
			String[] shunFengItems = getResources().getStringArray(R.array.strs4);
			String[] packageItems = getResources().getStringArray(R.array.strs1);
			if (currentPlaceM.name.equals(shunFengItems[0])) {
				// 北京
				if (currentPackageM.name.equals(packageItems[0])) {
					// 便利封(1KG)
					result = 8;
				}
				else if (currentPackageM.name.equals(packageItems[1])) {
					// 便利袋(1KG)
					result = 8;
				}
				else if (currentPackageM.name.equals(packageItems[2])) {
					// 1号便利箱(2KG)
					result = 13;
				}
				else if (currentPackageM.name.equals(packageItems[3])) {
					// 2号便利箱(3KG)
					result = 18;
				}
				else if (currentPackageM.name.equals(packageItems[4])) {
					// 3号便利箱(5KG)
					result = 28;
				}
				else if (currentPackageM.name.equals(packageItems[5])) {
					// 标准件
					result = 0;
				}
				else if (currentPackageM.name.equals(packageItems[6])) {
					// 其他
					result = 0;
				}
			}
			else if (currentPlaceM.name.equals(shunFengItems[1])) {
				// 江苏、上海等
				if (currentPackageM.name.equals(packageItems[0])) {
					// 便利封(1KG)
					result = 10;
				}
				else if (currentPackageM.name.equals(packageItems[1])) {
					// 便利袋(1KG)
					result = 10;
				}
				else if (currentPackageM.name.equals(packageItems[2])) {
					// 1号便利箱(2KG)
					result = 15;
				}
				else if (currentPackageM.name.equals(packageItems[3])) {
					// 2号便利箱(3KG)
					result = 20;
				}
				else if (currentPackageM.name.equals(packageItems[4])) {
					// 3号便利箱(5KG)
					result = 30;
				}
				else if (currentPackageM.name.equals(packageItems[5])) {
					// 标准件
					result = 0;
				}
				else if (currentPackageM.name.equals(packageItems[6])) {
					// 其他
					result = 0;
				}
			}
			else if (currentPlaceM.name.equals(shunFengItems[2])) {
				// 海南、青海等
				if (currentPackageM.name.equals(packageItems[0])) {
					// 便利封(1KG)
					result = 15;
				}
				else if (currentPackageM.name.equals(packageItems[1])) {
					// 便利袋(1KG)
					result = 15;
				}
				else if (currentPackageM.name.equals(packageItems[2])) {
					// 1号便利箱(2KG)
					result = 25;
				}
				else if (currentPackageM.name.equals(packageItems[3])) {
					// 2号便利箱(3KG)
					result = 35;
				}
				else if (currentPackageM.name.equals(packageItems[4])) {
					// 3号便利箱(5KG)
					result = 55;
				}
				else if (currentPackageM.name.equals(packageItems[5])) {
					// 标准件
					result = 0;
				}
				else if (currentPackageM.name.equals(packageItems[6])) {
					// 其他
					result = 0;
				}
			}
		}
		
		edittext_money.setText(result+"");
		return result;
	}
}
