/*
 * Copyright (C) 2008 ZXing authors
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

import java.util.ArrayList;
import java.util.Vector;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.zxing.scan.R;
import com.zxing.scan.zxing.CaptureActivity;
import com.zxing.scan.zxing.CaptureActivity.ModelEnum;
import com.zxing.scan.zxing.camera.CameraManager;
import com.zxing.scan.zxing.data.BookOrCDEntity;
import com.zxing.scan.zxing.view.ViewfinderResultPointCallback;

/**
 * This class handles all the messaging which comprises the state machine for
 * capture.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 * change by yutinglong
 */
public final class CaptureActivityHandler extends Handler {
	private final CaptureActivity activity;
	private DecodeThread decodeThread;
	private Vector<BarcodeFormat> decodeFormats;
	private String characterSet;
	private State state;
	
	private enum State {
		PREVIEW, SUCCESS, DONE
	}
	
	private int decode_type_id = R.id.decode;

	public CaptureActivityHandler(CaptureActivity activity,
			Vector<BarcodeFormat> decodeFormats, String characterSet) {
		this.activity = activity;
		this.decodeFormats = decodeFormats;
		this.characterSet = characterSet;
		decodeThread = new DecodeThread(activity, decodeFormats, characterSet,
				new ViewfinderResultPointCallback(activity.getViewfinderView()));
		decodeThread.start();
		state = State.SUCCESS;

		// Start ourselves capturing previews and decoding.
		CameraManager.get().startPreview();
		restartPreviewAndDecode();
	}

	@Override
	public void handleMessage(Message message) {
		switch (message.what) {
		case R.id.auto_focus:
			if (state == State.PREVIEW) {
				if(activity.currentModel != ModelEnum.PET_DOG){
					CameraManager.get().requestAutoFocus(this, R.id.auto_focus);
				}
			}
			break;
		case R.id.auto_focus_succeeded:
//			if(message.obj != null && activity != null){
//				boolean isScuess = (Boolean)message.obj;
//				activity.onAutoFocusDone(isScuess);
//			}
			break;
		case R.id.restart_preview:
			// 启动次数统计
			restartPreviewAndDecode();
			break;
		case R.id.decode_book_cd_succeeded:
			state = State.SUCCESS;
			activity.handleDecodeBookCD((ArrayList<BookOrCDEntity>) message.obj);
			break;
		case R.id.decode_succeeded:
			state = State.SUCCESS;
			Bundle bundle = message.getData();
			Bitmap barcode = bundle == null ? null : (Bitmap) bundle
					.getParcelable(DecodeThread.BARCODE_BITMAP);
			activity.handleDecode((Result) message.obj, barcode);
			break;
		case R.id.decode_failed:
			state = State.PREVIEW;
			CameraManager.get().requestPreviewFrame(decodeThread.getHandler(), decode_type_id);
			break;
		case R.id.return_scan_result:
			activity.setResult(Activity.RESULT_OK, (Intent) message.obj);
			activity.finish();
			break;
		case R.id.launch_product_query:
			String url = (String) message.obj;
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			activity.startActivity(intent);
			break;
		}
	}
	
	public void restartDecode(){
		decodeThread = new DecodeThread(activity, decodeFormats, characterSet,
				new ViewfinderResultPointCallback(activity.getViewfinderView()));
		decodeThread.start();
		state = State.SUCCESS;

		restartPreviewAndDecode();
		
		CameraManager.get().startAutoFocus(this, R.id.auto_focus);
	}
	
	public void pauseDecode(){
		state = State.DONE;
		Message quit = Message.obtain(decodeThread.getHandler(), R.id.quit);
		quit.sendToTarget();
		try {
			decodeThread.join();
		} catch (InterruptedException e) {
			// continue
		}
		CameraManager.get().stopAutoFocus();
		
		// Be absolutely sure we don't send any queued up messages
		removeMessages(R.id.decode_succeeded);
		removeMessages(R.id.decode_failed);
	}

	public void quitSynchronously() {
		state = State.DONE;
		CameraManager.get().stopPreview();
		Message quit = Message.obtain(decodeThread.getHandler(), R.id.quit);
		quit.sendToTarget();
		try {
			decodeThread.join();
		} catch (InterruptedException e) {
			// continue
		}

		// Be absolutely sure we don't send any queued up messages
		removeMessages(R.id.decode_succeeded);
		removeMessages(R.id.decode_failed);
	}
	
	public void changeModelType(ModelEnum mModelEnum){
		if(mModelEnum == ModelEnum.BARCOD
				|| mModelEnum == ModelEnum.TWO_BARCODE){
			decode_type_id = R.id.decode;
		}
		else if(mModelEnum == ModelEnum.BOOK_CD){
			decode_type_id = R.id.decode_book_cd;
		}
		else if(mModelEnum == ModelEnum.WORD){
			decode_type_id = R.id.decode_word;
		}
		else if(mModelEnum == ModelEnum.PET_DOG){
			decode_type_id = -1;
		}
		else{
			decode_type_id = R.id.decode;
		}
		
		CameraManager.get().requestPreviewFrame(decodeThread.getHandler(), decode_type_id);
	}
	
	private void restartPreviewAndDecode() {
		if (state == State.SUCCESS) {
			state = State.PREVIEW;
			CameraManager.get().requestPreviewFrame(decodeThread.getHandler(), decode_type_id);
			CameraManager.get().requestAutoFocus(this, R.id.auto_focus);
			activity.drawViewfinder();
		}
	}

}
