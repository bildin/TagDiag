package eu.dedb.nfc.tagdiag;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

public class LogActivity extends ListActivity {
	CharSequence statData, tagData, actData, allData;
	TextView info;
	ArrayList<String> logList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.log_activity_layout);

		findViewById(R.id.share).setOnClickListener(mOnClickListener);
		findViewById(R.id.reload_log).setOnClickListener(mOnClickListener);

		Intent intent = getIntent();
		logList = intent.getStringArrayListExtra(NfcDiagService.EXTRA_LOG);

		ListAdapter log = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, logList);
		setListAdapter(log);
	}

	OnClickListener mOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.share:
				sendBroadcast(new Intent(NfcDiagService.BROADCAST_SHARE));
				break;
			case R.id.reload_log:
				sendBroadcast(new Intent(NfcDiagService.BROADCAST_VIEW_LOG));
				break;
			default:
				break;
			}
		}
	};
}
