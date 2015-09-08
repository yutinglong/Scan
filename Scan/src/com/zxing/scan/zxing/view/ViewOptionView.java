package com.zxing.scan.zxing.view;

import java.util.Collection;
import java.util.HashSet;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.NinePatch;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import com.google.zxing.ResultPoint;
import com.zxing.scan.LogUtils;
import com.zxing.scan.R;

public final class ViewOptionView extends ImageView {

	private static final long ANIMATION_DELAY_LONG = 20L;
	private final Paint paint;
    private final int DELTA = 20;
    private int scannPos = -1;
    private final int bgColor;
    
	private final int maskColor;
	private final int frameColor;
	private final int laserColor;
	private Collection<ResultPoint> possibleResultPoints;
	private Bitmap bitmapLeftTop,bitmapLeftBottom,bitmapRightTop,bitmapRightBottom;
	private Drawable scannLine;
	private Rect framingRect;
	
	private boolean isHasChange = false;
	
	private State state;
	public static enum State {
		ONE, QR, OTHER
	}// 条形码、二维码、其他
	
	Paint textPaint;
	public ViewOptionView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		paint = new Paint();
		Resources resources = getResources();
		bgColor = resources.getColor(R.color.option_bg);
		maskColor = resources.getColor(R.color.viewfinder_mask);
		frameColor = resources.getColor(R.color.viewfinder_frame);
		laserColor = resources.getColor(R.color.viewfinder_laser);
		possibleResultPoints = new HashSet<ResultPoint>(5);
		bitmapLeftTop = BitmapFactory.decodeResource(getResources(), R.drawable.qr_left_top);
		bitmapLeftBottom = BitmapFactory.decodeResource(getResources(), R.drawable.qr_left_bottom);
		bitmapRightTop = BitmapFactory.decodeResource(getResources(), R.drawable.qr_right_top);
		bitmapRightBottom = BitmapFactory.decodeResource(getResources(), R.drawable.qr_right_bottom);
		scannLine = getResources().getDrawable(R.drawable.scan_line);
		
		textPaint = new Paint(); 
		DisplayMetrics dm = getResources().getDisplayMetrics();
	    float value = dm.scaledDensity;
	    textPaint.setTextSize(16 * value); 
	}

	@Override
	public void onDraw(Canvas canvas) {
		Rect frame = getFramingRect();
		if (frame == null) {
			return;
		}
		int width = canvas.getWidth();
		int height = canvas.getHeight();
		
		paint.setColor(bgColor);
		canvas.drawRect(0, 0, width, height, paint);
		
		super.onDraw(canvas);

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

		drawNinepath(canvas, R.drawable.zxing_search_bg, frame);
		
//		Paint p = new Paint();
//		canvas.drawBitmap(bitmapLeftTop, frame.left, frame.top, p);
//		canvas.drawBitmap(bitmapRightTop, frame.right-bitmapRightTop.getWidth()+1, frame.top, p);
//		canvas.drawBitmap(bitmapLeftBottom, frame.left, frame.bottom-bitmapLeftBottom.getHeight()+1, p);
//		canvas.drawBitmap(bitmapRightBottom, frame.right-bitmapRightBottom.getWidth()+2, frame.bottom-bitmapRightBottom.getHeight()+1, p);
		
		paint.setColor(laserColor);
		
		if(scannPos==-1){
			scannPos = frame.top+DELTA;
		}
		scannLine.setBounds(frame.left + 2, scannPos -scannLine.getIntrinsicHeight()/2 , frame.right - 1,
				scannPos + scannLine.getIntrinsicHeight()/2);
		scannLine.draw(canvas);
		scannPos +=10;
		if(scannPos>=frame.bottom - DELTA){
			scannPos = frame.top+DELTA;
		}
		
		if(this.isShown()){
			postInvalidateDelayed(ANIMATION_DELAY_LONG, frame.left, frame.top,
					frame.right, frame.bottom);
		}
	}

	public void drawViewOption() {
		isHasChange = true;
		invalidate();
	}

	public void addPossibleResultPoint(ResultPoint point) {
		possibleResultPoints.add(point);
	}
	
	public Rect getFramingRect() {
		//Point screenResolution = configManager.getScreenResolution();
		if (framingRect == null || isHasChange) {
			int width = getWidth();
			int height = getHeight();
			
			LogUtils.d("YTL", "ViewOptionView  width : height = " + width + ":" + height);
			int leftOffset = 0;
			int topOffset = 0;
			framingRect = new Rect(leftOffset, topOffset, leftOffset + width,
					topOffset + height);
			isHasChange = false;
		}
		return framingRect;
	}
	
	public void setState(State state){
		this.state = state;
		isHasChange = true;
		scannPos = -1;
	}
	
	
	
	private void drawNinepath(Canvas c, int id, Rect r1){
		Bitmap bmp= BitmapFactory.decodeResource(getResources(), id);
		NinePatch patch = new NinePatch(bmp, bmp.getNinePatchChunk(), null);
		patch.draw(c, r1);
	}
}
