package com.zxing.scan.module;

import java.io.IOException;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.AutoFocusMoveCallback;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.OnZoomChangeListener;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import com.zxing.scan.ApiHelper;
import com.zxing.scan.LogUtils;

public class CameraManager {
    private static final String TAG = "CameraManager";
    private static CameraManager sCameraManager = new CameraManager();

    // Thread progress signals
    private ConditionVariable mSig = new ConditionVariable();

    private Parameters mParameters;
    private IOException mReconnectException;

    private static final int RELEASE = 1;
    private static final int RECONNECT = 2;
    private static final int UNLOCK = 3;
    private static final int LOCK = 4;
    private static final int START_PREVIEW_ASYNC = 6;
    private static final int STOP_PREVIEW = 7;
    private static final int SET_PREVIEW_CALLBACK_WITH_BUFFER = 8;
    private static final int ADD_CALLBACK_BUFFER = 9;
    private static final int AUTO_FOCUS = 10;
    private static final int CANCEL_AUTO_FOCUS = 11;
    private static final int SET_AUTO_FOCUS_MOVE_CALLBACK = 12;
    private static final int SET_DISPLAY_ORIENTATION = 13;                                            
    private static final int SET_ZOOM_CHANGE_LISTENER = 14;
    private static final int SET_FACE_DETECTION_LISTENER = 15;
    private static final int START_FACE_DETECTION = 16;
    private static final int STOP_FACE_DETECTION = 17;
    private static final int SET_ERROR_CALLBACK = 18;
    private static final int SET_PARAMETERS = 19;
    private static final int GET_PARAMETERS = 20;
    private static final int SET_PARAMETERS_ASYNC = 21;
    private static final int WAIT_FOR_IDLE = 22;
    private static final int SET_PREVIEW_DISPLAY_ASYNC = 23;
    private static final int SET_PREVIEW_CALLBACK = 24;

    private Handler mCameraHandler;
    private CameraProxy mCameraProxy;
    private android.hardware.Camera mCamera;
    private volatile boolean mHasPreviewCallback = false;
    private volatile boolean mReleased = false;
    private volatile boolean mPreviewStarted = false;
    private volatile boolean mFaceDetectionStarted = false;
    private volatile boolean mIsCapturing = false;
    private volatile boolean mIsCaptureFailed = false;
    
    // Should call System.gc() to release memory when the accumulative times of take-picture
    // has over a certain threshold value.
    private static final int THRESHOLD_VALUE_TAKEPICTURE = 5;
    private int mAccumulativeTimes = THRESHOLD_VALUE_TAKEPICTURE;
    private int mPictureId = 0;

    public static CameraManager instance() {
        return sCameraManager;
    }

    private CameraManager() {
        HandlerThread ht = new HandlerThread("Camera Handler Thread");
        ht.start();
        mCameraHandler = new CameraHandler(ht.getLooper());
    }

    @SuppressLint("NewApi")
	private class CameraHandler extends Handler {
        CameraHandler(Looper looper) {
            super(looper);
        }

        @TargetApi(ApiHelper.VERSION_CODES.ICE_CREAM_SANDWICH)
        private void startFaceDetection() {
        	if (mPreviewStarted && !mFaceDetectionStarted) {
            	LogUtils.e(TAG, "mCamera.startFaceDetection");
        		mCamera.startFaceDetection();
        		mFaceDetectionStarted = true;
            	LogUtils.e(TAG, "mCamera.startFaceDetection done");
			}
        }

        @TargetApi(ApiHelper.VERSION_CODES.ICE_CREAM_SANDWICH)
        private void stopFaceDetection() {
        	if (!mPreviewStarted || !mFaceDetectionStarted) {
				return;
			}
        	LogUtils.e(TAG, "mCamera.stopFaceDetection");
    		mCamera.stopFaceDetection();
    		mFaceDetectionStarted = false;
        	LogUtils.e(TAG, "mCamera.stopFaceDetection done");
        }

        @TargetApi(ApiHelper.VERSION_CODES.ICE_CREAM_SANDWICH)
        private void setFaceDetectionListener(FaceDetectionListener listener) {
            mCamera.setFaceDetectionListener(listener);
        }

