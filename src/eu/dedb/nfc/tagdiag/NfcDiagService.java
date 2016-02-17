package eu.dedb.nfc.tagdiag;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import eu.dedb.utils.NfcUtils;
import eu.dedb.utils.StringUtils;
import eu.dedb.utils.SysUtils;

@SuppressLint("HandlerLeak")
public class NfcDiagService extends Service {

	public class NfcProxyBinder extends Binder {

		NfcProxyBinder() {
			this.attachInterface(null,
					"eu.dedb.nfc.tagemulation.NfcProxyService");
		}

		NfcDiagService getService() {
			return NfcDiagService.this;
		}

		@Override
		protected boolean onTransact(int code, Parcel data, Parcel reply,
				int flags) throws RemoteException {
			log(transactions.get(code)
					+ " from "
					+ getBaseContext().getPackageManager().getNameForUid(
							Binder.getCallingUid()), LOG_ACT);
			if (tagService != null)
				tagService.transact(code, data, reply, flags);
			else
				return false;
			parseTransaction(code, data, reply, flags);
			return true;
		}
	}

	public static final String BROADCAST_TAG_DISCOVERED = "eu.dedb.nfc.BROADCAST_TAG_DISCOVERED";
	public static final String BROADCAST_VIEW_LOG = "eu.dedb.nfc.BROADCAST_VIEW_LOG";
	public static final String BROADCAST_SHOW_NAVIGATOR = "eu.dedb.nfc.BROADCAST_SHOW_NAVIGATOR";
	public static final String BROADCAST_SHARE = "eu.dedb.nfc.BROADCAST_SHARE";
	public static final String BROADCAST_VIEW_SETTINGS = "eu.dedb.nfc.BROADCAST_VIEW_SETTINGS";
	public static final String BROADCAST_VIEW_HELP = "eu.dedb.nfc.BROADCAST_VIEW_HELP";
	public static final String BROADCAST_VIEW_INFO = "eu.dedb.nfc.BROADCAST_VIEW_INFO";
	public static final String DESCRIPTOR = "android.nfc.INfcTag";
	private static final int LOG_ACT = 4;
	private static final int LOG_ERR = 8;
	private static final int LOG_FULL = 0;
	private static final int LOG_STATE = 1;
	private static final int LOG_TAG = 2;
	private static final int NOTIFICATION_ID = 1;
	private static final int STATE_CHANGED_NFC = 0;
	private static final int STATE_CHANGED_SRV = 3;
	private static final int STATE_CHANGED_TAG = 1;
	public static final String EXTRA_INFO_FW = "FW";
	public static final String EXTRA_INFO_HW = "HW";
	public static final String EXTRA_INFO_SYS = "SYS";
	public static final String EXTRA_INFO_APP = "APP";
	public static final String EXTRA_LOG = "LOG";

	private StringBuilder appInfo;
	private List<String> attachedFiles;

	Timer cardWatchdog = new Timer();
	private NfcAdapter dAdapter;

	private TextView fullLog;
	private StringBuilder fwInfo;
	private StringBuilder hwInfo;
	protected int isEnabled = -1;
	protected int isPresent = -1;

