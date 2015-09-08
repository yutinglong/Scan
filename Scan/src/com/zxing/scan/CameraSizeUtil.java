package com.zxing.scan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import android.hardware.Camera.Size;
import android.os.Build;

/**
 * @Copyright(C) 2013 Baidu.Tech.Co.Ltd. All rights reserved.
 * @Author:Nodin
 * @Description:
 * 
 * @Version:1.0
 * @Update:
 * 
 */
public class CameraSizeUtil {
	private static final double TEN_THOUSAND = 10 * 1000;
	private static final double ONE_HUNDRED_THOUSAND = 100 * 1000;
	private static final double MILLION = 100 * TEN_THOUSAND;
	
	public static final int is43 = 1;
	public static final int is169 = 2;
	
	/**
	 * 获取有效像素数，以“万像素”为单位
	 * @param size
	 * @return
	 */
	public static int getEffectivePixels(CameraSize size) {
		int effectivePixels = 0;
		
		int pixels = size.width * size.height;
		double unit = 0.0d;
		if (pixels < ONE_HUNDRED_THOUSAND) {// 万像素
			unit = TEN_THOUSAND;
		} else if (pixels < MILLION) {// 十万像素
			unit = ONE_HUNDRED_THOUSAND;
		} else if (pixels < 5 * MILLION) {// 百万像素，500万以下
			unit = ONE_HUNDRED_THOUSAND;
		} else {// 百万像素，500万以上
			unit = MILLION;
		}
		
		effectivePixels = (int) Math.round(pixels / unit);
		
		return (int) (effectivePixels * unit / TEN_THOUSAND);
	}

    // Splits a comma delimited string to an ArrayList of Size.
    // Return null if the passing string is null or the size is 0.
    private static ArrayList<CameraSize> splitSize(String str) {
        ArrayList<CameraSize> sizeList = new ArrayList<CameraSize>();
        
        if (str == null) return sizeList;

        StringTokenizer tokenizer = new StringTokenizer(str, ",");
        while (tokenizer.hasMoreElements()) {
            CameraSize size = strToSize(tokenizer.nextToken());
            if (size != null) sizeList.add(size);
        }
        
        if (sizeList.size() == 0) return sizeList;
        
        return sizeList;
    }

    // Parses a string (ex: "480x320") to Size object.
    // Return null if the passing string is null.
    public static CameraSize strToSize(String str) {
        if (str == null) return null;

        int pos = str.indexOf('x');
        if (pos != -1) {
            String width = str.substring(0, pos);
            String height = str.substring(pos + 1);
            return new CameraSize(Integer.parseInt(width),
                            Integer.parseInt(height));
        }
        return null;
    }

	public static List<CameraSize> str2List(String sizeListString) {
//		List<Size> sizes = new ArrayList<Size>();
//		
//		String[] items = sizeListString.split(",");
//		for (String  item : items) {
//			String[] sizeArray = item.split("x");
//			sizes.add(new Size(Integer.valueOf(sizeArray[0]),
//					Integer.valueOf(sizeArray[1])));
//		}
//		
//		return sizes;
		return splitSize(sizeListString);
	}
	
	public static String listCameraSize2Str(List<CameraSize> sizes) {
		if (null == sizes || sizes.size() == 0) {
			return null;
		}
		
		StringBuilder builder = new StringBuilder();
		
		for (CameraSize size : sizes) {
			builder.append(size.width).append("x").append(size.height).append(",");
		}
		int index = builder.length() - 1;
		if (index >= 0) {
			builder.deleteCharAt(index);
		}
		return builder.toString();
	}

	public static String listSize2Str(List<Size> sizes) {
		if (null == sizes || sizes.size() == 0) {
			return null;
		}
		
		StringBuilder builder = new StringBuilder();
		
		for (Size size : sizes) {
			builder.append(size.width).append("x").append(size.height).append(",");
		}
		int index = builder.length() - 1;
		if (index >= 0) {
			builder.deleteCharAt(index);
		}
		return builder.toString();
	}
	
