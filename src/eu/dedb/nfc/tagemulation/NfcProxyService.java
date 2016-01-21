package eu.dedb.nfc.tagemulation;

import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.nfc.INfcTag;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

public class NfcProxyService extends Service {

	public static final String DESCRIPTOR = "android.nfc.INfcTag";
	private final IBinder mBinder = new NfcProxyBinder();
	private INfcTag mTagService;
	private Handler mHandler;

	public class NfcProxyBinder extends Binder {

		NfcProxyBinder() {
			this.attachInterface(null, "");
		}

		NfcProxyService getService() {
			return NfcProxyService.this;
		}

		@Override
		protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
			//log("code: " + code + "; flags: "+ flags +"; raw: " + Hex.toHex(data.marshall()));
			switch (code)
			{
			case INTERFACE_TRANSACTION:
			{
				reply.writeString(DESCRIPTOR);
				return true;
			}
			case TRANSACTION_close:
			{
				data.enforceInterface(DESCRIPTOR);
				int _arg0;
				_arg0 = data.readInt();
				int _result = this.close(_arg0);
				reply.writeNoException();
				reply.writeInt(_result);
				return true;
			}
			case TRANSACTION_connect:
			{
				data.enforceInterface(DESCRIPTOR);
				int _arg0;
				_arg0 = data.readInt();
				int _arg1;
				_arg1 = data.readInt();
				int _result = this.connect(_arg0, _arg1);
				reply.writeNoException();
				reply.writeInt(_result);
				return true;
			}
			case TRANSACTION_reconnect:
			{
				data.enforceInterface(DESCRIPTOR);
				int _arg0;
				_arg0 = data.readInt();
				int _result = this.reconnect(_arg0);
				reply.writeNoException();
				reply.writeInt(_result);
				return true;
			}
			case TRANSACTION_getTechList:
			{
				data.enforceInterface(DESCRIPTOR);
				int _arg0;
				_arg0 = data.readInt();
				int[] _result = this.getTechList(_arg0);
				reply.writeNoException();
				reply.writeIntArray(_result);
				return true;
			}
			case TRANSACTION_isNdef:
			{
				data.enforceInterface(DESCRIPTOR);
				int _arg0;
				_arg0 = data.readInt();
				boolean _result = this.isNdef(_arg0);
				reply.writeNoException();
				reply.writeInt(((_result)?(1):(0)));
				return true;
			}
			case TRANSACTION_isPresent:
			{
				data.enforceInterface(DESCRIPTOR);
				int _arg0;
				_arg0 = data.readInt();
				boolean _result = this.isPresent(_arg0);
				reply.writeNoException();
				reply.writeInt(((_result)?(1):(0)));
				return true;
			}
			case TRANSACTION_transceive:
			{
				data.enforceInterface(DESCRIPTOR);
				int _arg0;
				_arg0 = data.readInt();
				byte[] _arg1;
				_arg1 = data.createByteArray();
				boolean _arg2;
				_arg2 = (0!=data.readInt());
				android.nfc.TransceiveResult _result = this.transceive(_arg0, _arg1, _arg2);
				reply.writeNoException();
				if ((_result!=null)) {
					reply.writeInt(1);
					_result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
				}
				else {
					reply.writeInt(0);
				}
				return true;
			}
			case TRANSACTION_ndefRead:
			{
				data.enforceInterface(DESCRIPTOR);
				int _arg0;
				_arg0 = data.readInt();
				android.nfc.NdefMessage _result = this.ndefRead(_arg0);
				reply.writeNoException();
				if ((_result!=null)) {
					reply.writeInt(1);
					_result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
				}
				else {
					reply.writeInt(0);
				}
				return true;
			}
			case TRANSACTION_ndefWrite:
			{
				data.enforceInterface(DESCRIPTOR);
				int _arg0;
				_arg0 = data.readInt();
				android.nfc.NdefMessage _arg1;
				if ((0!=data.readInt())) {
					_arg1 = android.nfc.NdefMessage.CREATOR.createFromParcel(data);
				}
				else {
					_arg1 = null;
				}
				int _result = this.ndefWrite(_arg0, _arg1);
				reply.writeNoException();
				reply.writeInt(_result);
				return true;
			}
			case TRANSACTION_ndefMakeReadOnly:
			{
				data.enforceInterface(DESCRIPTOR);
				int _arg0;
				_arg0 = data.readInt();
				int _result = this.ndefMakeReadOnly(_arg0);
				reply.writeNoException();
				reply.writeInt(_result);
				return true;
			}
			case TRANSACTION_ndefIsWritable:
			{
				data.enforceInterface(DESCRIPTOR);
				int _arg0;
				_arg0 = data.readInt();
				boolean _result = this.ndefIsWritable(_arg0);
				reply.writeNoException();
				reply.writeInt(((_result)?(1):(0)));
				return true;
			}
			case TRANSACTION_formatNdef:
			{
				data.enforceInterface(DESCRIPTOR);
				int _arg0;
				_arg0 = data.readInt();
				byte[] _arg1;
				_arg1 = data.createByteArray();
				int _result = this.formatNdef(_arg0, _arg1);
				reply.writeNoException();
				reply.writeInt(_result);
				return true;
			}
			case TRANSACTION_rediscover:
			{
				data.enforceInterface(DESCRIPTOR);
				int _arg0;
				_arg0 = data.readInt();
				android.nfc.Tag _result = this.rediscover(_arg0);
				reply.writeNoException();
				if ((_result!=null)) {
					reply.writeInt(1);
					_result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
				}
				else {
					reply.writeInt(0);
				}
				return true;
			}
			case TRANSACTION_setTimeout:
			{
				data.enforceInterface(DESCRIPTOR);
				int _arg0;
				_arg0 = data.readInt();
				int _arg1;
				_arg1 = data.readInt();
				int _result = this.setTimeout(_arg0, _arg1);
				reply.writeNoException();
				reply.writeInt(_result);
				return true;
			}
			case TRANSACTION_getTimeout:
			{
				data.enforceInterface(DESCRIPTOR);
				int _arg0;
				_arg0 = data.readInt();
				int _result = this.getTimeout(_arg0);
				reply.writeNoException();
				reply.writeInt(_result);
				return true;
			}
			case TRANSACTION_resetTimeouts:
			{
				data.enforceInterface(DESCRIPTOR);
				this.resetTimeouts();
				reply.writeNoException();
				return true;
			}
			case TRANSACTION_canMakeReadOnly:
			{
				data.enforceInterface(DESCRIPTOR);
				int _arg0;
				_arg0 = data.readInt();
				boolean _result = this.canMakeReadOnly(_arg0);
				reply.writeNoException();
				reply.writeInt(((_result)?(1):(0)));
				return true;
			}
			case TRANSACTION_getMaxTransceiveLength:
			{
				data.enforceInterface(DESCRIPTOR);
				int _arg0;
				_arg0 = data.readInt();
				int _result = this.getMaxTransceiveLength(_arg0);
				reply.writeNoException();
				reply.writeInt(_result);
				return true;
			}
			case TRANSACTION_getExtendedLengthApdusSupported:
			{
				data.enforceInterface(DESCRIPTOR);
				boolean _result = this.getExtendedLengthApdusSupported();
				reply.writeNoException();
				reply.writeInt(((_result)?(1):(0)));
				return true;
			}
			}
			return super.onTransact(code, data, reply, flags);
		}

