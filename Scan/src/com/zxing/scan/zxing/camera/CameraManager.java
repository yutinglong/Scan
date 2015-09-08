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

package com.zxing.scan.zxing.camera;

import java.io.IOException;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.os.Build;
import android.os.Handler;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;

import com.zxing.scan.CameraHolder;
import com.zxing.scan.CameraRotationUtils;
import com.zxing.scan.LogUtils;
import com.zxing.scan.Utils;
import com.zxing.scan.module.CameraManager.CameraProxy;
import com.zxing.scan.zxing.CaptureActivity.ModelEnum;

/**
 * This object wraps the Camera service object and expects to be the only one
 * talking to it. The implementation encapsulates the steps needed to take
 * preview-sized images, which are used for both preview and decoding.
 * 
 * change by yutinglong
 */
public final class CameraManager {
	private static final String TAG = CameraManager.class.getSimpleName();
	
    // Camera states.
    private static final int CAMERA_STATE_PREVIEW_STOPPED = 0;
    private static final int CAMERA_STATE_IDLE = 1;  	// preview is active
    private static final int CAMERA_STATE_FOCUSING = 2;	// Focus is in progress.
    private static final int CAMERA_STATE_CAPTURING = 3;
    private static final int CAMERA_STATE_SWITCHING = 4;// Switching between cameras.
    private static final int CAMERA_STATE_ZOOMING = 5;
    private int mCameraState = CAMERA_STATE_PREVIEW_STOPPED;
    
	private int mDisplayRotation;
	private int mDisplayOrientation;
	private int mCameraDisplayOrientation;
	private int mScreenDirection = 0;
	
	private int mOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
	private int mJpegRotation;

	private static final int MIN_FRAME_WIDTH = 240;
//	private static final int MIN_FRAME_HEIGHT = 240;
	private static final int MAX_FRAME_WIDTH = 600;
//	private static final int MAX_FRAME_HEIGHT = 480;

	private static CameraManager cameraManager;
	private ModelEnum state;
	private boolean isZoom = false;
	
	private final Activity context;
	private final CameraConfigurationManager configManager;
	private CameraProxy mCameraDevice;
	private Rect framingRect;
	private Rect framingRectInPreview;
	private boolean initialized;
	private boolean previewing;
	private final boolean useOneShotPreviewCallback;
	/**
	 * Preview frames are delivered here, which we pass on to the registered
	 * handler. Make sure to clear the handler so it will only receive one
	 * message.
	 */
	private final PreviewCallback previewCallback;
	/**
	 * Autofocus callbacks arrive here, and are dispatched to the Handler which
	 * requested them.
	 */
	private final AutoFocusCallback autoFocusCallback;
	
	private Parameters mInitialParams;

	static final int SDK_INT; // Later we can use Build.VERSION.SDK_INT
	static {
		int sdkInt;
		try {
			sdkInt = Integer.parseInt(Build.VERSION.SDK);
		} catch (NumberFormatException nfe) {
			sdkInt = 10000;
		}
		SDK_INT = sdkInt;
	}
	
	/**
	 * Initializes this static object with the Context of the calling Activity.
	 * 
	 * @param context
	 *            The Activity which wants to use the camera.
	 */
	public static void init(Activity context) {
		if (cameraManager == null) {
			cameraManager = new CameraManager(context);
		}
	}

	/**
	 * Gets the CameraManager singleton instance.
	 * 
	 * @return A reference to the CameraManager singleton.
	 */
	public static CameraManager get() {
		return cameraManager;
	}

	private CameraManager(Activity context) {
		this.context = context;
		this.configManager = new CameraConfigurationManager(context);
		useOneShotPreviewCallback = Integer.parseInt(Build.VERSION.SDK) > 3; 

		previewCallback = new PreviewCallback(configManager, useOneShotPreviewCallback);
		autoFocusCallback = new AutoFocusCallback();
	}

	public Point getCameraResolution(){
		Point cameraResolution = null;
		if(configManager != null){
			cameraResolution = configManager.getCameraResolution();	
		}
		return cameraResolution;
	}
	
