package eu.dedb.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

public class SysUtils {

	public static final String ENV_STANDART = "sh";
	public static final String ENV_SUPERUSER = "su";

	private static int BUFFER_LENGTH = 1024;

	public static String[] grantPermission(Context ctx, String permission) {
		String command = "pm grant " + ctx.getPackageName() + " " + permission;
		String[] result = exec(ENV_SUPERUSER, command);
		if (result[1].length() == 0 && result[2].length() == 0) {
			return null;
		}
		return result;
	}

	public static String[] revokePermission(Context ctx, String permission) {
		String command = "pm revoke " + ctx.getPackageName() + " " + permission;
		String[] result = exec(ENV_SUPERUSER, command);
		if (result[1].length() == 0 && result[2].length() == 0) {
			return null;
		}
		return result;
	}
	
	public static String[] exec(String env, List<String> commands) {
		return exec(env, commands.toArray(new String[0]));
	}

	public static String[] exec(String env, String... commands) {

		String[] result = new String[] { "", "", "" };
		Process proc = null;
		OutputStream stdin = null;
		InputStream[] stdout = null;

		if (env == null)
			env = ENV_STANDART;

		try {
			proc = Runtime.getRuntime().exec(env);
			proc.exitValue();
			stdout = new InputStream[] { proc.getInputStream(),
					proc.getErrorStream() };
			// process terminated
			result[2] = "Process terminated!";
		} catch (IOException e) {
			// process not started
			result[2] = e.getMessage();
		} catch (IllegalThreadStateException e) {
			// process is active;
			stdin = proc.getOutputStream();
			stdout = new InputStream[] { proc.getInputStream(),
					proc.getErrorStream() };

			for (String command : commands) {
				try {
					stdin.write((command + "\n").getBytes());
					stdin.flush();
				} catch (IOException e1) {
					// write failed
					result[2] = "Write error";
				}
			}

			if (env.equals(ENV_STANDART) || env.equals(ENV_SUPERUSER)) {
				try {
					stdin.write(("exit\n").getBytes());
					stdin.flush();
					stdin.close();
				} catch (IOException e1) {
					// write failed
				}
			}

			try {
				proc.waitFor();
			} catch (InterruptedException e1) {
				// process interrupted
			}
		}

		if (stdout != null) {
			for (int i = 0; i < 2; i++) {
				byte[] buffer = new byte[BUFFER_LENGTH];
				int read = 0;
				while (read != -1) {
					try {
						read = stdout[i].read(buffer);
						if (read > 0)
							result[i] += new String(buffer, 0, read);

					} catch (IOException e1) {
						// read failed
					}
				}
				try {
					stdout[i].close();
				} catch (IOException e1) {
					// close failed
				}
			}
		}

		if (proc != null)
			proc.destroy();

		return result;
	}
	
	@SuppressLint("NewApi")
	public static List<String> getBuildInfo() {
		List<String> result = new ArrayList<String>();
		Field[] fields = Build.class.getFields();
		for (Field field : fields) {
			try {
				Object obj = field.get(null);
				if (obj instanceof String)
					result.add(field.getName() + " = " + (String) obj +";");
			} catch (IllegalAccessException e) {
			} catch (IllegalArgumentException e) {
			}
		}
		if(Build.VERSION.SDK_INT>=14) {
			result.add("getRadioVersion() = "+Build.getRadioVersion()+";");
		}
		fields = Build.VERSION.class.getFields();
		for (Field field : fields) {
			try {
				Object obj = field.get(null);
				if (obj instanceof String)
					result.add("VERSION." + field.getName() + " = " + (String) obj+";");
			} catch (IllegalAccessException e) {
			} catch (IllegalArgumentException e) {
			}
		}
		
		return result;
	}
	
	public static List<String> getFileList(String dir, String pattern) {
		List<String> filelist = new ArrayList<String>();
		if (pattern == null)
			pattern = "^(.*?)";
		File[] files = new File(dir).listFiles();
		for (File file : files) {
			if (file.getName().matches(pattern)) {
				filelist.add(file.getAbsolutePath());
			}
		}
		return filelist;
	}
	
	public static void email(Context context, String emailTo, String emailCC,
			String subject, String emailText, List<String> filePaths) {
		final Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
		emailIntent.setType("text/plain");
		emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
				new String[] { emailTo });
		emailIntent.putExtra(android.content.Intent.EXTRA_CC,
				new String[] { emailCC });
		emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
		emailIntent.putExtra(Intent.EXTRA_TEXT, emailText);
		ArrayList<Uri> uris = new ArrayList<Uri>();
		for (String file : filePaths) {
			File fileIn = new File(file);
			Uri u = Uri.fromFile(fileIn);
			uris.add(u);
		}
		emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
		Intent chooser = Intent.createChooser(emailIntent, "Share dump...");
		chooser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(chooser);
	}
	
}
