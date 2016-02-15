package eu.dedb.nfc.tagdiag;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Toast;

public class AppChooser extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#7F000000")));
		Intent intent = getIntent().getParcelableExtra(Intent.EXTRA_INTENT);
		if (intent != null) {
			Intent chooser = new Intent(Intent.ACTION_PICK_ACTIVITY);
			chooser.putExtra(Intent.EXTRA_INTENT, intent);
			startActivityForResult(chooser, 0);
		}
		Toast msg = Toast.makeText(this, "Choose App to diag", Toast.LENGTH_SHORT);
		msg.setGravity(Gravity.CENTER, 0, 0);
		msg.show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		if (intent != null)
			startActivity(intent);
		finish();
	}
}