        /*
         * This method does not deal with the build version check.  Everyone should
         * check first before sending message to this handler.
         */
        @Override
        public void handleMessage(final Message msg) {
        	if (mReleased) {
        		mSig.open();
				return;
			}

            try {
                switch (msg.what) {
                    case RELEASE:
                		LogUtils.e("Parameters", "CameraManager:handleMessage-->RELEASE");
                		if (mHasPreviewCallback) {
                    		mHasPreviewCallback = false;
                			mCamera.setPreviewCallback(null);
						}
                		
                        mCamera.release();
                		LogUtils.e("Parameters", "CameraManager:handleMessage-->mCamera.release() done");
                        mCamera = null;
                        mCameraProxy = null;
                        mReleased = true;
                        mPreviewStarted = false;
                        mFaceDetectionStarted = false;
                        mIsCaptureFailed = false;
                        mIsCapturing = false;
            			mAccumulativeTimes = 0;
                        break;

                    case RECONNECT:
                        mReconnectException = null;
                        try {
                            mCamera.reconnect();
                        } catch (IOException ex) {
                            mReconnectException = ex;
                        }
                        break;

                    case UNLOCK:
                        mCamera.unlock();
                        break;

                    case LOCK:
                        mCamera.lock();
                        break;

                    case SET_PREVIEW_DISPLAY_ASYNC:
                        try {
                        	if (CameraAttrs.supportSurfaceTexture() && (msg.obj instanceof SurfaceTexture)) {
                        		mCamera.setPreviewTexture((SurfaceTexture) msg.obj);
                        	} else if (msg.obj instanceof SurfaceHolder) {
                        		mCamera.setPreviewDisplay((SurfaceHolder) msg.obj);
                        	}
                        } catch(IOException e) {
                            throw new RuntimeException(e);
                        }
                        return;  // no need to call mSig.open()
//                        break;

                    case START_PREVIEW_ASYNC:
                        LogUtils.d(TAG, "mCamera.startPreview()--mPreviewStarted is " + mPreviewStarted);
                    	if (!mPreviewStarted) {
                    		mCamera.startPreview();
						}
                    	mPreviewStarted = true;
                        return;  // no need to call mSig.open()
//                        break;

                    case STOP_PREVIEW:
                        LogUtils.d(TAG, "mCamera.stopPreview()--mPreviewStarted is " + mPreviewStarted);
//                    	if (mPreviewStarted) {
                    		mCamera.stopPreview();
                            mPreviewStarted = false;
//						}
                        break;

                    case SET_PREVIEW_CALLBACK_WITH_BUFFER:
                    	mHasPreviewCallback = (null != msg.obj);
                    	if (null == msg.obj) {
                    		mCamera.setPreviewCallbackWithBuffer(null);
						} else {
							mCamera.setPreviewCallbackWithBuffer(
		                            (PreviewCallback) msg.obj);
						}
                        
                        break;

                    case ADD_CALLBACK_BUFFER:
                        mCamera.addCallbackBuffer((byte[]) msg.obj);
                        break;

                    case AUTO_FOCUS:
                    	LogUtils.e(TAG, "mCamera.autoFocus");
                    	if (mPreviewStarted && !mIsCapturing) {
                            mCamera.autoFocus((AutoFocusCallback) msg.obj);
						}
                        break;

                    case CANCEL_AUTO_FOCUS:
                    	LogUtils.e(TAG, "mCamera.cancelAutoFocus");
//                    	if (!mAutoFocusDone) {
                    		mCamera.cancelAutoFocus();
//                        	mAutoFocusDone = true;
//						}
                        break;

                    case SET_AUTO_FOCUS_MOVE_CALLBACK:
                        setAutoFocusMoveCallback(mCamera, msg.obj);
                        break;

                    case SET_DISPLAY_ORIENTATION:
                        mCamera.setDisplayOrientation(msg.arg1);
                        break;

                    case SET_ZOOM_CHANGE_LISTENER:
                    	if (null == msg.obj) {
                    		mCamera.setZoomChangeListener(null);
						} else {
							mCamera.setZoomChangeListener((OnZoomChangeListener) msg.obj);
						}
                        break;

                    case SET_FACE_DETECTION_LISTENER:
                    	if (null == msg.obj) {
                    		setFaceDetectionListener(null);
						} else {
							setFaceDetectionListener((FaceDetectionListener) msg.obj);
						}
                        break;

                    case START_FACE_DETECTION:
                        startFaceDetection();
                        break;

                    case STOP_FACE_DETECTION:
                        stopFaceDetection();
                        break;

                    case SET_ERROR_CALLBACK:
                    	if (null == msg.obj) {
                    		mCamera.setErrorCallback(null);
						} else {
							mCamera.setErrorCallback((ErrorCallback) msg.obj);
						}
                        break;

                    case SET_PARAMETERS:
                        LogUtils.d(TAG, "mCamera.setParameters()");
                        if (msg.obj instanceof Parameters) {
                        	mCamera.setParameters((Parameters) msg.obj);
						}
                        break;

                    case GET_PARAMETERS:
                        mParameters = mCamera.getParameters();
                        break;

                    case SET_PARAMETERS_ASYNC:
                        if (msg.obj instanceof Parameters) {
                        	mCamera.setParameters((Parameters) msg.obj);
                        }
                        return;  // no need to call mSig.open()

                    case SET_PREVIEW_CALLBACK:
                    	mHasPreviewCallback = (null != msg.obj);
                        if (null == msg.obj) {
                    		mCamera.setPreviewCallback(null);
						} else {
							mCamera.setPreviewCallback((PreviewCallback) msg.obj);
						}
                        break;

                    case WAIT_FOR_IDLE:
                        // do nothing
                        break;

                    default:
                        throw new RuntimeException("Invalid CameraProxy message=" + msg.what);
                }
            } catch (RuntimeException e) {
                if (msg.what != RELEASE && mCamera != null) {
                    try {
                    	if (mHasPreviewCallback) {
                    		mHasPreviewCallback = false;
                			mCamera.setPreviewCallback(null);
						}
                		
                        mCamera.release();
                    } catch (Exception ex) {
                        Log.e(TAG, "Fail to release the camera.");
                    }
                    mCamera = null;
                    mCameraProxy = null;
                    mReleased = true;
                    mPreviewStarted = false;
                    mFaceDetectionStarted = false;
                    mIsCaptureFailed = false;
                    mIsCapturing = false;
        			mAccumulativeTimes = 0;
                }
//                throw e;
            }
            mSig.open();
        }
    }

