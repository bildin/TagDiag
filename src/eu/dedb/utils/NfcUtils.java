package eu.dedb.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

public class NfcUtils {

	public static SparseArray<String> getSupportedTransactions() {
		SparseArray<String> transactions = new SparseArray<String>();
		try {
			Class<?> cl = Class.forName("android.nfc.INfcTag$Stub");
			Field[] flds = cl.getDeclaredFields();
			for (Field fld : flds) {
				String fldname = fld.getName();
				fld.setAccessible(true);
				Object fldvalue = fld.get(null);
				if (fldvalue instanceof Integer
						&& fldname.startsWith("TRANSACTION"))
					transactions.put((Integer) fldvalue, fldname);
			}
		} catch (Exception e) {
		}
		return transactions;
	}

	public static SparseArray<String> getSupportedTechnologies() {
		SparseArray<String> technologies = new SparseArray<String>();
		try {
			Class<?> cl = Class.forName("android.nfc.tech.TagTechnology");
			Field[] flds = cl.getDeclaredFields();
			for (Field fld : flds) {
				String fldname = fld.getName();
				fld.setAccessible(true);
				Object fldvalue = fld.get(null);
				if (fldvalue instanceof Integer)
					technologies.put((Integer) fldvalue, fldname);
			}
		} catch (Exception e) {
		}
		return technologies;
	}

	public static Tag createTag(byte[] id, int[] techList,
			Bundle[] techListExtras, int serviceHandle, IBinder tagService) {
		int isMock = (tagService == null) ? 1 : 0;
		Parcel nParcel = Parcel.obtain();
		nParcel.writeInt(id.length);
		nParcel.writeByteArray(id);
		nParcel.writeInt(techList.length);
		nParcel.writeIntArray(techList);
		nParcel.writeTypedArray(techListExtras, 0);
		nParcel.writeInt(serviceHandle);
		nParcel.writeInt(isMock);
		if (isMock == 0) {
			nParcel.writeStrongBinder(tagService);
		}
		nParcel.setDataPosition(0);
		Tag nTag = Tag.CREATOR.createFromParcel(nParcel);
		nParcel.recycle();
		return nTag;
	}

	public static String getTagInfo(Tag tag) {

		if (tag == null)
			return "TAG is null";

		String[] sTechList = tag.getTechList();

		StringBuilder sb = new StringBuilder();
		Parcel oParcel = Parcel.obtain();
		tag.writeToParcel(oParcel, 0);
		oParcel.setDataPosition(0);

		int len = oParcel.readInt();
		byte[] id = null;
		if (len >= 0) {
			id = new byte[len];
			oParcel.readByteArray(id);
		}
		int tl_len = oParcel.readInt();
		int[] oTechList = new int[tl_len];
		oParcel.readIntArray(oTechList);
		Bundle[] oTechExtras = oParcel.createTypedArray(Bundle.CREATOR);
		int serviceHandle = oParcel.readInt();
		int isMock = oParcel.readInt();
		IBinder tagService = null;
		if (isMock == 0) {
			tagService = oParcel.readStrongBinder();
		} else {
			tagService = null;
		}
		oParcel.recycle();

		sb.append("TAG:\n")
				.append("\t" + len + " byte UID: " + StringUtils.printBytes(id)
						+ "\n")
				.append("\t" + tl_len + " supported technologies:\n");

		for (int idx = 0; idx < oTechList.length; idx++) {
			sb.append("\t\t" + oTechList[idx] + " " + sTechList[idx] + "\n");
			if (oTechExtras[idx] != null) {
				sb.append("\t\t\tTech extras:\n");
				Set<String> keys = oTechExtras[idx].keySet();
				for (String key : keys.toArray(new String[0])) {
					Object obj = oTechExtras[idx].get(key);
					if (obj instanceof byte[]) {
						sb.append("\t\t\t\tbyte[] " + key + " = "
								+ StringUtils.printBytes((byte[]) obj) + "\n");
					} else if (obj instanceof Short) {
						sb.append("\t\t\t\tshort "
								+ key
								+ " = 0x"
								+ StringUtils
										.printBytes((byte) (((Short) obj) & 0xFF))
								+ "\n");
					} else if (obj instanceof Byte) {
						sb.append("\t\t\t\tbyte " + key + " = 0x"
								+ StringUtils.printBytes((Byte) obj) + "\n");
					} else if (obj instanceof Integer) {
						sb.append("\t\t\t\tint " + key + " = " + obj + "\n");
					} else if (obj instanceof Boolean) {
						sb.append("\t\t\t\tboolean " + key + " = " + obj + "\n");
					} else if (obj instanceof Parcelable) {
						Parcel p = Parcel.obtain();
						((Parcelable) obj).writeToParcel(p, 0);
						p.setDataPosition(0);
						sb.append("\t\t\t\tParcel " + key + " = "
								+ StringUtils.printBytes(p.marshall()) + "\n");
					}
				}
			} else {
				sb.append("\t\t\tTech extras: null\n");
			}
		}
		sb.append("---\n")
				.append("\tnativeHandle: " + serviceHandle + "\n")
				.append("\tINfcTag: "
						+ ((isMock == 0) ? tagService.toString() : "is mock")
						+ "\n");
		return sb.toString();
	}

