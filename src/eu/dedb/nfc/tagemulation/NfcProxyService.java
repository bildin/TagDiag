package eu.dedb.nfc.tagemulation;

import java.lang.reflect.Field;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

public class NfcProxyService extends Service {

	public static final String DESCRIPTOR = "android.nfc.INfcTag";
	private final IBinder mBinder = new NfcProxyBinder();
	private Handler mHandler;
	private IBinder tagService = null;
	String init = "";

	private enum TRANSACTION {
		TRANSACTION_close, TRANSACTION_connect, TRANSACTION_reconnect, TRANSACTION_getTechList, TRANSACTION_isNdef, TRANSACTION_isPresent, TRANSACTION_transceive, TRANSACTION_ndefRead, TRANSACTION_ndefWrite, TRANSACTION_ndefMakeReadOnly, TRANSACTION_ndefIsWritable, TRANSACTION_formatNdef, TRANSACTION_rediscover, TRANSACTION_setTimeout, TRANSACTION_getTimeout, TRANSACTION_resetTimeouts, TRANSACTION_canMakeReadOnly, TRANSACTION_getMaxTransceiveLength, TRANSACTION_getExtendedLengthApdusSupported
	};

	private SparseArray<TRANSACTION> transactions = new SparseArray<TRANSACTION>();

	private WindowManager windowManager;
	private ImageView chatHead;
	private WindowManager.LayoutParams params;

	public class NfcProxyBinder extends Binder {

		NfcProxyBinder() {
			this.attachInterface(null, "");
		}

		NfcProxyService getService() {
			return NfcProxyService.this;
		}

		@Override
		protected boolean onTransact(int code, Parcel data, Parcel reply,
				int flags) throws RemoteException {
			log("" + transactions.get(code));
			// log("Transact > code: " + code + "; flags: "+ flags +"; data: " +
			// Hex.toHexstr(data.marshall()));
			if (transactions.get(code) == TRANSACTION.TRANSACTION_transceive) {

			}
			if (tagService != null)
				tagService.transact(code, data, reply, flags);

			switch (transactions.get(code)) {
			case TRANSACTION_connect: {
				data.enforceInterface(DESCRIPTOR);
				int nativeHandle = data.readInt();
				int technology = data.readInt();

				reply.readException();
				int success = reply.readInt();

				log("Tech: " + technology + " "
						+ ((success == 0) ? "OK!" : success));

				break;
			}
			case TRANSACTION_transceive: {
				data.enforceInterface(DESCRIPTOR);
				// int nativeHandle;
				int nativeHandle = data.readInt();
				byte[] data_send = data.createByteArray();
				boolean raw = (0 != data.readInt());
				// data.setDataPosition(0);

				log("> " + Hex.toHexstr(data_send) + ((!raw) ? " !" : ""));

				reply.readException();
				int success = reply.readInt();
				if (success == 1) {
					int result = reply.readInt();
					if (result == 0) {
						int responseLength = reply.readInt();
						byte[] data_rcvd = new byte[responseLength];
						reply.readByteArray(data_rcvd);
						log("< " + Hex.toHexstr(data_rcvd));
					} else {
						switch (result) {
						case 2:
							log("< Tag was lost.");
							break;
						case 3:
							log("< Transceive length exceeds supported maximum.");
							break;
						default:
							log("< Transceive failed.");
						}
					}
				} else {
					log("< null");
				}
				break;
			}
			}
			// log("Transact < code: " + code + "; flags: "+ flags +"; reply: "
			// + Hex.toHexstr(reply.marshall()));
			return true;
		}

	
	}

	@Override
	public void onCreate() {
		super.onCreate();
		try {
			Class<?> cl = Class.forName("android.nfc.INfcTag$Stub");
			Field[] flds = cl.getDeclaredFields();
			for (Field fld : flds) {
				init += fld.getName() + "\n";
			}
		} catch (Exception e) {
			init += e.toString();
		}

		for (TRANSACTION t : TRANSACTION.values()) {
			try {
				init += t.name() + ":";
				Class<?> cl = Class.forName("android.nfc.INfcTag$Stub");
				Field transaction = cl.getDeclaredField(t.name());
				transaction.setAccessible(true);
				init += transaction.getInt(null) + "\n";
				;
				transactions.append(transaction.getInt(null), t);
			} catch (Exception e) {
				init += e.toString() + "\n";
				;
			}
		}

		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

		chatHead = new ImageView(this);
		chatHead.setImageResource(R.drawable.ic_launcher);


		params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
				PixelFormat.TRANSLUCENT);

		chatHead.setOnTouchListener(new View.OnTouchListener() {
			private int initialX;
			private int initialY;
			private float initialTouchX;
			private float initialTouchY;
			private boolean isMoving = false;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					initialX = params.x;
					initialY = params.y;
					initialTouchX = event.getRawX();
					initialTouchY = event.getRawY();
					return true;
				case MotionEvent.ACTION_UP:
					if (!isMoving) {
						mHandler.sendEmptyMessage(2);
						v.performClick();
					}
					else {
						isMoving = false;
					}
					return true;
				case MotionEvent.ACTION_MOVE:
					params.x = initialX + (int) (event.getRawX() - initialTouchX);
					params.y = initialY + (int) (event.getRawY() - initialTouchY);
					windowManager.updateViewLayout(chatHead, params);

					if(!isMoving) {
						int deltaX = params.x - initialX;
						int deltaY = params.y - initialY;
						if(!((deltaX*deltaX<1000)&&(deltaY*deltaY<1000)))
							isMoving = true;
					}
					return true;
				}
				return false;
			}
		});

		params.gravity = Gravity.TOP | Gravity.LEFT;
		params.x = 0;
		params.y = 100;

		windowManager.addView(chatHead, params);

	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (chatHead != null)
			windowManager.removeView(chatHead);
	}

	public void setTagService(IBinder tagService) {
		this.tagService = tagService;
	}

	public void setHandler(Handler mHandler) {
		this.mHandler = mHandler;
		// log(init);
	}

	private void log(String action) {
		Message msg = new Message();
		Bundle data = new Bundle();
		data.putString("action", action);
		msg.setData(data);
		mHandler.sendMessage(msg);
	}
}
