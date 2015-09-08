package com.zxing.scan;

import java.io.IOException;
import java.text.SimpleDateFormat;

import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.zxing.scan.module.CameraManager;
import com.zxing.scan.module.CameraManager.CameraProxy;


/**
 * The class is used to hold an {@code android.hardware.Camera} instance.
 *
 * <p>The {@code open()} and {@code release()} calls are similar to the ones
 * in {@code android.hardware.Camera}. The difference is if {@code keep()} is
 * called before {@code release()}, CameraHolder will try to hold the {@code
 * android.hardware.Camera} instance for a while, so if {@code open()} is
 * called soon after, we can avoid the cost of {@code open()} in {@code
 * android.hardware.Camera}.
 *
 * <p>This is used in switching between different modules.
 */
public class CameraHolder {
    private static final String TAG = "CameraHolder";
    private static final int KEEP_CAMERA_TIMEOUT = 3000; // 3 seconds
    private CameraProxy mCameraDevice;
    private long mKeepBeforeTime;  // Keep the Camera before this time.
    private final Handler mHandler;
    private boolean mCameraOpened;  // true if camera is opened
    private final int mNumberOfCameras;
    private int mCameraId = -1;  // current camera id
    private int mBackCameraId = -1;
    private int mFrontCameraId = -1;
    private final CameraInfo[] mInfo;
    private static CameraProxy mMockCamera[];
    private static CameraInfo mMockCameraInfo[];

    /* Debug double-open issue */
    private static final boolean DEBUG_OPEN_RELEASE = true;
    @SuppressWarnings("unused")
	private static SimpleDateFormat sDateFormat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS");

    // We store the camera parameters when we actually open the device,
    // so we can restore them in the subsequent open() requests by the user.
    // This prevents the parameters set by PhotoModule used by VideoModule
    // inadvertently.
    private Parameters mParameters;

    // Use a singleton.
    private static CameraHolder sHolder;
    public static synchronized CameraHolder instance() {
        if (sHolder == null) {
            sHolder = new CameraHolder();
        }
        return sHolder;
    }