	public static IBinder getTagService(Context ctx) {
		try {
			NfcAdapter dAdapter = NfcAdapter.getDefaultAdapter(ctx);
			Method getTagService = dAdapter.getClass().getMethod(
					"getTagService", (Class<?>[]) null);
			IInterface iNfcTag = (IInterface) getTagService.invoke(dAdapter,
					(Object[]) null);
			return iNfcTag.asBinder();
		} catch (Exception e) {
			return null;
		}
	}

	public static List<String> getNfcDeviceList() {
		List<String> devicelist = new ArrayList<String>();
		String[] result;
		result = SysUtils.exec(null, "ls -l /dev | grep -i -e nfc");
		String[] devices = result[0].split("\n");
		for (String device : devices)
			devicelist.add("/dev/"
					+ device.substring(device.lastIndexOf(" ") + 1));
		return devicelist;
	}

	public static String getErrorName(int code) {
		String name = null;
		try {
			Class<?> cl = Class.forName("android.nfc.ErrorCodes");
			Method asString = cl.getMethod("asString", int.class);
			name = (String) asString.invoke(null, code);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return name;
	}

	public static List<String> getServices(Context ctx, String action) {
		List<String> info = new ArrayList<String>();
		PackageManager pm = ctx.getPackageManager();
		List<ResolveInfo> rlist;
		rlist = pm.queryIntentServices(new Intent(action), 0);
		for (ResolveInfo rinfo : rlist) {
			info.add(rinfo.serviceInfo.packageName + "/"
					+ rinfo.serviceInfo.name);
		}
		return info;
	}

	public static List<String> getActivities(Context ctx, String action) {
		List<String> info = new ArrayList<String>();
		PackageManager pm = ctx.getPackageManager();
		List<ResolveInfo> rlist;
		rlist = pm.queryIntentActivities(new Intent(action), 0);
		for (ResolveInfo rinfo : rlist) {
			info.add(rinfo.activityInfo.packageName + "/"
					+ rinfo.activityInfo.name);
		}
		return info;
	}

	public static boolean setForegroundDispatch(Context ctx,
			PendingIntent intent, IntentFilter[] filter) {
		try {
			NfcAdapter dAdapter = NfcAdapter.getDefaultAdapter(ctx);
			Field sServiceField = dAdapter.getClass().getDeclaredField(
					"sService");
			sServiceField.setAccessible(true);
			Object sService = sServiceField.get(dAdapter);
			Class<?> classTechListParcel = Class
					.forName("android.nfc.TechListParcel");
			Method setForegroundDispatch = sService
					.getClass()
					.getDeclaredMethod(
							"setForegroundDispatch",
							new Class<?>[] { PendingIntent.class,
									IntentFilter[].class, classTechListParcel });
			setForegroundDispatch.invoke(sService, new Object[] { intent,
					filter, null });
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
