package eu.dedb.nfc.tagdiag;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ServiceStarter extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = new Intent(this, NfcDiagService.class);
		startService(intent);
		finish();
	}
}