    @TargetApi(ApiHelper.VERSION_CODES.JELLY_BEAN)
    private void setAutoFocusMoveCallback(android.hardware.Camera camera,
            Object cb) {
        camera.setAutoFocusMoveCallback((AutoFocusMoveCallback) cb);
    }

    // Open camera synchronously. This method is invoked in the context of a
    // background thread.
    public CameraProxy cameraOpen(int cameraId) {
        // Cannot open camera in mCameraHandler, otherwise all camera events
        // will be routed to mCameraHandler looper, which in turn will call
        // event handler like Camera.onFaceDetection, which in turn will modify
        // UI and cause exception like this:
        // CalledFromWrongThreadException: Only the original thread that created
        // a view hierarchy can touch its views.
        mCamera = android.hardware.Camera.open(cameraId);

		mHasPreviewCallback = false;
        if (mCamera != null) {
            mCameraProxy = new CameraProxy();
            mReleased = false;
            mPreviewStarted = false;
            mFaceDetectionStarted = false;
            mIsCaptureFailed = false;
            mIsCapturing = false;
			mAccumulativeTimes = 0;
            return mCameraProxy;
        } else {
            mReleased = true;
            mPreviewStarted = false;
            mFaceDetectionStarted = false;
            mIsCaptureFailed = false;
            mIsCapturing = false;
			mAccumulativeTimes = 0;
            return null;
        }
    }

    // Open camera synchronously. This method is invoked in the context of a
    // background thread.
    CameraProxy cameraOpen() {
        // Cannot open camera in mCameraHandler, otherwise all camera events
        // will be routed to mCameraHandler looper, which in turn will call
        // event handler like Camera.onFaceDetection, which in turn will modify
        // UI and cause exception like this:
        // CalledFromWrongThreadException: Only the original thread that created
        // a view hierarchy can touch its views.
        mCamera = android.hardware.Camera.open();
		mHasPreviewCallback = false;
        if (mCamera != null) {
            mCameraProxy = new CameraProxy();
            mReleased = false;
            mPreviewStarted = false;
            mFaceDetectionStarted = false;
            mIsCaptureFailed = false;
            mIsCapturing = false;
			mAccumulativeTimes = 0;
            return mCameraProxy;
        } else {
            mReleased = true;
            mPreviewStarted = false;
            mFaceDetectionStarted = false;
            mIsCaptureFailed = false;
            mIsCapturing = false;
			mAccumulativeTimes = 0;
            return null;
        }
    }

