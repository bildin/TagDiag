package eu.dedb.nfc.tagdiag;

import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;

public class NavigationGrid {

	final private Context ctx;
	final private LayoutParams itemLayoutParams = new LayoutParams(
			LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
			LayoutParams.TYPE_SYSTEM_OVERLAY, LayoutParams.FLAG_NOT_FOCUSABLE
					| LayoutParams.FLAG_LAYOUT_IN_SCREEN,
			PixelFormat.TRANSLUCENT);
	final private DisplayMetrics mDisplayMetrics;
	final private Handler menuHandler;
	final private int menuId;
	final private WindowManager mWindowManager;
	final private ImageView navigator;
	final private LayoutParams navigatorLayoutParams = new LayoutParams(
			LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
			LayoutParams.TYPE_PHONE, LayoutParams.FLAG_NOT_FOCUSABLE
					| LayoutParams.FLAG_LAYOUT_IN_SCREEN,
			PixelFormat.TRANSLUCENT);
	final private int xGrids;
	final private int yGrids;
	private long longClickDelay;
	private ImageView[] menuItems;

	public NavigationGrid(Context context, int xGrids, int yGrids,
			Handler menuHandler, int menuId) {
		this.xGrids = xGrids;
		this.yGrids = yGrids;
		this.ctx = context;
		this.menuHandler = menuHandler;
		this.menuId = menuId;
		this.mWindowManager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		mDisplayMetrics = new DisplayMetrics();
		mWindowManager.getDefaultDisplay().getMetrics(mDisplayMetrics);
		navigator = new ImageView(context);
		navigator.setOnTouchListener(mOnTouchListener);
		mWindowManager.addView(navigator, navigatorLayoutParams);
		menuItems = new ImageView[xGrids * yGrids];
	}

	public void destroy() {
		for (int x = 0; x < xGrids; x++)
			for (int y = 0; y < yGrids; y++)
				if (menuItems[y * xGrids + x] != null) {
					if (menuItems[y * xGrids + x].isShown())
						mWindowManager.removeView(menuItems[y * xGrids + x]);
				}
		if (navigator.isShown())
			mWindowManager.removeView(navigator);
	}

	final private Timer timer = new Timer();
	@SuppressLint("HandlerLeak")
	final private Handler onLongClickHandler = new Handler() {

		public void handleMessage(Message msg) {
			navigator.performLongClick();
		}
	};

	OnTouchListener mOnTouchListener = new OnTouchListener() {
		private float initialTouchX;
		private float initialTouchY;
		private int initialX;
		private int initialY;
		private boolean isMoving = false;
		private TimerTask onLongClickLauncher;

		@Override
		public boolean onTouch(final View v, MotionEvent event) {
			mWindowManager.getDefaultDisplay().getMetrics(mDisplayMetrics);
			int xGrid = mDisplayMetrics.widthPixels / xGrids;
			int yGrid = mDisplayMetrics.heightPixels / yGrids;
			float xOffset = ((float) xGrids - 1) / 2;
			float yOffset = ((float) yGrids - 1) / 2;

			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				initialX = navigatorLayoutParams.x;
				initialY = navigatorLayoutParams.y;
				initialTouchX = event.getRawX();
				initialTouchY = event.getRawY();
				onLongClickLauncher = new TimerTask() {
					@Override
					public void run() {
						onLongClickHandler.sendEmptyMessage(0);
					}
				};
				timer.schedule(onLongClickLauncher, longClickDelay);
				return true;
			case MotionEvent.ACTION_UP:
				if (!isMoving) {
					navigatorLayoutParams.x = initialX;
					navigatorLayoutParams.y = initialY;
					mWindowManager.updateViewLayout(navigator,
							navigatorLayoutParams);
					if (onLongClickLauncher.cancel())
						v.performClick();
				} else {
					int xPos = Math.round((navigatorLayoutParams.x + xOffset
							* xGrid)
							/ xGrid);
					int yPos = Math.round((navigatorLayoutParams.y + yOffset
							* yGrid)
							/ yGrid);
					navigatorLayoutParams.x = (int) ((xPos - xOffset) * xGrid);
					navigatorLayoutParams.y = (int) ((yPos - yOffset) * yGrid);

					mWindowManager.updateViewLayout(navigator,
							navigatorLayoutParams);
					menuHandler.obtainMessage(menuId, xPos, yPos)
							.sendToTarget();
					onLongClickLauncher.cancel();
					isMoving = false;
				}
				if (!isMoving)
					for (int x = 0; x < xGrids; x++)
						for (int y = 0; y < yGrids; y++)
							if (menuItems[y * xGrids + x] != null) {
								menuItems[y * xGrids + x]
										.setVisibility(View.GONE);
							}
				return true;
			case MotionEvent.ACTION_MOVE:
				navigatorLayoutParams.x = initialX
						+ (int) (event.getRawX() - initialTouchX);
				navigatorLayoutParams.y = initialY
						+ (int) (event.getRawY() - initialTouchY);
				mWindowManager.updateViewLayout(navigator,
						navigatorLayoutParams);

				if (!isMoving) {
					int deltaX = navigatorLayoutParams.x - initialX;
					int deltaY = navigatorLayoutParams.y - initialY;
					if (!((Math.abs(deltaX) < (xGrid / 4)) && (Math.abs(deltaY) < (yGrid / 4)))) {
						onLongClickLauncher.cancel();
						isMoving = true;
					}
				}

				if (isMoving)
					for (int x = 0; x < xGrids; x++)
						for (int y = 0; y < yGrids; y++)
							if (menuItems[y * xGrids + x] != null) {
								 itemLayoutParams.x = (int) ((x - xOffset) *
								 xGrid);
								 itemLayoutParams.y = (int) ((y - yOffset) *
								 yGrid);
								menuItems[y * xGrids + x]
										.setVisibility(View.VISIBLE);
								mWindowManager.updateViewLayout(menuItems[y
										* xGrids + x], itemLayoutParams);
							}
				return true;
			}
			return false;
		}
	};

	public void setCursor(int resId) {
		navigator.setImageResource(resId);
	}

	public void setOnClickListener(OnClickListener l) {
		navigator.setOnClickListener(l);
	}

	public void setOnLongClickListener(OnLongClickListener l, long delay) {
		this.longClickDelay = delay;
		navigator.setOnLongClickListener(l);
	}

	public void setMenuItem(int x, int y, int resId) {
		ImageView item = new ImageView(ctx);
		item.setImageResource(resId);
		item.setVisibility(View.GONE);
		menuItems[y * xGrids + x] = item;
		mWindowManager.getDefaultDisplay().getMetrics(mDisplayMetrics);
		int xGrid = mDisplayMetrics.widthPixels / xGrids;
		int yGrid = mDisplayMetrics.heightPixels / yGrids;
		float xOffset = ((float) xGrids - 1) / 2;
		float yOffset = ((float) yGrids - 1) / 2;
		itemLayoutParams.x = (int) ((x - xOffset) * xGrid);
		itemLayoutParams.y = (int) ((y - yOffset) * yGrid);
		mWindowManager.addView(item, itemLayoutParams);
	}

	public void hide() {
		navigator.setVisibility(View.GONE);
	}

	public void show() {
		navigator.setVisibility(View.VISIBLE);
	}
}
