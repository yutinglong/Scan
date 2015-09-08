package com.zxing.scan;

import android.content.Context;
import android.hardware.Camera.Size;

/**
 * @Copyright(C) 2012 Nodin Corporation. All Rights Reserved.
 * @Filename: Size.java Created On 2013年9月26日
 * @Author:dyj
 * @Description:
 * 
 * @Version:1.0
 * @Update:
 */

/**
 * Image size (width and height dimensions).
 */
public class CameraSize {
	public static final double ASPECT_TOLERANCE = 0.020d;
	public final static double RATIO_4T3 = 4d / 3d;
	public final static double RATIO_16T9 = 16d / 9d;
    
	/** width of the picture */
    public int width;
    /** height of the picture */
    public int height;
    
    /** effective pixel,  add by yutinglong*/
    public int effectivePixels;
    
    /**
     * Sets the dimensions for pictures.
     *
     * @param w the photo width (pixels)
     * @param h the photo height (pixels)
     */
    public CameraSize(int w, int h) {
        width = w;
        height = h;
    }
    
    public void set(int w, int h) {
    	width = w;
    	height = h;
    }
    
    /**
     * Parse a CameraSize from Size.
     * 
     * @param size	-	android.hardware.Camera.Size
     * @return	-	An instance of CameraSize or null if the parameter is null.
     */
    public static CameraSize parseFromSize(Size size) {
    	if (null == size) {
			return null;
		}
    	
    	return new CameraSize(size.width, size.height);
    }
    
    public static CameraSize parseFromStr(String candidate) {
    	if (null == candidate) return null;
    	
    	int index = candidate.indexOf('x');
    	if (index == -1) return null;
    	int width = Integer.parseInt(candidate.substring(0, index));
    	int height = Integer.parseInt(candidate.substring(index + 1));
    	
    	return new CameraSize(width, height);
    }
    
    /**
     * Compares {@code obj} to this size.
     *
     * @param obj the object to compare this size with.
     * @return {@code true} if the width and height of {@code obj} is the
     *         same as those of this size. {@code false} otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CameraSize)) {
            return false;
        }
        CameraSize s = (CameraSize) obj;
        return width == s.width && height == s.height;
    }
    
    @Override
	public String toString() {
		return width + "x" + height + "";
	}
    
    public int getPixels() {
        return width * height;
    }
    
    /** effective pixel, for view, add by yutinglong*/
    public double getRatio() {
    	return (double) width / height;
    }
    
    public boolean is4T3Ratio() {
    	return is4T3Ratio(getRatio());
    }
    
    public static boolean is4T3Ratio(double ratio) {
    	return (Math.abs(RATIO_4T3 - ratio) <= ASPECT_TOLERANCE);
    }
    
    public boolean isValidate() {
    	return width > 0 && height > 0;
    }
}
