package com.zxing.scan.module;

import android.os.Build;

import com.zxing.scan.LogUtils;

public class CameraAttrs {

	public static int orientationBack = 0;
	public static int orientationFront = 0;
	public static int orientationBackCapture = 0;
	public static int orientationFrontCapture = 0;
	public static boolean flipBack = false;
	public static boolean flipFront = true;

	public static boolean initOrientation() {
		if (Build.MODEL.equalsIgnoreCase("m9")) {
			orientationBack = 0;
			return true;
		} else if (Build.MODEL.equalsIgnoreCase("vivo V1")) {
			orientationBack = 0;
			return true;
		} else if (Build.MODEL.equalsIgnoreCase("GT-S5830i")) {
			orientationBack = 0;
			return true;
		}
		return false;
	}

	public static boolean supportLiveFilter() {
//		if (!TextUtils.isEmpty(Build.HARDWARE)
//				&& Build.HARDWARE.toLowerCase().startsWith("mt65")) {
//			return false;
//		}
//		if (!TextUtils.isEmpty(Build.DEVICE)
//				&& Build.DEVICE.startsWith("HM")) { //红米和亿通因为onPreviewFrame返回慢，所以不能使用实时滤镜。但SurfaceTexture方式的实时滤镜应该没有问题。
//			return false;
//		} else if (Build.MODEL.equalsIgnoreCase("ETON I6")) {
//			return false;
//		} else 
		if ("CT972 Q·Cosy".equalsIgnoreCase(Build.MODEL)) {
			return false;
		} else if ("V975 Core4".equalsIgnoreCase(Build.MODEL)) {
			return false;
		} else if ("N821".equalsIgnoreCase(Build.MODEL)) {
			return false;
		} else if ("IPH-800".equalsIgnoreCase(Build.MODEL)) {
			return false;
		} else if ("AMOI N821".equalsIgnoreCase(Build.MODEL)) {
			return false;
		} else if ("K8GT_H".equalsIgnoreCase(Build.MODEL)) {
			return false;
		} else if ("Xperia SL (LT26ii)".equalsIgnoreCase(Build.MODEL)) {
			return false;
		} else if ("S-F16".equalsIgnoreCase(Build.MODEL)) {
			return false;
		} else if ("T329w".equalsIgnoreCase(Build.MODEL)) {
			return false;
		}
		return true;
	}

