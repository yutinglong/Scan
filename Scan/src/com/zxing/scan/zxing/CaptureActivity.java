package com.zxing.scan.zxing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.Window;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.zxing.scan.LogUtils;
import com.zxing.scan.R;
import com.zxing.scan.SubActivity2;
import com.zxing.scan.zxing.camera.CameraManager;
import com.zxing.scan.zxing.data.BookOrCDEntity;
import com.zxing.scan.zxing.decoding.CaptureActivityHandler;
import com.zxing.scan.zxing.decoding.DecodeThread;
import com.zxing.scan.zxing.view.ViewOptionView;
import com.zxing.scan.zxing.view.ViewfinderView;

/**
 * 扫一扫模块扫描拍照界面
 * 
 * @author yutinglong
 */
public class CaptureActivity extends Activity implements Callback, android.view.View.OnClickListener,OnItemClickListener{
	private CaptureActivityHandler handler;
	private ViewfinderView viewfinderView;
	private boolean hasSurface;
	private View zxing_input;
	private Vector<BarcodeFormat> decodeFormats;
	private String characterSet;
	private SurfaceView surfaceView;
	
	// 识别成功的提示声音相关
	private MediaPlayer mediaPlayer;
	private boolean playBeep;
	private static final float BEEP_VOLUME = 0.10f;

	private TextView qrBack;// 返回按钮
	private ImageView barcodeTorchIcon;
	
	private FrameLayout previewLayout;
	
	private ViewOptionView optionView;
	private RelativeLayout optionLayout;
	private Button cancelBtn;
	
	private boolean isCancel;
	private String lastDecodeWord;
	
	private static final int OPENGALLERY_REQUEST = 1;
	
	public static final int NET_IMAGE_SIZE = 200;
	public static final int WordDelayTime = 4000;
	public static final int DECODE_DELAY_TIME = 100000;
//	private static final long AUTOFOCUS_TIMEOUT_DELAY = 2000L;
//	private final Runnable mAutoFocusTimeOutRunnable = new SuperCameraAutoFocusTimeOutRunnable();
	
//	private MotionFocus mMotionFocus;
    // This handles everything about focus.
//    private FocusOverlayManager mFocusManager;
//    private FocusRenderer mFocusRenderer;
//    private boolean mFrontCamera;
//    private Parameters mInitialParams;
	
	// 当前用户选择的模式
	public ModelEnum currentModel = ModelEnum.TWO_BARCODE;
	public static enum ModelEnum {
		BARCOD, TWO_BARCODE, BOOK_CD, WORD, PET_DOG, STAR_FACE
	}// 条形码, 二维码, 图书CD, 文字, 宠物狗, 明星脸
	
	// 当前界面的显示状态
	private ShowStatus currentStatus;
	public static enum ShowStatus {
		PREVIEW, OPTION
	}// 取景状态, 操作状态(动画)

//	private HorizontalListView zxingActionGallery;
//	private ZxingOptionGalleryAdapter mGalleryAdapter;
	
	private BroadcastReceiver mReceiver;
	private IntentFilter mIntenFilter;
	public boolean netStatus = true;
	
