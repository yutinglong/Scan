package com.zxing.scan.zxing.view;

import java.util.Collection;
import java.util.HashSet;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetrics;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import com.google.zxing.ResultPoint;
import com.zxing.scan.LogUtils;
import com.zxing.scan.R;
import com.zxing.scan.zxing.CaptureActivity.ModelEnum;

public final class ViewfinderView extends View {

	private static final int[] SCANNER_ALPHA = { 0, 64, 128, 192, 255, 192,
			128, 64 };
	private static final long ANIMATION_DELAY = 12L;
	private static final long ANIMATION_DELAY_LONG = 200L;
	private static final int OPAQUE = 0xFF;
	private static final int MIN_FRAME_WIDTH = 240;
//	private static final int MIN_FRAME_HEIGHT = 240;
	private static final int MAX_FRAME_WIDTH = 600;
	private final Paint paint;
    private final int DELTA = 20;
    private int scannPos = -1;
    private final int netBgColor;
    
	private final int maskColor;
	private final int frameColor;
	private final int laserColor;
	private final int resultPointColor;
	private int scannerAlpha;
	private Collection<ResultPoint> possibleResultPoints;
	private Collection<ResultPoint> lastPossibleResultPoints;
	private Bitmap bitmapLeftTop,bitmapLeftBottom,bitmapRightTop,bitmapRightBottom;
	private Drawable scannLine;
	private Rect framingRect;
	private String scannTipTop;
	private String netErrorTip;// 网络状态提示
	
	private String noResultTip;	// 未扫描到结果
	private String noResultTip2;// 轻触屏幕继续
	
	private String scannTip;
//	private String scannTip2;// 翻译时使用
	
	private boolean isShowScannLine = false;// 是否显示扫描线
	private boolean isHasChange = false;
	private boolean isNetOK = true;// 当前网络是否可用
	private boolean isNoResult = false;// 无扫描结果的提示
	private boolean isDrawAll = false;
	