	public static boolean supportSurfaceTexture() {
		if (isGoogleNexus5()) {
			return false;
		} else if ("Galaxy Nexus".equalsIgnoreCase(Build.MODEL)) {
			return false;
		} else if ("GT-i9260".equalsIgnoreCase(Build.MODEL)) {//Galaxy Nexus 升级版
			return false;
		} else if ("HTC 603e".equalsIgnoreCase(Build.MODEL)) {
			return false;
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * On this device, we should not use setPreviewCallback.
	 * Otherwise, the preview will be dark in low light conditions.
	 * @return
	 */
	public static boolean isGoogleNexus5() {
		return "Nexus 5".equalsIgnoreCase(Build.MODEL);
	}
	
	public static boolean supportContinueAutoFocus() {
		if (Build.MODEL.equalsIgnoreCase("HUAWEI T8951")) {
			return false;
		} else if (Build.MODEL.equalsIgnoreCase("NEXUS ONE")) {
			return false;
		} else if (Build.MODEL.equalsIgnoreCase("GT-N7100")) {
			return false;
		} 
		return true;
	}

	/**
	 * There is no need to start preview after taking pictures.
	 * So far, this situation is only found on some Samsung devices and the camera is back.
	 * <Br />
	 * Also on these devices, if the camera is front and we use setPreviewCallbackWithBuffer()
	 * to set a PreviewCallback instance to Camera Device, the preview data will be never pushed
	 * in onPreviewFrame(byte[] data, Camera camera) method after taking a picture. 
	 */
	public static boolean HAS_START_PREVIEW_AFTER_TAKE_PICTURE = canStartPreviewAfterTakePicture();

	private static boolean canStartPreviewAfterTakePicture() {
		if (Build.MODEL.equals("GT-I9300")) {
			return false;
		} else if (Build.MODEL.equals("GT-N7100")) {
			return false;
		} else if (Build.MODEL.equals("GT-N7102")) {
			return false;
		} else if (Build.MODEL.equals("GT-N7108")) {
			return false;
		} else if (Build.MODEL.equals("SCH-N719")) {
			return false;
		} else {
			return true;
		}
	}

	public static boolean canInterruptFaceDetection() {
		return canStartPreviewAfterTakePicture();
	}

	public static boolean shouldGCBeforeTakePicture() {
		if (Build.MODEL.equals("MI-ONE Plus")) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean supportSmooth() {
		if ("HUAWEI MT1-T00".equalsIgnoreCase(Build.MODEL)) {
			return false;
		}
		return true;
	}

	public static boolean supportSetPreviewAndPictureSize() {
		String model = Build.MODEL;
		if (null == model || "CoolPAD8190".equalsIgnoreCase(model.trim())) {
			return false;
		}

		return true;
	}

	/**
	 * 此列表内的低端机支持设置PreviewSize和PictuerSize
	 * 
	 * @return
	 */
	public static boolean isLowLevelButSupportSetPreviewSizeAndPictureSize() {
		if ("HTC Desire S".equalsIgnoreCase(Build.MODEL)
				|| "HTC Sensation XL with Beats Audio X315e"
						.equalsIgnoreCase(Build.MODEL)) {
			return true;
		}

		return false;
	}

	public static boolean supportFaceDetection() {
		LogUtils.e("CameraAttrs", "Model-->" + Build.MODEL);// K723

		return true;
	}

	public static boolean shouldSwitchToFlashOffBeforeAutoFocusInNormalMode() {
		if ("Nexus 4".equals(Build.MODEL)) {
			return true;
		}

		return false;
	}
	
	public static boolean supportLargeThumbSize() {
		if (Build.MODEL.equalsIgnoreCase("GT-I9500")) {
			return true;
		} else if (Build.MODEL.equalsIgnoreCase("GT-I9502")) {
			return true;
		} else if (Build.MODEL.equalsIgnoreCase("GT-I9508")) {
			return true;
		} else if (Build.MODEL.equalsIgnoreCase("GT-I959")) {
			return true;
		} else if (Build.MODEL.equalsIgnoreCase("GT-N7100")) {
			return true;
		} else if (Build.MODEL.equalsIgnoreCase("GT-N7102")) {
			return true;
		} else if (Build.MODEL.equalsIgnoreCase("GT-N7108")) {
			return true;
		} else if (Build.MODEL.equalsIgnoreCase("GT-N9006")) {
			return true;
		} else if (Build.MODEL.equalsIgnoreCase("GT-N9002")) {
			return true;
		} else if (Build.MODEL.equalsIgnoreCase("GT-N9008")) {
			return true;
		} else if (Build.MODEL.equalsIgnoreCase("GT-N9009")) {
			return true;
		} else if (Build.MODEL.equalsIgnoreCase("Nexus 4")) {
			return true;
		} else if (Build.MODEL.equalsIgnoreCase("Nexus 5")) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * This method return the offset between normal device and special one. You
	 * should use it in onOrientationChanged(int orientation) method.
	 * {@link android.view.OrientationEventListener} For example:
	 * 
	 * <pre>
	 * private class MyOrientationEventListener extends OrientationEventListener {
	 * 
	 * 	private int mOrientation;
	 * 
	 * 	public MyOrientationEventListener(Context context) {
	 * 		super(context);
	 * 	}
	 * 
	 * 	&#064;Override
	 * 	public void onOrientationChanged(int orientation) {
	 * 		if (orientation == ORIENTATION_UNKNOWN)
	 * 			return;
	 * 
	 * 		mOrientation = orientation - CameraAttrs.getOrientationOffset();
	 * 	}
	 * }
	 * </pre>
	 * 
	 * @return
	 */
	public static int getOrientationOffset() {
		if ("SM-P900".equalsIgnoreCase(Build.MODEL)) {
			return 90;
		} else {
			return 0;
		}
	}
}
