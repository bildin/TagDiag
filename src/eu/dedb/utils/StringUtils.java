package eu.dedb.utils;

import java.util.List;

import android.util.SparseArray;

public class StringUtils {

	public static String printList(List<String> list, String caption, String bullet){
		String result = caption +"\n";
		for(String item : list)
			result += bullet + item + "\n";
		return result;
	}
	
	public static String printSparseArray(SparseArray<String> list, String caption, String bullet){
		String result = caption +"\n";
		for(int index = 0; index < list.size(); index++)
			result += bullet + list.valueAt(index) +" = "+ list.keyAt(index) + ";\n";
		return result;
	}
	
	public static String printBytes(byte... bytes) {
		return printBytes(" ", bytes);
	}
	
	public static String printBytes(String spacer, byte... bytes) {
		StringBuffer buff = new StringBuffer();
		for (byte b : bytes) {
			buff.append(String.format("%02X%s", b, spacer));
		}
		return buff.toString();
	}

	public static byte[] parseBytes(String str) {
		str = str.replace(" ", "");
		final int bytes = str.length() / 2;
		if (2 * bytes != str.length()) {
			throw new IllegalArgumentException(
					"Hex string must have an even number of digits");
		}

		byte[] result = new byte[bytes];
		for (int i = 0; i < str.length(); i += 2) {
			result[i / 2] = (byte) Integer.parseInt(str.substring(i, i + 2),
					16);
		}
		return result;
	}
}