	private ModelEnum currentState;
	Paint textPaint;
	TextPaint textPaintL;
	private Handler viewHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			postInvalidate();
			super.handleMessage(msg);
		}
	};
	
	public ViewfinderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		paint = new Paint();
		Resources resources = getResources();
		netBgColor = resources.getColor(R.color.black_alpha_85);
		maskColor = resources.getColor(R.color.viewfinder_mask);
		frameColor = resources.getColor(R.color.viewfinder_frame);
		laserColor = resources.getColor(R.color.viewfinder_laser);
		resultPointColor = resources.getColor(R.color.possible_result_points);
		scannerAlpha = 0;
		possibleResultPoints = new HashSet<ResultPoint>(5);
		bitmapLeftTop = BitmapFactory.decodeResource(getResources(), R.drawable.qr_left_top);
		bitmapLeftBottom = BitmapFactory.decodeResource(getResources(), R.drawable.qr_left_bottom);
		bitmapRightTop = BitmapFactory.decodeResource(getResources(), R.drawable.qr_right_top);
		bitmapRightBottom = BitmapFactory.decodeResource(getResources(), R.drawable.qr_right_bottom);
		scannLine = getResources().getDrawable(R.drawable.scan_line);
		scannTip = getResources().getString(R.string.zxing_scan_tips);
		netErrorTip = getResources().getString(R.string.zxing_no_net_tip);
		
		noResultTip = getResources().getString(R.string.zxing_no_result_tip);
		noResultTip2 = getResources().getString(R.string.zxing_no_result_tip2);
		
		textPaint = new Paint(); 
		DisplayMetrics dm = getResources().getDisplayMetrics();
	    float value = dm.scaledDensity;
	    textPaint.setTextSize(16 * value); 
	    
		textPaintL = new TextPaint();
		textPaintL.setColor(0xf0ffffff); 
		textPaintL.setAntiAlias(true);
		textPaintL.setTextAlign(Align.CENTER); 
	    textPaintL.setTextSize(16 * value);
	}

	public void setScannTip(String scannTipTop){
		this.scannTipTop = scannTipTop;
	}
	
	public void setScannTip(String scannTip, String scannTip2){
		this.scannTip = scannTip + "\n\n" + scannTip2;
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		Rect frame = getFramingRect();
		if (frame == null) {
			return;
		}
		int width = canvas.getWidth();
		int height = canvas.getHeight();

		paint.setColor(maskColor);
		canvas.drawRect(0, 0, width, frame.top, paint);
		canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
		canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1,
				paint);
		canvas.drawRect(0, frame.bottom + 1, width, height, paint);

		paint.setColor(frameColor);
		canvas.drawRect(frame.left, frame.top, frame.right + 1, frame.top + 2,
				paint);
		
		canvas.drawRect(frame.left, frame.top + 2, frame.left + 2,
				frame.bottom - 1, paint);
		canvas.drawRect(frame.right - 1, frame.top, frame.right + 1,
				frame.bottom - 1, paint);
		canvas.drawRect(frame.left, frame.bottom - 1, frame.right + 1,
				frame.bottom + 1, paint);

		
		Paint p = new Paint();
		canvas.drawBitmap(bitmapLeftTop, frame.left, frame.top, p);
		canvas.drawBitmap(bitmapRightTop, frame.right-bitmapRightTop.getWidth()+1, frame.top, p);
		canvas.drawBitmap(bitmapLeftBottom, frame.left, frame.bottom-bitmapLeftBottom.getHeight()+1, p);
		canvas.drawBitmap(bitmapRightBottom, frame.right-bitmapRightBottom.getWidth()+2, frame.bottom-bitmapRightBottom.getHeight()+1, p);
		
		paint.setColor(laserColor);
		
		boolean isDrawNoResultTip = false;
		boolean isDrawNetTip = false;
		if(this.currentState != ModelEnum.BARCOD
				&& this.currentState != ModelEnum.TWO_BARCODE){
			if(!isNetOK){
				isDrawNetTip = true;
			}
		}

		if(isNoResult){
			isDrawNoResultTip = true;
		}
		
		
		if(isShowScannLine && !isDrawNetTip && !isDrawNoResultTip){
			if(scannPos==-1){
				scannPos = frame.top+DELTA;
			}
			scannLine.setBounds(frame.left + 2, scannPos -scannLine.getIntrinsicHeight()/2 , frame.right - 1,
					scannPos + scannLine.getIntrinsicHeight()/2);
			scannLine.draw(canvas);
			scannPos += 7;
			if(scannPos>=frame.bottom - DELTA){
				scannPos = frame.top+DELTA;
			}
			if(isDrawAll){
				viewHandler.sendEmptyMessageDelayed(1, ANIMATION_DELAY);
				isDrawAll = false;
			}
			else{
				postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top,
						frame.right, frame.bottom);
			}
		}
		else{
			if(isDrawAll){
				viewHandler.sendEmptyMessageDelayed(1, ANIMATION_DELAY_LONG);
				isDrawAll = false;
			}
			else{
				postInvalidateDelayed(ANIMATION_DELAY_LONG, frame.left, frame.top,
						frame.right, frame.bottom);
			}
		}

		textPaint.setColor(0xf0ffffff); 
		textPaint.setTextAlign(Align.CENTER); 
		textPaint.setAntiAlias(true);
		int y = frame.bottom + getHeight()/20;
		int y2 = frame.top - getHeight()/20;
		
