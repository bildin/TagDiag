package eu.dedb.nfc.tagdiag;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import eu.dedb.utils.SysUtils;

public class SettingsActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings_activity_layout);

		CheckBox permission = (CheckBox) findViewById(R.id.permission);
		permission
				.setChecked(checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED);
		permission.setOnCheckedChangeListener(togglePermission);
	}

	OnCheckedChangeListener togglePermission = new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			if (isChecked) {
				if (checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED)
					SysUtils.grantPermission(getBaseContext(),
							android.Manifest.permission.WRITE_SECURE_SETTINGS);
			} else {
				if (checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED)
					SysUtils.revokePermission(getBaseContext(),
							android.Manifest.permission.WRITE_SECURE_SETTINGS);
			}
		}

	};
}