    public class CameraProxy {
        private CameraProxy() {
        }

        public android.hardware.Camera getCamera() {
            return mCamera;
        }

        public void release() {
            mSig.close();
//            mCameraHandler.removeCallbacksAndMessages(null);
            mCameraHandler.sendEmptyMessage(RELEASE);
            mSig.block();//1000
        }

        public void reconnect() throws IOException {
            mSig.close();
            mCameraHandler.sendEmptyMessage(RECONNECT);
            mSig.block();
            if (mReconnectException != null) {
                throw mReconnectException;
            }
        }

        public void unlock() {
            mSig.close();
            mCameraHandler.sendEmptyMessage(UNLOCK);
            mSig.block();
        }

        public void lock() {
            mSig.close();
            mCameraHandler.sendEmptyMessage(LOCK);
            mSig.block();
        }

        public void setPreviewDisplayAsync(final SurfaceHolder surfaceHolder) {
        	// No need to close
//            mSig.close();
            mCameraHandler.obtainMessage(SET_PREVIEW_DISPLAY_ASYNC, surfaceHolder).sendToTarget();
//            mSig.block();
        }

        public void setPreviewDisplayAsync(final SurfaceTexture surfaceTexture) {
        	// No need to close
//            mSig.close();
            mCameraHandler.obtainMessage(SET_PREVIEW_DISPLAY_ASYNC, surfaceTexture).sendToTarget();
//            mSig.block();
        }

        public void startPreviewAsync() {
        	// No need to close
//            mSig.close();
            mCameraHandler.sendEmptyMessage(START_PREVIEW_ASYNC);
//            mSig.block();
        }
        
        public void startPreviewAsyncWithDelay() {
            mCameraHandler.sendEmptyMessageDelayed(START_PREVIEW_ASYNC, 150);
        }

        public void stopPreview() {
            mSig.close();
            mCameraHandler.sendEmptyMessage(STOP_PREVIEW);
            mSig.block();
        }

        public void setPreviewCallback(final PreviewCallback cb) {
            mSig.close();
            mCameraHandler.obtainMessage(SET_PREVIEW_CALLBACK, cb).sendToTarget();
            mSig.block();
        }

        public void setPreviewCallbackWithBuffer(final PreviewCallback cb) {
            mSig.close();
            mCameraHandler.obtainMessage(SET_PREVIEW_CALLBACK_WITH_BUFFER, cb).sendToTarget();
            mSig.block();
        }

        public void addCallbackBuffer(byte[] callbackBuffer) {
            mSig.close();
            mCameraHandler.obtainMessage(ADD_CALLBACK_BUFFER, callbackBuffer).sendToTarget();
            mSig.block();
        }

        public void autoFocus(AutoFocusCallback cb) {
            mSig.close();
            mCameraHandler.obtainMessage(AUTO_FOCUS, cb).sendToTarget();
            mSig.block();
        }

        public void cancelAutoFocus() {
            mSig.close();
            mCameraHandler.sendEmptyMessage(CANCEL_AUTO_FOCUS);
            mSig.block();
        }

        @TargetApi(ApiHelper.VERSION_CODES.JELLY_BEAN)
        public void setAutoFocusMoveCallback(AutoFocusMoveCallback cb) {
            mSig.close();
            mCameraHandler.obtainMessage(SET_AUTO_FOCUS_MOVE_CALLBACK, cb).sendToTarget();
            mSig.block();
        }