//		FontMetrics fm = textPaint.getFontMetrics();  
//		canvas.drawText(scannTip, getWidth()/2, y, textPaint);
//		int textHeight = (int) (Math.ceil(fm.descent - fm.ascent) + 2);
//		canvas.drawText(scannTip2, getWidth()/2, y + textHeight, textPaint);
		
		canvas.save();
		StaticLayout layout = new StaticLayout(scannTip, textPaintL, (getWidth() - 100), Alignment.ALIGN_NORMAL, 1.0F, 0.0F, true); 
		canvas.translate(getWidth()/2, y);
		layout.draw(canvas);
		canvas.restore();
		
		// 绘制上方tip
		canvas.drawText(scannTipTop, getWidth()/2, y2, textPaint);
		
		if(isDrawNetTip){
			paint.setColor(netBgColor);
			canvas.drawRect(0, 0, width, height, paint);
			
			// 网络状态提示
			canvas.drawText(netErrorTip, getWidth()/2, getHeight()/2, textPaint);
		}
		
		if(isDrawNoResultTip && !isDrawNetTip){
			paint.setColor(netBgColor);
			canvas.drawRect(0, 0, width, height, paint);
			
			// 超时提示
			FontMetrics fm = textPaint.getFontMetrics();  
			canvas.drawText(noResultTip, getWidth()/2, getHeight()/2, textPaint);
			int textHeight = (int) (Math.ceil(fm.descent - fm.ascent) + 2);
			canvas.drawText(noResultTip2, getWidth()/2, getHeight()/2 + textHeight, textPaint);
		}
	}

	public void drawViewfinder() {
		viewHandler.removeMessages(1);
		invalidate();
	}

	public void addPossibleResultPoint(ResultPoint point) {
		possibleResultPoints.add(point);
	}
	
	
	public Rect getFramingRect() {
		//Point screenResolution = configManager.getScreenResolution();
		if (framingRect == null || isHasChange) {
			
			int width = getWidth() * 3 / 4;
			if (width < MIN_FRAME_WIDTH) {
				width = MIN_FRAME_WIDTH;
			} else if (width > MAX_FRAME_WIDTH) {
				width = MAX_FRAME_WIDTH;
			}
			int height = getHeight() * 3 / 4;
			
			if(currentState == ModelEnum.BARCOD){
				// 条形码为方形的
				height = width * 3 / 4;
			}
			else if(currentState == ModelEnum.TWO_BARCODE){
				// 二维码为方形的
				height = width;	
			}
			else if(currentState == ModelEnum.WORD){
				// 文字识别
				width = getWidth() * 1 / 2;
				if (width < MIN_FRAME_WIDTH) {
					width = MIN_FRAME_WIDTH;
				} else if (width > MAX_FRAME_WIDTH) {
					width = MAX_FRAME_WIDTH;
				}
				height = width/4 + 10;
			}
			else if(currentState == ModelEnum.PET_DOG){// 宠物狗
				height = width * 4 / 3;
			}
			else {
				// 其他模式
				height = width;	
			}
			
			LogUtils.d("YTL", "ViewFinderView  width : height = " + width + ":" + height);
			int leftOffset = (getWidth() - width) / 2;
			int topOffset = (getHeight() - height) / 2;
			if(currentState == ModelEnum.WORD){
				topOffset = topOffset - getHeight()/10;
			}
			framingRect = new Rect(leftOffset, topOffset, leftOffset + width,
					topOffset + height);
			isHasChange = false;
		}
		return framingRect;
	}
	
	public void setState(ModelEnum state){
		this.currentState = state;
		isHasChange = true;
		isDrawAll = true;
		scannPos = -1;
		
		if(state == ModelEnum.BARCOD){
			this.scannTipTop = getResources().getString(R.string.zxing_scan_tips_one);
			isShowScannLine = true;
		}
		else if(state == ModelEnum.TWO_BARCODE){
			this.scannTipTop = getResources().getString(R.string.zxing_scan_tips);
			isShowScannLine = true;
		}
		else if(state == ModelEnum.BOOK_CD){
			this.scannTipTop = getResources().getString(R.string.zxing_scan_tips_book_cd);
			isShowScannLine = true;
		}
		else {
			this.scannTipTop = "";
			isShowScannLine = false;
		}
		
		scannTip = "";
//		scannTip2 = "";
	}
	
	public void setNetState(boolean isNetOK){
		this.isNetOK = isNetOK;
		this.isDrawAll = true;
	}
	
	public void setNoResult(boolean isNoResult){
		this.isNoResult = isNoResult;
		this.isDrawAll = true;
	}
	
	public boolean getNoResult(){
		return isNoResult;
	}
}
