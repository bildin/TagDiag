package eu.dedb.nfc.tagdiag;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Toast;

public class ServiceStarter extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = new Intent(this, NfcDiagService.class);
		startService(intent);
		Toast msg = Toast.makeText(this, "Move TagDiag Navigation Icon", Toast.LENGTH_LONG);
		msg.setGravity(Gravity.CENTER, 0, 0);
		msg.show();
		finish();
	}
}
