package eu.dedb.nfc.tagdiag;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import eu.dedb.utils.SysUtils;

public class SettingsActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.settings_activity_layout);
	}

	protected void grantPermission() {
		// TODO grant WRITE_SECURE_SETTINGS permission (root)
		if (checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
			SysUtils.grantPermission(this,
					android.Manifest.permission.WRITE_SECURE_SETTINGS);
		}
	}
}