	private Handler selectHandler = new Handler(){
		@Override
		public void handleMessage(Message message) {
			switch (message.what) {
			case R.id.decode_succeeded:// 本地图片识别，识别成功
				Bundle bundle = message.getData();
				Bitmap barcode = bundle == null ? null : (Bitmap) bundle.getParcelable(DecodeThread.BARCODE_BITMAP);
				handleDecode((Result) message.obj, barcode);
				break;
			case R.id.decode_book_cd_succeeded:// 本地图片识别，识别成功
				if(!isCancel){
					handleDecodeBookCD((ArrayList<BookOrCDEntity>) message.obj);	
				}
				break;
			case R.id.decode_failed:// 本地图片识别，识别失败
				if(!isCancel){
					if(currentStatus == ShowStatus.OPTION){
						quitOption();
					}
				}
				break;
			case R.id.change_type_ok:
				bottomSelectCanera();
				break;
			case R.id.decode_delay:// 识别一直失败的超时
				if(handler != null){
					LogUtils.d("YTL", "==========10秒扫描超时");
					selectHandler.removeMessages(R.id.decode_delay);
					handler.pauseDecode();
					
			    	if(viewfinderView != null){
			    		viewfinderView.setNoResult(true);
			    	}
			    	viewfinderView.invalidate();
			    	viewfinderView.setOnTouchListener(new OnTouchListener() {
						@Override
						public boolean onTouch(View v, MotionEvent event) {
							if(viewfinderView.getNoResult()){
								// 重新开始decode
								 handler.restartDecode();
								 viewfinderView.setNoResult(false);
								 if(selectHandler != null){
									selectHandler.sendEmptyMessageDelayed(R.id.decode_delay, CaptureActivity.DECODE_DELAY_TIME);	
								 }
							}
							return false;
						}
					});
				}
				break;
			}
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		setContentView(R.layout.capture_zxing_capture);
		// 初始化 CameraManager
		CameraManager.init(this);
		initView();
		hasSurface = false;
		
		
		changeTitle(currentModel);
		
//		mGestureDetector = new GestureDetector(this, new MyGestureListener());
//		mMotionFocus = new MotionFocus(this);
		
		// 网络状态变化监听
		mIntenFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);  
		mReceiver = new BroadcastReceiver() {  
		    @Override  
		    public void onReceive(Context context, Intent intent) {  
		    	netWorkChange();
		    }  
		};
		
		
		
		
//		new Thread(){
//			public void run(){
//				try {
//					Thread.sleep(10000);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//				
//				LogUtils.d("YTL", "十秒倒计时到");
//				
//				handler.pauseDecode();
//				handler.restartDecode();
//			}
//		}.start();
	}
	
	private void initView(){
		// 上层绘制框框的View
		viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
		zxing_input = findViewById(R.id.zxing_input);
		zxing_input.setOnClickListener(this);
		
//		zxingActionGallery = (HorizontalListView) findViewById(R.id.zxing_action_gallery);
//		mGalleryAdapter = new ZxingOptionGalleryAdapter(this);
//		zxingActionGallery.setAdapter(mGalleryAdapter);
//		zxingActionGallery.setOnItemClickListener(this);
		
		
		barcodeTorchIcon = (ImageView) findViewById(R.id.barcode_torch_icon);
		barcodeTorchIcon.setOnClickListener(this);
		
		qrBack = (TextView) findViewById(R.id.newmore_btn_back);
		qrBack.setOnClickListener(this);
		
		optionView = (ViewOptionView) findViewById(R.id.option_view);
		optionLayout = (RelativeLayout) findViewById(R.id.option_layout);
		
		previewLayout = (FrameLayout) findViewById(R.id.preview_layout);
		cancelBtn = (Button) findViewById(R.id.cancel_btn);
		cancelBtn.setOnClickListener(this);
		
		viewfinderView.setState(currentModel);
	}

	@Override
	protected void onResume() {
		super.onResume();
		
//		if (null != mMotionFocus) {
//			mMotionFocus.onResume(this);
//		}
		
		LogUtils.e("YTL", "CaptureZxingActivity on resume");
		surfaceView = (SurfaceView) findViewById(R.id.preview_view);
		
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (hasSurface) {
			LogUtils.e("YTL", "CaptureZxingActivity init camera in resume");
			if(optionLayout.getVisibility() != View.VISIBLE){
				initCamera(surfaceHolder);
			}
		} else {
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			surfaceHolder.addCallback(this);
		}
		decodeFormats = null;
		characterSet = null;

		playBeep = true;
		AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
		if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
			playBeep = false;
		}
		
		// 初始化成功的声音
		initBeepSound();
		
		isCancel = false;
		
		if(mReceiver != null && mIntenFilter != null){
			registerReceiver(mReceiver, mIntenFilter);	
		}
		
