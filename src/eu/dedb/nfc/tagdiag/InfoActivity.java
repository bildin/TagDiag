package eu.dedb.nfc.tagdiag;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class InfoActivity extends Activity {

	CharSequence fwData, hwData, sysData, appData;
	TextView info;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.info_activity_layout);

		findViewById(R.id.fw).setOnClickListener(mOnClickListener);
		findViewById(R.id.hw).setOnClickListener(mOnClickListener);
		findViewById(R.id.sys).setOnClickListener(mOnClickListener);
		findViewById(R.id.app).setOnClickListener(mOnClickListener);
		info = (TextView) findViewById(R.id.info);
		info.setMovementMethod(new ScrollingMovementMethod());

		Intent intent = getIntent();

		fwData = intent.getCharSequenceExtra(NfcDiagService.EXTRA_INFO_FW);
		hwData = intent.getCharSequenceExtra(NfcDiagService.EXTRA_INFO_HW);
		sysData = intent.getCharSequenceExtra(NfcDiagService.EXTRA_INFO_SYS);
		appData = intent.getCharSequenceExtra(NfcDiagService.EXTRA_INFO_APP);

		info.setText(fwData);
	}

	OnClickListener mOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.fw:
				info.setText(fwData);
				break;
			case R.id.hw:
				info.setText(hwData);
				break;
			case R.id.sys:
				info.setText(sysData);
				break;
			case R.id.app:
				info.setText(appData);
				break;
			default:
				info.setText("");
				break;
			}
			info.scrollTo(0, 0);
		}
	};
}