    private static final int RELEASE_CAMERA = 1;
    private class MyHandler extends Handler {
        MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case RELEASE_CAMERA:
//            		LogUtils.i("PhotoModule", "CameraHolder:handleMessage--RELEASE_CAMERA");
                    synchronized (CameraHolder.this) {
                        // In 'CameraHolder.open', the 'RELEASE_CAMERA' message
                        // will be removed if it is found in the queue. However,
                        // there is a chance that this message has been handled
                        // before being removed. So, we need to add a check
                        // here:
                        if (!mCameraOpened) release();
                    }
                    break;
            }
        }
    }

    public static void injectMockCamera(CameraInfo[] info, CameraProxy[] camera) {
        mMockCameraInfo = info;
        mMockCamera = camera;
        sHolder = new CameraHolder();
    }

    private CameraHolder() {
        HandlerThread ht = new HandlerThread("CameraHolder");
        ht.start();
        mHandler = new MyHandler(ht.getLooper());
        if (mMockCameraInfo != null) {
            mNumberOfCameras = mMockCameraInfo.length;
            mInfo = mMockCameraInfo;
        } else {
        	// Note: Some shit devices return negative numbers.
            mNumberOfCameras = android.hardware.Camera.getNumberOfCameras();
            if (mNumberOfCameras > 0) {
            	mInfo = new CameraInfo[mNumberOfCameras];
            	try {
    				for (int i = 0; i < mNumberOfCameras; i++) {
    				    mInfo[i] = new CameraInfo();
    				    android.hardware.Camera.getCameraInfo(i, mInfo[i]);
    				}
    			} catch (RuntimeException e) {
    				e.printStackTrace();
    			}
			} else {
				mInfo = null;
			}
        }

        if (null != mInfo && mInfo.length > 0) {
        	 // get the first (smallest) back and first front camera id
            for (int i = 0; i < mNumberOfCameras; i++) {
            	if (null == mInfo[i]) {
            		continue;
            	}
                if (mBackCameraId == -1 && mInfo[i].facing == CameraInfo.CAMERA_FACING_BACK) {
                    mBackCameraId = i;
                } else if (mFrontCameraId == -1 && mInfo[i].facing == CameraInfo.CAMERA_FACING_FRONT) {
                    mFrontCameraId = i;
                }
            }
		}
    }

    public int getNumberOfCameras() {
        return mNumberOfCameras;
    }

    public CameraInfo[] getCameraInfo() {
        return mInfo;
    }

    public synchronized CameraProxy open(int cameraId)
            throws Exception {
        if (DEBUG_OPEN_RELEASE) {
            if (mCameraOpened) {
                LogUtils.e(TAG, "double open");
            }
        }
        if (mCameraDevice != null /*&& mCameraId != cameraId*/) {
            mCameraDevice.release();
            mCameraDevice = null;
            mCameraId = -1;
        }
        if (mCameraDevice == null) {
            try {
                LogUtils.d(TAG, "open camera " + cameraId);
                if (mMockCameraInfo == null) {
                    mCameraDevice = CameraManager.instance().cameraOpen(cameraId);
                } else {
                    if (mMockCamera == null)
                        throw new RuntimeException();
                    mCameraDevice = mMockCamera[cameraId];
                }
                mCameraId = cameraId;
            } catch (RuntimeException e) {
//            	e.printStackTrace();
                LogUtils.e(TAG, "fail to connect Camera" + e.getMessage());
                throw new Exception(e);
            }
            if (null != mCameraDevice) {
            	mParameters = mCameraDevice.getParameters();
            }
        } else {
            try {
                mCameraDevice.reconnect();
            } catch (IOException e) {
                LogUtils.e(TAG, "reconnect failed.");
                throw new Exception(e);
            }
            mCameraDevice.setParameters(mParameters);
        }
        mCameraOpened = true;
        mHandler.removeMessages(RELEASE_CAMERA);
        mKeepBeforeTime = 0;
        return mCameraDevice;
    }

    /**
     * Tries to open the hardware camera. If the camera is being used or
     * unavailable then return {@code null}.
     */
    public synchronized CameraProxy tryOpen(int cameraId) {
        try {
            return !mCameraOpened ? open(cameraId) : null;
        } catch (Exception e) {
            // In eng build, we throw the exception so that test tool
            // can detect it and report it
            if ("eng".equals(Build.TYPE)) {
                throw new RuntimeException(e);
            }
            return null;
        }
    }
    
    public synchronized void releaseImmediately() {
//    	LogUtils.i("PhotoModule", "CameraHolder:releaseImmediately");
    	if (mCameraDevice == null) return;
    	mHandler.removeMessages(RELEASE_CAMERA);
//    	LogUtils.i("PhotoModule", "CameraHolder:releaseImmediately--mCameraDevice.release()");
        mCameraOpened = false;
        mCameraDevice.release();
        mCameraDevice = null;
        // We must set this to null because it has a reference to Camera.
        // Camera has references to the listeners.
        mParameters = null;
        mCameraId = -1;
    }

    /**
     * Must setPreviewCallback(null) before releasing camera.
     */
    public synchronized void release() {
        if (mCameraDevice == null) return;

        long now = System.currentTimeMillis();
        if (now < mKeepBeforeTime) {
            if (mCameraOpened) {
//    			LogUtils.i("PhotoModule", "CameraHolder:release--mCameraOpened set to false");
                mCameraOpened = false;
                mCameraDevice.stopPreview();
            }
            long delay = mKeepBeforeTime - now;
            mHandler.sendEmptyMessageDelayed(RELEASE_CAMERA,
            		delay);
//			LogUtils.i("PhotoModule", "CameraHolder:release--sendEmptyMessageDelayed;delay is " + delay);
            return;
        }
//		LogUtils.i("PhotoModule", "CameraHolder:release--CameraDevice.release()");
        mCameraOpened = false;
        mCameraDevice.release();
        mCameraDevice = null;
        // We must set this to null because it has a reference to Camera.
        // Camera has references to the listeners.
        mParameters = null;
        mCameraId = -1;
    }

    public void keep() {
        keep(KEEP_CAMERA_TIMEOUT);
    }

    public synchronized void keep(int time) {
        // We allow mCameraOpened in either state for the convenience of the
        // calling activity. The activity may not have a chance to call open()
        // before the user switches to another activity.
        mKeepBeforeTime = System.currentTimeMillis() + time;
    }

    public int getBackCameraId() {
        return mBackCameraId;
    }

    public int getFrontCameraId() {
        return mFrontCameraId;
    }
    
    public boolean isFrontCamera() {
    	if (mCameraId == -1) {
			return false;
		}
    	
    	return mCameraId == mFrontCameraId;
    }
    
    public int getAvailiableCameraId(){
    	if(mBackCameraId!=-1){
			LogUtils.i("java_bing", "camera cameraId." + mBackCameraId);
    		return mBackCameraId;
    		// edit by java_bing panorama can't support front camera
    	/*}else{
    		if(mFrontCameraId != -1){
    			LogUtils.i("java_bing", "camera cameraId." + mFrontCameraId);
    			return mFrontCameraId;
    		}*/
    	}
		return -1;
	}    
	
    public boolean supportSwitch() {
    	return (-1 != mBackCameraId) && (-1 != mFrontCameraId);
    }
    
    public boolean isReleased() {
    	return null == mCameraDevice;
    }
}
