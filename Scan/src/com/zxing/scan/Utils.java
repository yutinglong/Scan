/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.zxing.scan;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.location.Location;
import android.media.ExifInterface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.FloatMath;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import com.zxing.scan.module.CameraAttrs;
import com.zxing.scan.module.CameraManager;

/**
 * Collection of utility functions used in this package.
 */
public class Utils {
	private static final String TAG = "Util";
	
	// Orientation hysteresis amount used in rounding, in degrees
	public static final int ORIENTATION_HYSTERESIS = 5;

	public static final String REVIEW_ACTION = "com.android.camera.action.REVIEW";

    // Fields from android.hardware.Camera.Parameters
    public static final String FOCUS_MODE_CONTINUOUS_PICTURE = "continuous-picture";
    public static final String RECORDING_HINT = "recording-hint";
    private static final String AUTO_EXPOSURE_LOCK_SUPPORTED = "auto-exposure-lock-supported";
    private static final String AUTO_WHITE_BALANCE_LOCK_SUPPORTED = "auto-whitebalance-lock-supported";
    public static final String SCENE_MODE_HDR = "hdr";
    public static final String TRUE = "true";
    public static final String FALSE = "false";

    public static boolean isSupported(String value, List<String> supported) {
        return supported == null ? false : supported.indexOf(value) >= 0;
    }

    public static boolean isAutoExposureLockSupported(Parameters params) {
    	if (null == params) {
			return false;
		}
        return TRUE.equals(params.get(AUTO_EXPOSURE_LOCK_SUPPORTED));
    }

    public static boolean isAutoWhiteBalanceLockSupported(Parameters params) {
    	if (null == params) {
			return false;
		}
        return TRUE.equals(params.get(AUTO_WHITE_BALANCE_LOCK_SUPPORTED));
    }

    public static boolean isCameraHdrSupported(Parameters params) {
    	if (null == params) {
			return false;
		}
        List<String> supported = params.getSupportedSceneModes();
        return (supported != null) && supported.contains(SCENE_MODE_HDR);
    }

    @TargetApi(ApiHelper.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static boolean isMeteringAreaSupported(Parameters params) {
    	if(android.os.Build.MODEL != null
    			&& android.os.Build.MODEL.equals("SM-N900")){
    		// 三星note3不支持测光对焦分离
    		return false;
    	}
        if (null != params && ApiHelper.HAS_CAMERA_METERING_AREA) {
            return params.getMaxNumMeteringAreas() > 0;
        }
        return false;
    }

    @TargetApi(ApiHelper.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static boolean isFocusAreaSupported(Parameters params) {
        if (null != params && ApiHelper.HAS_CAMERA_FOCUS_AREA) {
            return (params.getMaxNumFocusAreas() > 0
                    && isSupported(Parameters.FOCUS_MODE_AUTO,
                            params.getSupportedFocusModes()));
        }
        return false;
    }

    // Private intent extras. Test only.
    @SuppressWarnings("unused")
	private static final String EXTRAS_CAMERA_FACING =
            "android.intent.extras.CAMERA_FACING";

	private static float sPixelDensity = 1;

    private Utils() {
    }
    public static float getPixelDensity()
    {
    	return sPixelDensity;
    }
	public static void initialize(float density) {
		sPixelDensity = density;
	}

	public static int dpToPixel(float dp) {
		LogUtils.e("AbstractNavigation", "dpToPixel--sPixelDensity=" + sPixelDensity);
		LogUtils.e("AbstractNavigation", "dpToPixel--result=" + Math.round(sPixelDensity * dp));
		return Math.round(sPixelDensity * dp);
	}
	
	@SuppressWarnings("deprecation")
	public static int getScreenWidth(Context context){
    	WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    	int width = wm.getDefaultDisplay().getWidth();
    	
    	return width;
	}

	// Rotates the bitmap by the specified degree.
	// If a new bitmap is created, the original bitmap is recycled.
	public static Bitmap rotate(Bitmap b, int degrees) {
		return rotateAndMirror(b, degrees, false);
	}

	// Rotates and/or mirrors the bitmap. If a new bitmap is created, the
	// original bitmap is recycled.
	public static Bitmap rotateAndMirror(Bitmap b, int degrees, boolean mirror) {
		if ((degrees != 0 || mirror) && b != null) {
			Matrix m = new Matrix();
			// Mirror first.
			// horizontal flip + rotation = -rotation + horizontal flip
			if (mirror) {
				m.postScale(-1, 1);
				degrees = (degrees + 360) % 360;
				if (degrees == 0 || degrees == 180) {
					m.postTranslate(b.getWidth(), 0);
				} else if (degrees == 90 || degrees == 270) {
					m.postTranslate(b.getHeight(), 0);
				} else {
					throw new IllegalArgumentException("Invalid degrees="
							+ degrees);
				}
			}
			if (degrees != 0) {
				// clockwise
				m.postRotate(degrees, (float) b.getWidth() / 2,
						(float) b.getHeight() / 2);
			}

			try {
				Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(),
						b.getHeight(), m, true);
				if (b != b2) {
					b.recycle();
					b = b2;
				}
			} catch (OutOfMemoryError ex) {
				// We have no memory to rotate. Return the original bitmap.
			}
		}
		return b;
	}

	/*
	 * Compute the sample size as a function of minSideLength and
	 * maxNumOfPixels. minSideLength is used to specify that minimal width or
	 * height of a bitmap. maxNumOfPixels is used to specify the maximal size in
	 * pixels that is tolerable in terms of memory usage.
	 * 
	 * The function returns a sample size based on the constraints. Both size
	 * and minSideLength can be passed in as -1 which indicates no care of the
	 * corresponding constraint. The functions prefers returning a sample size
	 * that generates a smaller bitmap, unless minSideLength = -1.
	 * 
	 * Also, the function rounds up the sample size to a power of 2 or multiple
	 * of 8 because BitmapFactory only honors sample size this way. For example,
	 * BitmapFactory downsamples an image by 2 even though the request is 3. So
	 * we round up the sample size to avoid OOM.
	 */
	public static int computeSampleSize(BitmapFactory.Options options,
			int minSideLength, int maxNumOfPixels) {
		int initialSize = computeInitialSampleSize(options, minSideLength,
				maxNumOfPixels);

		int roundedSize;
		if (initialSize <= 8) {
			roundedSize = 1;
			while (roundedSize < initialSize) {
				roundedSize <<= 1;
			}
		} else {
			roundedSize = (initialSize + 7) / 8 * 8;
		}

		return roundedSize;
	}

	private static int computeInitialSampleSize(BitmapFactory.Options options,
			int minSideLength, int maxNumOfPixels) {
		double w = options.outWidth;
		double h = options.outHeight;

		int lowerBound = (maxNumOfPixels < 0) ? 1 : (int) Math.ceil(Math.sqrt(w
				* h / maxNumOfPixels));
		int upperBound = (minSideLength < 0) ? 128 : (int) Math.min(
				Math.floor(w / minSideLength), Math.floor(h / minSideLength));

		if (upperBound < lowerBound) {
			// return the larger one when there is no overlapping zone.
			return lowerBound;
		}

		if (maxNumOfPixels < 0 && minSideLength < 0) {
			return 1;
		} else if (minSideLength < 0) {
			return lowerBound;
		} else {
			return upperBound;
		}
	}

	public static Bitmap makeBitmap(byte[] jpegData, int maxNumOfPixels) {
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory
					.decodeByteArray(jpegData, 0, jpegData.length, options);
			if (options.mCancel || options.outWidth == -1
					|| options.outHeight == -1) {
				return null;
			}
			options.inSampleSize = computeSampleSize(options, -1,
					maxNumOfPixels);
			options.inJustDecodeBounds = false;

			options.inDither = false;
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;
			return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length,
					options);
		} catch (OutOfMemoryError ex) {
			Log.e(TAG, "Got oom exception ", ex);
			return null;
		}
	}

	public static void closeSilently(Closeable c) {
		if (c == null)
			return;
		try {
			c.close();
		} catch (Throwable t) {
			// do nothing
		}
	}

	public static void Assert(boolean cond) {
		if (!cond) {
//			throw new AssertionError();
			Log.e(TAG, "Assert error: double open.");
		}
	}

    @TargetApi(ApiHelper.VERSION_CODES.ICE_CREAM_SANDWICH)
    private static void throwIfCameraDisabled(Activity activity) throws Exception {
        // Check if device policy has disabled the camera.
        if (ApiHelper.HAS_GET_CAMERA_DISABLED) {
            DevicePolicyManager dpm = (DevicePolicyManager) activity.getSystemService(
                    Context.DEVICE_POLICY_SERVICE);
            if (dpm.getCameraDisabled(null)) {
                throw new Exception();
            }
        }
    }

    // This is for test only. Allow the camera to launch the specific camera.
