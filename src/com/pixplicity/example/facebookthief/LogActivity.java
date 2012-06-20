package com.pixplicity.example.facebookthief;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ToggleButton;

public class LogActivity extends Activity {

	protected static final int MAX_ITEMS = 5;

	protected static final String TAG = "LogTrapper";

	private ToggleButton mBtn;
	private ListView mList;

	private ArrayAdapter<TrapperResult> mListAdapter;

	private ProgressDialog mProgress;
	private ResponseReceiver mReceiver;

	private boolean mServiceAlive;

	private class ResponseReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Response from service:
			LogActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (mProgress != null) {
						mProgress.dismiss();
						mProgress = null;
					}
				}
			});
			mServiceAlive = true;
			if (intent.getAction().equals(
					LogService.SERVICE_RESPONSE)) {
				if (intent.getBooleanExtra("alive", true)) {
					// Clear the list as the service is sending fresh results
					mListAdapter.clear();
				} else {
					mServiceAlive = false;
				}
			} else if (intent.getAction().equals(
					LogService.TRAPPER_RESPONSE)) {
				final TrapperResult response = (TrapperResult) intent
						.getSerializableExtra("result");
				synchronized (mListAdapter) {
					// Add the item
					mListAdapter.add(response);
					// Make sure the adapter doesn't contain too many items
					for (int i = 0; i < mListAdapter.getCount() - MAX_ITEMS; i++) {
						Log.d(TAG, "remove index 0; i=" + i);
						mListAdapter.remove(mListAdapter.getItem(0));
					}
				}
				mList.post(new Runnable() {
					@Override
					public void run() {
						mListAdapter.notifyDataSetChanged();
					}
				});
			}
			mBtn.post(new Runnable() {
				@Override
				public void run() {
					mBtn.setChecked(mServiceAlive);
				}
			});
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mBtn = (ToggleButton) findViewById(R.id.toggleButton1);
		mBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent serviceIntent = new Intent(LogActivity.this,
						LogService.class);
				if (mBtn.isChecked()) {
					mProgress = new ProgressDialog(
							LogActivity.this);
					mProgress.setMessage("Starting service...");
					mProgress.show();
					mProgress.setOnCancelListener(new OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							stopService(serviceIntent);
						}
					});
					startService(serviceIntent);
				} else {
					stopService(serviceIntent);
				}
			}
		});
		mListAdapter = new LogListAdapter(this);
		mList = (ListView) findViewById(R.id.listView1);
		mList.setAdapter(mListAdapter);
		mList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				TrapperResult result = mListAdapter.getItem(position);
				Intent intent = result.getIntent();
				if (intent != null) {
					startActivity(intent);
				}
			}
		});

	}

	@Override
	protected void onResume() {
		super.onResume();
		// Register the response receiver to listen to service broadcasts
		if (mReceiver == null) {
			mReceiver = new ResponseReceiver();
		}
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(LogService.TRAPPER_RESPONSE);
		intentFilter.addAction(LogService.SERVICE_RESPONSE);
		registerReceiver(mReceiver, intentFilter);
		mBtn.setChecked(false);
		// Query the service
		sendBroadcast(new Intent(LogService.SERVICE_QUERY));
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Stop receiving service broadcasts
		if (mReceiver != null) {
			unregisterReceiver(mReceiver);
		}
		if (mProgress != null) {
			mProgress.dismiss();
			mProgress = null;
		}
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}

	protected ListView getList() {
		return mList;
	}

}