package com.zxing.scan;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.zxing.scan.module.CameraAttrs;

/**
 * 
 * @author zhaozheng01
 * 
 */
public class CameraRotationUtils {

	private static final String PREF_PREVIEW_ROTATION_FRONT = "pref_preview_rotation_front";
	private static final String PREF_PREVIEW_ROTATION_BACK = "pref_preview_rotation_back";
	private static final String PREF_PICTURE_ROTATION_FRONT = "pref_picture_rotation_front";
	private static final String PREF_PICTURE_ROTATION_BACK = "pref_picture_rotation_back";
	
	/**
	 * 
	 * @param front is the front camera active?
	 * @return rotation degree for Camera.setDisplayOrientation
	 */
	public static int getPreviewRotation(Context cx, boolean front) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(cx);
		if (front) {
			int frontRot = sp.getInt(PREF_PREVIEW_ROTATION_FRONT, -1);
			if (frontRot != -1) {
				return frontRot;
			}
		} else {
			int backRot = sp.getInt(PREF_PREVIEW_ROTATION_BACK, -1);
			if (backRot != -1) {
				return backRot;
			}
		}
		int displayOrientation = 90;
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
				int cid = 0;
				int degrees = 0 - CameraAttrs.getOrientationOffset();
				cid = Utils.getCameraId(front);
				if (cid != -1) {
					displayOrientation = Utils.getDisplayOrientation(degrees, cid);
					LogUtils.e("getPreviewRotation", "displayOrientation==" + displayOrientation);
				}
			}
			if (CameraAttrs.initOrientation()) {
				displayOrientation = CameraAttrs.orientationBack;

				LogUtils.e("getPreviewRotation", "initOrientation==" + displayOrientation);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		LogUtils.e("getPreviewRotation", "getPreviewRotation==" + displayOrientation);
		return displayOrientation;
	}

	/**
	 * 
	 * @param front
	 *            is the front camera active?
	 * @param screenDirection
	 *            the screen direction, that is: ((orientation + 45) % 360) /
	 *            90. This orientation generated from OrientationEventListener
	 * @param previewRotation
	 * @return
	 */
	public static int getPictureRotation(Context cx, boolean front, int screenDirection,
			int previewRotation) {
		LogUtils.e("getPictureRotation", "front[" + front + "]--screenDirection[" + screenDirection + "]--previewRotation[" + previewRotation + "]");
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(cx);
		int rot;
		if (front) {
			rot = sp.getInt(PREF_PICTURE_ROTATION_FRONT, 0);
		} else {
			rot = sp.getInt(PREF_PICTURE_ROTATION_BACK, 0);
		}
		boolean cameraFilp;
		if (front) {
			cameraFilp = CameraAttrs.flipFront;
		} else {
			cameraFilp = CameraAttrs.flipBack;
		}
		LogUtils.e("getPictureRotation", "rot[" + rot + "]");
		int direction = (screenDirection + rot - previewRotation / 90 + 5) % 4;
		LogUtils.e("getPictureRotation", "init direction[" + direction + "]");
		int pictureOrientation = 0;
		if (cameraFilp) {
			// Add by dyj to explain an alternative expression
			// Here we can replace (4 - direction + 3) with (3 - direction)
			pictureOrientation = (4 - direction + 3) * 90 % 360;
		} else {
			pictureOrientation = (direction + 1) * 90 % 360;
		}
		LogUtils.e("getPictureRotation", "result pictureOrientation[" + pictureOrientation + "]");
		
		return pictureOrientation;
	}
	
	public static void savePreviewRotation(Context cx, boolean front, int rot) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(cx);
		if (front) {
			sp.edit().putInt(PREF_PREVIEW_ROTATION_FRONT, rot).commit();
		} else {
			sp.edit().putInt(PREF_PREVIEW_ROTATION_BACK, rot).commit();
		}
	}

	public static void savePictureRotation(Context cx, boolean front, int rot) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(cx);
		if (front) {
			sp.edit().putInt(PREF_PICTURE_ROTATION_FRONT, rot).commit();
		} else {
			sp.edit().putInt(PREF_PICTURE_ROTATION_BACK, rot).commit();
		}
	}
	
	public static int getSavedPictureRotation(Context cx, boolean front) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(cx);
		if (front) {
			return sp.getInt(PREF_PICTURE_ROTATION_FRONT, 0);
		} else {
			return sp.getInt(PREF_PICTURE_ROTATION_BACK, 0);
		}
	}
}
