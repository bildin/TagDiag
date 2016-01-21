package eu.dedb.nfc.tagemulation;



import java.io.DataOutputStream;
import java.lang.reflect.Method;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.nfc.INfcTag;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.text.method.TransformationMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import eu.dedb.nfc.tagemulation.NfcProxyService.NfcProxyBinder;


public class NfcManager extends Activity
{
    NfcProxyService mService;
	boolean mBound;
	private TextView log;
	private NfcAdapter dAdapter;
	private INfcTag iNfcTag;
	protected IBinder mBinder;
	private PendingIntent pendingIntent;
	private IntentFilter[] mFilters;
	private String[][] mTechLists;
	protected Tag mTag;
	private Activity me;

	@Override
    public void onCreate(Bundle savedInstanceState)
	{
		me = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_manager);
        // agjust log view
		log = (TextView) findViewById(R.id.log);
		log("");
		
		// try to get permission (needs root)
		//*
		if(checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS)!=PackageManager.PERMISSION_GRANTED) {
			log("no permission!");
			//*
			//log.append("pm grant " + getPackageName() +" "+android.Manifest.permission.WRITE_SECURE_SETTINGS+"\n");
			try{
				Process p = Runtime.getRuntime().exec("su");
				DataOutputStream os = new DataOutputStream(p.getOutputStream());
				String cmd = "pm grant " + getPackageName() +" "+android.Manifest.permission.WRITE_SECURE_SETTINGS+"\nexit\n";
				os.writeBytes(cmd);
				os.flush();
				os.close();
			}
			catch(Exception e){
				log("failed to get permission");
			}
			//*/
			
		}
		else
			log("permission exists!");
		//*/
		// get NfcAdapter
		dAdapter = NfcAdapter.getDefaultAdapter(this);
		iNfcTag = null;
		try{
			Method getTagService = dAdapter.getClass().getMethod("getTagService",(Class<?>[])null);
			iNfcTag = (INfcTag) getTagService.invoke(dAdapter, (Object[]) null);
		}
		catch(Exception e){
			log("getTagService failed!");
		}
		
		findViewById(R.id.btn_emulate).setOnClickListener(emulate);
		
		Intent intent = new Intent(this, NfcProxyService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		
		pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
		mFilters = new IntentFilter[] { tech };
		mTechLists = new String[][] { new String[] { android.nfc.tech.NfcA.class.getName() } };
    }
	
	@Override
	protected void onNewIntent(Intent intent) {

		Tag tag = null;
		
		if (intent.getAction().equals(NfcAdapter.ACTION_TECH_DISCOVERED)
				&& ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY))
			tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		
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
			} else {
				tagService = null;
			}
			oParcel.recycle();

			int nfca_idx = -1;
			short oSak = 0;
			short nSak = 0;
			for (int idx = 0; idx < oTechList.length; idx++) {
				if (oTechList[idx] == 1) {
					if (nfca_idx == -1) {
						nfca_idx = idx;
						if (oTechExtras[idx] != null
								&& oTechExtras[idx].containsKey("sak")) {
							oSak = oTechExtras[idx].getShort("sak");
							nSak = oSak;
						}
					} else {
						if (oTechExtras[idx] != null
								&& oTechExtras[idx].containsKey("sak")) {
							nSak = (short) (nSak | oTechExtras[idx].getShort("sak"));
						}
					}
				}
			}
			

			log("UID: "+Hex.toHexstr(id)+ "; SAK: " + Hex.toHexstr(new byte[]{(byte)nSak}) + ((oSak!=nSak)?"!":""));
			
			iNfcTag = INfcTag.Stub.asInterface(tagService);
			if(mBound)
				mService.setService(iNfcTag);
			
			mTag = createTag(id, oTechList, oTechExtras, serviceHandle, mBinder);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (dAdapter != null)
			dAdapter.enableForegroundDispatch(this, pendingIntent, mFilters,
					mTechLists);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (dAdapter != null)
			dAdapter.disableForegroundDispatch(this);
	}
	
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }
	
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
			log("Connected");
			mBinder = service;
            NfcProxyBinder binder = (NfcProxyBinder) service;
            mService = binder.getService();
            mBound = true;
            mService.setService(iNfcTag);
            mService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
			log("Disconnected");
            mBound = false;
        }
    };
    
    private Tag createTag(byte[] id, int[] techList, Bundle[] techListExtras, int serviceHandle, IBinder tagService) {
    	int isMock = (tagService == null)?1:0;
		Parcel nParcel = Parcel.obtain();
		nParcel.writeInt(id.length);
		nParcel.writeByteArray(id);
		nParcel.writeInt(techList.length);
		nParcel.writeIntArray(techList);
		nParcel.writeTypedArray(techListExtras,0);
		nParcel.writeInt(serviceHandle);
		nParcel.writeInt(isMock);
		if(isMock==0) {
			nParcel.writeStrongBinder(tagService);
		}
		nParcel.setDataPosition(0);
		Tag nTag = Tag.CREATOR.createFromParcel(nParcel);
		nParcel.recycle();
    	return nTag;
    }
    
    private void dispatchTag(Tag tag) {
		try {
			Method dispatch = dAdapter.getClass().getMethod("dispatch", tag.getClass());
			dispatch.invoke(dAdapter, tag);
		}
		catch(Exception e) {
			log("dispatchTag failed "+e);
			e.printStackTrace();
		}
    }
    
    @SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {
    	public void handleMessage(Message msg) {
    		log(msg.getData().getString("action", "-"));
    	}
    };
    
	private OnClickListener emulate = new OnClickListener(){
		@Override
		public void onClick(View v) {
			if (dAdapter != null)
				dAdapter.disableForegroundDispatch(me);
			dispatchTag(mTag);
			if (dAdapter != null)
				dAdapter.enableForegroundDispatch(me, pendingIntent, mFilters,
						mTechLists);	
		}
	};
	
	private void log(String line){
		log.append(line+"\n");
	}
}