	Handler logHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			int size = mLOG.size();
			int count = (size > 50) ? 50 : size;
			String last20 = "";
			for (int index = 0; index < count; index++)
				last20 += mLOG.get(size - index - 1) + "\n";
			fullLog.setText(last20);
		}
	};
	private WindowManager.LayoutParams logLayout = new WindowManager.LayoutParams(
			LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,
			WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
			WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
					| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
			PixelFormat.TRANSLUCENT);
	private final IBinder mBinder = new NfcProxyBinder();
	private NotificationCompat.Builder mBuilder;
	Handler menuHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			int x = msg.arg1;
			int y = msg.arg2;

			if (x != 0 && fullLog.isShown()) {
				windowManager.removeView(fullLog);
			}

			if (x == 2 && y == 2) {
				openHelp();
			}

			if (x == 0 && !fullLog.isShown()) {
				windowManager.addView(fullLog, logLayout);
			} else if (x == 4) {
				switch (y) {
				case 0:
					startCaptureActivity();
					break;
				case 1:
					openInfo();
					break;
				case 2:
					openLog();
					break;
				case 3:
					share();
					break;
				case 4:
					openSettings();
					break;
				default:
					break;
				}
			} else if (y == 4) {
				stopSelf();
			} else if (y == 0) {
				mNavigator.hide();
			}
		}
	};
	ArrayList<String> mLOG = new ArrayList<String>();
	private NavigationGrid mNavigator;

	TimerTask mNFCWatchDog = new TimerTask() {
		@Override
		public void run() {
			int enabled = -2;
			if (dAdapter != null)
				enabled = (dAdapter.isEnabled()) ? 1 : 0;
			// check STATE_CHANGED_NFC
			if (isEnabled != enabled) {
				isEnabled = enabled;
				stateChangeHandler.sendEmptyMessage(STATE_CHANGED_NFC);
			}
			// check STATE_CHANGED_TAG
			if (isEnabled == 1) {
				int present = -2;
				present = isPresent();
				if (isPresent != present) {
					isPresent = present;
					stateChangeHandler.sendEmptyMessage(STATE_CHANGED_TAG);
				}
			}
		}
	};
	private NotificationManager mNotifyManager;

	private Tag mTag;

	protected int nativeHandle = 1;
	private final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			log("Received broadcast " + intent.getAction(), LOG_FULL);
			if (intent.getAction().equals(BROADCAST_TAG_DISCOVERED)) {
				if (overridedDispatchEnabled)
					toggleForegroundDispatch();
				proxyTag((Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG));
			} else if (intent.getAction().equals(BROADCAST_VIEW_LOG)) {
				openLog();
			} else if (intent.getAction().equals(BROADCAST_VIEW_HELP)) {
				openHelp();
			} else if (intent.getAction().equals(BROADCAST_VIEW_INFO)) {
				openInfo();
			} else if (intent.getAction().equals(BROADCAST_VIEW_SETTINGS)) {
				openSettings();
			} else if (intent.getAction().equals(BROADCAST_SHARE)) {
				share();
			} else if (intent.getAction().equals(BROADCAST_SHOW_NAVIGATOR)) {
				mNavigator.show();
			}
		}
	};
	Handler stateChangeHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			String state = "";
			int logo = R.drawable.rainbow;

			if (msg.what == STATE_CHANGED_SRV) {
				log("Tag service recovered " + tagService, LOG_ACT);
			}

			if (msg.what == STATE_CHANGED_NFC) {
				switch (isEnabled) {
				case 0:
					state = "NFC DISABLED";
					logo = R.drawable.magenta;
					break;
				case 1:
					state = "NFC ENABLED";
					logo = R.drawable.blue;
					break;
				case -2:
					state = "NO NFC ADAPTER";
					logo = R.drawable.gray;
					break;
				default:
					state = "NFC STATE " + isEnabled;
					logo = R.drawable.rainbow;
					break;
				}
			}

			if (msg.what == STATE_CHANGED_TAG) {
				switch (isPresent) {
				case 0:
					if (mTag == null) {
						state = "NO TAG";
						logo = R.drawable.yellow;
					} else {
						state = "BUFFERED TAG";
						logo = R.drawable.cyan;
					}
					break;
				case 1:
					state = "TAG IS PRESENT";
					logo = R.drawable.green;
					break;
				case -1:
					state = "TAG SERVICE DEAD";
					logo = R.drawable.red;
					break;
				case -2:
					state = "UNRESOLVED";
					logo = R.drawable.rainbow;
					break;
				default:
					state = "TAG STATE " + isPresent;
					logo = R.drawable.rainbow;
					break;
				}
			}

			mNavigator.setCursor(logo);
			log("State changed: " + state, LOG_STATE);

			// New card detected
			if (msg.what == STATE_CHANGED_TAG && isPresent == 1) {
				Tag tag = redicover();
				proxyTag(tag);
				dispatchTag();
			}

			// Update notification
			if (mTag != null)
				mBuilder.setContentText("UID: "
						+ StringUtils.printBytes(mTag.getId()));
			mBuilder.setSmallIcon(logo).setContentTitle(state)
					.setWhen(System.currentTimeMillis());
			mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
		}
	};

	private StringBuilder sysInfo;

	private IBinder tagService = null;

	private SparseArray<String> technologies = NfcUtils
			.getSupportedTechnologies();

	private SparseArray<String> transactions = NfcUtils
			.getSupportedTransactions();

	private WindowManager windowManager;
	private boolean overridedDispatchEnabled;

	private void configureNavigator() {
		mNavigator = new NavigationGrid(this, 5, 5, menuHandler, 0);
		mNavigator.setCursor(R.drawable.rainbow);

		mNavigator.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dispatchTag();
			}
		});

		mNavigator.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				toggleForegroundDispatch();
				return true;
			}
		}, 1000);

		mNavigator.setMenuItem(0, 2, android.R.drawable.ic_menu_view);

		mNavigator.setMenuItem(2, 0, android.R.drawable.ic_menu_delete);
		
		mNavigator.setMenuItem(2, 2, android.R.drawable.ic_menu_help);

		mNavigator.setMenuItem(2, 4,
				android.R.drawable.ic_menu_close_clear_cancel);

		mNavigator.setMenuItem(4, 0, android.R.drawable.ic_menu_add);
		mNavigator.setMenuItem(4, 1, android.R.drawable.ic_menu_info_details);
		mNavigator.setMenuItem(4, 2, android.R.drawable.ic_menu_recent_history);
		mNavigator.setMenuItem(4, 3, android.R.drawable.ic_menu_share);
		mNavigator.setMenuItem(4, 4, android.R.drawable.ic_menu_preferences);
	}

	protected void toggleForegroundDispatch() {
		if (overridedDispatchEnabled) {
			boolean overridedDispatch = NfcUtils.setForegroundDispatch(this,
					null, null);
			overridedDispatchEnabled = !overridedDispatch;
			log("Stop Tag capture (SRV): " + overridedDispatch, LOG_FULL);
		} else {
			boolean overridedDispatch = NfcUtils.setForegroundDispatch(this,
					PendingIntent.getBroadcast(this, 0, new Intent(
							BROADCAST_TAG_DISCOVERED), 0),
					new IntentFilter[] { new IntentFilter(
							NfcAdapter.ACTION_TAG_DISCOVERED) });
			log("Start Tag capture (SRV): " + overridedDispatch, LOG_FULL);
			if (overridedDispatch) {
				overridedDispatchEnabled = overridedDispatch;
				Toast msg = Toast.makeText(this, "Tap Tag to diag",
						Toast.LENGTH_SHORT);
				msg.setGravity(Gravity.CENTER, 0, 0);
				msg.show();
			} else {
				startCaptureActivity();
			}
		}
	}

	@SuppressWarnings("deprecation")
	protected void getForegroundActivity() {
		ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		try {
			log("FGA: "
					+ am.getRunningTasks(1).get(0).topActivity
							.flattenToShortString(),
					LOG_FULL);
		} catch (Exception e) {
			log(e, LOG_FULL);
		}
	}

	public void dispatchTag() {
		log("Dispatching...", LOG_FULL);
		if (mTag == null)
			return;
		Intent intent = new Intent(NfcAdapter.ACTION_TECH_DISCOVERED, null);
		intent.putExtra(NfcAdapter.EXTRA_TAG, mTag);
		intent.putExtra(NfcAdapter.EXTRA_ID, mTag.getId());
		Intent chooser = new Intent(Intent.ACTION_PICK_ACTIVITY);
		chooser.putExtra(Intent.EXTRA_INTENT, intent);
		chooser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TASK);
		chooser.setClass(this, AppChooser.class);
		startActivity(chooser);

		// if (SysUtils.hasPermission(getBaseContext(),
		// android.Manifest.permission.WRITE_SECURE_SETTINGS)
		// && mTag != null) {
		// try {
		// Method dispatch = dAdapter.getClass().getMethod("dispatch",
		// mTag.getClass());
		// dispatch.invoke(dAdapter, mTag);
		// } catch (Exception e) {
		// log("dispatchTag failed " + e);
		// }
		// }
	}

	private int getKey(SparseArray<String> array, String value) {
		for (int index = 0; index < array.size(); index++)
			if (array.valueAt(index).equals(value))
				return array.keyAt(index);
		return -1;
	}

	int isPresent() {
		int _result = -2;
		int TRANSACTION_isPresent = getKey(transactions,
				"TRANSACTION_isPresent");
		if (TRANSACTION_isPresent >= 0) {
			android.os.Parcel _data = android.os.Parcel.obtain();
			android.os.Parcel _reply = android.os.Parcel.obtain();
			_result = -1;
			try {
				_data.writeInterfaceToken(DESCRIPTOR);
				_data.writeInt(nativeHandle);
				tagService.transact(TRANSACTION_isPresent, _data, _reply, 0);
				_reply.readException();
				_result = _reply.readInt();
			} catch (RemoteException e) {
				// NFC Service is dead
				serviceRecovery();
			} finally {
				_reply.recycle();
				_data.recycle();
			}
		}
		return _result;
	}

	private void log(Object action, int type) {
		mLOG.add("" + action);
		logHandler.sendEmptyMessage(0);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mBuilder = new NotificationCompat.Builder(this);
		mBuilder.addAction(android.R.drawable.ic_menu_compass, "",
				PendingIntent.getBroadcast(this, 0, new Intent(
						BROADCAST_SHOW_NAVIGATOR), 0));
		mBuilder.addAction(android.R.drawable.ic_menu_info_details, "",
				PendingIntent.getBroadcast(this, 0, new Intent(
						BROADCAST_VIEW_INFO), 0));
		mBuilder.addAction(android.R.drawable.ic_menu_recent_history, "",
				PendingIntent.getBroadcast(this, 0, new Intent(
						BROADCAST_VIEW_LOG), 0));
		mBuilder.addAction(android.R.drawable.ic_menu_share, "",
				PendingIntent.getBroadcast(this, 0, new Intent(
						BROADCAST_SHARE), 0));
		mBuilder.addAction(android.R.drawable.ic_menu_preferences, "",
				PendingIntent.getBroadcast(this, 0, new Intent(
						BROADCAST_VIEW_SETTINGS), 0));
		mBuilder.addAction(android.R.drawable.ic_menu_help, "",
				PendingIntent.getBroadcast(this, 0, new Intent(
						BROADCAST_VIEW_HELP), 0));
		startForeground(NOTIFICATION_ID, mBuilder.build());

		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

		fullLog = new TextView(this);
		fullLog.setTextSize(10);
		fullLog.setTextColor(Color.parseColor("#7FFF0000"));

		configureNavigator();
		collectInfo();

		dAdapter = NfcAdapter.getDefaultAdapter(this);

		if (dAdapter != null) {
			try {
				Method getTagService = dAdapter.getClass().getMethod(
						"getTagService", (Class<?>[]) null);
				IInterface iNfcTag = (IInterface) getTagService.invoke(
						dAdapter, (Object[]) null);
				this.tagService = iNfcTag.asBinder();
			} catch (Exception e) {
				log(e, LOG_ERR);
			}
		}

		cardWatchdog.scheduleAtFixedRate(mNFCWatchDog, 0, 250);

		IntentFilter filter = new IntentFilter();
		filter.addAction(BROADCAST_TAG_DISCOVERED);
		filter.addAction(BROADCAST_VIEW_LOG);
		filter.addAction(BROADCAST_SHOW_NAVIGATOR);
		filter.addAction(BROADCAST_SHARE);
		filter.addAction(BROADCAST_VIEW_HELP);
		filter.addAction(BROADCAST_VIEW_SETTINGS);
		filter.addAction(BROADCAST_VIEW_INFO);
		registerReceiver(receiver, filter);
	}

	private void collectInfo() {
		List<String> build = SysUtils.getBuildInfo();
		List<String> devs = NfcUtils.getNfcDeviceList();
		List<String> libs = SysUtils.getFileList("/system/lib", "(?i).*nfc.*");
		List<String> configs = SysUtils.getFileList("/system/etc",
				"(?i).*nfc.*conf");

		attachedFiles = new ArrayList<String>();
		attachedFiles.addAll(libs);
		attachedFiles.addAll(configs);

		// get device firmware info
		fwInfo = new StringBuilder("#FIRMWARE INFO\n");
		fwInfo.append(StringUtils.printList(build, "Build:", "\t"));

		// get NFC hardware info
		hwInfo = new StringBuilder("#HARDWARE INFO\n");
		hwInfo.append(StringUtils.printList(devs, "NFC devices:", "\t"));
		hwInfo.append(StringUtils.printList(libs, "NFC libs:", "\t"));
		hwInfo.append(StringUtils.printList(configs, "NFC configs:", "\t"));

		// get system API info
		sysInfo = new StringBuilder("#SYSTEM INFO\n");
		sysInfo.append(StringUtils.printSparseArray(transactions,
				"INfcTag supported transactions:", "\t"));
		sysInfo.append(StringUtils.printSparseArray(technologies,
				"TagTechnology supported techs:", "\t"));
		sysInfo.append(PackageManager.FEATURE_NFC
				+ ": "
				+ getBaseContext().getPackageManager().hasSystemFeature(
						PackageManager.FEATURE_NFC) + "\n");
		sysInfo.append("android.hardware.nfc.hce"
				+ ": "
				+ getBaseContext().getPackageManager().hasSystemFeature(
						"android.hardware.nfc.hce") + "\n");

		sysInfo.append("com.nxp.mifare: "
				+ getBaseContext().getPackageManager().hasSystemFeature(
						"com.nxp.mifare") + "\n");

		// get activities info
		appInfo = new StringBuilder("#ACTIVITIES INFO\n");
		appInfo.append(StringUtils.printList(
				NfcUtils.getActivities(this, NfcAdapter.ACTION_TECH_DISCOVERED),
				"TECH_DISCOVERED:", "\t"));
		appInfo.append(StringUtils.printList(
				NfcUtils.getActivities(this, NfcAdapter.ACTION_TAG_DISCOVERED),
				"TAG_DISCOVERED:", "\t"));
		appInfo.append(StringUtils.printList(NfcUtils.getServices(this,
				"android.nfc.cardemulation.action.HOST_APDU_SERVICE"),
				"HOST_APDU_SERVICE:", "\t"));

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(receiver);
		if (overridedDispatchEnabled)
			toggleForegroundDispatch();
		cardWatchdog.cancel();
		mNavigator.destroy();
	}

	protected void openInfo() {
		Intent intent = new Intent(this, InfoActivity.class);
		intent.putExtra(EXTRA_INFO_FW, fwInfo.toString());
		intent.putExtra(EXTRA_INFO_HW, hwInfo.toString());
		intent.putExtra(EXTRA_INFO_SYS, sysInfo.toString());
		intent.putExtra(EXTRA_INFO_APP, appInfo.toString());
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(intent);
	}

	protected void openSettings() {
		Intent intent = new Intent(this, SettingsActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(intent);
	}

	protected void openHelp() {
		Intent intent = new Intent(this, HelpActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(intent);
	}

	protected void openLog() {
		Intent intent = new Intent(this, LogActivity.class);
		intent.putExtra(EXTRA_LOG, mLOG);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(intent);
	}

	public void parseTransaction(int code, Parcel data, Parcel reply, int flags) {

		if (transactions.get(code).equals("TRANSACTION_connect")) {
			data.enforceInterface(DESCRIPTOR);
			data.readInt();
			int technology = data.readInt();

			reply.readException();
			int success = reply.readInt();

			log("Connect " + technologies.get(technology) + " (" + technology
					+ ") result " + NfcUtils.getErrorName(success)
					+ ((success == 0) ? " (" : (" ERROR (")) + success + ")",
					LOG_ACT);
		}

		if (transactions.get(code).equals("TRANSACTION_reconnect")) {
			reply.readException();
			int success = reply.readInt();

			log("Reconnect result " + NfcUtils.getErrorName(success)
					+ ((success == 0) ? " (" : (" ERROR (")) + success + ")",
					LOG_ACT);
		}

		if (transactions.get(code).equals("TRANSACTION_transceive")) {
			data.enforceInterface(DESCRIPTOR);
			data.readInt(); // nativeHandle
			byte[] data_send = data.createByteArray();
			boolean raw = (0 != data.readInt());

			log("> " + StringUtils.printBytes(data_send) + ((!raw) ? " !" : ""),
					LOG_ACT);

			if (!raw) {
				switch (data_send[0]) {
				case 0x30:
					log("(Read block/page 0x"
							+ StringUtils.printBytes(data_send[1]) + ")",
							LOG_ACT);
					break;
				case 0x60:
				case 0x61:
					byte[] key = new byte[6];
					System.arraycopy(data_send, 6, key, 0, 6);
					log("(Auth block 0x" + StringUtils.printBytes(data_send[1])
							+ " with key "
							+ ((data_send[0] == 0x60) ? "A" : "B") + ": "
							+ StringUtils.printBytes(key) + ")", LOG_ACT);
					break;
				default:
					break;
				}
			}

			reply.readException();
			int success = reply.readInt();
			if (success == 1) {
				int result = reply.readInt();
				if (result == 0) {
					int responseLength = reply.readInt();
					byte[] data_rcvd = new byte[responseLength];
					reply.readByteArray(data_rcvd);
					log("< " + StringUtils.printBytes(data_rcvd), LOG_ACT);
				} else {
					switch (result) {
					case 2:
						log("< Tag was lost.", LOG_ACT);
						break;
					case 3:
						log("< Transceive length exceeds supported maximum.",
								LOG_ACT);
						break;
					default:
						log("< Transceive failed.", LOG_ACT);
					}
				}
			} else {
				log("< null", LOG_ACT);
			}
		}
	}

	public Tag proxyTag(Tag tag) {
		log("Discovered " + NfcUtils.getTagInfo(tag), LOG_TAG);
		if (tag != null) {
			IBinder tagService = null;

			Parcel oParcel = Parcel.obtain();
			tag.writeToParcel(oParcel, 0);
			oParcel.setDataPosition(0);

			int len = oParcel.readInt();
			byte[] id = null;
			if (len >= 0) {
				id = new byte[len];
				oParcel.readByteArray(id);
			}
			int[] oTechList = new int[oParcel.readInt()];
			oParcel.readIntArray(oTechList);
			Bundle[] oTechExtras = oParcel.createTypedArray(Bundle.CREATOR);
			int serviceHandle = oParcel.readInt();
			int isMock = oParcel.readInt();
			if (isMock == 0) {
				tagService = oParcel.readStrongBinder();
			}
			oParcel.recycle();

			// Patch part begins here
			List<Integer> withSAK = new ArrayList<Integer>();
			List<Integer> withNullTechMifare = new ArrayList<Integer>();
			boolean patchedSAK = false;
			short nSak = 0;
			for (int idx = 0; idx < oTechList.length; idx++) {
				if (oTechExtras[idx] != null
						&& oTechExtras[idx].containsKey("sak")) {
					withSAK.add(idx);
					short sak = oTechExtras[idx].getShort("sak");
					nSak |= sak;
					if (sak != nSak)
						patchedSAK |= true;
				}

				if (oTechList[idx] == 8 && oTechExtras[idx] == null) {
					withNullTechMifare.add(idx);
				}
			}

			if (patchedSAK) {
				for (int idx : withSAK) {
					oTechExtras[idx].putShort("sak", nSak);
				}
				log("SAK is patched", LOG_TAG);
			}

			if (!withNullTechMifare.isEmpty() && !withSAK.isEmpty()) {
				for (int idx : withNullTechMifare) {
					oTechExtras[idx] = oTechExtras[withSAK.get(0)];
				}
				log("Mifare nulled tech is patched", LOG_TAG);
			}
			// Patch part ends here

			Tag nTag = NfcUtils.createTag(id, oTechList, oTechExtras,
					serviceHandle, mBinder);

			log("Proxied " + NfcUtils.getTagInfo(nTag), LOG_TAG);

			if (!tagService.equals(mBinder))
				this.tagService = tagService;
			this.nativeHandle = serviceHandle;
			this.mTag = nTag;
			return nTag;
		}
		return null;
	}

	Tag redicover() {
		Tag _result = null;
		int TRANSACTION_rediscover = getKey(transactions,
				"TRANSACTION_rediscover");
		if (TRANSACTION_rediscover > 0) {
			Parcel _data = Parcel.obtain();
			Parcel _reply = Parcel.obtain();
			try {
				_data.writeInterfaceToken(DESCRIPTOR);
				_data.writeInt(nativeHandle);
				tagService.transact(TRANSACTION_rediscover, _data, _reply, 0);
				_reply.readException();
				if ((0 != _reply.readInt())) {
					_result = Tag.CREATOR.createFromParcel(_reply);
				} else {
					_result = null;
				}
			} catch (RemoteException e) {
				// NFC Service is dead
			} finally {
				_reply.recycle();
				_data.recycle();
			}
		}
		return _result;
	}

	void serviceRecovery() {
		IBinder nTagService = NfcUtils.getTagService(getBaseContext());
		if (nTagService != null & nTagService != tagService) {
			tagService = nTagService;
			stateChangeHandler.sendEmptyMessage(STATE_CHANGED_SRV);
		}
	}

	void share() {
		String msgText = "This message generate with TagDiag.\n\n";
		msgText += fwInfo.toString() + "\n" + hwInfo.toString() + "\n"
				+ sysInfo.toString() + "\n" + appInfo.toString() + "\n";
		msgText += StringUtils.printList(mLOG, "#LOG:", "");
		msgText += StringUtils.printList(attachedFiles,
				"\n\n---\nAttached files:", "\t");
		SysUtils.email(this, "dedb.eu@gmail.com", "dedb.eu@gmail.com",
				"TagDiag Dump", msgText, attachedFiles);
	}

	protected void startCaptureActivity() {
		log("Start Tag capture (ACT)", LOG_FULL);
		Intent intent = new Intent(this, TagCaptureActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(intent);
	}

}
