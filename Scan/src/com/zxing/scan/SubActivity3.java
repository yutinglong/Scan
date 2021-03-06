package com.zxing.scan;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.zxing.scan.module.RecipientUser;
import com.zxing.scan.net.BaseAPI;
import com.zxing.scan.net.BaseAPI.RequestListener;

// 代收货
public class SubActivity3 extends Activity implements OnClickListener {
	private View backBtn;
	private ListView user_listview;
	private View refresh_view;
	private List<RecipientUser> mDataList = new ArrayList<RecipientUser>();
	private MyAdapter mMyAdapter;
	public static RecipientUser currentRecipientUser;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_sub3);

		initView();
	}
	


	@Override
	protected void onResume() {
		super.onResume();
		getData();
	}

	private void initView() {
		backBtn = (View) findViewById(R.id.newmore_btn_back);
		backBtn.setOnClickListener(this);

		user_listview = (ListView) findViewById(R.id.user_listview);
		refresh_view = findViewById(R.id.refresh_view);
		refresh_view.setOnClickListener(this);
		
		user_listview.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> paramAdapterView,
					View paramView, int paramInt, long paramLong) {
				RecipientUser mRecipientUser = mDataList.get(paramInt);
				currentRecipientUser = mRecipientUser;
				
				Intent intentL = new Intent(SubActivity3.this, UserGetData.class);
				startActivity(intentL);
			}
		});
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.newmore_btn_back:
			SubActivity3.this.finish();
			break;
		case R.id.refresh_view:// 刷新
			getData();
			break;
		}
	}

	private void getData() {
		mDataList.clear();
		
		String urlStr = "http://" + BaseAPI.IP_HOST
				+ "/dxs/yjAction_getPersonList.action";

		try {
			BaseAPI.requestByGet(urlStr, new RequestListener() {
				@Override
				public void onSuccess(String result) {
					parseStr(result);
					Log.d("YTL", "======mDataList = " + mDataList.size());
					
					if (mDataList == null || mDataList.size() == 0) {
						Toast.makeText(SubActivity3.this, "数据为空", Toast.LENGTH_SHORT).show();
					}
					
					mMyAdapter = new MyAdapter(SubActivity3.this);
					user_listview.setAdapter(mMyAdapter);
				}

				@Override
				public void onFailure() {
					Toast.makeText(SubActivity3.this, "操作失败，请检查网络或IP设置", Toast.LENGTH_SHORT).show();
				}

			});
		} catch (Exception e) {
			e.printStackTrace();
		}

//		parseStr(tempStr);
//		Log.d("YTL", "======mDataList = " + mDataList.size());
//		mMyAdapter = new MyAdapter(this);
//		user_listview.setAdapter(mMyAdapter);
	}

	private String tempStr = "[{\"recipientUserName\":\"张aa\",\"recipientUserTelephone\":\"13717674044\",\"date\":\"2015-09-03 14:01:31\",\"num\":\"2\"}, {\"recipientUserName\":\"张五\",\"recipientUserTelephone\":\"12717674041\",\"date\":\"2015-09-03 14:01:31\",\"num\":\"1\"}]";

	private void parseStr(String response) {
		JSONArray mJSONArray = (JSONArray) JSONValue.parse(response);

		int count = mJSONArray.size();
		for (int i = 0; i < count; i++) {
			JSONObject twoObject = (JSONObject) mJSONArray.get(i);
			RecipientUser mRecipientUser = RecipientUser.builder(twoObject);
			mDataList.add(mRecipientUser);
		}
	}

	private class MyAdapter extends BaseAdapter {
		private LayoutInflater mInflater;

		public MyAdapter(Context mContext) {
			this.mInflater = LayoutInflater.from(mContext);
		}

		@Override
		public int getCount() {
			return mDataList.size();
		}

		@Override
		public Object getItem(int position) {
			return mDataList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}
		
		private String getStringDate(String date) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
			try {
				Date mDate =  sdf.parse(date);
				
				SimpleDateFormat formatter = new SimpleDateFormat("MM/dd hh:mm");
				String dateStr = formatter.format(new Date());//格式化数据
				
				return dateStr;
			} catch (ParseException e) {
				e.printStackTrace();
				return "";
			}
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.listview_item, null);
				holder = new ViewHolder();
				/* 得到各个控件的对象 */
				holder.title = (TextView) convertView.findViewById(R.id.name);
				holder.time = (TextView) convertView.findViewById(R.id.time);
				holder.num_str = (TextView) convertView
						.findViewById(R.id.num_str);
				convertView.setTag(holder);// 绑定ViewHolder对象
			} else {
				holder = (ViewHolder) convertView.getTag();// 取出ViewHolder对象
			}
			holder.title.setText(mDataList.get(position).recipientUserName
					+ "/" + mDataList.get(position).recipientUserTelephone);
			
			
			holder.time.setText(getStringDate(mDataList.get(position).date));
			holder.num_str.setText(mDataList.get(position).num + "个包裹");

			holder.title.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					RecipientUser mRecipientUser = mDataList.get(position);
					currentRecipientUser = mRecipientUser;
					
					Intent intentL = new Intent(SubActivity3.this, UserGetData.class);
					startActivity(intentL);
				}
			});
			
			return convertView;
		}

		public final class ViewHolder {
			public TextView title;
			public TextView time;
			public TextView num_str;
		}
	}
}