		public int close(int nativeHandle) throws android.os.RemoteException
		{
			log("close("+nativeHandle+")");
			int _result;
			_result = mTagService.close(nativeHandle);
			return 0;
		}

		public int connect(int nativeHandle, int technology) throws android.os.RemoteException
		{
			log("connect("+nativeHandle+", "+technology+")");
			int _result;
			_result = mTagService.connect(nativeHandle, technology);
			return 0;
		}

		public int reconnect(int nativeHandle) throws android.os.RemoteException
		{
			log("reconnect("+nativeHandle+")");
			int _result;
			_result = mTagService.reconnect(nativeHandle);
			return 0;
		}

		public int[] getTechList(int nativeHandle) throws android.os.RemoteException
		{
			log("getTechList("+nativeHandle+")");
			int[] _result;
			_result = mTagService.getTechList(nativeHandle);
			return _result;
		}

		public boolean isNdef(int nativeHandle) throws android.os.RemoteException
		{
			log("isNdef("+nativeHandle+")");
			boolean _result;
			_result = mTagService.isNdef(nativeHandle);
			return _result;
		}

		public boolean isPresent(int nativeHandle) throws android.os.RemoteException
		{
			log("isPresent("+nativeHandle+")");
			boolean _result;
			_result = mTagService.isPresent(nativeHandle);
			return true;
		}

		public android.nfc.TransceiveResult transceive(int nativeHandle, byte[] data, boolean raw) throws android.os.RemoteException
		{
			log("transceive("+nativeHandle+", "+Hex.toHexstr(data)+", "+raw+")");
			android.nfc.TransceiveResult _result;
			_result = mTagService.transceive(nativeHandle, data, raw);
			
			try {
				log("return " + Hex.toHexstr(_result.getResponseOrThrow()));
			} catch (IOException e) {
				log(e.getMessage());
			}
			/*
			int mResult = 1;
			byte[] mResponseData = Hex.fromHex("0403020100");

			android.os.Parcel _reply = android.os.Parcel.obtain();
			_reply.writeInt(mResult);
			if(mResult == 0) {
				_reply.writeInt(mResponseData.length);
				_reply.writeByteArray(mResponseData);
			}
			_reply.setDataPosition(0);
			
			_result = android.nfc.TransceiveResult.CREATOR.createFromParcel(_reply);
			_reply.recycle();
			//*/
			
			return _result;
		}

