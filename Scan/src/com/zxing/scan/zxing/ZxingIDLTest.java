package com.zxing.scan.zxing;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.util.Base64;

/**
 * IDL测试类 ,直接访问IDL接口，拉取识别数据
 * 
 * @author yutinglong
 */
public class ZxingIDLTest {

	// base64 code
	public static byte[] bitmaptoString(Bitmap bitmap) {
		// 将Bitmap转换成字符串

		ByteArrayOutputStream bStream = new ByteArrayOutputStream();
		bitmap.compress(CompressFormat.JPEG, 100, bStream);

		byte[] bytes = bStream.toByteArray();
		byte[] picData = Base64.encode(bytes, Base64.NO_WRAP);
		return picData;
	}

	public static String reqIdentyWord(byte[] img_base64) throws Exception {
		URL url = new URL("http://10.81.64.11:8090/vis-api.fcgi");
		HttpURLConnection httpUrlConnection = (HttpURLConnection) url
				.openConnection();
		httpUrlConnection.setRequestProperty("Connection", "keep-alive");
		httpUrlConnection.setRequestMethod("POST");
		httpUrlConnection.setConnectTimeout(10 * 1000);
		httpUrlConnection.setDoOutput(true);
		httpUrlConnection.setDoInput(true);
		httpUrlConnection.connect();
		DataOutputStream out = new DataOutputStream(
				httpUrlConnection.getOutputStream());
		StringBuilder builder = new StringBuilder();

		builder.append("type=st_ocrapi&encoding =1&appid=10025&version=1.0.0&from=android&clientip=");
		builder.append(getHostIp());
		builder.append("&ocrdecttype=0&image=");

		out.write(builder.toString().getBytes());
		out.write(img_base64, 0, img_base64.length); // filecontentdata为文件内容
		out.flush();
		out.close();
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				httpUrlConnection.getInputStream()));
		String ServerResponseCode = String.valueOf(httpUrlConnection
				.getResponseCode());
		String ServerResponseString = reader.readLine();
		try {

			if (ServerResponseCode == "200") {
				String msg = ServerResponseString;
				return msg;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// ServerResponseString.
//		String content = getStringFromJson(ServerResponseString);
		return ServerResponseString;
	}
	

	// client ip
	private static String getHostIp() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> ipAddr = intf.getInetAddresses(); ipAddr
						.hasMoreElements();) {
					InetAddress inetAddress = ipAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress();
					}
				}
			}
		} catch (SocketException ex) {
			ex.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
