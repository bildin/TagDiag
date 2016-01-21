/* 
 * a little cheat magic for use hidden API with standard SDK
 */
package android.nfc;

public interface INfcTag extends android.os.IInterface
{
	public static abstract class Stub extends android.os.Binder implements android.nfc.INfcTag
	{
		public static android.nfc.INfcTag asInterface(android.os.IBinder obj) {
			return null;
		}
	}

public int close(int nativeHandle) throws android.os.RemoteException;
public int connect(int nativeHandle, int technology) throws android.os.RemoteException;
public int reconnect(int nativeHandle) throws android.os.RemoteException;
public int[] getTechList(int nativeHandle) throws android.os.RemoteException;
public boolean isNdef(int nativeHandle) throws android.os.RemoteException;
public boolean isPresent(int nativeHandle) throws android.os.RemoteException;
public android.nfc.TransceiveResult transceive(int nativeHandle, byte[] data, boolean raw) throws android.os.RemoteException;
public android.nfc.NdefMessage ndefRead(int nativeHandle) throws android.os.RemoteException;
public int ndefWrite(int nativeHandle, android.nfc.NdefMessage msg) throws android.os.RemoteException;
public int ndefMakeReadOnly(int nativeHandle) throws android.os.RemoteException;
public boolean ndefIsWritable(int nativeHandle) throws android.os.RemoteException;
public int formatNdef(int nativeHandle, byte[] key) throws android.os.RemoteException;
public android.nfc.Tag rediscover(int nativehandle) throws android.os.RemoteException;
public int setTimeout(int technology, int timeout) throws android.os.RemoteException;
public int getTimeout(int technology) throws android.os.RemoteException;
public void resetTimeouts() throws android.os.RemoteException;
public boolean canMakeReadOnly(int ndefType) throws android.os.RemoteException;
public int getMaxTransceiveLength(int technology) throws android.os.RemoteException;
public boolean getExtendedLengthApdusSupported() throws android.os.RemoteException;

//	Depricated in 4.2.1 r1.2
public int connect(int nativeHandle) throws android.os.RemoteException;
public java.lang.String getType(int nativeHandle) throws android.os.RemoteException;
public byte[] getUid(int nativeHandle) throws android.os.RemoteException;
public byte[] transceive(int nativeHandle, byte[] data) throws android.os.RemoteException;
public int getLastError(int nativeHandle) throws android.os.RemoteException;
public android.nfc.NdefMessage read(int nativeHandle) throws android.os.RemoteException;
public int write(int nativeHandle, android.nfc.NdefMessage msg) throws android.os.RemoteException;
public int makeReadOnly(int nativeHandle) throws android.os.RemoteException;
public int getModeHint(int nativeHandle) throws android.os.RemoteException;
}