	/**
	 * 过滤Size，获取4:3及16:9的尺寸,并且4.0系统前置640以上，后置320以上
	 * @param frontCamera 是否为前置摄像头
	 * @param allSupported 需要过滤的Size列表
	 * @return
	 */
	public static List<CameraSize> getSupportedSize(boolean frontCamera, List<Size> allSupported){
		if (null == allSupported || allSupported.size() == 0) {
			return null;
		}
		
        Set<CameraSize> optionals = new TreeSet<CameraSize>(new Comparator<CameraSize>() {
			@Override
			public int compare(CameraSize lhs, CameraSize rhs) {
				return rhs.getPixels() - lhs.getPixels();
			}
		});
        
        boolean noLessThanIceCreamSandwich = Build.VERSION.SDK_INT >= ApiHelper.VERSION_CODES.ICE_CREAM_SANDWICH;
		for (Size size : allSupported) {
			double ratio = (double) 1d * size.width / size.height;
			if (Math.abs(ratio - CameraSize.RATIO_4T3) > CameraSize.ASPECT_TOLERANCE
            		&& Math.abs(ratio - CameraSize.RATIO_16T9) > CameraSize.ASPECT_TOLERANCE) {
            	continue;
            }
			
			if(noLessThanIceCreamSandwich && !frontCamera){// 4.0以上系统、后置
				if(size.width >= 640){
					// 只获取4:3及16:9的尺寸
					optionals.add(new CameraSize(size.width, size.height));
				}
			}
			else{
				if(size.width > 320){
					// 只获取4:3及16:9的尺寸
					optionals.add(new CameraSize(size.width, size.height));
				}
			}
		}

		List<CameraSize> optionalSizes = new ArrayList<CameraSize>(optionals);
		
		return optionalSizes;
	}
	
	public static CameraSize getLargestCameraSize(List<CameraSize> list, double targetRatio) {
		if (null == list || list.size() == 0) {
			return null;
		}
		
		final double defaultRatio = 4d / 3d;
		final double defaultRatio16to9 = 16d / 9d;
		
		Set<CameraSize> optionals = new TreeSet<CameraSize>(new Comparator<CameraSize>() {
			@Override
			public int compare(CameraSize lhs, CameraSize rhs) {
				return rhs.getPixels() - lhs.getPixels();
			}
		});
		optionals.addAll(list);
		List<CameraSize> optionalSizes = new ArrayList<CameraSize>(optionals);
		
		CameraSize defaultSize = null;
		for (CameraSize cameraSize : optionalSizes) {
			if (Math.abs(targetRatio - cameraSize.getRatio()) < CameraSize.ASPECT_TOLERANCE) {
				return cameraSize;
			} else if (null == defaultSize
					&& Math.abs(defaultRatio - cameraSize.getRatio()) < CameraSize.ASPECT_TOLERANCE) {
				defaultSize = cameraSize;
			} else if (null == defaultSize
					&& Math.abs(defaultRatio16to9 - cameraSize.getRatio()) < CameraSize.ASPECT_TOLERANCE) {
				defaultSize = cameraSize;
			}
		}
		
		return defaultSize;
	}
	
	public static int is43or169(CameraSize size){
		if(size == null){
			return -1;
		}
        
        double ratio = size.getRatio();
		if (Math.abs(ratio - CameraSize.RATIO_4T3) < CameraSize.ASPECT_TOLERANCE) {
			return is43;
        }
		else if(Math.abs(ratio - CameraSize.RATIO_16T9) < CameraSize.ASPECT_TOLERANCE){
			return is169;
		}
		return -1;
	}
	
	public static boolean is4t3(CameraSize size) {
		return is43or169(size) == is43;
	}

	public static boolean is4t3(Size size) {
		return is43or169(new CameraSize(size.width, size.height)) == is43;
	}
	
	public static boolean is16t9(Size size) {
		return is43or169(new CameraSize(size.width, size.height)) == is169;
	}
	
	public static boolean is16t9(CameraSize size) {
		return is43or169(size) == is169;
	}
	
	public static boolean isEqual(Size size, CameraSize cameraSize) {
		if (null == size && null == cameraSize) {
			return true;
		} else if (null != size && null != cameraSize) {
			return (size.width == cameraSize.width && size.height == cameraSize.height);
		} else {
			return false;
		}
	}
	
	public static boolean isValidInstanceOfCameraSize(CameraSize obj) {
		if (null != obj && obj.width != 0 && obj.height != 0) {
			return true;
		}
		
		return false;
	}
}
