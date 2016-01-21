package eu.dedb.nfc.tagemulation;

public class Hex {
	
	public static String toHexstr(byte[] bytes) {
		StringBuffer buff = new StringBuffer();
		for (byte b : bytes) {
			buff.append(String.format("%02X", b));
		}
		return buff.toString();
	}

	public static byte[] fromHexstr(String digits) {
		digits = digits.replace(" ", "");
		final int bytes = digits.length() / 2;
		if (2 * bytes != digits.length()) {
			throw new IllegalArgumentException(
					"Hex string must have an even number of digits");
		}

		byte[] result = new byte[bytes];
		for (int i = 0; i < digits.length(); i += 2) {
			result[i / 2] = (byte) Integer.parseInt(digits.substring(i, i + 2),
					16);
		}
		return result;
	}
}