        @Deprecated
        public void takePicture(final ShutterCallback shutter, final PictureCallback raw,
                final PictureCallback jpeg, final int cameraState, final int focusState) {
            mSig.close();
            // Too many parameters, so use post for simplicity
            mCameraHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                    	mIsCaptureFailed = false;
                    	if (mPreviewStarted && !mIsCapturing) {
                    		mIsCapturing = true;
                    		if (CameraAttrs.shouldGCBeforeTakePicture()
                    				|| mAccumulativeTimes >= THRESHOLD_VALUE_TAKEPICTURE) {
                    			mAccumulativeTimes = 0;
                    			LogUtils.w(TAG, "takePicture - picId[" + mPictureId + "] System.gc()");
                        		System.gc();
							}
                    		mCamera.takePicture(shutter, raw, jpeg);
                    		mFaceDetectionStarted = false;
                    		mIsCapturing = false;
                    		mAccumulativeTimes++;
                    		mPreviewStarted = false;
//                    		mPreviewStarted = !CameraAttrs.HAS_START_PREVIEW_AFTER_TAKE_PICTURE;
                    	}
                    } catch (RuntimeException e) {
                        Log.w(TAG, "take picture - picId[" + mPictureId + "] failed; cameraState:" + cameraState
                            + ", focusState:" + focusState);
                        mIsCaptureFailed = true;
                        mIsCapturing = false;
                		mPreviewStarted = false;
//                		mPreviewStarted = !CameraAttrs.HAS_START_PREVIEW_AFTER_TAKE_PICTURE;
//                        throw e;
                        if (null != jpeg) {
                        	jpeg.onPictureTaken(null, mCamera);
						}
                    }
                    mSig.open();
                }
            });
            mSig.block();
        }

        public void takePicture2(final ShutterCallback shutter, final PictureCallback raw,
                final PictureCallback postview, final PictureCallback jpeg,
                final int cameraState, final int focusState) {
            LogUtils.w(TAG, "takePicture2 close");
            mSig.close();
            // Too many parameters, so use post for simplicity
            mCameraHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                    	mIsCaptureFailed = false;
                    	if (mPreviewStarted && !mIsCapturing) {
                    		mIsCapturing = true;
                    		mPictureId++;
                    		if (CameraAttrs.shouldGCBeforeTakePicture()
                    				|| mAccumulativeTimes >= THRESHOLD_VALUE_TAKEPICTURE) {
                    			mAccumulativeTimes = 0;
                    			LogUtils.w(TAG, "takePicture2 System.gc() -- picId[" + mPictureId + "]");
                        		System.gc();
							}
                			LogUtils.w(TAG, "takePicture2 before -- picId[" + mPictureId + "]");
                			postTakePictureTimeoutChecker(jpeg);
                    		mCamera.takePicture(shutter, raw, postview, jpeg);
                    		if (mIsTakePictureTimeout) {
                    			LogUtils.w(TAG, "takePicture2 timeout -- picId[" + mPictureId + "]");
							} else {
								removeTakePictureTimeoutChecker();
                    			LogUtils.w(TAG, "takePicture2 after -- picId[" + mPictureId + "]");
	                    		mFaceDetectionStarted = false;
	                    		mIsCapturing = false;
	                    		mAccumulativeTimes++;
	                    		mPreviewStarted = false;//!CameraAttrs.HAS_START_PREVIEW_AFTER_TAKE_PICTURE;
							}
						}
                    } catch (RuntimeException e) {
                        LogUtils.w(TAG, "take picture failed -- picId[" + mPictureId + "]; cameraState:" + cameraState
                            + ", focusState:" + focusState);

                        if (mIsTakePictureTimeout) {
//                			LogUtils.w(TAG, "takePicture2 timeout -- picId[" + mPictureId + "]");
						} else {
							removeTakePictureTimeoutChecker();
							processWhenTakePictureTimeoutOrFailed(jpeg);
						}
                    }
        			LogUtils.w(TAG, "takePicture2 mSig.open() -- " + mPreviewStarted);
                    mSig.open();
                }
            });
            LogUtils.w(TAG, "takePicture2 block");
            mSig.block();
        }
        
        private void processWhenTakePictureTimeoutOrFailed(PictureCallback jpegCallback) {
            mIsCaptureFailed = true;
            mIsCapturing = false;
    		mPreviewStarted = false;//!CameraAttrs.HAS_START_PREVIEW_AFTER_TAKE_PICTURE;
            // We don't throw exception, but should reset camera states.
//            throw e;
            if (null != jpegCallback) {
            	jpegCallback.onPictureTaken(null, mCamera);
			}
        }
        
        private static final long TIME_OUT = 5000L;
        private volatile boolean mIsTakePictureTimeout = false;
        private final TakePictureTimeoutRunnable mTakePictureTimeoutRunnable = new TakePictureTimeoutRunnable();
        private class TakePictureTimeoutRunnable implements Runnable {
        	protected boolean isRunning;
        	protected PictureCallback jpegCallback;
			@Override
			public void run() {
				mIsTakePictureTimeout = true;
				isRunning = true;
	        	processWhenTakePictureTimeoutOrFailed(jpegCallback);
				isRunning = false;
	        	jpegCallback = null;
			}
        }
        private void postTakePictureTimeoutChecker(PictureCallback jpegCallback) {
        	removeTakePictureTimeoutChecker();
        	mCameraHandler.postDelayed(mTakePictureTimeoutRunnable, TIME_OUT);
        	mTakePictureTimeoutRunnable.jpegCallback = jpegCallback;
        }
        private void removeTakePictureTimeoutChecker() {
        	mIsTakePictureTimeout = false;
        	mCameraHandler.removeCallbacks(mTakePictureTimeoutRunnable);
        	mTakePictureTimeoutRunnable.jpegCallback = null;
        }
        
        public boolean isTakePictureTimeout() {
        	return mIsTakePictureTimeout && !mTakePictureTimeoutRunnable.isRunning;
        }
        
        public boolean isCaptureFailed() {
        	return mIsCaptureFailed;
        }
        
        public boolean isCapturing() {
        	return mIsCapturing;
        }
        
        public boolean isPreviewStarted() {
        	return mPreviewStarted;
        }
        
        public void setPreviewStarted() {
			LogUtils.w(TAG, "setPreviewStarted true");
        	mPreviewStarted = true;
        	mIsCaptureFailed = false;
        }

        public void setDisplayOrientation(int degrees) {
            mSig.close();
            mCameraHandler.obtainMessage(SET_DISPLAY_ORIENTATION, degrees, 0)
                    .sendToTarget();
            mSig.block();
        }

        public void setZoomChangeListener(OnZoomChangeListener listener) {
            mSig.close();
            mCameraHandler.obtainMessage(SET_ZOOM_CHANGE_LISTENER, listener).sendToTarget();
            mSig.block();
        }

        @TargetApi(ApiHelper.VERSION_CODES.ICE_CREAM_SANDWICH)
        public void setFaceDetectionListener(FaceDetectionListener listener) {
            mSig.close();
            mCameraHandler.obtainMessage(SET_FACE_DETECTION_LISTENER, listener).sendToTarget();
            mSig.block();
        }

        public void startFaceDetection() {
//        	Parameters params = getParameters();
//        	if (null != params && params.getMaxNumDetectedFaces() > 0) {
        		mSig.close();
        		mCameraHandler.sendEmptyMessage(START_FACE_DETECTION);
                mSig.block();
//        	}
        }
        
        @TargetApi(ApiHelper.VERSION_CODES.ICE_CREAM_SANDWICH)
        public void startFaceDetectionAsync() {
        	if (mPreviewStarted && !mFaceDetectionStarted) {
            	LogUtils.e(TAG, "mCamera.startFaceDetectionAsync");
        		mCamera.startFaceDetection();
        		mFaceDetectionStarted = true;
			}
        }

        public void stopFaceDetection() {
//        	Parameters params = getParameters();
//        	if (null != params && params.getMaxNumDetectedFaces() > 0) {
        		mSig.close();
        		mCameraHandler.sendEmptyMessage(STOP_FACE_DETECTION);
                mSig.block();
//        	}
        }

        public void setErrorCallback(ErrorCallback cb) {
            mSig.close();
            mCameraHandler.obtainMessage(SET_ERROR_CALLBACK, cb).sendToTarget();
            mSig.block();
        }

        public void setParameters(Parameters parameters) {
            mSig.close();
            mCameraHandler.obtainMessage(SET_PARAMETERS, parameters).sendToTarget();
            mSig.block();
        }

        public void setParametersAsync(Parameters params) {
            mCameraHandler.removeMessages(SET_PARAMETERS_ASYNC);
            mCameraHandler.obtainMessage(SET_PARAMETERS_ASYNC, params).sendToTarget();
        }
        
        public void shutOffFlash() {
        	if (!mReleased) {
        		Parameters parameters = mCamera.getParameters();
        		parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
        		mCamera.setParameters(parameters);
			}
        }

        public Parameters getParameters() {
            mSig.close();
            mCameraHandler.sendEmptyMessage(GET_PARAMETERS);
            mSig.block();
            Parameters parameters = mParameters;
            mParameters = null;
            return parameters;
        }

        public void waitForIdle() {
            mSig.close();
            mCameraHandler.sendEmptyMessage(WAIT_FOR_IDLE);
            mSig.block();
        }
    }
}