		netWorkChange();
	}

	@Override
	protected void onPause() {
		super.onPause();
		
//		if (null != mMotionFocus) {
//			mMotionFocus.onPause();
//		}
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		if(selectHandler != null){
			selectHandler.removeMessages(R.id.decode_delay);	
		}
		CameraManager.get().closeDriver();
		
//        if (mFocusManager != null) {
//        	mFocusManager.onCameraReleased();
//        }
		
		if(mReceiver != null){
			unregisterReceiver(mReceiver); 
		}
	}
	@Override
	protected void onDestroy() {
//		if (null != mFocusManager) {
//			mFocusManager.onDestory();
//			mFocusManager = null;
//		}
		super.onDestroy();
	}

	private void initCamera(SurfaceHolder surfaceHolder) {
		try {
			CameraManager.get().openDriver(surfaceHolder);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return;
		} catch (RuntimeException e) {
			e.printStackTrace();
			return;
		}
		if (handler == null) {
			handler = new CaptureActivityHandler(this, decodeFormats, characterSet);
			handler.changeModelType(currentModel);
		}
		
		if(selectHandler != null){
			selectHandler.sendEmptyMessageDelayed(R.id.decode_delay, CaptureActivity.DECODE_DELAY_TIME);	
		}
		
//		initializeFocusManager();
		
		
//        if (mFocusManager != null) {
//    		Size mSize = CameraManager.get().getPreviewSize();
//    		LogUtils.d("YTL", "mSize ====== " + mSize.width + ":" + mSize.height);
//    		mFocusManager.setPreviewSize(mSize.height, mSize.width);
//        	mFocusManager.onPreviewStarted();
//        }
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (!hasSurface) {
			hasSurface = true;
			LogUtils.e("YTL", "init camera in surface created");
			if(optionLayout.getVisibility() != View.VISIBLE){
				initCamera(holder);	
			}
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (requestCode == OPENGALLERY_REQUEST) {
			if (resultCode == RESULT_OK) {
				try {
					isCancel = false;
					Uri originalUri = intent.getData();
					try {
					} catch (OutOfMemoryError e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					}
				} catch (NullPointerException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * 刷新当前闪光灯状态的icon
	 */
	private void refreshTorchIcon(){
		if(CameraManager.get().isOpenFlash()){
			barcodeTorchIcon.setImageResource(R.drawable.barcode_torch_icon_on);
		}
		else{
			barcodeTorchIcon.setImageResource(R.drawable.barcode_torch_icon_off);
		}
	}
	
	public ViewfinderView getViewfinderView() {
		return viewfinderView;
	}

	public Handler getHandler() {
		return handler;
	}

	public void drawViewfinder() {
		viewfinderView.drawViewfinder();
	}

	// 条形码二维码识别成功的回调
	public void handleDecode(Result obj, Bitmap barcode) {
		if(currentModel != ModelEnum.BARCOD
				&& currentModel != ModelEnum.TWO_BARCODE){
			return;
		}
		playBeepSoundAndVibrate();

//		Intent intent = new Intent(this, SubActivity2.class);
//		intent.putExtra("result_data", obj.getText());
//		intent.putExtra("BarcodeFormat", obj.getBarcodeFormat().toString());
//		startActivity(intent);
		
		if(selectHandler != null){
			selectHandler.removeMessages(R.id.decode_delay);
			selectHandler.sendEmptyMessageDelayed(R.id.decode_delay, CaptureActivity.DECODE_DELAY_TIME);	
		}
		
		Log.d("YTL", "obj.getText()  == " + obj.getText());
		
		String resultString = obj.getText().toString();
        Intent sendIntent = new Intent();
        sendIntent.putExtra("resultString", resultString);
        setResult(RESULT_OK, sendIntent);
        finish();
	}
	
	public void handleDecodeBookCD(ArrayList<BookOrCDEntity> obj) {
		if(currentModel != ModelEnum.BOOK_CD){
			return;
		}
		playBeepSoundAndVibrate();

//		// 测试
//		obj.clear();
//		obj = new ArrayList<BookOrCDEntity>();
//		BookOrCDEntity mBookOrCDEntity = new BookOrCDEntity();
//		mBookOrCDEntity.netUrl = "http://www.baidu.com";
//		mBookOrCDEntity.name = "浪潮之巅";
//		mBookOrCDEntity.typeName = "图书";
//		
//		obj.add(mBookOrCDEntity);
//		mBookOrCDEntity = new BookOrCDEntity();
//		mBookOrCDEntity.netUrl = "http://music.baidu.com/album/8083758";
////		mBookOrCDEntity.netUrl = "http://music.baidu.com/#/!/album/8083758";
//		mBookOrCDEntity.name = "流浪汉";
//		mBookOrCDEntity.typeName = "CD";
//		obj.add(mBookOrCDEntity);
//		
//		mBookOrCDEntity = new BookOrCDEntity();
//		mBookOrCDEntity.netUrl = "http://music.baidu.com/#/!/album/8083758";
//		mBookOrCDEntity.name = "流浪汉(#!)";
//		mBookOrCDEntity.typeName = "CD";
//		obj.add(mBookOrCDEntity);
//		// 测试
		
		
	}
	
	
	/**
	 * 初始化识别成功的声音
	 */
	private void initBeepSound() {
		if (playBeep && mediaPlayer == null) {
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setOnCompletionListener(beepListener);

			try {
				AssetFileDescriptor file = getResources().getAssets().openFd("raw/beep.ogg");
				mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
				file.close();
				mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
				mediaPlayer.prepare();
			} catch (IOException e) {
				mediaPlayer = null;
			}
		}
	}

	/**
	 * 播放识别成功的提示音
	 */
	private void playBeepSoundAndVibrate() {
		if (playBeep && mediaPlayer != null) {
			mediaPlayer.start();
		}
	}

	/**
	 * 声音的回调，seek to 0
	 */
	private final OnCompletionListener beepListener = new OnCompletionListener() {
		public void onCompletion(MediaPlayer mediaPlayer) {
			if(mediaPlayer != null){
				mediaPlayer.seekTo(0);	
			}
		}
	};
	
	
//	@Override
//	public void finish() {
//		Intent data = new Intent();
//		data.putExtra("MODE_2D_CODE", true);
//		setResult(RESULT_OK, data);
//		super.finish();
//	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.newmore_btn_back:// 返回按钮
			finish();
			break;
		case R.id.barcode_torch_icon:// 闪光灯
			CameraManager.get().changeFlashZxing();
			refreshTorchIcon();
			break;
		case R.id.cancel_btn:// 操作动画页面的取消按钮
			isCancel = true;
			quitOption();
			break;
		case R.id.zxing_input:// 手动输入
			Intent data = new Intent();
			data.putExtra("MODE_2D_CODE", true);
			setResult(RESULT_OK, data);
			super.finish();
			break;
		}
	}

	private void onBottomSelect(ModelEnum mModel){
		selectHandler.removeMessages(R.id.decode_delay);
		if (selectHandler != null) {
			selectHandler.sendEmptyMessageDelayed(R.id.decode_delay, CaptureActivity.DECODE_DELAY_TIME);
		}
		if (viewfinderView != null && viewfinderView.getNoResult()) {
			// 重新开始decode
			handler.restartDecode();
			viewfinderView.setNoResult(false);
		}
		
		if(currentModel == mModel){
			return;
		}
		
		currentModel = mModel;
		
		if(currentModel == ModelEnum.TWO_BARCODE){
		}
		else if(currentModel == ModelEnum.BARCOD){
		}
		else if(currentModel == ModelEnum.BOOK_CD){
		}
		else if(currentModel == ModelEnum.WORD){
		}
		
		changeTitle(currentModel);
		
		selectHandler.sendEmptyMessage(R.id.change_type_ok);
	}
	
	private void bottomSelectCanera(){
		if (handler == null || viewfinderView == null) {
			return;
		}
		handler.removeMessages(R.id.restart_preview);

		viewfinderView.setState(currentModel);
		viewfinderView.drawViewfinder();
		handler.changeModelType(currentModel);
		CameraManager.get().setState(currentModel);
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
//		if(handler == null || mGalleryAdapter == null
//				|| viewfinderView == null || selectPhoto == null){
//			return;
//		}
//		handler.removeMessages(R.id.restart_preview);
//		
//		mGalleryAdapter.setSelectItem(position);
//		mGalleryAdapter.notifyDataSetChanged();
//		currentModel = mGalleryAdapter.getCurrentModel();
//		viewfinderView.setState(currentModel);
//		viewfinderView.drawViewfinder();
//		handler.changeModelType(currentModel);
//		CameraManager.get().setState(currentModel);
//		hideOrShowCapture(currentModel);
//		changeTitle(currentModel);
//		
//		if(currentModel == ModelEnum.WORD){// 翻译模式下，不能选图
//			selectPhoto.setVisibility(View.GONE);
//		}
//		else{
//			selectPhoto.setVisibility(View.VISIBLE);	
//		}
		
		
		// 对焦
//		CameraManager.get().requestAutoFocus(handler, R.id.auto_focus);
		
//		if(currentModel == ModelEnum.PET_DOG){
//			// 宠物狗模式下可以移动对焦
//			if (null != mMotionFocus) {
//				mMotionFocus.setEnable(true);
//			}
//		}
//		else{
//			if (null != mMotionFocus) {
//				mMotionFocus.setEnable(false);
//			}
//		}
	}
	
	private void changeTitle(ModelEnum currentModel){
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if(currentStatus == ShowStatus.OPTION){
				quitOption();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	
	/**
	 * 显示或者隐藏拍照按钮
	 * @param cModel
	 */
	private void hideOrShowCapture(ModelEnum cModel){
	}
	/**
	 * 显示界面操作布局
	 */
	private void showOptionLayout(){
		hidePreviewLayout();
		
		optionLayout.setVisibility(View.VISIBLE);
		optionView.drawViewOption();
		currentStatus = ShowStatus.OPTION;
	}
	
	/**
	 * 显示界面取景布局
	 */
	private void showPreviewLayout(){
		hideOptionLayout();
		
		previewLayout.setVisibility(View.VISIBLE);
		currentStatus = ShowStatus.PREVIEW;
	}
	
	private void hideOptionLayout(){
		optionLayout.setVisibility(View.GONE);
	}
	
	private void hidePreviewLayout(){
		previewLayout.setVisibility(View.GONE);
	}
	
	private void initOptrionLayout(Bitmap bitmap){
		optionView.setImageBitmap(bitmap);
		
		LayoutParams layoutParams = (LayoutParams) optionView.getLayoutParams();
		layoutParams.width = viewfinderView.getFramingRect().width();
		layoutParams.height = layoutParams.width * 4 / 3;
		optionView.setLayoutParams(layoutParams);
	}
	
	private void quitOption(){
		showPreviewLayout();
		
		if(surfaceView != null && handler == null){
			SurfaceHolder surfaceHolder = surfaceView.getHolder();
			if (hasSurface) {
				initCamera(surfaceHolder);	
			}
		}

		CameraManager.get().startPreview();
	}
	
	private void netWorkChange(){
    	if(viewfinderView != null){
    		viewfinderView.setNetState(netStatus);
    	}
	}
	
//	/** PictureCallBack */
//	private final class NormalPictureCallback implements PictureCallback {
//		public NormalPictureCallback() { }
//
//		@Override
//		public void onPictureTaken(byte[] data, Camera camera) {
//			LogUtils.i("YTL", "onPictureTaken_Normal");
//			
//			Bitmap resultBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
//			
//			initOptrionLayout(resultBitmap);
//			showOptionLayout();
//		}
//	}
	
	
	
//	@Override
//	public boolean onTouchEvent(MotionEvent event) {
//		return mGestureDetector.onTouchEvent(event);
//	}
//
//	private GestureDetector mGestureDetector; 
//	public class MyGestureListener extends SimpleOnGestureListener {
//		MyGestureListener() {
//		}
//
//		@Override
//		public boolean onDown(MotionEvent e) {
//			return false;
//		}
//
//		@Override
//		public void onShowPress(MotionEvent e) {
//		}
//
//		@Override
//		public boolean onSingleTapUp(MotionEvent e) {
//			LogUtils.e("YTL", "============= onSingleTapUp =============");
//			if(currentModel == ModelEnum.PET_DOG){
//				// 只有宠物狗才允许触屏对焦
//				mFocusManager.onSingleTapUp((int) e.getX(), (int) e.getY(), true, true);
//			}
//			return false;
//		}
//
//		@Override
//		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
//			return false;
//		}
//
//		@Override
//		public void onLongPress(MotionEvent e) {
//		}
//
//		@Override
//		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,float velocityY) {
//			return false;
//		}
//
//		@Override
//		public boolean onDoubleTap(MotionEvent e) {
//			return false;
//		}
//
//		@Override
//		public boolean onDoubleTapEvent(MotionEvent e) {
//			return false;
//		}
//
//		@Override
//		public boolean onSingleTapConfirmed(MotionEvent e) {
//			return false;
//		}
//	}
//	
//	
//    public void onAutoFocusDone(boolean focused) {
//    	if(mFocusManager != null){
//    		mFocusManager.onAutoFocus(focused, false, true); 
//    	}
//    }
//
//    /**
//     * The focus manager is the first UI related element to get initialized,
//     * and it requires the RenderOverlay, so initialize it here
//     */
//    private void initializeFocusManager() {
//    	mFrontCamera = CameraHolder.instance().isFrontCamera();
//    	mInitialParams = CameraManager.get().getInitParameters();
//    	if (null == mFocusManager) {
//    		// Create FocusManager object. startPreview needs it.
//        	mFocusRenderer = (FocusRenderer) findViewById(R.id.zxing_focus_renderer);
//            // if mFocusManager not null, reuse it
//            // otherwise create a new instance
//            String[] defaultFocusModes = this.getResources().getStringArray(
//                    R.array.pref_camera_focusmode_default_array);
//            mFocusManager = new FocusOverlayManager(defaultFocusModes,
//                    mInitialParams, this, mFrontCamera,
//                    this.getMainLooper(), this);
//            mFocusManager.setFocusRenderer(mFocusRenderer);
//		} else {
//			mFocusManager.removeMessages("initializeFocusManager");
//			mFocusManager.setMirror(mFrontCamera);
//	        mFocusManager.setParameters(mInitialParams);
//		}
//    }
//
//	@Override
//	public void autoFocus() {
//		CameraManager.get().requestAutoFocus(handler, R.id.auto_focus);
//		selectHandler.postDelayed(mAutoFocusTimeOutRunnable, AUTOFOCUS_TIMEOUT_DELAY);
//	}
//
//	@Override
//	public void cancelAutoFocus() {
//		CameraManager.get().cancelAutoFocus();
//	}
//
//	@Override
//	public void finishFocus() {
//	}
//
//	@Override
//	public boolean capture() {
//		return false;
//	}
//
//	@Override
//	public void continuousCapture() {
//	}
//
//	@Override
//	public void shutterDownCapture() {
//	}
//
//	@Override
//	public void startFaceDetection() {
//	}
//
//	@Override
//	public void stopFaceDetection() {
//	}
//
//	@Override
//	public void setFocusParameters() {
//	}
//
//	@Override
//	public boolean onMotionFocus() {
//		LogUtils.e("YTL", "CaptureActivity onMotionFocus =============== ");
//		mFocusManager.doFocus();
//		return true;
//	}
//	
//	/**
//	 * 对焦超时
//	 */
//    private final class SuperCameraAutoFocusTimeOutRunnable implements Runnable {
//		@Override
//		public void run() {
//			cancelAutoFocus();
//			onAutoFocusDone(false);
//		}
//    }
}