	/**
	 * Opens the camera driver and initializes the hardware parameters.
	 * 
	 * @param holder
	 *            The surface object which the camera will draw preview frames
	 *            into.
	 * @throws IOException
	 *             Indicates the camera driver failed to open.
	 */
	public void openDriver(Object surfaceTextureOrHolder) throws IOException {
		if (mCameraDevice == null) {
			try {
//				mCameraDevice = Utils.openCamera((Activity)context, com.baidu.supercamera.module.CameraHolder.instance().getBackCameraId());
				mCameraDevice = CameraHolder.instance().open(CameraHolder.instance().getBackCameraId());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (AssertionError e){
				e.printStackTrace();
			}
			if (mCameraDevice == null) {
				throw new IOException();
			}
			
//			if (CameraAttrs.supportSurfaceTexture()) {
//				mCameraDevice.setPreviewDisplayAsync((SurfaceTexture)surfaceTextureOrHolder);
//			} else {
				mCameraDevice.setPreviewDisplayAsync((SurfaceHolder)surfaceTextureOrHolder);
//			}
			//camera.setPreviewDisplay(holder);
			LogUtils.d("baidu", "setPreviewDisplay");
			if (!initialized) {
				initialized = true;
				configManager.initFromCameraParameters(mCameraDevice.getCamera());
			}
			configManager.setDesiredCameraParameters(mCameraDevice.getCamera(), isZoom);
			
			setState(state);

			mInitialParams = mCameraDevice.getParameters();
		}
	}

	public Parameters getInitParameters(){
		return mInitialParams;
	}
	
	public Size getPreviewSize(){
		return configManager.getPreviewSize();
	}
	
	/**
	 * Closes the camera driver if still in use.
	 */
	public void closeDriver() {
		if (mCameraDevice != null) {
//			FlashlightManager.disableFlashlight();
//			camera.stopPreview();
			mCameraDevice.setErrorCallback(null);
			mCameraDevice.setPreviewCallback(null);
            CameraHolder.instance().release();
			mCameraDevice = null;
		}
	}

	/**
	 * Asks the camera hardware to begin drawing preview frames to the screen.
	 */
	public void startPreview() {
		if (mCameraDevice != null && !previewing) {
			LogUtils.d("YTL", "start preview");
			
			// 方向矫正
			setDisplayOrientation();
			mCameraDevice.setDisplayOrientation(mCameraDisplayOrientation);
			
			mCameraDevice.startPreviewAsync();
			setCameraState(CAMERA_STATE_IDLE);
			previewing = true;
		}
	}
	
	/**
	 * 判断闪光灯是否已经开启
	 * 注意:次方法只扫一扫的取景页使用，内部只判断了闪光灯的手电筒是否开启 
	 * @return
	 */
	public boolean isOpenFlash(){
		boolean result = false;
		Parameters mParameters = mCameraDevice.getParameters();
		String flashMode = mParameters.getFlashMode();
		if(Parameters.FLASH_MODE_TORCH.equals(flashMode)){
			result = true;
		}
		return result;
	}
	
	public void changeFlashZxing(){
		Parameters mParameters = mCameraDevice.getParameters();
		
		if(isOpenFlash()){
			mParameters.setFlashMode(Parameters.FLASH_MODE_OFF);
		}
		else{
			mParameters.setFlashMode(Parameters.FLASH_MODE_TORCH);			
		}
		mCameraDevice.setParameters(mParameters);
	}
	
	/**
	 * Tells the camera to stop drawing preview frames.
	 */
	public void stopPreview() {
		if (mCameraDevice != null && previewing) {
			if (!useOneShotPreviewCallback) {
				mCameraDevice.setPreviewCallback(null);
			}
			mCameraDevice.stopPreview();
			setCameraState(CAMERA_STATE_PREVIEW_STOPPED);
			previewCallback.setHandler(null, 0);
			autoFocusCallback.setHandler(null, 0);
			previewing = false;
		}
	}
	
	public void stopAutoFocus(){
		if (mCameraDevice != null && previewing) {
			autoFocusCallback.setHandler(null, 0);
		}
	}
	
	public void startAutoFocus(Handler handler, int message){
		if (mCameraDevice != null && previewing) {
			autoFocusCallback.setHandler(handler, message);
		}
	}

	/**
	 * A single preview frame will be returned to the handler supplied. The data
	 * will arrive as byte[] in the message.obj field, with width and height
	 * encoded as message.arg1 and message.arg2, respectively.
	 * 
	 * @param handler
	 *            The handler to send the message to.
	 * @param message
	 *            The what field of the message to be sent.
	 */
	public void requestPreviewFrame(Handler handler, int message) {
		if (mCameraDevice != null && previewing) {
			previewCallback.setHandler(handler, message);
			try{
				if (useOneShotPreviewCallback) {
					mCameraDevice.getCamera().setOneShotPreviewCallback(previewCallback);
				} else {
					mCameraDevice.setPreviewCallback(previewCallback);
				}
			}catch(NullPointerException e){
				e.printStackTrace();
			}
			
		}
	}
	
	public void takePicture(PictureCallback mPictureCallback){
		// 方向矫正
        LogUtils.d("YTL", "SetParameters:setRotation is " + mJpegRotation);
        Parameters mParameters = mCameraDevice.getParameters();
        mParameters.setRotation(mJpegRotation);
        mCameraDevice.setParameters(mParameters);

        setCameraState(CAMERA_STATE_CAPTURING);
        // 拍照
		mCameraDevice.takePicture2(null, null, null, mPictureCallback,
                mCameraState, 0);
		previewing = false;
	}

	/**
	 * Asks the camera hardware to perform an autofocus.
	 * 
	 * @param handler
	 *            The Handler to notify when the autofocus completes.
	 * @param message
	 *            The message to deliver.
	 */
	public void requestAutoFocus(Handler handler, int message) {
		if (mCameraDevice != null && previewing) {
			autoFocusCallback.setHandler(handler, message);
			// Log.d(TAG, "Requesting auto-focus callback");
			mCameraDevice.autoFocus(autoFocusCallback);
		}
	}
	
	public void cancelAutoFocus(){
		if (mCameraDevice != null && previewing) {
			mCameraDevice.cancelAutoFocus();
		}
	}

	/**
	 * Calculates the framing rect which the UI should draw to show the user
	 * where to place the barcode. This target helps with alignment as well as
	 * forces the user to hold the device far enough away to ensure the image
	 * will be in focus.
	 * 
	 * @return The rectangle to draw on screen in window coordinates.
	 */
	public Rect getFramingRect() {
		Point screenResolution = configManager.getScreenResolution();
		if (framingRect == null) {
			if (mCameraDevice == null) {
				return null;
			}
			int width = screenResolution.x * 3 / 4;
			if (width < MIN_FRAME_WIDTH) {
				width = MIN_FRAME_WIDTH;
			} else if (width > MAX_FRAME_WIDTH) {
				width = MAX_FRAME_WIDTH;
			}
			int height = screenResolution.y * 3 / 4;
			
			if(state == ModelEnum.BARCOD){
				// 条形码为方形的
				height = width * 3 / 4;	
			}
			else if(state == ModelEnum.TWO_BARCODE){
				// 二维码为方形的
				height = width;	
			}
			else if(state == ModelEnum.WORD){
				// 文字识别
				width = screenResolution.x * 1 / 2;
				if (width < MIN_FRAME_WIDTH) {
					width = MIN_FRAME_WIDTH;
				} else if (width > MAX_FRAME_WIDTH) {
					width = MAX_FRAME_WIDTH;
				}
				height = width/4 + 10;	
			}
			else if(state == ModelEnum.PET_DOG){// 宠物狗
				height = width * 4 / 3;
			}
			else {
				// 其他模式
				height = width;	
			}
			LogUtils.d("YTL", "CameraManager  width : height = " + width + ":" + height);
			int leftOffset = (screenResolution.x - width) / 2;
			int topOffset = (screenResolution.y - height) / 2;
			if(state == ModelEnum.WORD){
				topOffset = topOffset - screenResolution.y/10;
			}
			framingRect = new Rect(leftOffset, topOffset, leftOffset + width,
					topOffset + height);
			LogUtils.d(TAG, "Calculated framing rect: " + framingRect);
		}
		return framingRect;
	}

	/**
	 * Like {@link #getFramingRect} but coordinates are in terms of the preview
	 * frame, not UI / screen.
	 */
	public Rect getFramingRectInPreview() {
		if (framingRectInPreview == null) {
			Rect rect = new Rect(getFramingRect());
			Point cameraResolution = configManager.getCameraResolution();
			Point screenResolution = configManager.getScreenResolution();
			
			rect.left = rect.left * cameraResolution.y / screenResolution.x;
			rect.right = rect.right * cameraResolution.y / screenResolution.x;
			rect.top = rect.top * cameraResolution.x / screenResolution.y;
			rect.bottom = rect.bottom * cameraResolution.x / screenResolution.y;

			framingRectInPreview = rect;
		}
		return framingRectInPreview;
	}

	/**
	 * Converts the result points from still resolution coordinates to screen
	 * coordinates.
	 * 
	 * @param points
	 *            The points returned by the Reader subclass through
	 *            Result.getResultPoints().
	 * @return An array of Points scaled to the size of the framing rect and
	 *         offset appropriately so they can be drawn in screen coordinates.
	 */
	/*
	 * public Point[] convertResultPoints(ResultPoint[] points) { Rect frame =
	 * getFramingRectInPreview(); int count = points.length; Point[] output =
	 * new Point[count]; for (int x = 0; x < count; x++) { output[x] = new
	 * Point(); output[x].x = frame.left + (int) (points[x].getX() + 0.5f);
	 * output[x].y = frame.top + (int) (points[x].getY() + 0.5f); } return
	 * output; }
	 */

	/**
	 * A factory method to build the appropriate LuminanceSource object based on
	 * the format of the preview buffers, as described by Camera.Parameters.
	 * 
	 * @param data
	 *            A preview frame.
	 * @param width
	 *            The width of the image.
	 * @param height
	 *            The height of the image.
	 * @return A PlanarYUVLuminanceSource instance.
	 */
	public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data,
			int width, int height) {
		Rect rect = getFramingRectInPreview();
		int previewFormat = configManager.getPreviewFormat();
		String previewFormatString = configManager.getPreviewFormatString();
		
		switch (previewFormat) {
		case PixelFormat.YCbCr_420_SP:
		case PixelFormat.YCbCr_422_SP:
			return new PlanarYUVLuminanceSource(data, width, height, rect.left,
					rect.top, rect.width(), rect.height());
		default:
			if ("yuv420p".equals(previewFormatString)) {
				return new PlanarYUVLuminanceSource(data, width, height,
						rect.left, rect.top, rect.width(), rect.height());
			}
		}
		throw new IllegalArgumentException("Unsupported picture format: "
				+ previewFormat + '/' + previewFormatString);
	}
	
//	public Bitmap buildBitmapSource(byte[] data, int width, int height) {
//		int previewFormat = configManager.getPreviewFormat();
//		Rect rect = new Rect(0, 0, width, height);
//		YuvImage yuvImg = new YuvImage(data, previewFormat, width, height, null);
//		ByteArrayOutputStream outputstream = new ByteArrayOutputStream();
//		yuvImg.compressToJpeg(rect, 100, outputstream);
//		Bitmap rawbitmap = BitmapFactory.decodeByteArray(
//				outputstream.toByteArray(), 0, outputstream.size());
//		return rawbitmap;
//	}
	
