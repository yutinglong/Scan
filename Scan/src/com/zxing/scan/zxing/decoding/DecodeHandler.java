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

import org.apache.http.HttpResponse;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.zxing.scan.LogUtils;
import com.zxing.scan.R;
import com.zxing.scan.zxing.CaptureActivity;
import com.zxing.scan.zxing.camera.CameraManager;
import com.zxing.scan.zxing.camera.PlanarYUVLuminanceSource;

final class DecodeHandler extends Handler {
	private final CaptureActivity activity;
	private final MultiFormatReader multiFormatReader;
	
	private static final int DECODE_DELAY = 1000; 

	DecodeHandler(CaptureActivity activity,
			Hashtable<DecodeHintType, Object> hints) {
		multiFormatReader = new MultiFormatReader();
		multiFormatReader.setHints(hints);
		this.activity = activity;
	}

	@Override
	public void handleMessage(Message message) {
		switch (message.what) {
		case R.id.decode_book_cd:
			LogUtils.d("YTL", "DecodeHandler 请求封面图书数据");
			decodeBookCd((byte[]) message.obj, message.arg1, message.arg2);
			break;
		case R.id.decode_word:
			LogUtils.d("YTL", "DecodeHandler 请求翻译识别数据");
			decodeWord((byte[]) message.obj, message.arg1, message.arg2);
			break;			
		case R.id.decode:
			LogUtils.d("YTL", "DecodeHandler 识别二维码、条形码数据");
			decode((byte[]) message.obj, message.arg1, message.arg2);
			break;
		case R.id.quit:
			Looper.myLooper().quit();
			break;
		default:
			LogUtils.d("YTL", "DecodeHandler default");
			break;
		}
	}

	/**
	 * Decode the data within the viewfinder rectangle, and time how long it
	 * took. For efficiency, reuse the same reader objects from one decode to
	 * the next.
	 * 
	 * @param data
	 *            The YUV preview frame.
	 * @param width
	 *            The width of the preview frame.
	 * @param height
	 *            The height of the preview frame.
	 */
	private void decode(byte[] data, int width, int height) {
		Result rawResult = null;

		byte[] rotatedData = new byte[data.length];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++)
				rotatedData[x * height + height - y - 1] = data[x + y * width];
		}
		int tmp = width; // Here we are swapping, that's the difference to #11
		width = height;
		height = tmp;

		data = rotatedData;

		PlanarYUVLuminanceSource source = CameraManager.get()
				.buildLuminanceSource(data, width, height);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
		try {
			rawResult = multiFormatReader.decodeWithState(bitmap);
		} catch (ReaderException re) {
			// continue
			re.printStackTrace();
		} finally {
			multiFormatReader.reset();
		}

		if (rawResult != null) {
			Message message = Message.obtain(activity.getHandler(),
					R.id.decode_succeeded, rawResult);
			Bundle bundle = new Bundle();
			bundle.putParcelable(DecodeThread.BARCODE_BITMAP,
					source.renderCroppedGreyscaleBitmap());
			message.setData(bundle);
			message.sendToTarget();
		} else {
			Message message = Message.obtain(activity.getHandler(),
					R.id.decode_failed);
			message.sendToTarget();
		}
	}
	
	private void decodeBookCd(final byte[] data, final int width, final int height) {
		if(activity.netStatus){// 网络可用
			try {
				long t1 = System.currentTimeMillis();
				byte[] rotatedData = new byte[data.length];
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++)
						rotatedData[x * height + height - y - 1] = data[x + y * width];
				}
				int tmp = width; // Here we are swapping, that's the difference to #11
				int newWidth = height;
				int newHeight = tmp;

				PlanarYUVLuminanceSource source = CameraManager.get()
						.buildLuminanceSource(rotatedData, newWidth, newHeight);
				Bitmap resultBitmap = source.renderCroppedGreyscaleBitmap();
				
				bookOrCDInterface(resultBitmap);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else{
			Message message = Message.obtain(activity.getHandler(), R.id.decode_failed);
			activity.getHandler().sendMessageDelayed(message, DECODE_DELAY);
		}
	}
	
	private void decodeWord(final byte[] data, final int width, final int height) {
		if(activity.netStatus){// 网络可用
			try {
				byte[] rotatedData = new byte[data.length];
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++)
						rotatedData[x * height + height - y - 1] = data[x + y * width];
				}
				int tmp = width; // Here we are swapping, that's the difference to #11
				int newWidth = height;
				int newHeight = tmp;

				PlanarYUVLuminanceSource source = CameraManager.get()
						.buildLuminanceSource(rotatedData, newWidth, newHeight);
				Bitmap resultBitmap = source.renderCroppedGreyscaleBitmap();
				
				transInterface(resultBitmap);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else{
			Message message = Message.obtain(activity.getHandler(), R.id.decode_failed);
			activity.getHandler().sendMessageDelayed(message, DECODE_DELAY);
		}
	}
	
	private void transInterface(Bitmap mBitmap){
	}
	
	private void bookOrCDInterface(Bitmap mBitmap){
	}
}
