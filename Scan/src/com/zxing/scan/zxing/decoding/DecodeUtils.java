/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zxing.scan.zxing.decoding;

import java.util.Hashtable;
import java.util.Vector;

import org.apache.http.HttpResponse;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.zxing.scan.LogUtils;
import com.zxing.scan.R;
import com.zxing.scan.zxing.camera.PlanarYUVLuminanceSource;

public final class DecodeUtils {
	private static MultiFormatReader multiFormatReader;

	private static Hashtable<DecodeHintType, Object> getHints() {
		Hashtable<DecodeHintType, Object> hints;

		hints = new Hashtable<DecodeHintType, Object>(3);
		Vector<BarcodeFormat> decodeFormats = new Vector<BarcodeFormat>();
		decodeFormats.addAll(DecodeFormatManager.ONE_D_FORMATS);
		decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
		decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
		hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);

		return hints;
	}

	/**
	 * 异步decode图片
	 * @param bitmap
	 * @param myHandler
	 */
	public static void decode(final Bitmap bitmap, final Handler myHandler) {
		multiFormatReader = new MultiFormatReader();
		multiFormatReader.setHints(getHints());
		new Thread(){
			public void run(){
				Result rawResult = null;
				// 首先，要取得该图片的像素数组内容
				int width = bitmap.getWidth();
				int height = bitmap.getHeight();
				// --------------------------------------------------
				// rgb模式
				int[] data = new int[width * height];
				bitmap.getPixels(data, 0, width, 0, 0, width, height);
				Result rgbResult = rgbModeDecode(data, width, height);
				if (rgbResult != null) {
					data = null;
					rawResult = rgbResult;
				}

				if (rawResult == null) {
					// yuv
					byte[] bitmapPixels = new byte[width * height];
					bitmap.getPixels(data, 0, width, 0, 0, width, height);
					// 将int数组转换为byte数组
					for (int i = 0; i < data.length; i++) {
						bitmapPixels[i] = (byte) data[i];
					}
					Result yuvResult = yuvModeDecode(bitmapPixels, width, height);
					bitmapPixels = null;

					if (yuvResult != null) {
						data = null;
						rawResult = yuvResult;
					}
				}

				if (rawResult != null) {
					Message message = Message.obtain(myHandler, R.id.decode_succeeded,
							rawResult);
					// Message message = new Message();
					// message.obj = rawResult;
					// message.what = R.id.decode_succeeded;
					Bundle bundle = new Bundle();
					bundle.putParcelable(DecodeThread.BARCODE_BITMAP, bitmap);
					message.setData(bundle);
					// myHandler.sendMessage(message);
					message.sendToTarget();
				} else {
					// Message message = new Message();
					// message.obj = rawResult;
					// message.what = R.id.decode_failed;
					// myHandler.sendMessage(message);
					Message message = Message.obtain(myHandler, R.id.decode_failed);
					message.sendToTarget();
				}
			}
		}.start();
	}

	public static Result rgbModeDecode(int[] data, int width, int height) {
		Result rawResult = null;
		RGBLuminanceSource source = new RGBLuminanceSource(width, height, data);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
		try {
			rawResult = multiFormatReader.decodeWithState(bitmap);
		} catch (ReaderException re) {
			// continue
			re.printStackTrace();
		} finally {
			multiFormatReader.reset();
		}

		return rawResult;
	}

	public static Result yuvModeDecode(byte[] data, int width, int height) {
		Result rawResult = null;
		PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data,
				width, height, 0, 0, width, height);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
		try {
			rawResult = multiFormatReader.decodeWithState(bitmap);
		} catch (ReaderException re) {
			// continue
			re.printStackTrace();
		} finally {
			multiFormatReader.reset();
		}

		return rawResult;
	}
	
	
	public static void bookOrCDInterface(Context mContext, Bitmap mBitmap, final Handler myHandler){
		final long t1 = System.currentTimeMillis();
		final long minTime = 4000;
	}
}