		public android.nfc.NdefMessage ndefRead(int nativeHandle) throws android.os.RemoteException
		{
			log("ndefRead("+nativeHandle+")");
			android.nfc.NdefMessage _result;
			_result = mTagService.ndefRead(nativeHandle);
			return _result;
		}

		public int ndefWrite(int nativeHandle, android.nfc.NdefMessage msg) throws android.os.RemoteException
		{
			log("ndefWrite("+nativeHandle+", "+Hex.toHexstr(msg.toByteArray())+")");
			int _result;
			_result = mTagService.ndefWrite(nativeHandle, msg);
			return _result;
		}

		public int ndefMakeReadOnly(int nativeHandle) throws android.os.RemoteException
		{
			log("ndefMakeReadOnly("+nativeHandle+")");
			int _result;
			_result = mTagService.ndefMakeReadOnly(nativeHandle);
			return _result;
		}

		public boolean ndefIsWritable(int nativeHandle) throws android.os.RemoteException
		{
			log("ndefIsWritable("+nativeHandle+")");
			boolean _result;
			_result = mTagService.ndefIsWritable(nativeHandle);
			return _result;
		}

		public int formatNdef(int nativeHandle, byte[] key) throws android.os.RemoteException
		{
			log("formatNdef("+nativeHandle+", "+Hex.toHexstr(key)+")");
			int _result;
			_result = mTagService.formatNdef(nativeHandle, key);
			return _result;
		}

		public android.nfc.Tag rediscover(int nativehandle) throws android.os.RemoteException
		{
			log("rediscover("+nativehandle+")");
			android.nfc.Tag _result;
			_result = mTagService.rediscover(nativehandle);
			return _result;
		}

		public int setTimeout(int technology, int timeout) throws android.os.RemoteException
		{
			log("setTimeout("+technology+", "+timeout+")");
			int _result;
			_result = mTagService.setTimeout(technology, timeout);
			return _result;
		}

		public int getTimeout(int technology) throws android.os.RemoteException
		{
			log("getTimeout("+technology+")");			
			int _result;
			_result = mTagService.getTimeout(technology);
			return _result;
		}

		public void resetTimeouts() throws android.os.RemoteException
		{
			log("resetTimeouts()");
			mTagService.resetTimeouts();
		}

		public boolean canMakeReadOnly(int ndefType) throws android.os.RemoteException
		{
			log("canMakeReadOnly("+ndefType+")");
			boolean _result;
			_result = mTagService.canMakeReadOnly(ndefType);
			return _result;
		}

		public int getMaxTransceiveLength(int technology) throws android.os.RemoteException
		{
			log("getMaxTransceiveLength("+technology+")");	
			int _result;
			_result = mTagService.getMaxTransceiveLength(technology);
			return _result;
		}

		public boolean getExtendedLengthApdusSupported() throws android.os.RemoteException
		{
			log("getExtendedLengthApdusSupported()");
			boolean _result;
			_result = mTagService.getExtendedLengthApdusSupported();
			return _result;
		}

		static final int TRANSACTION_close = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
		static final int TRANSACTION_connect = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
		static final int TRANSACTION_reconnect = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
		static final int TRANSACTION_getTechList = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
		static final int TRANSACTION_isNdef = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
		static final int TRANSACTION_isPresent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
		static final int TRANSACTION_transceive = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
		static final int TRANSACTION_ndefRead = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
		static final int TRANSACTION_ndefWrite = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
		static final int TRANSACTION_ndefMakeReadOnly = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
		static final int TRANSACTION_ndefIsWritable = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
		static final int TRANSACTION_formatNdef = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
		static final int TRANSACTION_rediscover = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
		static final int TRANSACTION_setTimeout = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
		static final int TRANSACTION_getTimeout = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
		static final int TRANSACTION_resetTimeouts = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
		static final int TRANSACTION_canMakeReadOnly = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
		static final int TRANSACTION_getMaxTransceiveLength = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
		static final int TRANSACTION_getExtendedLengthApdusSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public void setService(INfcTag mTagService) {
		this.mTagService = mTagService;
	}
	
	public void setHandler(Handler mHandler) {
		this.mHandler = mHandler;
	}
	
	private void log(String action) {
		Message msg = new Message();
		Bundle data = new Bundle();
		data.putString("action", action);
		msg.setData(data);
		mHandler.sendMessage(msg);
	}
}