	public void setState(ModelEnum state){
		this.state = state;
		
		if(configManager == null || mCameraDevice == null
				|| mCameraDevice.getCamera() == null){
			return;
		}
		if(state == ModelEnum.BOOK_CD
				|| state == ModelEnum.WORD){
			if(isZoom == true){
				configManager.changeZoom(mCameraDevice.getCamera(), false);	
				isZoom = false;
			}
		}
		else{
			if(isZoom == false){
				configManager.changeZoom(mCameraDevice.getCamera(), true);
				isZoom = true;
			}
		}
		
		framingRectInPreview = null;
		framingRect = null;
	}
	
	/**
	 * 摄像头的状态
	 * @param state
	 */
	private void setCameraState(int state) {
		mCameraState = state;
	}
	
	/**
	 * 获取设置中摄像头矫正的信息
	 */
    private void setDisplayOrientation() {
        mDisplayRotation = Utils.getDisplayRotation(context);
        mDisplayOrientation = Utils.getDisplayOrientation(mDisplayRotation, CameraHolder.instance().getBackCameraId());
        mCameraDisplayOrientation = CameraRotationUtils.getPreviewRotation(context, false);
        LogUtils.d("YTL", "mCameraDisplayOrientation : " + mCameraDisplayOrientation);
    }
    
    
	public void onOrientationChanged(int orientation) {
        int newOrientation = Utils.roundOrientation(orientation, mOrientation);
        if (mOrientation != newOrientation) {
        	mOrientation = newOrientation;
        	
        	mScreenDirection = ((mOrientation + 45) % 360) / 90;
        }
	}
}
