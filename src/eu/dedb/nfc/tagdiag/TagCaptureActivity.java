package eu.dedb.nfc.tagdiag;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Toast;

public class TagCaptureActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.getWindow().setBackgroundDrawable(
				new ColorDrawable(Color.parseColor("#7F000000")));

	}

	@Override
	public void onResume() {
		super.onResume();
		NfcAdapter nfc = NfcAdapter.getDefaultAdapter(this);
		Toast msg = Toast.makeText(this, "Tap Tag to diag", Toast.LENGTH_SHORT);
		msg.setGravity(Gravity.CENTER, 0, 0);
		msg.show();
		if (nfc == null) {
			msg.setText("No NFC Adapter");
			finish();
		} else {
			nfc.enableForegroundDispatch(this, PendingIntent.getBroadcast(this,
					1, new Intent(NfcDiagService.BROADCAST_TAG_DISCOVERED), 0),
					new IntentFilter[] { new IntentFilter(
							NfcAdapter.ACTION_TAG_DISCOVERED) }, null);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		try {
			NfcAdapter.getDefaultAdapter(this).disableForegroundDispatch(this);
		} catch (Exception e) {
		}
	}
}