//    public static int getCameraFacingId(Activity currentActivity) {
//        int cameraId = -1;
//
//        if (isFrontCameraDefault(currentActivity)) {
//            // Check if the front camera exist
//            int frontCameraId = CameraHolder.instance().getFrontCameraId();
//            if (frontCameraId != -1) {
//                cameraId = frontCameraId;
//            }
//        } else {
//            // Check if the back camera exist
//            int backCameraId = CameraHolder.instance().getBackCameraId();
//            if (backCameraId != -1) {
//                cameraId = backCameraId;
//            }
//        }
//        return cameraId;
//    }
//
//    private static boolean isFrontCameraIntent(int intentCameraId) {
//        return (intentCameraId == android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT);
//    }
//
//    private static boolean isBackCameraIntent(int intentCameraId) {
//        return (intentCameraId == android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK);
//    }

    public static CameraManager.CameraProxy openCamera(Activity activity, int cameraId)
            throws Exception {
        throwIfCameraDisabled(activity);

        try {
            return CameraHolder.instance().open(cameraId);
        } catch (Exception e) {
            // In eng build, we throw the exception so that test tool
            // can detect it and report it
//            if ("eng".equals(Build.TYPE)) {
//                throw new RuntimeException("openCamera failed", e);
//            } else {
                throw e;
//            }
        }
    }

	public static <T> T checkNotNull(T object) {
		if (object == null)
			throw new NullPointerException();
		return object;
	}

	public static boolean equals(Object a, Object b) {
		return (a == b) || (a == null ? false : a.equals(b));
	}

	public static int nextPowerOf2(int n) {
		n -= 1;
		n |= n >>> 16;
		n |= n >>> 8;
		n |= n >>> 4;
		n |= n >>> 2;
		n |= n >>> 1;
		return n + 1;
	}

	public static float distance(float x, float y, float sx, float sy) {
		float dx = x - sx;
		float dy = y - sy;
		return FloatMath.sqrt(dx * dx + dy * dy);
	}

	public static int clamp(int x, int min, int max) {
		if (x > max)
			return max;
		if (x < min)
			return min;
		return x;
	}

	public static int getDisplayRotation(Activity activity) {
		int rotation = activity.getWindowManager().getDefaultDisplay()
				.getRotation();
		switch (rotation) {
		case Surface.ROTATION_0:
			return 0;
		case Surface.ROTATION_90:
			return 90;
		case Surface.ROTATION_180:
			return 180;
		case Surface.ROTATION_270:
			return 270;
		}
		return 0;
	}

	public static int getDisplayOrientation(int degrees, int cameraId) {
		// See android.hardware.Camera.setDisplayOrientation for
		// documentation.
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		LogUtils.d("Orientation", "info.orientation==" + info.orientation);
		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360; // compensate the mirror
		} else { // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		return result;
	}

	public static int getCameraOrientation(int cameraId) {
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		return info.orientation;
	}
	
	public static int getRoundCameraOrientation(int cameraId) {
		return getCameraOrientation(cameraId) + CameraAttrs.getOrientationOffset();
	}

	public static int roundOrientation(int orientation, int orientationHistory) {
		boolean changeOrientation = false;
		if (orientationHistory == OrientationEventListener.ORIENTATION_UNKNOWN) {
			changeOrientation = true;
		} else {
			int dist = Math.abs(orientation - orientationHistory);
			dist = Math.min(dist, 360 - dist);
			changeOrientation = (dist >= 45 + ORIENTATION_HYSTERESIS);
		}
		if (changeOrientation) {
			return ((orientation + 45) / 90 * 90) % 360;
		}
		return orientationHistory;
	}
	
	/**
	 * 根据照片比例ID获取照片比例
	 * 
	 * @param picRatioID	-	照片比例ID
	 * @param screenRatio	-	屏幕比例
	 * @return
	 */
	@Deprecated
	public static double getPicRatio(int picRatioID, double screenRatio) {
		double ratio = -1;
		switch (picRatioID) {
		case 0:
			ratio = -1;
			break;
		case 1:
			ratio = screenRatio;
			break;
		case 2:
			ratio = CameraSize.RATIO_4T3;
			break;
		case 3:
			ratio = CameraSize.RATIO_16T9;
			break;
		}
		
		return ratio;
	}
	
	/**
	 * 根据PreviewSize和PictureSize，判断并获取照片比例ID
	 * 
	 * @param preSize	-	选定的PreviewSize
	 * @param picSize	-	选定的PictureSize
	 * @param screenRatio	-	屏幕比例
	 * @return	-	照片比例ID：0-系统默认Size；1-全屏；2-4:3；3-16:9
	 */
	@Deprecated
	public static int getPicRatioID(Point preSize, Point picSize, double screenRatio) {
		int picRatioID = 0;
		
		double ratioPreSize = 1d * preSize.x / preSize.y;
		double ratioPicSize = 1d * picSize.x / picSize.y;
		if (Math.abs(ratioPreSize - ratioPicSize) > CameraSize.ASPECT_TOLERANCE) {
			picRatioID = 0;
		} else {
			double diff = Math.abs(ratioPicSize - screenRatio);
			if (Math.abs(ratioPicSize - 16d /9d) <= CameraSize.ASPECT_TOLERANCE) {
				// 如果全屏也是16:9，则按全屏处理
				if (diff <= CameraSize.ASPECT_TOLERANCE) {
					picRatioID = 1;
				} else {
					picRatioID = 3;
				}
			} else if (Math.abs(ratioPicSize - 4d / 3d) <= CameraSize.ASPECT_TOLERANCE) {
				picRatioID = 2;
			} else if (diff <= CameraSize.ASPECT_TOLERANCE) {
				picRatioID = 1;
			} else {
				picRatioID = 0;
			}
		}
		
		return picRatioID;
	}
		
	/**
	 * 判断Camera提供的候选Size是否可正常设置
	 * 
	 * @param	sizeList	-	候选Size的list
	 * @return
	 */
	@Deprecated
	private static boolean isBadSize(List<Size> sizeList) {
		if (null == sizeList || sizeList.size() == 0) {
			return true;
		}
		
//		for (Size size : sizeList) {
//			LogUtils.e(TAG, "SizeList: list-->w*h: " + size.width + "*" + size.height + ";r: " + (1d * size.width / size.height));
//		}

		// If the max preview or picture size is not greater than MIN_SIZE, then use default size
		final int MIN_SIZE = 640;
		
		boolean badSize = true;
		for (Size size : sizeList) {
			if (Math.max(size.width, size.height) > MIN_SIZE) {
				badSize = false;
				break;
			}
		}
		
		return badSize;
	}
	
	/**
	 * 判断候选Size中是否存在满足特定比例的Size
	 * 
	 * @param sizeList	-	optional size list
	 * @param ratio	-	target ratio
	 * @return
	 */
	@Deprecated
	private static boolean supportCertainRatio(List<Size> sizeList, double ratio) {
		boolean support = false;
		double r = 0.0d;
		for (Size size : sizeList) {
			r = 1d * size.width / size.height;
			if (Math.abs(ratio - r) <= CameraSize.ASPECT_TOLERANCE) {
				support = true;
				break;
			}
		}
		
		return support;
	}

	/**
	 * 判断候选Size中是否存在满足特定比例且不低于目标尺寸的Size
	 * 
	 * @param sizeList	-	optional size list
	 * @param ratio	-	target ratio
	 * @param targetHeight	-	目标尺寸
	 * @return
	 */
	@Deprecated
	private static boolean supportCertainRatioAndHeight(List<Size> sizeList, double ratio, int targetHeight) {
		boolean support = false;
		double r = 0.0d;
		int diff = 0;
		for (Size size : sizeList) {
			r = 1d * size.width / size.height;
			diff = Math.max(size.width, size.height) - targetHeight;
			if (diff >= 0 && Math.abs(ratio - r) <= CameraSize.ASPECT_TOLERANCE) {
				support = true;
				break;
			}
		}
		
		return support;
	}
	
	/**
	 * 是否存在支持全屏比例的PreviewSize和PictureSize
	 * 
	 * @param preSizes	-	PreviewSize的候选list
	 * @param picSizes	-	PictureSize的候选list
	 * @param ratio	-	屏幕比例（长边比短边）
	 * @param targetHeight	-	目标尺寸
	 * @return
	 */
	@Deprecated
	public static boolean supportSizeRatioFullScreen(List<Size> preSizes, List<Size> picSizes, double ratio, int targetHeight) {
		if (isBadSize(preSizes) || isBadSize(picSizes)) {
			return false;
		}
		
		return supportCertainRatio(preSizes, ratio) && supportCertainRatioAndHeight(picSizes, ratio, targetHeight);
	}

	/**
	 * 是否存在支持4:3比例的PreviewSize和PictureSize
	 * 
	 * @param preSizes	-	PreviewSize的候选list
	 * @param picSizes	-	PictureSize的候选list
	 * @param targetHeight	-	目标尺寸
	 * @return
	 */
	@Deprecated
	public static boolean supportSizeRatio43(List<Size> preSizes, List<Size> picSizes, int targetHeight) {
		if (isBadSize(preSizes) || isBadSize(picSizes)) {
			return false;
		}
		
		double ratio = 4d / 3d;
		return supportCertainRatio(preSizes, ratio) && supportCertainRatioAndHeight(picSizes, ratio, targetHeight);
	}

	/**
	 * 是否存在支持16:9比例的PreviewSize和PictureSize
	 * 
	 * @param preSizes	-	PreviewSize的候选list
	 * @param picSizes	-	PictureSize的候选list
	 * @param targetHeight	-	目标尺寸
	 * @return
	 */
	@Deprecated
	public static boolean supportSizeRatio169(List<Size> preSizes, List<Size> picSizes, int targetHeight) {
		if (isBadSize(preSizes) || isBadSize(picSizes)) {
			return false;
		}
		
		double ratio = CameraSize.RATIO_16T9;
		return supportCertainRatio(preSizes, ratio) && supportCertainRatioAndHeight(picSizes, ratio, targetHeight);
	}
	
	/**
	 * 获取最优的PreviewSize和PictureSize
	 * 
	 * @param preSizes	-	PreviewSize的候选list
	 * @param picSizes	-	PictureSize的候选list
	 * @param sizePoint	-	屏幕尺寸PoinitF，x=短边，y=长边
	 * @param targetHeight	-	照片目标尺寸（不低于该尺寸）
	 * @param targetRatio	-	目标比例，-1表示不参考该值
	 * @return	-	new Size[]{optimalPreviewSize, optimalPictureSize} or null
	 */
	@Deprecated
	public static Size[] getOptimalSizes(List<Size> preSizes, List<Size> picSizes, PointF sizePoint,
			double targetRatio, int targetHeight, int maxPreviewHeight) {
		// 判断Camera提供的候选Size是否可正常设置（PreviewSize和PitcureSize同时存在大于MIN_SIZE的Size，MIN_SIZE=640）
		if (!CameraAttrs.isLowLevelButSupportSetPreviewSizeAndPictureSize()
				&& (isBadSize(preSizes) || isBadSize(picSizes))) {
			return null;
		}
		
		double minSizeDiffOptimal = Double.MAX_VALUE;
		double targetScreenRatio = 1d * sizePoint.y / sizePoint.x;
		double targetStandardRatio = 1.333333d;
		if (targetRatio > 0) {
			targetScreenRatio = targetRatio;
			targetStandardRatio = targetRatio;
		}
		
		// First: find candidate size list
		List<Size> preSizesScreenRatio = new ArrayList<Size>();
		List<Size> preSizesStandardRatio = new ArrayList<Size>();
		LogUtils.e(TAG, "PreviewSize: list");
		for (Size size : preSizes) {
			LogUtils.e(TAG, "PreviewSize: list-->w*h: " + size.width + "*" + size.height + ";r: " + (1d * size.width / size.height));
			double ratio = 1d * size.width / size.height;
			if (Math.abs(targetScreenRatio - ratio) <= CameraSize.ASPECT_TOLERANCE) {
				preSizesScreenRatio.add(size);
			} else if (Math.abs(targetStandardRatio - ratio) <= CameraSize.ASPECT_TOLERANCE) {
				preSizesStandardRatio.add(size);
			}
		}
		
		List<Size> picSizesScreenRatio = new ArrayList<Size>();
		List<Size> picSizesStandardRatio = new ArrayList<Size>();
		LogUtils.e(TAG, "PictureSize: list");
		for (Size size : picSizes) {
			LogUtils.e(TAG, "PictureSize: list-->w*h: " + size.width + "*" + size.height + ";r: " + (1d * size.width / size.height));
			double ratio = 1d * size.width / size.height;
			if (Math.abs(targetScreenRatio - ratio) <= CameraSize.ASPECT_TOLERANCE) {
				picSizesScreenRatio.add(size);
			} else if (Math.abs(targetStandardRatio - ratio) <= CameraSize.ASPECT_TOLERANCE) {
				picSizesStandardRatio.add(size);
			}
		}
		
		// 屏幕比例和标准比例列表都不完整，返回null即使用默认Size
		if ((preSizesScreenRatio.isEmpty() || picSizesScreenRatio.isEmpty())
				&& (preSizesStandardRatio.isEmpty() || picSizesStandardRatio.isEmpty())) {
			return null;
		}
		
		// Second-1: find the optimal preview size closest to the screen size
		Size optimalPreviewSize = null;
		minSizeDiffOptimal = Double.MAX_VALUE;
		for (Size size : preSizesScreenRatio) {
			int diff = Math.abs(Math.max(size.width, size.height) - maxPreviewHeight);
			if (diff < minSizeDiffOptimal) {
				optimalPreviewSize = size;
				minSizeDiffOptimal = diff;
			}
		}
		// Second-2: find the optimal picture size closest to the screen size
		Size optimalPictureSize = null;
		minSizeDiffOptimal = -1d;
		for (Size size : picSizesScreenRatio) {
			int diff = Math.max(size.width, size.height) - targetHeight;
			// There must be at least one of width and height of size not less than the target picture size.
			if (diff < 0) {
				continue;
			}
			
			if (diff > minSizeDiffOptimal) {
				optimalPictureSize = size;
				minSizeDiffOptimal = diff;
			}
		}
		if (null != optimalPreviewSize && null != optimalPictureSize) {
			LogUtils.e(TAG, "PreviewSize: optimalSize-->w*h: " + optimalPreviewSize.width + "*" + optimalPreviewSize.height + ";r: " + (1d * optimalPreviewSize.width / optimalPreviewSize.height));
			LogUtils.e(TAG, "PictureSize: optimalSize-->w*h: " + optimalPictureSize.width + "*" + optimalPictureSize.height + ";r: " + (1d * optimalPictureSize.width / optimalPictureSize.height));
			return new Size[]{optimalPreviewSize, optimalPictureSize};
		}
		
		// Third-1: find the optimal preview size closest to the standard size
		optimalPreviewSize = null;
		minSizeDiffOptimal = Double.MAX_VALUE;
		for (Size size : preSizesStandardRatio) {
			int diff = Math.abs(Math.max(size.width, size.height) - maxPreviewHeight);
			if (diff < minSizeDiffOptimal) {
				optimalPreviewSize = size;
				minSizeDiffOptimal = diff;
			}
		}
		// Third-2: find the optimal picture size closest to the screen size
		optimalPictureSize = null;
		minSizeDiffOptimal = -1d;
		for (Size size : picSizesStandardRatio) {
			int diff = Math.max(size.width, size.height) - targetHeight;
			// There must be at least one of width and height of size not less than the target picture size.
			if (diff < 0) {
				continue;
			}
			
			if (diff > minSizeDiffOptimal) {
				optimalPictureSize = size;
				minSizeDiffOptimal = diff;
			}
		}
		if (null != optimalPreviewSize && null != optimalPictureSize) {
			LogUtils.e(TAG, "PreviewSize: optimalSize-->w*h: " + optimalPreviewSize.width + "*" + optimalPreviewSize.height + ";r: " + (1d * optimalPreviewSize.width / optimalPreviewSize.height));
			LogUtils.e(TAG, "PictureSize: optimalSize-->w*h: " + optimalPictureSize.width + "*" + optimalPictureSize.height + ";r: " + (1d * optimalPictureSize.width / optimalPictureSize.height));
			return new Size[]{optimalPreviewSize, optimalPictureSize};
		}	
		
		return null;
	}
	
	public static List<CameraSize> getOptionalSizes(List<Size> sizes) {
		 // Use a very small tolerance because we want an exact match.
        if (sizes == null) return null;
        
        double targetRatio4To3 = CameraSize.RATIO_4T3;
        double targetRatio16To9 = CameraSize.RATIO_16T9;
        
        Set<CameraSize> optionals = new TreeSet<CameraSize>(new Comparator<CameraSize>() {
			@Override
			public int compare(CameraSize lhs, CameraSize rhs) {
				return rhs.hashCode() - lhs.hashCode();
			}
		});
        
        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) 1d * size.width / size.height;
            if (Math.abs(ratio - targetRatio4To3) > CameraSize.ASPECT_TOLERANCE
            		&& Math.abs(ratio - targetRatio16To9) > CameraSize.ASPECT_TOLERANCE) {
            	continue;
            }
            optionals.add(new CameraSize(size.width, size.height));
        }
        
        List<CameraSize> optionalSizes = new ArrayList<CameraSize>(optionals);
        
        return optionalSizes;
	}
	
	public static List<CameraSize> getOptionalSizes(List<Size> sizes, double targetRatio) {
		 // Use a very small tolerance because we want an exact match.
        if (sizes == null) return null;
        
        Set<CameraSize> optionals = new TreeSet<CameraSize>(new Comparator<CameraSize>() {
			@Override
			public int compare(CameraSize lhs, CameraSize rhs) {
				return rhs.hashCode() - lhs.hashCode();
			}
		});
        
        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) 1d * size.width / size.height;
            if (Math.abs(ratio - targetRatio) > CameraSize.ASPECT_TOLERANCE) continue;
            optionals.add(new CameraSize(size.width, size.height));
        }
        
        List<CameraSize> optionalSizes = new ArrayList<CameraSize>(optionals);
        
        return optionalSizes;
	}

    @SuppressWarnings("deprecation")
    @TargetApi(ApiHelper.VERSION_CODES.HONEYCOMB_MR2)
    private static Point getDefaultDisplaySize(Activity activity, Point size) {
        Display d = activity.getWindowManager().getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= ApiHelper.VERSION_CODES.HONEYCOMB_MR2) {
            d.getSize(size);
        } else {
            size.set(d.getWidth(), d.getHeight());
        }
        return size;
    }

    public static Size getOptimalPreviewSize(Activity currentActivity,
            List<Size> sizes, double targetRatio, boolean liveFilterMode) {
        // Use a very small tolerance because we want an exact match.
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int minDiffWidth = Integer.MAX_VALUE;

        // Because of bugs of overlay and layout, we sometimes will try to
        // layout the viewfinder in the portrait orientation and thus get the
        // wrong size of preview surface. When we change the preview size, the
        // new overlay will be created before the old one closed, which causes
        // an exception. For now, just get the screen size.
        Point point = getDefaultDisplaySize(currentActivity, new Point());
        int targetHeight = Math.min(point.x, point.y);
        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > CameraSize.ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
            	if (liveFilterMode) {
            		if (Math.abs(size.width - 1280) < minDiffWidth) {
            			minDiffWidth = Math.abs(size.width - 1280);
            			optimalSize = size;
                        minDiff = Math.abs(size.height - targetHeight);
					}
				} else {
					optimalSize = size;
	                minDiff = Math.abs(size.height - targetHeight);
				}
            }
        }
        // Cannot find the one match the aspect ratio. This should not happen.
        // Ignore the requirement.
        if (optimalSize == null) {
            Log.w(TAG, "No preview size match the aspect ratio");
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }


	// Returns the largest picture size which matches the given aspect ratio.
	public static Size getOptimalVideoSnapshotPictureSize(List<Size> sizes,
			double targetRatio) {
		// Use a very small tolerance because we want an exact match.
		if (sizes == null)
			return null;

		Size optimalSize = null;

		// Try to find a size matches aspect ratio and has the largest width
		for (Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > CameraSize.ASPECT_TOLERANCE)
				continue;
			if (optimalSize == null || size.width > optimalSize.width) {
				optimalSize = size;
			}
		}

		// Cannot find one that matches the aspect ratio. This should not
		// happen.
		// Ignore the requirement.
		if (optimalSize == null) {
			Log.w(TAG, "No picture size match the aspect ratio");
			for (Size size : sizes) {
				if (optimalSize == null || size.width > optimalSize.width) {
					optimalSize = size;
				}
			}
		}
		return optimalSize;
	}

	public static void dumpParameters(Parameters parameters) {
    	if (null == parameters) {
			return ;
		}
		String flattened = parameters.flatten();
		StringTokenizer tokenizer = new StringTokenizer(flattened, ";");
		LogUtils.d(TAG, "Dump all camera parameters:");
		while (tokenizer.hasMoreElements()) {
			LogUtils.d(TAG, tokenizer.nextToken());
		}
	}

	/**
	 * Returns whether the device is voice-capable (meaning, it can do MMS).
	 */
	public static boolean isMmsCapable(Context context) {
		TelephonyManager telephonyManager = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		if (telephonyManager == null) {
			return false;
		}

		try {
			Class<?> partypes[] = new Class[0];
			Method sIsVoiceCapable = TelephonyManager.class.getMethod(
					"isVoiceCapable", partypes);

			Object arglist[] = new Object[0];
			Object retobj = sIsVoiceCapable.invoke(telephonyManager, arglist);
			return (Boolean) retobj;
		} catch (java.lang.reflect.InvocationTargetException ite) {
			// Failure, must be another device.
			// Assume that it is voice capable.
		} catch (IllegalAccessException iae) {
			// Failure, must be an other device.
			// Assume that it is voice capable.
		} catch (NoSuchMethodException nsme) {
		}
		return true;
	}

	private static int sLocation[] = new int[2];

	// This method is not thread-safe.
	public static boolean pointInView(float x, float y, View v) {
		v.getLocationInWindow(sLocation);
		return x >= sLocation[0] && x < (sLocation[0] + v.getWidth())
				&& y >= sLocation[1] && y < (sLocation[1] + v.getHeight());
	}

	public static int[] getRelativeLocation(View reference, View view) {
		reference.getLocationInWindow(sLocation);
		int referenceX = sLocation[0];
		int referenceY = sLocation[1];
		view.getLocationInWindow(sLocation);
		sLocation[0] -= referenceX;
		sLocation[1] -= referenceY;
		return sLocation;
	}

	public static boolean isUriValid(Uri uri, ContentResolver resolver) {
		if (uri == null)
			return false;

		try {
			ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "r");
			if (pfd == null) {
				LogUtils.e(TAG, "Fail to open URI. URI=" + uri);
				return false;
			}
			pfd.close();
		} catch (IOException ex) {
			return false;
		}
		return true;
	}

	public static void viewUri(Uri uri, Context context) {
		if (!isUriValid(uri, context.getContentResolver())) {
			LogUtils.e(TAG, "Uri invalid. uri=" + uri);
			return;
		}

		try {
			context.startActivity(new Intent(Utils.REVIEW_ACTION, uri));
		} catch (ActivityNotFoundException ex) {
			try {
				context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
			} catch (ActivityNotFoundException e) {
				Log.e(TAG, "review image fail. uri=" + uri, e);
			}
		}
	}

	public static void dumpRect(RectF rect, String msg) {
		LogUtils.v(TAG, msg + "=(" + rect.left + "," + rect.top + "," + rect.right
				+ "," + rect.bottom + ")");
	}

	public static void rectFToRect(RectF rectF, Rect rect) {
		rect.left = Math.round(rectF.left);
		rect.top = Math.round(rectF.top);
		rect.right = Math.round(rectF.right);
		rect.bottom = Math.round(rectF.bottom);
	}

	public static void prepareMatrix(Matrix matrix, boolean mirror,
			int displayOrientation, int viewWidth, int viewHeight) {
		// Need mirror for front camera.
		matrix.setScale(mirror ? -1 : 1, 1);
		// This is the value for android.hardware.Camera.setDisplayOrientation.
		matrix.postRotate(displayOrientation);
		// Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
		// UI coordinates range from (0, 0) to (width, height).
		matrix.postScale(viewWidth / 2000f, viewHeight / 2000f);
		matrix.postTranslate(viewWidth / 2f, viewHeight / 2f);
	}

	public static void fadeIn(View view, float startAlpha, float endAlpha,
			long duration) {
		if (view.getVisibility() == View.VISIBLE)
			return;

		view.setVisibility(View.VISIBLE);
		Animation animation = new AlphaAnimation(startAlpha, endAlpha);
		animation.setDuration(duration);
		view.startAnimation(animation);
	}

	public static void fadeIn(View view) {
		fadeIn(view, 0F, 1F, 400);

		// We disabled the button in fadeOut(), so enable it here.
		view.setEnabled(true);
	}

	public static void fadeOut(View view) {
		if (view.getVisibility() != View.VISIBLE)
			return;

		// Since the button is still clickable before fade-out animation
		// ends, we disable the button first to block click.
		view.setEnabled(false);
		Animation animation = new AlphaAnimation(1F, 0F);
		animation.setDuration(400);
		view.startAnimation(animation);
		view.setVisibility(View.GONE);
	}

	public static void setGpsParameters(Parameters parameters, Location loc) {
		if (null == parameters || null == loc) {
			return;
		}
		// Clear previous GPS location from the parameters.
		parameters.removeGpsData();

		// We always encode GpsTimeStamp
		parameters.setGpsTimestamp(System.currentTimeMillis() / 1000);

		// Set GPS location.
		if (loc != null) {
			double lat = loc.getLatitude();
			double lon = loc.getLongitude();
			boolean hasLatLon = (lat != 0.0d) || (lon != 0.0d);

			if (hasLatLon) {
				LogUtils.d(TAG, "Set gps location");
				parameters.setGpsLatitude(lat);
				parameters.setGpsLongitude(lon);
				String provider = loc.getProvider();
				if (!TextUtils.isEmpty(provider)) {
					parameters.setGpsProcessingMethod(provider.toUpperCase());
				}
				if (loc.hasAltitude()) {
					parameters.setGpsAltitude(loc.getAltitude());
				} else {
					// for NETWORK_PROVIDER location provider, we may have
					// no altitude information, but the driver needs it, so
					// we fake one.
					parameters.setGpsAltitude(0);
				}
				if (loc.getTime() != 0) {
					// Location.getTime() is UTC in milliseconds.
					// gps-timestamp is UTC in seconds.
					long utcTimeSeconds = loc.getTime() / 1000;
					parameters.setGpsTimestamp(utcTimeSeconds);
				}
			} else {
				loc = null;
			}
		}
	}

	@TargetApi(9)
	public static boolean hasFrontCamera() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
			return false;
		}
		int num = Camera.getNumberOfCameras();
		for (int i = 0; i < num; i++) {
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(i, info);
			if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
				return true;
			}
		}
		return false;
	}

	public static boolean isZoomSupported(Camera camera) {
		if(camera == null){
			return false;
		}
		Parameters params = camera.getParameters();
		return params.isZoomSupported();
	}

	public Ringtone getDefaultRingtone(Context context, int type) {
		return RingtoneManager.getRingtone(context,
				RingtoneManager.getActualDefaultRingtoneUri(context, type));
	}
	
	public static Uri getDefaultRingtoneUri(Context context, int type) {
		return RingtoneManager.getActualDefaultRingtoneUri(context, type);
	}
	
	public static int getCameraId(boolean front) {
		int num = Camera.getNumberOfCameras();
		for (int i = 0; i < num; i++) {
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(i, info);
			if (info.facing == CameraInfo.CAMERA_FACING_FRONT && front) {
				return i;
			}
			if (info.facing == CameraInfo.CAMERA_FACING_BACK && !front) {
				return i;
			}
		}
		return -1;
	}

	public static String getVersionName(Context cx) {
		String versionName = "1.0.0";
		
		String pkName = cx.getPackageName();
		PackageInfo info = null;
		try {
			info = cx.getPackageManager().getPackageInfo(pkName, PackageManager.GET_CONFIGURATIONS);
			versionName = info.versionName;
		} catch (NameNotFoundException e) {
//			e.printStackTrace();
		} catch (Exception e) { //中兴U880s(2.3.7) java.lang.RuntimeException:Packagemanagerhasdied
			e.printStackTrace();
		}
		
		return versionName;
	}
    
	public static String getChannel(Context cx) {
		String channel = null;
		
		// TODO: maybe use this method
//		channel = MainifestKey.getString(cx, "BaiduMobAd_CHANNEL");
//		if (null == channel || "null".equals(channel)) {
//			channel = "unknown";
//		}
		channel = getFromAssets(cx, "channel");
		if ("".equals(channel)) {
			channel = "unknown";
		}
		
		return channel.trim();
	}
	
	public static String getFromAssets(Context cx, String fileName) {
		StringBuilder builder = new StringBuilder();
		
		InputStreamReader isReader = null;
		BufferedReader bufferedReader = null;
		try {
			if(cx!=null){
				isReader = new InputStreamReader(cx.getResources().getAssets().open(fileName));
				bufferedReader = new BufferedReader(isReader);
				String line = null;
				while (null != (line = bufferedReader.readLine())) {
					builder.append(line);
				}
			}
		} catch (IOException e) {
			builder.setLength(0);
//			e.printStackTrace();
		} finally {
			try {
				if (null != isReader) {
					isReader.close();
				}
				
				if (null != bufferedReader) {
					bufferedReader.close();
				}
			} catch (IOException e) {
				builder.setLength(0);
//				e.printStackTrace();
			}
		}
		return builder.toString();
	}
	
	@Deprecated
	public static boolean isNV21Supported(Camera camera) {
		boolean result = false;
		
		if (null != camera) {
			Parameters parameter = camera.getParameters();
			if (null != parameter) {
				try {
					List<Integer> previewFormatList = parameter.getSupportedPreviewFormats();
					if (null != previewFormatList && previewFormatList.size() > 0
							&& previewFormatList.contains(ImageFormat.NV21)) {
						result = true;
					}
				} catch (NullPointerException e) {
//					e.printStackTrace();
				}
			}
		}
		
		return result;
	}
	
	public static boolean isNV21Supported(Parameters parameters) {
		boolean result = false;
		
		if (null != parameters) {
			try {
				List<Integer> previewFormatList = parameters.getSupportedPreviewFormats();
				if (null != previewFormatList && previewFormatList.size() > 0
						&& previewFormatList.contains(ImageFormat.NV21)) {
					result = true;
				}
			} catch (NullPointerException e) {
//				e.printStackTrace();
			}
		}
		
		return result;
	}
	
