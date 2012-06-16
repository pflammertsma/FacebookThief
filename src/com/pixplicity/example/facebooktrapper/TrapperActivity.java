package com.pixplicity.example.facebooktrapper;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ToggleButton;

public class TrapperActivity extends Activity {

	protected static final int MAX_ITEMS = 200;

	private Thread mThread;

	private ToggleButton mBtn;
	private ListView mList;

	private ArrayAdapter<String> mListAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mBtn = (ToggleButton) findViewById(R.id.toggleButton1);
		mBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mBtn.isChecked()) {
					if (mThread != null) {
						mThread.interrupt();
					}
					mThread = new TrapperThread(TrapperActivity.this) {
						@Override
						public void run() {
							showNotification(0, "Facebook Trapper running",
									null);
							doInBackground(mList, mListAdapter);
							showNotification(-1, "Facebook Trapper stopped",
									null);
							mBtn.post(new Runnable() {
								@Override
								public void run() {
									mBtn.setChecked(false);
								}
							});
						};
					};
					mThread.start();
				} else if (mThread != null) {
					mThread.interrupt();
				}
			}
		});
		mListAdapter = new ArrayAdapter<String>(this, R.layout.list_item);
		mList = (ListView) findViewById(R.id.listView1);
		mList.setAdapter(mListAdapter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mThread != null && mThread.isAlive()) {
			mBtn.setChecked(true);
		} else {
			mBtn.setChecked(false);
		}
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		if (mThread != null && mThread.isAlive()) {
			mBtn.setChecked(true);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mThread != null) {
			mThread.interrupt();
		}
	}

}