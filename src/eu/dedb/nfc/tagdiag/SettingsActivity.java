package eu.dedb.nfc.tagdiag;

import android.app.Activity;
import android.os.Bundle;
import eu.dedb.utils.SysUtils;

public class SettingsActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.settings_activity_layout);
	}

	protected void grantPermission() {
		// try to get WRITE_SECURE_SETTINGS permission (needs root)
		if (!SysUtils.hasPermission(this,
				android.Manifest.permission.WRITE_SECURE_SETTINGS)) {
			SysUtils.grantPermission(this,
					android.Manifest.permission.WRITE_SECURE_SETTINGS);
		}
	}
}