//	@Deprecated
//	public static boolean isFrontCameraDefault(Context cx) {
//		return CameraPreferences.getBoolean(cx, CameraPreferences.KEY_SETTINGS_FRONT_CAMERA_DEFAULT);
//	}
//
//	@Deprecated
//	public static void saveFrontCamera(Context cx, boolean front) {
//		CameraPreferences.save(cx, CameraPreferences.KEY_SETTINGS_FRONT_CAMERA_DEFAULT, front);
//	}
//
//	@Deprecated
//	public static void validateDefaultCameraType(Context cx) {
//		if (isFrontCameraDefault(cx) && !hasFrontCamera()) {
//			saveFrontCamera(cx, false);
//		}
//	}
	
	/**
	 * 判断两个矩形之间是否有交集（相交25%以上）
	 * 
	 * @param r	-	矩形1
	 * @param rect	-	矩形2
	 * @return
	 */
	public static boolean hasIntersection(Rect r, Rect rect) {
		if (null == r || null == rect) {
			return false;
		}
		
		int diffX = Math.abs(rect.centerX() - r.centerX());
		int diffY = Math.abs(rect.centerY() - r.centerY());
		boolean result = diffX <= Math.abs(rect.width() / 4 + r.width() / 4)
				&& diffY <= Math.abs(rect.height() / 4 + r.height() / 4);
		
		return result;
	}	
	
	/**
	 * Get the size of surfaceview which will show camera preview data
	 * 
	 * @param screenPoint		-	The size of display screen, x always means the short side, y always means the long side
	 * @param previewSizeRatio	-	The ratio of preview size: 1d * previewSize.width / previewSize.height
	 * @param portrait			-	Whether is portrait orientation
	 * @return					-	The view size point: x means view width, y means view height
	 */
	public static Point getCameraPreviewViewSize(PointF screenPoint, double previewSizeRatio, boolean portrait) {
		Point preScreenSizePoint = new Point();
		preScreenSizePoint.x = (int) screenPoint.x;
		preScreenSizePoint.y = (int) screenPoint.y;
		double screenRatio = 0.0d;
		if (portrait) {
			screenRatio = 1d * screenPoint.y / screenPoint.x;
		} else {
			screenRatio = 1d * screenPoint.x / screenPoint.y;
		}
		
		if (previewSizeRatio > screenRatio) {
			preScreenSizePoint.x = (int) (screenPoint.y / previewSizeRatio);
		} else {
			preScreenSizePoint.y = (int) (1f * screenPoint.x * previewSizeRatio);
		}
		
		Point viewSizePoint = new Point();
		
		if (portrait) {
			viewSizePoint.x = preScreenSizePoint.x;
			viewSizePoint.y = preScreenSizePoint.y;
		} else {
			viewSizePoint.x = preScreenSizePoint.y;
			viewSizePoint.y = preScreenSizePoint.x;
		}
		
		return viewSizePoint;
	}

	public static boolean isSmallerThanIceCreamSandwich() {
		int sdkInt = Build.VERSION.SDK_INT;
//		if (sdkInt <= Build.VERSION_CODES.GINGERBREAD) {
//			return true;
//		} else if (sdkInt <= Build.VERSION_CODES.GINGERBREAD_MR1) {
//			return true;
//		} else if (sdkInt <= Build.VERSION_CODES.HONEYCOMB) {
//			return true;
//		} else if (sdkInt <= Build.VERSION_CODES.HONEYCOMB_MR1) {
//			return true;
//		} else if (sdkInt <= Build.VERSION_CODES.HONEYCOMB_MR2) {
//			return true;
//		} else {// equals to or greater than Build.VERSION_CODES.ICE_CREAM_SANDWICH, Android 4.0
//			return false;
//		}
		
		if (sdkInt <= ApiHelper.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return true;
		} else {
			return false;
		}
	}

