/* 
 * a little cheat magic for use hidden API with standard SDK
 */


package android.nfc;

import java.io.IOException;

import android.os.Parcel;
import android.os.Parcelable;

public abstract class TransceiveResult {
    public abstract byte[] getResponseOrThrow() throws IOException;
	public abstract void writeToParcel(Parcel reply, int parcelableWriteReturnValue);
	public static final Parcelable.Creator<TransceiveResult> CREATOR = null;
}
