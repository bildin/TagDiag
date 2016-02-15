package eu.dedb.nfc.tagdiag;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
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

		Intent intent = getIntent();
		logList = intent.getStringArrayListExtra(NfcDiagService.EXTRA_LOG);

		ListAdapter log = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, logList);
		setListAdapter(log);
	}
}