//	
//	/**
//	 * 判断是否安装了某个APP
//	 * 比较耗时
//	 * @param cx
//	 * @param intent
//	 * @return
//	 */
//	public static boolean hasApplication(Context cx, Intent intent){  
//        PackageManager packageManager = cx.getPackageManager();  
//        //查询是否有该Intent的Activity  
//        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);  
//        //activities里面不为空就有，否则就没有  
//        return activities.size() > 0 ? true : false;  
//	}
//	
//	/**
//	 * 判断是否安装了某个APP
//	 * 比较耗时
//	 * @param cx
//	 * @param packageName
//	 * @return
//	 */
//	public static boolean hasApplication(Context cx, String packageName) {
//		if (null == packageName || "".equalsIgnoreCase(packageName)) {
//			throw new IllegalArgumentException("The argument [packageName] is null-string.");
//		}
//		
//		PackageManager packageManager = cx.getPackageManager();
//		List<PackageInfo> pkgList = packageManager.getInstalledPackages(0);
//		for (int i = 0; i < pkgList.size(); i++) {
//			PackageInfo pI = pkgList.get(i);
//			//根据安装的应用的包名判断
//			if (pI.packageName.equalsIgnoreCase(packageName)) {
//				return true;
//			}
//		}
//		 
//		return false;
//	}
	
	/*
	 * 判断本版信息
	 * 
	 * added by liuhao
	 */
	   public static boolean hasFroyo() {
	        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
	    }

	    public static boolean hasGingerbread() {
	        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
	    }

	    public static boolean hasHoneycomb() {
	        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	    }
}
